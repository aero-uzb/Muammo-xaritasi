package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val problems by viewModel.problems.collectAsState()
    val adminLogs by viewModel.adminLogs.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Hisobotlar (Stats), 1: Tizim Jurnali (Logs), 2: Foydalanuvchilar (Users)

    val stats = remember(problems) {
        val total = problems.size
        val resolved = problems.count { it.status == "RESOLVED" }
        val inProgress = problems.count { it.status == "IN_PROGRESS" }
        val accepted = problems.count { it.status == "ACCEPTED" }
        val newCount = problems.count { it.status == "NEW" }
        
        val percentResolved = if (total > 0) (resolved * 100) / total else 0
        AdminStats(total, newCount, accepted, inProgress, resolved, percentResolved)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Admin Boshqaruv Paneli", fontWeight = FontWeight.Bold) },
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
    ) { padValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal navigation Tab controls
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text(text = "Statistika", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Filled.Info, contentDescription = "Stats icon", modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("admin_tab_stats")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text(text = "Tizim Jurnali", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Filled.List, contentDescription = "Logs", modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("admin_tab_logs")
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text(text = "Fuqarolar", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = "Users", modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("admin_tab_users")
                )
            }

            when (activeTab) {
                0 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Kommunal Tizim Statistikalari",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Large solve rate metrics
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Yechilgan Muammolar Foizi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${stats.solvedPercent}%", fontSize = 38.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = stats.solvedPercent / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Stat Grid List
                    val statItems = listOf(
                        StatEntry("Jami Hisobotlar", stats.total.toString(), Icons.Filled.Warning, MaterialTheme.colorScheme.primary),
                        StatEntry("Yangi Muammolar", stats.newCount.toString(), Icons.Filled.Info, Color(0xFFFF9800)),
                        StatEntry("Qabul qilindi", stats.accepted.toString(), Icons.Filled.Check, Color(0xFFFDD835)),
                        StatEntry("Jarayonda", stats.inProgress.toString(), Icons.Filled.Build, Color(0xFF2196F3)),
                        StatEntry("Hal etilgan", stats.resolved.toString(), Icons.Filled.CheckCircle, Color(0xFF4CAF50))
                    )

                    statItems.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { item ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Icon(imageVector = item.icon, contentDescription = item.title, tint = item.color, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = item.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = item.value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                1 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Tizim Jurnallari (Admin Logs)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(adminLogs) { log ->
                        val logTimeText = remember(log.timestamp) {
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            sdf.format(Date(log.timestamp))
                        }

                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.List, contentDescription = "Log", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = log.action, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(text = logTimeText, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                2 -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Ro'yxatga olingan fuqarolar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(viewModel.adminUsersList) { user ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Filled.Person, contentDescription = "User", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = user.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = user.role,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (user.role == "ADMIN") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Email: ${user.email} | Tel: ${user.phone}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class AdminStats(
    val total: Int,
    val newCount: Int,
    val accepted: Int,
    val inProgress: Int,
    val resolved: Int,
    val solvedPercent: Int
)

data class StatEntry(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)
