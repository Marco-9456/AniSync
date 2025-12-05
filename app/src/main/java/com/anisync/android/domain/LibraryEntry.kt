package com.anisync.android.domain

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
    val status: LibraryStatus
)
