package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalComment
import com.example.data.LocalProblem
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemDetailsScreen(
    problemId: Int,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allProblems by viewModel.problems.collectAsState()
    val problem = remember(allProblems, problemId) {
        allProblems.find { it.id == problemId }
    }

    val comments by viewModel.getCommentsForProblem(problemId).collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }

    if (problem == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Yuklanmoqda...", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.navigateTo(Screen.DiscoverMap) }) {
                    Text(text = "Orqaga qaytish")
                }
            }
        }
        return
    }

    val dateFormatted = remember(problem.createdAt) {
        val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(problem.createdAt))
    }

    val categoryColor = when (problem.categoryId) {
        "road" -> Color(0xFFE57373)
        "trash" -> Color(0xFF81C784)
        "traffic_light" -> Color(0xFFFFD54F)
        "water" -> Color(0xFF64B5F6)
        "electricity" -> Color(0xFFFFB74D)
        else -> MaterialTheme.colorScheme.secondary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Batafsil ma'lumot", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.ProblemsList) }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    // RBAC Feature: Allow Admin/Moderator to Delete or Manage reports!
                    if (viewModel.currentUser.role == "ADMIN") {
                        IconButton(
                            onClick = { viewModel.adminDeleteProblem(problem) },
                            modifier = Modifier.testTag("admin_delete_problem_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(text = "Fikr-mulohaza yoki yechim...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input_field"),
                        maxLines = 2,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    IconButton(
                        onClick = {
                            viewModel.addComment(problemId, commentText)
                            commentText = ""
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("comment_submit_btn"),
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Filled.Send, contentDescription = "Send comment", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        modifier = modifier
    ) { padValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category info card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (problem.categoryId) {
                                    "road" -> Icons.Filled.Build
                                    "trash" -> Icons.Filled.Delete
                                    "traffic_light" -> Icons.Filled.PlayArrow
                                    "water" -> Icons.Filled.Warning
                                    "electricity" -> Icons.Filled.Star
                                    else -> Icons.Filled.Info
                                },
                                contentDescription = problem.categoryName,
                                modifier = Modifier.size(18.dp),
                                tint = categoryColor
                            )
                        }

                        Column {
                            Text(text = problem.categoryName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "Sana: $dateFormatted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }

                    // Votes
                    Button(
                        onClick = { viewModel.toggleVote(problem.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (problem.hasVoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (problem.hasVoted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("detail_vote_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.ThumbUp, contentDescription = "Upvote", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "${problem.votesCount} ta ovoz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Title and Description
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = problem.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = problem.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    )
                }
            }

            // Status timeline progression panel
            item {
                StatusProgressTimeline(activeStatus = problem.status)
            }

            // AI Metadata info tags
            item {
                AnimatedVisibility(visible = problem.aiLabels.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.List, contentDescription = "Teglar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Column {
                                Text(text = "Tizim AI yorliqlari:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = problem.aiLabels, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            // Location coordinates card
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Place icon", tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(text = "Mahalla / Manzil:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = problem.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Admin panel workflow controls inside the detail sheet (if Admin)
            if (viewModel.currentUser.role == "ADMIN") {
                item {
                    AdminIncidentControls(
                        problem = problem,
                        onUpdateStatus = { newStatus ->
                            viewModel.adminUpdateStatus(problem.id, newStatus)
                        }
                    )
                }
            }

            // Comments Header
            item {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.AccountBox, contentDescription = "Comments tag", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Muxokamalar (${comments.size} ta fikr)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // List of Comments
            if (comments.isEmpty()) {
                item {
                    Text(
                        text = "Hozircha hech qanday sharhlar yo'q. Birinchi bo'lib o'z fikringizni bildiring!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(comments, key = { it.id }) { comment ->
                    CommentBubble(comment = comment)
                }
            }
        }
    }
}

@Composable
fun CommentBubble(comment: LocalComment, modifier: Modifier = Modifier) {
    val dateText = remember(comment.timestamp) {
        val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(comment.timestamp))
    }

    val bubbleColor = when (comment.authorRole) {
        "ADMIN", "MODERATOR" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    val tagColor = when (comment.authorRole) {
        "ADMIN" -> Color(0xFF4CAF50)
        "MODERATOR" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val roleUz = when (comment.authorRole) {
        "ADMIN" -> "Obodonlashtirish Xizmati"
        "MODERATOR" -> "Tizim AI"
        else -> "Fuqaro"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bubbleColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = comment.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .background(tagColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = roleUz,
                            color = tagColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = dateText,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.content,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun StatusProgressTimeline(activeStatus: String, modifier: Modifier = Modifier) {
    val states = listOf("NEW", "ACCEPTED", "IN_PROGRESS", "RESOLVED")
    val stateNames = listOf("Yuborilgan", "Qabul qilindi", "Jarayonda", "Hal etildi")

    val activeIndex = states.indexOf(activeStatus).coerceAtLeast(0)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Muammo holat rivojlanishi:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                states.forEachIndexed { idx, state ->
                    val isDone = idx <= activeIndex
                    val nodeColor = when {
                        isDone && state == "RESOLVED" -> Color(0xFF4CAF50)
                        isDone -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(nodeColor.copy(alpha = 0.2f), CircleShape)
                                .border(2.dp, nodeColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Done",
                                    modifier = Modifier.size(12.dp),
                                    tint = nodeColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stateNames[idx],
                            fontSize = 9.sp,
                            fontWeight = if (idx == activeIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == activeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminIncidentControls(
    problem: LocalProblem,
    onUpdateStatus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Mod panel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Moderator boshqaruv knopkasi", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Ushbu xabarning statusini hokimiyat yoki obodonlashtirish nomidan o'zgartiring:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("NEW", "Yangi"),
                    Pair("ACCEPTED", "Qabul qilish"),
                    Pair("IN_PROGRESS", "Jarayon"),
                    Pair("RESOLVED", "Hal qilish")
                ).forEach { state ->
                    val isCurrent = problem.status == state.first
                    ElevatedButton(
                        onClick = { onUpdateStatus(state.first) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface,
                            contentColor = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("admin_set_status_${state.first}"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(text = state.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
