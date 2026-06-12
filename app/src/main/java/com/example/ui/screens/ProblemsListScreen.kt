package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalProblem
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemsListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val itemsList by viewModel.filteredProblems.collectAsState()
    var searchPhrase by remember { mutableStateOf("") }

    val filteredBySearch = remember(itemsList, searchPhrase) {
        if (searchPhrase.isBlank()) {
            itemsList
        } else {
            itemsList.filter {
                it.title.contains(searchPhrase, ignoreCase = true) ||
                        it.description.contains(searchPhrase, ignoreCase = true) ||
                        it.address.contains(searchPhrase, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Muammolar Ro'yxati", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.DiscoverMap) }) {
                        Icon(imageVector = Icons.Filled.Place, contentDescription = "Xaritaga o'tish", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        modifier = modifier
    ) { padValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input text field
            OutlinedTextField(
                value = searchPhrase,
                onValueChange = { searchPhrase = it },
                placeholder = { Text(text = "Mavzu yoki mahalla bo'yicha qidiruv...", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("problems_search_input"),
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchPhrase.isNotBlank()) {
                        IconButton(onClick = { searchPhrase = "" }) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            // Horizontal filters indicators summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Topilgan hisobotlar: ${filteredBySearch.size} ta",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                if (viewModel.selectedCategoryFilter != null) {
                    TextButton(onClick = { viewModel.selectedCategoryFilter = null }) {
                        Text(text = "Kategoriyani tozalash ×", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (filteredBySearch.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Empty state icon",
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Hech qanday muammo topilmadi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.PlatformPadding())
                        Text(
                            text = "Tanlangan kategoriya yoki qidiruv so'zi bo'yicha hech qanday fuqarolik xabarlari ro'yxatga olinmagan.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredBySearch, key = { it.id }) { item ->
                        ProblemCard(
                            problem = item,
                            onClick = { viewModel.navigateTo(Screen.ProblemDetails(item.id)) },
                            onVoteClick = { viewModel.toggleVote(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProblemCard(
    problem: LocalProblem,
    onClick: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = remember(problem.createdAt) {
        val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(problem.createdAt))
    }

    val statusColor = when (problem.status) {
        "NEW" -> Color(0xFFFF9800)
        "ACCEPTED" -> Color(0xFFFDD835)
        "IN_PROGRESS" -> Color(0xFF2196F3)
        "RESOLVED" -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

    val statusUz = when (problem.status) {
        "NEW" -> "Yangi"
        "ACCEPTED" -> "Qabul qilindi"
        "IN_PROGRESS" -> "Jarayonda"
        "RESOLVED" -> "Hal etildi"
        else -> problem.status
    }

    val categoryColor = when (problem.categoryId) {
        "road" -> Color(0xFFE57373)
        "trash" -> Color(0xFF81C784)
        "traffic_light" -> Color(0xFFFFD54F)
        "water" -> Color(0xFF64B5F6)
        "electricity" -> Color(0xFFFFB74D)
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("problem_card_${problem.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Simulated thumbnail based on category / index
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(categoryColor.copy(alpha = 0.2f)),
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
                    modifier = Modifier.size(34.dp),
                    tint = categoryColor
                )
            }

            // Text values details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusUz,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = problem.categoryName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = problem.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = problem.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sana: $dateText",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // Voting pill status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (problem.hasVoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onVoteClick() }
                            .testTag("vote_badge_btn_${problem.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ThumbUp,
                                contentDescription = "Ovoz berish",
                                modifier = Modifier.size(11.dp),
                                tint = if (problem.hasVoted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${problem.votesCount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (problem.hasVoted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Convenient extension function for dynamic spacer margin
@Composable
fun Modifier.PlatformPadding() = this.height(8.dp)
