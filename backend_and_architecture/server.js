require('dotenv').config();
const express = require('express');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const { PrismaClient } = require('@prisma/client');
const { GoogleGenAI } = require('@google/generative-ai');

// Initialize Prisma and Express
const prisma = new PrismaClient();
const app = express();
const port = process.env.PORT || 3000;

app.use(express.json());
app.use(cors());

// Simple Rate Limiting to prevent brute-force attacks
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: { error: 'Too many requests from this IP, please try again after 15 minutes.' }
});
app.use('/auth/', limiter);

// Helper for Mock File uploads (Multer fallback in memory)
const upload = multer({ storage: multer.memoryStorage() });

// JWT Secret Key verify
const JWT_SECRET = process.env.JWT_SECRET || 'fallback_secret_for_muammo_xaritasi';

// Auth Middleware
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) return res.status(401).json({ error: 'Access token required' });

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.status(403).json({ error: 'Invalid or expired token' });
    req.user = user;
    next();
  });
};

// Admin Protection Middleware
const requireAdmin = (req, res, next) => {
  if (req.user && (req.user.role === 'ADMIN' || req.user.role === 'MODERATOR')) {
    next();
  } else {
    res.status(403).json({ error: 'Administrative privileges required' });
  }
};

// Google Gemini API automatic categorization & moderation helper
async function runGeminiAnalysis(imageUrl, promptInstruction) {
  if (!process.env.GEMINI_API_KEY) {
    console.log("No Gemini API Key found. Skipping automatic AI check.");
    return null;
  }
  try {
    const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
    const model = ai.getGenerativeModel({ model: 'gemini-3.5-flash' });
    const content = `Analyze this reported city infrastructure issue. Instructions: ${promptInstruction}. Image URL is: ${imageUrl}`;
    const result = await model.generateContent(content);
    return result.response?.text || null;
  } catch (error) {
    console.error("Gemini analysis error:", error);
    return null;
  }
}

// ======================== AUTH ENDPOINTS ========================

// Register route
app.post('/auth/register', async (req, res) => {
  try {
    const { name, email, password, phone } = req.body;
    if (!name || !email || !password) {
      return res.status(400).json({ error: 'Name, email and password are required' });
    }

    // Hash user password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Save user to Postgres DB via Prisma
    const user = await prisma.user.create({
      data: {
        name,
        email,
        password: hashedPassword,
        phone,
        role: 'USER'
      }
    });

    const token = jwt.sign({ id: user.id, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '7d' });
    res.status(201).json({ user: { id: user.id, name: user.name, email: user.email, role: user.role }, token });
  } catch (err) {
    if (err.code === 'P2002') return res.status(400).json({ error: 'Email or phone already registered' });
    res.status(500).json({ error: 'Registration failed: ' + err.message });
  }
});

// Login route
app.post('/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) return res.status(400).json({ error: 'Email and password are required' });

    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return res.status(401).json({ error: 'Invalid email or password' });

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(401).json({ error: 'Invalid email or password' });

    const token = jwt.sign({ id: user.id, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '7d' });
    res.json({ user: { id: user.id, name: user.name, email: user.email, role: user.role }, token });
  } catch (err) {
    res.status(500).json({ error: 'Login failed: ' + err.message });
  }
});

// User profile route
app.get('/users/profile', authenticateToken, async (req, res) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.user.id },
      select: { id: true, name: true, email: true, phone: true, avatar: true, role: true, createdAt: true }
    });
    res.json(user);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ======================== PROBLEMS ENDPOINTS ========================

