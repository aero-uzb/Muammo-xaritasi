package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalProblem
import com.example.ui.components.UzbekistanInteractiveMap
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverMapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val filteredList by viewModel.filteredProblems.collectAsState()
    val allProblems by viewModel.problems.collectAsState()

    // Interactive category definition
    val categories = listOf(
        CategoryFilterItem("all", "Barchasi", Icons.Filled.List, MaterialTheme.colorScheme.primary),
        CategoryFilterItem("road", "Yo'llar", Icons.Filled.Build, Color(0xFFE57373)),
        CategoryFilterItem("trash", "Chiqindi", Icons.Filled.Delete, Color(0xFF81C784)),
        CategoryFilterItem("traffic_light", "Svetofor", Icons.Filled.PlayArrow, Color(0xFFFFD54F)),
        CategoryFilterItem("water", "Suv muammolari", Icons.Filled.Warning, Color(0xFF64B5F6)),
        CategoryFilterItem("electricity", "Elektr tizimi", Icons.Filled.Star, Color(0xFFFFB74D))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Muammo Xaritasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "O'zbekiston fuqarolik nazorati portali",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.UserProfile) },
                        modifier = Modifier.testTag("map_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profil",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.ReportIncident) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("fab_report_incident")
                    .padding(bottom = 16.dp),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Muammo qo'shish")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Yangi hisobot", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { padValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Leaflet OSM Web Map integration
            UzbekistanInteractiveMap(
                problems = filteredList,
                onProblemSelected = { id ->
                    viewModel.navigateTo(Screen.ProblemDetails(id))
                },
                onCoordinatePicked = { lat, lng ->
                    // coordinates selected directly via tap
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic filter bar on top of map
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = if (cat.id == "all") {
                            viewModel.selectedCategoryFilter == null
                        } else {
                            viewModel.selectedCategoryFilter == cat.id
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) cat.color else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .clickable {
                                    viewModel.selectedCategoryFilter = if (cat.id == "all") null else cat.id
                                }
                                .testTag("category_filter_${cat.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.name,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.White else cat.color
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Small Stats overlay
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ),
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.Start)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Jami muammolar: ${allProblems.size} ta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

data class CategoryFilterItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)
