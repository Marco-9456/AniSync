package com.anisync.android.domain

import com.anisync.android.type.MediaType

enum class LibraryStatus {
    CURRENT,
    PLANNING,
    COMPLETED,
    DROPPED,
    PAUSED,
    REPEATING,
    UNKNOWN
}

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
    val status: LibraryStatus,
    val nextAiringEpisode: Int? = null,
    val timeUntilAiring: Int? = null,
    val mediaStatus: String? = null
)