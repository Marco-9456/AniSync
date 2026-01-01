package com.anisync.android.domain

import androidx.compose.runtime.Immutable
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType
import kotlinx.serialization.Serializable

enum class LibraryStatus {
    CURRENT,
    PLANNING,
    COMPLETED,
    DROPPED,
    PAUSED,
    REPEATING,
    UNKNOWN
}

@Immutable
@Serializable
data class LibraryEntry(
    val id: Int,
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val progress: Int,
    val totalEpisodes: Int?,
    val totalChapters: Int?,
    val totalVolumes: Int?,
    val type: MediaType?,
    val format: MediaFormat? = null,
    val status: LibraryStatus,
    val nextAiringEpisode: Int? = null,
    val timeUntilAiring: Int? = null,
    val mediaStatus: String? = null,
    val averageScore: Int? = null,
    val score: Double? = 0.0,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val rewatches: Int = 0,
    val notes: String? = null,
    val updatedAt: Long? = null,
    val createdAt: Long? = null,
    val mediaStartDate: Long? = null
)