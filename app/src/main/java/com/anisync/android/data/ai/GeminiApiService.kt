package com.anisync.android.data.ai

import com.anisync.android.domain.ai.AiGroundingSource
import com.anisync.android.domain.ai.AiMediaFocusContext
import com.anisync.android.domain.ai.AiUserDataEntry
import com.anisync.android.domain.ai.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApiService @Inject constructor(
    private val baseOkHttpClient: OkHttpClient
) {
    private val client: OkHttpClient by lazy {
        baseOkHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    data class GenerateResult(
        val text: String,
        val sources: List<AiGroundingSource> = emptyList()
    )

    suspend fun generateChatResponse(
        apiKey: String,
        modelName: String = "gemini-2.5-flash",
        conversationHistory: List<ChatMessage>,
        latestUserMessage: String,
        useWebSearch: Boolean = true,
        allowSpoilers: Boolean = false,
        userData: List<AiUserDataEntry> = emptyList(),
        mediaFocus: AiMediaFocusContext? = null
    ): GenerateResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Please enter your Google Gemini API key in Settings -> AI Assistant.")
        }

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val systemPrompt = buildSystemPrompt(allowSpoilers, userData, mediaFocus)

        val requestJson = buildJsonObject {
            // System instructions
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", systemPrompt) })
                }
            }

            // Chat contents
            putJsonArray("contents") {
                // Add conversation history (up to last 12 messages for context)
                val relevantHistory = conversationHistory.takeLast(12)
                for (msg in relevantHistory) {
                    if (msg.isError || msg.text.isBlank()) continue
                    add(buildJsonObject {
                        put("role", if (msg.isUser) "user" else "model")
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", msg.text) })
                        }
                    })
                }

                // Add current message
                add(buildJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", latestUserMessage) })
                    }
                })
            }

            // Google Grounding / Web search tool
            if (useWebSearch) {
                putJsonArray("tools") {
                    add(buildJsonObject {
                        putJsonObject("googleSearch") {}
                    })
                }
            }
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response from Gemini API")

        if (!response.isSuccessful) {
            val errorMsg = runCatching {
                val element = json.parseToJsonElement(responseBody).jsonObject
                element["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            }.getOrNull() ?: "Gemini API error (HTTP ${response.code})"
            throw IOException(errorMsg)
        }

        parseGeminiResponse(responseBody)
    }

    private fun parseGeminiResponse(responseBody: String): GenerateResult {
        val root = json.parseToJsonElement(responseBody).jsonObject
        val candidates = root["candidates"]?.jsonArray
        if (candidates.isNullOrEmpty()) {
            throw IOException("No response generated by Gemini model.")
        }

        val firstCandidate = candidates[0].jsonObject
        val content = firstCandidate["content"]?.jsonObject
        val parts = content?.get("parts")?.jsonArray

        val textBuilder = StringBuilder()
        parts?.forEach { part ->
            part.jsonObject["text"]?.jsonPrimitive?.contentOrNull?.let {
                textBuilder.append(it)
            }
        }

        val sources = mutableListOf<AiGroundingSource>()
        val groundingMetadata = firstCandidate["groundingMetadata"]?.jsonObject
        val groundingChunks = groundingMetadata?.get("groundingChunks")?.jsonArray

        groundingChunks?.forEach { chunk ->
            val web = chunk.jsonObject["web"]?.jsonObject
            val title = web?.get("title")?.jsonPrimitive?.contentOrNull
            val uri = web?.get("uri")?.jsonPrimitive?.contentOrNull
            if (!uri.isNullOrBlank()) {
                sources.add(AiGroundingSource(title = title ?: uri, url = uri))
            }
        }

        return GenerateResult(
            text = textBuilder.toString().ifBlank { "I was unable to generate a response." },
            sources = sources.distinctBy { it.url }
        )
    }

    private fun buildSystemPrompt(
        allowSpoilers: Boolean,
        userData: List<AiUserDataEntry>,
        mediaFocus: AiMediaFocusContext?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("You are the AniSync AI Assistant, a knowledgeable, passionate, and helpful anime & manga companion inside the AniSync Android app.")
        sb.appendLine("You assist users with recommendations, character breakdowns, lore discussions, plot analysis, airing schedules, and reviewing their anime/manga lists.")
        sb.appendLine("You have broad encyclopedic knowledge of AniList anime/manga stats, characters, staff, and release details.")
        sb.appendLine()

        if (mediaFocus != null) {
            sb.appendLine("### CURRENTLY FOCUSED ANIME / MANGA CONTEXT (Opened from details page):")
            sb.appendLine("Title: ${mediaFocus.title}")
            mediaFocus.format?.let { sb.appendLine("Format: $it") }
            mediaFocus.status?.let { sb.appendLine("Status: $it") }
            mediaFocus.averageScore?.let { sb.appendLine("Average AniList Score: $it/100") }
            mediaFocus.episodes?.let { sb.appendLine("Episodes: $it") }
            if (mediaFocus.genres.isNotEmpty()) {
                sb.appendLine("Genres: ${mediaFocus.genres.joinToString(", ")}")
            }
            mediaFocus.studio?.let { sb.appendLine("Studio / Producers: $it") }
            mediaFocus.description?.let {
                sb.appendLine("Synopsis: ${it.take(1500)}")
            }
            sb.appendLine("Prioritize this specific title in your answers when relevant.")
            sb.appendLine()
        }

        if (allowSpoilers) {
            sb.appendLine("### SPOILER POLICY: SPOILERS ALLOWED")
            sb.appendLine("The user has enabled spoilers. You may openly discuss major plot twists, character fates, and endings.")
        } else {
            sb.appendLine("### SPOILER POLICY: STRICTLY NO SPOILERS")
            sb.appendLine("The user has disabled spoilers. Keep discussions spoiler-free and do NOT reveal major twists, deaths, or secret identity reveals without a clear warning.")
        }
        sb.appendLine()

        if (userData.isNotEmpty()) {
            sb.appendLine("### USER'S PERSONAL ANILIST LIBRARY DATA (User Data toggle is ON):")
            sb.appendLine("You have access to the user's personal watch/read library, including their scores, progress, personal notes, and dates.")
            sb.appendLine("Always check this list when the user asks about their notes, score, opinions, or list progress on any anime/manga (e.g. Domestic Girlfriend, Frieren, etc.):")
            for (entry in userData) {
                val titlePart = buildString {
                    append(entry.titleUserPreferred)
                    val altTitles = listOfNotNull(entry.titleRomaji, entry.titleEnglish, entry.titleNative)
                        .filter { it != entry.titleUserPreferred }
                        .distinct()
                    if (altTitles.isNotEmpty()) {
                        append(" (aka ${altTitles.joinToString(" / ")})")
                    }
                }
                val scorePart = entry.score?.let { "$it/100" } ?: "Not rated"
                val notePart = if (!entry.notes.isNullOrBlank()) " | Notes: \"${entry.notes.replace("\n", " ")}\"" else ""
                val totalPart = entry.totalEpisodesOrChapters?.let { "/$it" } ?: ""
                sb.appendLine("• [$titlePart] Type: ${entry.mediaType} | Status: ${entry.status} | Progress: ${entry.progress}$totalPart | User Score: $scorePart$notePart")
            }
            sb.appendLine()
        } else {
            sb.appendLine("### USER DATA POLICY: OFF")
            sb.appendLine("The user data toggle is currently OFF. You do not have access to their personal notes or private library records, but you can freely discuss general AniList facts, synopsis, and public stats.")
            sb.appendLine()
        }

        sb.appendLine("Format your response clearly using rich Markdown (bolding, bullet points, headers, quotes) formatted for mobile screens.")
        return sb.toString()
    }

    suspend fun fetchNewsRadar(
        apiKey: String,
        modelName: String = "gemini-2.5-flash",
        topic: String = "All"
    ): List<com.anisync.android.domain.ai.AiNewsItem> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Please configure your Gemini API key in Settings > Gemini AI Assistant.")
        }

        val prompt = buildString {
            appendLine("You are the AI Anime News Radar for AniSync.")
            appendLine("Fetch the latest, most exciting real-time anime news, trailer drops, release dates, voice actor / studio announcements, and major industry updates.")
            if (topic != "All") {
                appendLine("Focus specifically on topic: $topic.")
            }
            appendLine("Format your response as a list of 5-8 news items formatted strictly as JSON array with this schema:")
            appendLine("""[{"title": "Headline", "summary": "2-3 sentence summary of the news and what fans should know", "category": "TRAILER|RELEASE|ANNOUNCEMENT|INDUSTRY", "timeAgo": "e.g. 2h ago or Today"}]""")
            appendLine("Output ONLY the valid JSON array and nothing else.")
        }

        val effectiveModel = modelName.ifBlank { "gemini-2.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent?key=$apiKey"

        val requestJson = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                    }
                })
            }
            putJsonObject("tools") {
                putJsonArray("google_search") {}
            }
        }

        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: throw IOException("Empty response from Gemini API")

        if (!response.isSuccessful) {
            val errorMsg = runCatching {
                val jsonEl = json.parseToJsonElement(bodyString).jsonObject
                jsonEl["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            }.getOrNull() ?: "API Error (${response.code})"
            throw IOException(errorMsg)
        }

        val root = json.parseToJsonElement(bodyString).jsonObject
        val candidate = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
        val rawText = candidate?.get("content")?.jsonObject?.get("parts")?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()

        val sources = mutableListOf<AiGroundingSource>()
        val groundingMetadata = candidate?.get("groundingMetadata")?.jsonObject
        val searchChunks = groundingMetadata?.get("groundingChunks")?.jsonArray
        searchChunks?.forEach { chunkEl ->
            val web = chunkEl.jsonObject["web"]?.jsonObject
            val webTitle = web?.get("title")?.jsonPrimitive?.contentOrNull
            val webUri = web?.get("uri")?.jsonPrimitive?.contentOrNull
            if (!webUri.isNullOrBlank()) {
                sources.add(AiGroundingSource(title = webTitle ?: "Source", url = webUri))
            }
        }

        val jsonStartIndex = rawText.indexOf('[')
        val jsonEndIndex = rawText.lastIndexOf(']')
        if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex) {
            val jsonArrayStr = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
            val parsed = runCatching {
                val array = json.parseToJsonElement(jsonArrayStr).jsonArray
                array.map { el ->
                    val obj = el.jsonObject
                    com.anisync.android.domain.ai.AiNewsItem(
                        title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Anime Update",
                        summary = obj["summary"]?.jsonPrimitive?.contentOrNull ?: "",
                        category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "NEWS",
                        timeAgo = obj["timeAgo"]?.jsonPrimitive?.contentOrNull ?: "Recent",
                        sources = sources
                    )
                }
            }.getOrDefault(emptyList())
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        listOf(
            com.anisync.android.domain.ai.AiNewsItem(
                title = "Latest Anime Radar",
                summary = rawText.take(500),
                category = "NEWS",
                timeAgo = "Recent",
                sources = sources
            )
        )
    }
}
