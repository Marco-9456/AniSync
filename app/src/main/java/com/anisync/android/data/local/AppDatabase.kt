package com.anisync.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.data.local.dao.SavedForumThreadDao
import com.anisync.android.data.local.dao.UserProfileDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.data.local.entity.MediaDetailsEntity
import com.anisync.android.data.local.entity.SavedForumThreadEntity
import com.anisync.android.data.local.entity.TrendingEntity
import com.anisync.android.data.local.entity.UserProfileEntity

/**
 * Room database for offline caching.
 *
 * Version History:
 * ─────────────────────────────────────────────────────────────────────────────
 * v4 (Mar 2026):
 *   - Added saved_forum_threads table for local thread bookmarks
 *     • threadId, title, authorName, authorAvatarUrl
 *     • replyCount, viewCount, likeCount
 *     • isLiked, isLocked
 *     • repliedAt, replyUserName, replyUserAvatarUrl
 *     • categories, mediaTitle, mediaCoverUrl, savedAt
 *
 * v3 (Feb 2026):
 *   - Added fields to media_details:
 *     • source - Source material (Manga, Light Novel, Original, etc.)
 *     • Tag description field for tooltip support
 *
 * v2 (Feb 2026):
 *   - Added fields to media_details:
 *     • endDate - Formatted end date string
 *     • duration - Episode duration in minutes
 *     • tags - List of content tags (themes, warnings)
 *     • trailer - Trailer info (id, site, thumbnail)
 *
 * v1 (Fresh Start - June 2025):
 *   - Initial production schema (reset from development iterations)
 *   - Tables:
 *     • library_entries - User's anime/manga library with 9 indices for sorting/filtering
 *     • media_details - Cached media details with characters, relations, external links
 *     • user_profile - User profile with stats, favorites, and activity
 *     • airing_schedule - Airing schedule items with watching status
 *     • trending_media - Trending media for home screen
 *   - TypeConverters: JSON-based serialization for complex types (Converters.kt)
 *
 * Migration Guidelines:
 *   - Auto-migrations: Use for simple changes (add columns, tables, indices)
 *   - Manual migrations: Use for complex changes (see Migrations.kt)
 *   - Always test migrations before release (see MigrationTest.kt)
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Database(
    entities = [
        LibraryEntryEntity::class,
        MediaDetailsEntity::class,
        UserProfileEntity::class,
        AiringScheduleEntity::class,
        TrendingEntity::class,
        SavedForumThreadEntity::class
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        androidx.room.AutoMigration(from = 2, to = 3),
        androidx.room.AutoMigration(from = 3, to = 4),
        androidx.room.AutoMigration(from = 4, to = 5)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun mediaDetailsDao(): MediaDetailsDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun airingScheduleDao(): com.anisync.android.data.local.dao.AiringScheduleDao
    abstract fun trendingDao(): com.anisync.android.data.local.dao.TrendingDao
    abstract fun savedForumThreadDao(): SavedForumThreadDao
}
