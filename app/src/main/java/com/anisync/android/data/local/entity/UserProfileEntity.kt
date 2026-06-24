package com.anisync.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anisync.android.domain.AnimeStatusCounts
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.StudioInfo
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
    val animeStatusCounts: AnimeStatusCounts = AnimeStatusCounts(),
    val favoriteAnime: List<LibraryEntry>,
    val activities: List<UserActivity> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    
    @ColumnInfo(defaultValue = "[]")
    val topGenres: List<GenreStat> = emptyList(),
    
    @ColumnInfo(defaultValue = "[]")
    val favoriteMangaOverview: List<LibraryEntry> = emptyList(),
    
    @ColumnInfo(defaultValue = "[]")
    val favoriteCharactersOverview: List<CharacterInfo> = emptyList(),
    
    @ColumnInfo(defaultValue = "[]")
    val favoriteStaffOverview: List<StaffDetails> = emptyList(),

    @ColumnInfo(defaultValue = "[]")
    val favoriteStudiosOverview: List<StudioInfo> = emptyList(),

    @ColumnInfo(defaultValue = "0")
    val donatorTier: Int = 0,

    @ColumnInfo(defaultValue = "")
    val donatorBadge: String? = null,

    @ColumnInfo(defaultValue = "[]")
    val moderatorRoles: List<String> = emptyList(),

    @ColumnInfo(defaultValue = "0")
    val createdAt: Long? = null,

    // Raw markdown form of the bio (about asHtml:false), cached alongside the rendered HTML so the
    // bio editor loads clean source instead of falling back to the asHtml-wrapped HTML.
    @ColumnInfo(defaultValue = "")
    val aboutMarkdown: String? = null
)
