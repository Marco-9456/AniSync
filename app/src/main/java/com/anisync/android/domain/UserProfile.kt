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
    val favoriteAnime: List<LibraryEntry>,
    val activities: List<UserActivity>
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
