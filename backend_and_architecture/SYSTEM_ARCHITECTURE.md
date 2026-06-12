# Muammo Xaritasi (Problem Map) - System Architecture & API

This document provides a comprehensive technical overview of the **Muammo Xaritasi** ecosystem, detailing the deployment schema, API contracts, folder structures, and interaction workflows for the Mobile App, Backend Service, and Admin Dashboard.

---

## 1. System Architecture Diagram

```
                       ┌─────────────────────────────────────┐
                       │          Client Interfaces          │
                       └──────────────────┬──────────────────┘
                                          │
                  ┌───────────────────────┼───────────────────────┐
                  ▼                       ▼                       ▼
         ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
         │   Flutter App   │     │   React Admin   │     │  Third-Party    │
         │  (Citizen App)  │     │   Dashboard     │     │  Municipal GIS  │
         └────────┬────────┘     └────────┬────────┘     └────────┬────────┘
                  │                       │                       │
                  └───────────────┬───────┴───────────────────────┘
                                  │ (HTTPS / JSON + JWT)
                                  ▼
                       ┌─────────────────────────────────────┐
                       │        NGINX Reverse Proxy          │
                       └──────────────────┬──────────────────┘
                                          │
                                          ▼
                       ┌─────────────────────────────────────┐
                       │        Express Backend API          │
                       │     (Node.js REST Services)         │
                       └───────────┬──────────────┬──────────┘
                                   │              │
         ┌─────────────────────────┼──────────────┼─────────────────────────┐
         ▼                         ▼              ▼                         ▼
┌──────────────┐            ┌─────────────┐┌──────────────┐          ┌─────────────┐
│ PostgreSQL   │            │ Cloudinary  ││ Gemini AI    │          │ Firebase    │
│ (Prisma ORM) │            │ (Media CDN) ││ (REST API)   │          │ (MCM Push)  │
│ [State, Auth,│            │ [Incident   ││ [Auto-Tag,   │          │ [Status     │
│  Reports, OS]│            │  Images KPI]││  Spam Moder.]│          │  Alerts]    │
└──────────────┘            └─────────────┘└──────────────┘          └─────────────┘
```

---

## 2. Technical Stack Specifications

### A. Mobile Application (Flutter)
- **Framework**: Flutter (Multi-platform Native Engine)
- **State Management**: Riverpod (Reactive dependency injection & cache management)
- **Navigation**: Go Router (Declarative, type-safe path-based navigation)
- **Mapping & Geolocation**: OpenStreetMap tile servers integration via `flutter_map` (Leaflet equivalent) & `geolocator` for highly precise coordinate capture
- **Asynchronous IO**: `dio` with custom interceptors for transparent JWT refresh tokens and local caching fallback

### B. Admin Dashboard (React)
- **Libraries**: React (Vite bundler), TypeScript for compile-time type-safety
- **Styling & Components**: Tailwind CSS combined with highly responsive Material 3 layout principles
- **Data Synchronizer**: React Query (`@tanstack/react-query`) for robust local caching, automatic background revalidation, and request batching
- **Data Visuals**: Recharts for incident reports trends, density heatmaps, and resolution cycle intervals

### C. Backend Engine (Node.js)
- **Environment**: Express.js REST application
- **Persistence Layer**: Prisma ORM over PostgreSQL 15, structured for foreign-key integrity and indexing on geographic coordinates (PostGIS ready)
- **Auth Engine**: JSON Web Tokens (JWT) using asymmetric cryptokeys, paired with Argon2 for database password hashing
- **Security Protocols**: Helmet middleware, strict CORS boundaries, express-rate-limit protection, and parameterized SQL queries generated natively by Prisma to avoid injections.

---

## 3. Environment Variables (.env)

| Key | Example Value | Description |
| :--- | :--- | :--- |
| `DATABASE_URL` | `postgresql://user:pass@localhost:5432/muammo_db` | Postgres connection config url |
| `PORT` | `3000` | Local hosting port for REST app |
| `JWT_SECRET` | `6256c70b6be57b8552194565611f8b1b...` | Server-side JWT private key |
| `CLOUDINARY_CLOUD_NAME` | `muammo_cloud` | CDN storage profile identification |
| `CLOUDINARY_API_KEY` | `123456789012345` | API Key for media processing |
| `CLOUDINARY_API_SECRET` | `K8p1...` | Private credential for secure upload signature |
| `GEMINI_API_KEY` | `AIzaSy...` | Generative AI integration credential |

---

## 4. REST API Documentation

### A. Authentication Module
- **`POST /auth/register`** -> Registers a new user.
  - **Body**: `{ "name": "John Doe", "email": "john@gmail.com", "password": "securePass123", "phone": "+998901234567" }`
  - **Returns**: `{ "user": { "id": "...", "name": "..." }, "token": "JWT_STRING" }`
- **`POST /auth/login`** -> Logins an existing user.
  - **Body**: `{ "email": "john@gmail.com", "password": "securePass123" }`
  - **Returns**: `{ "user": { "id": "...", "role": "USER" }, "token": "JWT_STRING" }`
- **`GET /users/profile`** -> Fetch current user profile details.
  - **Headers**: `Authorization: Bearer <TOKEN>`
  - **Returns**: `{ "id": "...", "name": "...", "email": "...", "avatar": "...", "role": "USER" }`

### B. Incident Reporting Module (Problems)
- **`GET /problems`** -> Returns list of incidents. Supports filters (`status`, `categoryId`, `bounds`).
  - **Query Docs**: `?status=NEW&categoryId=road_damage_id`
  - **Returns**: `[ { "id": "...", "title": "Large Pothole", "latitude": 41.31, "status": "PENDING", "votesCount": 12, "category": { "name": "Road Damage" } } ]`
