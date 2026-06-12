package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.example.ui.components.UzbekistanInteractiveMap
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MapClickState
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showSuccessDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        ReportCat("road", "Yo'l muammolari", Icons.Filled.Build, Color(0xFFE57373)),
        ReportCat("trash", "Chiqindi muammolari", Icons.Filled.Delete, Color(0xFF81C784)),
        ReportCat("traffic_light", "Svetofor nosozligi", Icons.Filled.PlayArrow, Color(0xFFFFD54F)),
        ReportCat("water", "Suv muammolari", Icons.Filled.Warning, Color(0xFF64B5F6)),
        ReportCat("electricity", "Elektr muammolari", Icons.Filled.Star, Color(0xFFFFB74D))
    )

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.navigateTo(Screen.ProblemsList)
                    }
                ) {
                    Text(text = "Tushunarli")
                }
            },
            title = { Text(text = "Hisobot Qabul Qilindi", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Hisobotingiz muvaffaqiyatli saqlandi. Muammo avtomatik ravishda xaritaga tushirildi va tegishli davlat organlariga taqdim etildi. Rahmat bo'ling!",
                    lineHeight = 18.sp
                )
            },
            icon = { Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "OK icon", tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp)) }
        )
    }

    if (viewModel.mapClickState == MapClickState.PICKING) {
        // Overlay screen for Picking Map Coordinates directly!
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Koordinatani Tanlang", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.mapClickState = MapClickState.IDLE }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                )
            }
        ) { mapPads ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(mapPads)
            ) {
                UzbekistanInteractiveMap(
                    problems = emptyList(),
                    onProblemSelected = {},
                    onCoordinatePicked = { lat, lng ->
                        viewModel.reportLat = lat
                        viewModel.reportLng = lng
                        viewModel.reportAddress = "Toshkent shahri (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
                    },
                    pickingCoordinates = true,
                    pickLat = viewModel.reportLat,
                    pickLng = viewModel.reportLng,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Tanlangan Joy:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lat: ${String.format("%.6f", viewModel.reportLat)}  |  Lng: ${String.format("%.6f", viewModel.reportLng)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.mapClickState = MapClickState.IDLE },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Joylashuvni Tasdiqlash")
                        }
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Muammo haqida xabar berish", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateTo(Screen.DiscoverMap) }) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                )
            },
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Muammo Tafsilotlari",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Title TextInput
                OutlinedTextField(
                    value = viewModel.reportTitle,
                    onValueChange = { viewModel.reportTitle = it },
                    label = { Text(text = "Sarlavha (Kalam bilan kiriting...)") },
                    placeholder = { Text(text = "Masalan: Chuqur yo'l, Oqayotgan suv quvuri...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_title_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description TextInput
                OutlinedTextField(
                    value = viewModel.reportDescription,
                    onValueChange = { viewModel.reportDescription = it },
                    label = { Text(text = "Batafsil tavsif (Muammo tafsiloti)") },
                    placeholder = { Text(text = "Atrofingizdagi muammo haqida batafsil yozing, bu mutasaddilarga yordam beradi...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("report_desc_field"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time AI Categorization Trigger Panel!
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Face,
                                    contentDescription = "AI face logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sun'iy intellekt (AI Auto-Tahlil)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (viewModel.aiAnalysisLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Button(
                                    onClick = { viewModel.triggerAiReportAnalysis() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.height(28.dp).testTag("ai_analyse_btn"),
                                    enabled = viewModel.reportTitle.isNotBlank() && viewModel.reportDescription.isNotBlank()
                                ) {
                                    Text(text = "Tahlil", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }

                        // Display Gemini API results gracefully
                        viewModel.aiAnalysisResult?.let { result ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (result.isSpamOrInappropriate) "⚠️ Diqqat: Muammo spam deb baholandi." else "✅ AI Xulosasi: Muammo tasdiqlandi.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (result.isSpamOrInappropriate) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = result.aiModerationComment,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Show detected tags
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.labels.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } ?: run {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Matnlarni kiritgandan so'ng tahlil tugmasini bosing. AI avtomatik kategoriya, moderatsiya va teglarni tavsiya qiladi.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual Category selection overrides
                Text(
                    text = "Kategoriya",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = viewModel.reportCategory == cat.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) cat.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) cat.color else Color.Transparent
                            ),
                            modifier = Modifier
                                .clickable { viewModel.reportCategory = cat.id }
                                .testTag("select_cat_${cat.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.name,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) Color.White else cat.color
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.name.split(" ")[0], // short name
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // GPS and Location logging
                Text(
                    text = "GPS Joylashuv",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.mapClickState = MapClickState.PICKING
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("pick_coordinates_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Pick on map", tint = Color.White)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Koordinata tanlash",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = viewModel.reportAddress,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit report button
                Button(
                    onClick = {
                        viewModel.submitReport {
                            showSuccessDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_report_button"),
                    enabled = viewModel.reportTitle.isNotBlank() && viewModel.reportDescription.isNotBlank()
                ) {
                    Text(text = "Hisobotni Yuborish", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class ReportCat(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)
