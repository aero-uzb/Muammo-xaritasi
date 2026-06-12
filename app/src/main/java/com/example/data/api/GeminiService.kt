package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiReportResult(
    val suggestedCategory: String, // "road", "trash", "traffic_light", "water", "electricity"
    val isSpamOrInappropriate: Boolean,
    val aiModerationComment: String,
    val labels: List<String>
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val reportResultAdapter = moshi.adapter(GeminiReportResult::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Calls Gemini 3.5 Flash to automatically detect the category, inspect spam/vandalism/bad language,
     * and generate high-fidelity subtags.
     */
    suspend fun analyzeReport(title: String, description: String): GeminiReportResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // If API key is placeholder or empty, fallback gracefully to a smart semantic parser
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is missing or default. Falling back to local semantic logic.")
            return@withContext runLocalSemanticAnalysis(title, description)
        }

        // Prompt that strictly demands a JSON output representing our object schema
        val systemPrompt = """
            Siz "Muammo Xaritasi" (Muammolar xaritasi) shahar boshqaruvi tizimining AI modelisiz.
            Sizga kelgan muammo xabarini (Sarlavha va Tavsif) tahlil qilishingiz zarur.
            Natijani faqatgina quyidagi JSON formatida qaytaring, boshqa hech qanday izoh qo'shmang:
            {
              "suggestedCategory": "road" yoki "trash" yoki "traffic_light" yoki "water" yoki "electricity",
              "isSpamOrInappropriate": true/false (spam, behayo so'zlar, bema'ni xat bo'lsa true qaytaring),
              "aiModerationComment": "Sarlavha va tasnif bo'yicha qisqacha o'zbek tilida AI xulosasi va tahlili (maksimal 2 ta gaplar)",
              "labels": ["yorliq1", "yorliq2"] (ushbu muammoga tegishli 3 ta kalit so'z yoki yorliqlar)
            }
        """.trimIndent()

        val userPrompt = """
            Sarlavha: "$title"
            Muammo tavsifi: "$description"
        """.trimIndent()

        try {
            // Build direct HTTP POST Request
            val requestBodyJson = JSONObject().apply {
                val contentsArray = org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply { put("text", userPrompt) })
                        })
                    })
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                }
                put("systemInstruction", systemInstructionObj)

                val generationConfigObj = JSONObject().apply {
                    put("temperature", 0.4)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfigObj)
            }

            val requestBodyString = requestBodyJson.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyString.toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API execution failed with HTTP code: ${response.code}")
                return@withContext runLocalSemanticAnalysis(title, description)
            }

            val bodyString = response.body?.string() ?: ""
            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textOutput = parts.getJSONObject(0).getString("text").trim()

            val result = reportResultAdapter.fromJson(textOutput)
            result ?: runLocalSemanticAnalysis(title, description)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API, applying safe local fallback: ${e.message}", e)
            runLocalSemanticAnalysis(title, description)
        }
    }

    /**
     * Dynamic keyword analysis fallback that processes strings semantically if internet is offline or API keys are missing.
     * Ensures high-fidelity, reactive feedback!
     */
    private fun runLocalSemanticAnalysis(title: String, description: String): GeminiReportResult {
        val combined = "$title $description".lowercase()
        
        val category = when {
            combined.contains("yo'l") || combined.contains("chuqur") || combined.contains("asfalt") || combined.contains("yo’l") || combined.contains("yol") -> "road"
            combined.contains("chiqind") || combined.contains("musor") || combined.contains("axlat") || combined.contains("bozor") || combined.contains("sassiq") -> "trash"
            combined.contains("svetofor") || combined.contains("traffic") || combined.contains("belgi") -> "traffic_light"
            combined.contains("suv") || combined.contains("truba") || combined.contains("quvur") || combined.contains("jo'mrak") || combined.contains("potop") -> "water"
            combined.contains("elektr") || combined.contains("tok") || combined.contains("sim") || combined.contains("kabel") || combined.contains("chiroq") || combined.contains("svet") -> "electricity"
            else -> "road" // Default suggested
        }

        val labels = when (category) {
            "road" -> listOf("Yo'l ta'miri", "Chuqurlik", "Avtotransport")
            "trash" -> listOf("Sanitariya holati", "Chiqindi", "Ekologiya")
            "traffic_light" -> listOf("Yo'l harakati xavfsizligi", "Svetofor", "Chorraha")
            "water" -> listOf("Kommunal soha", "Suv sizishi", "Truba")
            "electricity" -> listOf("Elektr uzatish", "Havfsizlik", "Yuqori kuchlanish")
            else -> listOf("Obodonlashtirish", "Mahalla")
        }

        val defaultComments = when (category) {
            "road" -> "Tizim sarlavha va chuqurlik so'zlarini aniqladi. Yo'l asfalt qoplamasi shikastlangani taxmin qilinmoqda."
            "trash" -> "Mahalliy maishiy yoki noqonuniy chiqindilar to'planishining semantik aloqasi tasdiqlandi."
            "traffic_light" -> "Yo'l harakatini tartibga solish vositalari yoki svetoforlardagi shikastlanish belgilari aniqlandi."
            "water" -> "Ichimlik ko'chasida ichimlik yoki oqova quvur yorilgani taxmin qilindi."
            "electricity" -> "Xavfli elektr simlari yoki podstansiya aloqasi tahlili bajarildi."
            else -> "Xabar mazmuni obodonlashtirish kategoriyasiga muvofiq."
        }

        val isSpam = combined.contains("reklama") || combined.contains("kazin") || combined.contains("sotiladi") || combined.length < 10

        val moderationResult = if (isSpam) {
            "Tizim xabarda spam yoki juda qisqa tavsif topdi. Iltimos qayta tekshiring."
        } else {
            "$defaultComments [Lokal tahlil ishlatildi]"
        }

        return GeminiReportResult(
            suggestedCategory = category,
            isSpamOrInappropriate = isSpam,
            aiModerationComment = moderationResult,
            labels = labels
        )
    }
}
