package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.GeminiReportResult
import com.example.data.api.GeminiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface Screen {
    object DiscoverMap : Screen
    object ProblemsList : Screen
    object ReportIncident : Screen
    data class ProblemDetails(val problemId: Int) : Screen
    object AdminDashboard : Screen
    object UserProfile : Screen
}

enum class MapClickState { IDLE, PICKING }

data class ProfileUser(
    val name: String,
    val email: String,
    val phone: String,
    val role: String, // "USER" or "ADMIN"
    val avatarUrl: String
)

data class AdminLogEntry(
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MuammoDatabase.getDatabase(application)
    private val repository = ProblemRepository(database.problemDao())

    // UI navigation state
    var currentScreen by mutableStateOf<Screen>(Screen.DiscoverMap)
        private set

    // Switch screens safely
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    // Active User session (supports toggling Citizen vs Admin mode!)
    var currentUser by mutableStateOf(
        ProfileUser(
            name = "Sardor Ahmedov",
            email = "sardor.shox@gmail.com",
            phone = "+998 90 321 44 55",
            role = "USER",
            avatarUrl = ""
        )
    )
        private set

    fun switchUserRole(newRole: String) {
        currentUser = currentUser.copy(role = newRole)
        adminAddLog("SWITCH_ROLE", "User format altered to $newRole")
    }

    fun updateProfile(name: String, email: String, phone: String) {
        currentUser = currentUser.copy(name = name, email = email, phone = phone)
    }

    // Problems List state
    val problems: StateFlow<List<LocalProblem>> = repository.allProblems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getCommentsForProblem(problemId: Int): Flow<List<LocalComment>> =
        repository.getCommentsForProblem(problemId)

    // Filtering options
    var selectedCategoryFilter by mutableStateOf<String?>(null)
    var selectedStatusFilter by mutableStateOf<String?>(null)

    val filteredProblems: StateFlow<List<LocalProblem>> = combine(
        problems,
        snapshotFlow { selectedCategoryFilter },
        snapshotFlow { selectedStatusFilter }
    ) { list: List<LocalProblem>, cat: String?, stat: String? ->
        list.filter { item ->
            val matchesCat = cat == null || item.categoryId == cat
            val matchesStat = stat == null || item.status == stat
            matchesCat && matchesStat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active problem report writing states
    var reportTitle by mutableStateOf("")
    var reportDescription by mutableStateOf("")
    var reportCategory by mutableStateOf("road")
    var reportLat by mutableStateOf(41.311081)
    var reportLng by mutableStateOf(69.279737)
    var reportAddress by mutableStateOf("Amir Temur shoh ko'chasi, Toshkent")
    var reportImageIndex by mutableStateOf(0) // Pre-populated graphics/icons index (0 to 4)

    // Interactive map pick coordinates state
    var mapClickState by mutableStateOf(MapClickState.IDLE)

    // Gemini AI recommendation in creation form
    var aiAnalysisLoading by mutableStateOf(false)
    var aiAnalysisResult by mutableStateOf<GeminiReportResult?>(null)

    // Seeding trigger inside Constructor
    init {
        viewModelScope.launch {
            repository.seedMockDataIfEmpty()
        }
    }

    // Trigger AI report classification dynamically from report details
    fun triggerAiReportAnalysis() {
        if (reportTitle.isBlank() || reportDescription.isBlank()) return
        viewModelScope.launch {
            aiAnalysisLoading = true
            try {
                val analysis = GeminiService.analyzeReport(reportTitle, reportDescription)
                aiAnalysisResult = analysis
                // Pre-select category based on AI suggestion!
                reportCategory = analysis.suggestedCategory
            } catch (e: Exception) {
                // local fallback executed automatically in GeminiService
            } finally {
                aiAnalysisLoading = false
            }
        }
    }

    // Create report and insert into Room
    fun submitReport(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val catName = when (reportCategory) {
                "road" -> "Yo'l muammolari"
                "trash" -> "Chiqindi muammolari"
                "traffic_light" -> "Svetofor nosozligi"
                "water" -> "Suv muammolari"
                "electricity" -> "Elektr muammolari"
                else -> "Obodonlashtirish"
            }

            val finalLabels = aiAnalysisResult?.labels?.joinToString(", ") ?: "Siz yuborgan hisobot"

            val newReport = LocalProblem(
                title = reportTitle,
                description = reportDescription,
                categoryId = reportCategory,
                categoryName = catName,
                latitude = reportLat,
                longitude = reportLng,
                address = reportAddress,
                status = "NEW",
                votesCount = 0,
                hasVoted = false,
                imageUrl = "report_img_$reportImageIndex", // stored simulation identifier
                reporterName = currentUser.name,
                isAiApproved = aiAnalysisResult?.isSpamOrInappropriate != true,
                aiLabels = finalLabels
            )

            val pId = repository.insertProblem(newReport)

            // Auto-comment as Tizim Moderatori containing moderation results
            val moderationComment = aiAnalysisResult?.aiModerationComment 
                ?: "Muammo muvaffaqiyatli ro'yxatga olindi. Tizim avtomatik tahlili yakunlandi."

            repository.addComment(
                LocalComment(
                    problemId = pId.toInt(),
                    authorName = "Tizim AI Moderatori",
                    authorRole = "MODERATOR",
                    content = moderationComment
                )
            )

            // Reset reporting states
            reportTitle = ""
            reportDescription = ""
            reportCategory = "road"
            reportLat = 41.311081
            reportLng = 69.279737
            reportAddress = "Toshkent shahri"
            reportImageIndex = (reportImageIndex + 1) % 5
            aiAnalysisResult = null

            onSuccess()
        }
    }

    // Social actions: Upvote toggling
    fun toggleVote(problemId: Int) {
        viewModelScope.launch {
            repository.toggleVote(problemId)
        }
    }

    // Add user/admin feedback comments
    fun addComment(problemId: Int, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val comment = LocalComment(
                problemId = problemId,
                authorName = currentUser.name,
                authorRole = currentUser.role,
                content = content
            )
            repository.addComment(comment)
        }
    }

    // Admin commands
    fun adminUpdateStatus(problemId: Int, newStatus: String) {
        viewModelScope.launch {
            val list = problems.value
            val match = list.find { it.id == problemId }
            if (match != null) {
                val updated = match.copy(status = newStatus)
                repository.updateProblem(updated)
                adminAddLog("UPDATE_STATUS", "Report ID $problemId marked as $newStatus")

                // Automatically generate municipal comment outlining progress updates!
                val progressComment = when (newStatus) {
                    "ACCEPTED" -> "Tuman hokimligi: Muammo qabul qilindi. Tegishli mutaxassislar jalb etilmoqda."
                    "IN_PROGRESS" -> "Kommunal Maxsus Xizmati: Qurilish-montaj va obodonlashtirish jamoasi voqea joyida ish boshladi."
                    "RESOLVED" -> "Maxsus Xizmat: Nosozlik to'liq bartaraf etildi. Barqaror foydalanishga topshirildi."
                    else -> "Muammo holati statusi yangilandi: $newStatus"
                }

                repository.addComment(
                    LocalComment(
                        problemId = problemId,
                        authorName = "Tizim Operatori",
                        authorRole = "ADMIN",
                        content = progressComment
                    )
                )
            }
        }
    }

    fun adminDeleteProblem(problem: LocalProblem) {
        viewModelScope.launch {
            repository.deleteProblem(problem)
            adminAddLog("DELETE_PROBLEM", "Problem report matches ID ${problem.id} deleted successfully")
            navigateTo(Screen.ProblemsList)
        }
    }

    // Admin Logs and simulated stats
    private val _adminLogs = MutableStateFlow<List<AdminLogEntry>>(
        listOf(
            AdminLogEntry("DATABASE_INIT", "Database migrated and initial Uzbekistan demo entries populated successfully"),
            AdminLogEntry("SYSTEM_BOOT", "Application successfully booted in Uzbekistan Asian Region container")
        )
    )
    val adminLogs: StateFlow<List<AdminLogEntry>> = _adminLogs.asStateFlow()

    private fun adminAddLog(action: String, details: String) {
        val entry = AdminLogEntry(action, details)
        _adminLogs.update { listOf(entry) + it }
    }

    // Administrative User lists simulation
    val adminUsersList = listOf(
        ProfileUser("Sardor Ahmedov", "sardor.shox@gmail.com", "+998 90 321 44 55", "USER", ""),
        ProfileUser("Sanjar Alimov", "sanjar.ali@gov.uz", "+998 90 550 11 22", "ADMIN", ""),
        ProfileUrl("Laylo Karimova", "laylo.k@gov.uz", "+998 93 456 00 21", "ADMIN", ""),
        ProfileUser("Dilshod Bekov", "d.bekov@gmail.com", "+998 94 999 11 00", "USER", "")
    )
}

// Data holder classes for static compilation
fun ProfileUrl(name: String, email: String, phone: String, role: String, avatarUrl: String) =
    ProfileUser(name, email, phone, role, avatarUrl)