- **`GET /problems/:id`** -> Detailed view of a single report including comments.
  - **Returns**: `{ "id": "...", "title": "Large Pothole", "description": "...", "comments": [ { "id": "...", "content": "Confirmed", "user": { "name": "Ali" } } ] }`
- **`POST /problems`** -> Reports a new infrastructure problem.
  - **Headers**: Authenticated `Bearer <TOKEN>`
  - **Body (Multipart-form)**: `{ "title": "Broken pipe", "description": "Water flowing in street...", "latitude": 41.311, "longitude": 69.241, "categoryId": "...", "image": [File] }`
  - *Note*: Backend automatically triggers **Gemini AI Vision** to double check if the uploaded image matches the selected category, checks for duplicate alerts within a 50-meter radius, analyzes duplicate reports, and flags spam/inappropriate pictures.
- **`PUT /problems/:id`** -> Updates an reported incident. (Allowed only for owner or moderators).
- **`DELETE /problems/:id`** -> Deletes a report (Moderator/Admin override only).

### C. Community Traction & Social Interactions
- **`POST /votes`** -> Toggles an upvote for an infrastructure issue.
  - **Body**: `{ "problemId": "UUID_STRING" }`
  - **Returns**: `{ "votesCount": 13, "hasVoted": true }`
- **`POST /comments`** -> Post feedback or status check on a report.
  - **Body**: `{ "problemId": "...", "content": "Maintenance crew arrived!", "parentId": null }`
  - **Returns**: `{ "id": "...", "content": "...", "user": { "name": "John Doe" } }`

### D. Administrative and Moderation Services
- **`GET /admin/users`** -> Returns user records (Paginated, filters for role).
- **`GET /admin/problems`** -> Fetch unresolved or controversial flagged incidents.
- **`PUT /admin/problem/status`** -> Shift status representation dynamically.
  - **Body**: `{ "problemId": "...", "status": "IN_PROGRESS" | "RESOLVED" }`
  - *Process*: System automatically sends a Firebase notification to reporter & voters.
- **`DELETE /admin/problem`** -> Removes an incident if it violates conditions or contains junk spam.

---

## 5. Clean Directories Folder Structure

### Backend Workspace Structure (Node/TypeScript/Prisma)
```
muammo_backend/
├── prisma/
│   └── schema.prisma           # Complete database model structures
├── src/
│   ├── config/
│   │   ├── db.ts               # Prisma Client initialization instance
│   │   └── gemini.ts           # Google Gemini generative client config
│   ├── controllers/
│   │   ├── auth.controller.ts  # Token issue, encryption routines
│   │   ├── admin.controller.ts # User bans, status overrides, stats metrics
│   │   └── report.controller.ts# Geolocation reports creation & file upload
│   ├── middleware/
│   │   ├── auth.verify.ts      # Claims analysis, RBAC policies
│   │   └── gemini.ai.ts        # Intelligent auto-moderator pipeline
│   ├── routes/
│   │   ├── auth.routes.ts      # Authentication mapping
│   │   ├── reports.routes.ts   # Reporting & map fetches
│   │   └── admin.routes.ts     # Admin capabilities endpoints
│   ├── services/
│   │   ├── cloudinary.ts       # Secure attachment CDN handling
│   │   └── fcm.service.ts      # Push notifications scheduler
│   └── server.ts               # Main HTTP Express setup
├── Dockerfile                  # Base container config
├── docker-compose.yml          # Complex setup for server database
└── tsconfig.json               # Type compiler constraints
```

### Flutter Citizen App Structure
```
muammo_flutter/
├── android/ ...                # Native Gradle project settings
├── ios/ ...                    # Native Cocoa pods configs
├── lib/
│   ├── core/
│   │   ├── network/            # HTTP Clients (Dio settings + Interceptors)
│   │   ├── theme/              # Material 3 Color definitions & Typography
│   │   └── utils/              # Locations, permission manager helpers
│   ├── features/
│   │   ├── auth/               # Logins & Profile states
│   │   │   ├── data/           # Repository implementations, API calls
│   │   │   ├── domain/         # Users entity interfaces
│   │   │   └── presentation/   # Jetpack-style flutter layout screens
│   │   ├── reporting/          # Interactive map & GPS logging
│   │   │   ├── data/           # Incident models, image payload endpoints
│   │   │   └── presentation/   # Map overlays, camera forms, category chooser
│   │   └── admin_panel/        # Administrative statistics screens
│   ├── main.dart               # Riverpod App bootstrapping point
│   └── routes.dart             # Declarative GoRouter tree paths
└── pubspec.yaml                # Libraries catalog
```

### React-TS Administrator Console
```
muammo_admin_react/
├── public/                     # Static elements & favicon indicators
├── src/
│   ├── api/                    # Axios instances + Tanstack hooks
│   ├── components/             # Reusable elements (Sidebar, StatCards, Maps)
│   ├── context/                # Authentication wrappers
│   ├── pages/                  # Layout Views
│   │   ├── Dashboard.tsx       # Chart widgets and incidents counts
│   │   ├── Moderation.tsx      # Spam flags queue, comments filters
│   │   └── UserManagement.tsx  # User list & admin promotions
│   ├── styles/                 # Tailwind utility structures
│   ├── App.tsx                 # Core UI entry
│   └── main.tsx                # Bootstrap React runtime
├── package.json
└── tailwind.config.js          # Responsive colors
```