// List problems with coordinates and status filtering
app.get('/problems', async (req, res) => {
  try {
    const { status, categoryId } = req.query;
    const filter = {};
    if (status) filter.status = status;
    if (categoryId) filter.categoryId = categoryId;

    const problems = await prisma.problem.findMany({
      where: filter,
      include: {
        category: true,
        user: { select: { name: true, avatar: true } },
        attachments: true
      },
      orderBy: { createdAt: 'desc' }
    });
    res.json(problems);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Get single problem details
app.get('/problems/:id', async (req, res) => {
  try {
    const problem = await prisma.problem.findUnique({
      where: { id: req.params.id },
      include: {
        category: true,
        user: { select: { name: true, email: true, phone: true } },
        attachments: true,
        comments: {
          include: {
            user: { select: { name: true, avatar: true, role: true } }
          },
          orderBy: { createdAt: 'asc' }
        }
      }
    });

    if (!problem) return res.status(404).json({ error: 'Report not found' });
    res.json(problem);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// File/photo report submission
app.post('/problems', authenticateToken, upload.single('image'), async (req, res) => {
  try {
    const { title, description, latitude, longitude, address, categoryId } = req.body;

    if (!title || !description || !latitude || !longitude || !categoryId) {
      return res.status(400).json({ error: 'Missing reporting coordinates, description or category.' });
    }

    // Real Cloudinary image upload simulation
    let imageUrl = 'https://res.cloudinary.com/muammo-cdn/raw/pothole_placeholder.jpg';
    if (req.file) {
      // In production, upload buffer to Cloudinary SDK:
      // const uploadResult = await cloudinary.uploader.upload_stream(req.file.buffer)...
      imageUrl = `https://res.cloudinary.com/muammo-cdn/image/upload/${Date.now()}_muammo.jpg`;
    }

    // Build the report
    const problem = await prisma.problem.create({
      data: {
        title,
        description,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        address,
        userId: req.user.id,
        categoryId,
        status: 'NEW',
        attachments: {
          create: [
            { url: imageUrl }
          ]
        }
      },
      include: {
        attachments: true,
        category: true
      }
    });

    // Run AI content checks in background
    runGeminiAnalysis(imageUrl, `Verify if the photo represents category ${problem.category?.name || 'Municipal issue'}. Detect potential spam, duplicate issues or vandalism content.`)
      .then(aiReport => {
        if (aiReport) {
          console.log(`[AI Auto-Mod Log for Problem ${problem.id}]: ${aiReport}`);
          // In production, update report flag status based on AI assessment
        }
      });

    res.status(201).json(problem);
  } catch (err) {
    res.status(500).json({ error: 'Failed to file report: ' + err.message });
  }
});

// Toggle Voting action
app.post('/votes', authenticateToken, async (req, res) => {
  try {
    const { problemId } = req.body;
    if (!problemId) return res.status(400).json({ error: 'problemId is required' });

    // Check if user has already voted
    const existingVote = await prisma.vote.findUnique({
      where: {
        userId_problemId: {
          userId: req.user.id,
          problemId
        }
      }
    });

    if (existingVote) {
      // Retract/cancel vote (toggle mechanism)
      await prisma.vote.delete({ where: { id: existingVote.id } });
      const updatedProblem = await prisma.problem.update({
        where: { id: problemId },
        data: { votesCount: { decrement: 1 } }
      });
      return res.json({ hasVoted: false, votesCount: updatedProblem.votesCount });
    } else {
      // Save vote record
      await prisma.vote.create({
        data: { userId: req.user.id, problemId }
      });
      const updatedProblem = await prisma.problem.update({
        where: { id: problemId },
        data: { votesCount: { increment: 1 } }
      });
      return res.json({ hasVoted: true, votesCount: updatedProblem.votesCount });
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Cast Comments
app.post('/comments', authenticateToken, async (req, res) => {
  try {
    const { problemId, content, parentId } = req.body;
    if (!problemId || !content) return res.status(400).json({ error: 'problemId and content are required' });

    const comment = await prisma.comment.create({
      data: {
        content,
        problemId,
        userId: req.user.id,
        parentId
      },
      include: {
        user: { select: { name: true, avatar: true } }
      }
    });
    res.status(201).json(comment);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ======================== ADMINISTRATION ENDPOINTS ========================

// Fetch admin statistics dashboard stats
app.get('/admin/statistics', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const totalReports = await prisma.problem.count();
    const resolvedCount = await prisma.problem.count({ where: { status: 'RESOLVED' } });
    const inProgressCount = await prisma.problem.count({ where: { status: 'IN_PROGRESS' } });
    const acceptedCount = await prisma.problem.count({ where: { status: 'ACCEPTED' } });
    const newCount = await prisma.problem.count({ where: { status: 'NEW' } });
    
    const categoryBreakdown = await prisma.problem.groupBy({
      by: ['categoryId'],
      _count: { id: true }
    });

    res.json({
      summary: {
        totalReports,
        new: newCount,
        accepted: acceptedCount,
        inProgress: inProgressCount,
        resolved: resolvedCount
      },
      categoryCounts: categoryBreakdown
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Fetch reports inside admin console
app.get('/admin/problems', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const problems = await prisma.problem.findMany({
      include: {
        category: true,
        user: { select: { name: true, email: true } }
      },
      orderBy: { createdAt: 'desc' }
    });
    res.json(problems);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Fetch users inside admin console
app.get('/admin/users', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const users = await prisma.user.findMany({
      select: { id: true, name: true, email: true, phone: true, role: true, createdAt: true },
      orderBy: { createdAt: 'desc' }
    });
    res.json(users);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Update critical problem status workflow
app.put('/admin/problem/status', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const { problemId, status } = req.body;
    if (!problemId || !status) return res.status(400).json({ error: 'problemId and status are required' });

    const problem = await prisma.problem.update({
      where: { id: problemId },
      data: { status },
      include: { user: true }
    });

    // Log the administrative action
    await prisma.adminLog.create({
      data: {
        adminId: req.user.id,
        action: 'CHANGE_STATUS',
        details: `Updated report ${problemId} to state ${status}`
      }
    });

    // Simulated Firebase push notification call (FCM integration)
    console.log(`[FCM Notification dispatched to reporter ${problem.user.name}]: Your reported issue "${problem.title}" is now marked as "${status}".`);

    res.json(problem);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Delete problem
app.delete('/admin/problem/:id', authenticateToken, requireAdmin, async (req, res) => {
  try {
    await prisma.problem.delete({ where: { id: req.params.id } });
    
    await prisma.adminLog.create({
      data: {
        adminId: req.user.id,
        action: 'DELETE_REPORT',
        details: `Deleted problem report ID: ${req.params.id}`
      }
    });

    res.json({ message: 'Problem report successfully removed' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Start listening
app.listen(port, () => {
  console.log(`Muammo Xaritasi platform system running securely on port ${port}`);
});
