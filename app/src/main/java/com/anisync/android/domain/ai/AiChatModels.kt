package com.anisync.android.domain.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiGroundingSource(
    val title: String,
    val url: String
)

@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<AiGroundingSource> = emptyList(),
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

data class AiUserDataEntry(
    val titleUserPreferred: String,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val mediaType: String,
    val status: String,
    val progress: Int,
    val totalEpisodesOrChapters: Int?,
    val score: Double?,
    val notes: String?,
    val startedAt: Long?,
    val completedAt: Long?
)

data class AiMediaFocusContext(
    val mediaId: Int,
    val title: String,
    val description: String?,
    val genres: List<String>,
    val format: String?,
    val status: String?,
    val averageScore: Int?,
    val episodes: Int?,
    val studio: String?
)

@Serializable
data class AiChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mediaId: Int? = null,
    val mediaTitle: String? = null,
    val messages: List<ChatMessage> = emptyList()
)

@Serializable
data class AiNewsItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val category: String = "NEWS",
    val timeAgo: String = "Recent",
    val sources: List<AiGroundingSource> = emptyList()
)
