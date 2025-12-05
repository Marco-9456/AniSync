package com.anisync.android.domain

data class MediaDetails(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val bannerUrl: String?,
    val description: String,
    val score: Int?,
    val episodes: Int?,
    val status: String,
    val format: String?,
    val genres: List<String>,
    val studio: String?,
    val year: Int?,
    // User's list entry (null if not in user's list)
    val listEntryId: Int?,
    val listStatus: LibraryStatus?,
    val listProgress: Int?
)
