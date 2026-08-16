package com.anisync.android.data.ai

import com.anisync.android.domain.ai.AiGroundingSource
import com.anisync.android.domain.ai.AiUserNoteContext
import com.anisync.android.domain.ai.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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
        userNotes: List<AiUserNoteContext> = emptyList()
    ): GenerateResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Please enter your Google Gemini API key in Settings -> AI Assistant.")
        }

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val systemPrompt = buildSystemPrompt(allowSpoilers, userNotes)

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

    private fun buildSystemPrompt(allowSpoilers: Boolean, userNotes: List<AiUserNoteContext>): String {
        val sb = StringBuilder()
        sb.appendLine("You are the AniSync AI Assistant, a knowledgeable, friendly, and helpful anime and manga companion inside the AniSync app.")
        sb.appendLine("You assist users with anime/manga recommendations, character information, episode discussions, plot summaries, staff info, and analyzing their library.")
        sb.appendLine()

        if (allowSpoilers) {
            sb.appendLine("### SPOILER POLICY: SPOILERS ALLOWED")
            sb.appendLine("The user has explicitly turned ON spoilers. You are allowed to discuss plot twists, major events, character fates, and endings openly when asked.")
        } else {
            sb.appendLine("### SPOILER POLICY: STRICTLY NO SPOILERS")
            sb.appendLine("The user has turned OFF spoilers. DO NOT reveal major plot twists, character deaths, identity reveals, or climactic endings unless they specifically and explicitly ask about a finished season. If discussing upcoming plot, keep descriptions spoiler-free and add a polite warning if touching upon critical elements.")
        }
        sb.appendLine()

        if (userNotes.isNotEmpty()) {
            sb.appendLine("### USER'S PERSONAL ANIME/MANGA LIBRARY NOTES:")
            sb.appendLine("The user has enabled notes access. Here are their saved personal notes on anime/manga entries in their library. Use this context to personalize your recommendations and reference what they noted if relevant:")
            for (note in userNotes.take(40)) {
                sb.appendLine("- ${note.title} (${note.mediaType}) [Status: ${note.status}, Score: ${note.score ?: "N/A"}/100, Progress: ${note.progress}]: \"${note.note.replace("\n", " ")}\"")
            }
            sb.appendLine()
        }

        sb.appendLine("Format your responses clearly using Markdown (bolding, lists, code blocks, quote blocks) where appropriate for great readability on mobile devices.")
        return sb.toString()
    }
}
