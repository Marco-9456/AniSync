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

data class AiUserNoteContext(
    val title: String,
    val mediaType: String,
    val status: String,
    val score: Double?,
    val progress: Int,
    val note: String
)
