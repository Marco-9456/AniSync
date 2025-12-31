package com.anisync.android.domain

import kotlinx.serialization.Serializable

data class UserProfile(
    val id: Int,
    val name: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val about: String?,
    val activeAt: Long?,
    val animeCount: Int,
    val daysWatched: Float, // Converted from minutes
    val mangaCount: Int,
    val chaptersRead: Int,
    val meanScore: Float,
    val animeStatusCounts: AnimeStatusCounts,
    val favoriteAnime: List<LibraryEntry>,
    val activities: List<UserActivity>
)

/**
 * Holds counts of anime by status for the profile status bar.
 */
@Serializable
data class AnimeStatusCounts(
    val watching: Int = 0,
    val completed: Int = 0,
    val onHold: Int = 0,
    val dropped: Int = 0,
    val planning: Int = 0
)

@Serializable
data class UserActivity(
    val id: Int,
    val status: String?, // e.g. "watched episode", "read chapter"
    val progress: String?, // "1", "12 - 13"
    val mediaTitle: String,
    val mediaCoverUrl: String?,
    val timestamp: Long,
    val mediaScore: Int? = null
)
