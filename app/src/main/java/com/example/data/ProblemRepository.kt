package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProblemRepository(private val problemDao: ProblemDao) {

    val allProblems: Flow<List<LocalProblem>> = problemDao.getAllProblemsFlow()

    fun getProblemById(id: Int): Flow<LocalProblem?> = problemDao.getProblemById(id)

    fun getCommentsForProblem(problemId: Int): Flow<List<LocalComment>> =
        problemDao.getCommentsForProblem(problemId)

    suspend fun insertProblem(problem: LocalProblem): Long = withContext(Dispatchers.IO) {
        problemDao.insertProblem(problem)
    }

    suspend fun updateProblem(problem: LocalProblem) = withContext(Dispatchers.IO) {
        problemDao.updateProblem(problem)
    }

    suspend fun deleteProblem(problem: LocalProblem) = withContext(Dispatchers.IO) {
        problemDao.deleteProblem(problem)
    }

    suspend fun addComment(comment: LocalComment) = withContext(Dispatchers.IO) {
        problemDao.insertComment(comment)
    }

    // Toggle voting logic safely
    suspend fun toggleVote(problemId: Int): Boolean = withContext(Dispatchers.IO) {
        val problem = problemDao.getProblemById(problemId).first() ?: return@withContext false
        val changedVotedState = !problem.hasVoted
        val newVotesCount = if (changedVotedState) problem.votesCount + 1 else (problem.votesCount - 1).coerceAtLeast(0)
        
        problemDao.updateProblem(
            problem.copy(
                hasVoted = changedVotedState,
                votesCount = newVotesCount
            )
        )
        changedVotedState
    }

    // Check if empty and seed initial data for live Uzbek engagement demo
    suspend fun seedMockDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = problemDao.getTotalProblemsCount()
        if (count == 0) {
            val initialProblems = listOf(
                LocalProblem(
                    title = "Katta o'yiqlar (Amir Temur ko'chasi)",
                    description = "Metro yaqinidagi yo'lda katta o'yiq paydo bo'lgan. Mashinalar chetlab o'tishga majbur bo'lib, xavfli avariya vaziyatlarini yuzaga keltirmoqda.",
                    categoryId = "road",
                    categoryName = "Yo'l muammolari",
                    latitude = 41.311081,
                    longitude = 69.279737,
                    address = "Amir Temur shoh ko'chasi, Toshkent",
                    status = "NEW",
                    votesCount = 28,
                    hasVoted = false,
                    reporterName = "Dilshod To'rayev",
                    isAiApproved = true,
                    aiLabels = "Yo'l qoplamasi shikastlangan, Chuqurlik, Toshkent"
                ),
                LocalProblem(
                    title = "Noqonuniy chiqindixona (Mirobod bozor orqasi)",
                    description = "Mirobod bozori yonidagi ko'p qavatli uylar orqasida noqonuniy chiqindixona paydo bo'lgan. Katta antisanitariya va hid butun mahallaga tarqalmoqda. Mutasaddilardan amaliy yordam kutib qolamiz.",
                    categoryId = "trash",
                    categoryName = "Chiqindi muammolari",
                    latitude = 41.298910,
                    longitude = 69.271010,
                    address = "Mirobod ko'chasi 45, Toshkent",
                    status = "IN_PROGRESS",
                    votesCount = 42,
                    hasVoted = true,
                    reporterName = "Nigora Umarova",
                    isAiApproved = true,
                    aiLabels = "Maishiy chiqindilar, Antisanitariya, Ekologiya"
                ),
                LocalProblem(
                    title = "Svetofor ishlamayapti (Chilonzor chorraha)",
                    description = "Bunyodkor tarmoq chorrahasidagi svetofor 2 kundan beri ishlamayapti. Katta tirbandliklar va piyodalar xavfsizligi buzilmoqda. Ko'plab avariya holatlari yuzaga keldi.",
                    categoryId = "traffic_light",
                    categoryName = "Svetofor nosozligi",
                    latitude = 41.282920,
                    longitude = 69.211420,
                    address = "Bunyodkor ko'chasi cross, Toshkent",
                    status = "ACCEPTED",
                    votesCount = 15,
                    hasVoted = false,
                    reporterName = "Sherzod Mirzayev",
                    isAiApproved = true,
                    aiLabels = "Yo'l harakati, Nosoz svetofor, Chilonzor"
                ),
                LocalProblem(
                    title = "Suv tarmog'i yorilishi (Samarqand shahri)",
                    description = "Registonga olib boruvchi ko'chada ichimlik suvi quvuri yorilib, ko'chani suv bosmoqda. 10 soatdan beri ichimlik suvi behuda oqmoqda. Samarqand SUV ta'minoti vakillari kelishini so'raymiz.",
                    categoryId = "water",
                    categoryName = "Suv muammolari",
                    latitude = 39.654710,
                    longitude = 66.959720,
                    address = "Registon ko'chasi, Samarqand",
                    status = "RESOLVED",
                    votesCount = 106,
                    hasVoted = false,
                    reporterName = "Ahmad Aliyev",
                    isAiApproved = true,
                    aiLabels = "Suv isrofi, Quvur avariyasi, Samarqand"
                ),
                LocalProblem(
                    title = "Kabel uzilib osilib qolgan xavf",
                    description = "Buxoro shahrining G'ijduvon ko'chasida yuqori kuchlanishli sim yog'och ustundan uzilib, yo'lak ustiga osilib qolgan. Bolalar o'ynaydigan joy. Juda xavfli!",
                    categoryId = "electricity",
                    categoryName = "Elektr muammolari",
                    latitude = 39.775820,
                    longitude = 64.417210,
                    address = "G'ijduvon ko'chasi, Buxoro",
                    status = "NEW",
                    votesCount = 38,
                    hasVoted = false,
                    reporterName = "Zilola Ergashova",
                    isAiApproved = true,
                    aiLabels = "Elektr simlari, Havfsizlik, Yuqori kuchlanish"
                )
            )

            // Insert seeded reports
            for (p in initialProblems) {
                val pId = problemDao.insertProblem(p)
                // Add initial comments
                problemDao.insertComment(
                    LocalComment(
                        problemId = pId.toInt(),
                        authorName = "Tizim Moderatori",
                        authorRole = "MODERATOR",
                        content = "Muammo muvaffaqiyatli ro'yxatga olindi. Ma'lumotlaringiz tegishli tuman hokimligi va maxsus obodonlashtirish xizmatlariga yo'naltirildi."
                    )
                )

                if (p.status == "IN_PROGRESS" || p.status == "RESOLVED") {
                    problemDao.insertComment(
                        LocalComment(
                            problemId = pId.toInt(),
                            authorName = "Obodonlashtirish Xizmati",
                            authorRole = "ADMIN",
                            content = "Ushbu hudud bo'yicha maxsus brigada safarbar qilindi. Tez orada bartaraf etish muddatlari ma'lum qilinadi."
                        )
                    )
                }

                if (p.status == "RESOLVED") {
                    problemDao.insertComment(
                        LocalComment(
                            problemId = pId.toInt(),
                            authorName = "Obodonlashtirish Xizmati",
                            authorRole = "ADMIN",
                            content = "Nosozliklar to'liq bartaraf etildi. Barcha ta'mirlash ishlari muvaffaqiyatli yakunlandi. Fuqarolarga faollik uchun rahmat!"
                        )
                    )
                }
            }
        }
    }
}
