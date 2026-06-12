package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.with
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContentContainer(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainContentContainer(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        bottomBar = {
            // Display Bottom Navigation Bar on top-level screens: DiscoverMap, ProblemsList, ReportIncident, UserProfile
            val current = viewModel.currentScreen
            if (current is Screen.DiscoverMap || current is Screen.ProblemsList || current is Screen.ReportIncident || current is Screen.UserProfile) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    modifier = Modifier
                        .testTag("app_bottom_bar")
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = current is Screen.DiscoverMap,
                        onClick = { viewModel.navigateTo(Screen.DiscoverMap) },
                        icon = { Icon(imageVector = Icons.Filled.Place, contentDescription = "Xarita") },
                        label = { Text(text = "Xarita", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_btn_map")
                    )

                    NavigationBarItem(
                        selected = current is Screen.ProblemsList,
                        onClick = { viewModel.navigateTo(Screen.ProblemsList) },
                        icon = { Icon(imageVector = Icons.Filled.List, contentDescription = "Muammolar") },
                        label = { Text(text = "Ro'yxat", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_btn_list")
                    )

                    NavigationBarItem(
                        selected = current is Screen.ReportIncident,
                        onClick = { viewModel.navigateTo(Screen.ReportIncident) },
                        icon = { Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "Yangi hisobot") },
                        label = { Text(text = "Yangi", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_btn_report")
                    )

                    NavigationBarItem(
                        selected = current is Screen.UserProfile,
                        onClick = { viewModel.navigateTo(Screen.UserProfile) },
                        icon = { Icon(imageVector = Icons.Filled.AccountCircle, contentDescription = "Profil") },
                        label = { Text(text = "Profil", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_btn_profile")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // Direct View switcher powered by Kotlin sealed structure Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (viewModel.currentScreen is Screen.ProblemDetails || viewModel.currentScreen is Screen.AdminDashboard) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            when (val screen = viewModel.currentScreen) {
                is Screen.DiscoverMap -> DiscoverMapScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                is Screen.ProblemsList -> ProblemsListScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                is Screen.ReportIncident -> ReportIncidentScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                is Screen.ProblemDetails -> ProblemDetailsScreen(
                    problemId = screen.problemId,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                is Screen.AdminDashboard -> AdminDashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                is Screen.UserProfile -> UserProfileScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
