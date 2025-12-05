package com.anisync.android.domain

data class UserProfile(
    val id: Int,
    val name: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val animeCount: Int,
    val daysWatched: Float, // Converted from minutes
    val mangaCount: Int,
    val chaptersRead: Int,
    val meanScore: Float,
    val favoriteAnime: List<LibraryEntry>
)
