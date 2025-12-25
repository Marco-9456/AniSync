package com.anisync.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.UserActivity

/**
 * Room entity for caching user profile.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val about: String?,
    val activeAt: Long?,
    val animeCount: Int,
    val daysWatched: Float,
    val mangaCount: Int,
    val chaptersRead: Int,
    val meanScore: Float,
    val favoriteAnime: List<LibraryEntry>,
    val activities: List<UserActivity> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)
