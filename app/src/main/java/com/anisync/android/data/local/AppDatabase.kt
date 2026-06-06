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
 * v18 (Jun 2026):
 *   - Added field to library_entries:
 *     • ownerId - AniList user id the entry belongs to, so multiple accounts'
 *       libraries persist side by side (instant switch from cache, no bleed).
 *       Existing rows default to 0 and are re-tagged to the real id on first
 *       account reconcile.
 *
 * v17 (Jun 2026):
 *   - Added fields to media_details:
 *     • isRecommendationBlocked - hides the "add recommendation" action when true
 *     • isReviewBlocked - hides the "write review" action when true
 *
 * v16 (May 2026):
 *   - Added field to media_details:
 *     • coverColor - average cover hex color, used to tint rich-text link cards
 *       (previously only fetched on the network path, so cached media link cards
 *        rendered with no accent tint)
 *
 * v15 (May 2026):
 *   - Added fields to media_details:
 *     • popularity, favourites - AniList community stats
 *     • nextAiringTimeUntil - seconds-snapshot fallback for next episode airing
 *     • staff - lightweight staff list for media details page
 *
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
    version = 18,
    exportSchema = true,
    autoMigrations = [
        androidx.room.AutoMigration(from = 2, to = 3),
        androidx.room.AutoMigration(from = 3, to = 4),
        androidx.room.AutoMigration(from = 4, to = 5),
        androidx.room.AutoMigration(from = 5, to = 7),
        androidx.room.AutoMigration(from = 7, to = 8),
        androidx.room.AutoMigration(from = 8, to = 9),
        androidx.room.AutoMigration(from = 9, to = 10),
        androidx.room.AutoMigration(from = 10, to = 11),
        androidx.room.AutoMigration(from = 11, to = 12),
        androidx.room.AutoMigration(from = 12, to = 13),
        androidx.room.AutoMigration(from = 13, to = 14),
        androidx.room.AutoMigration(from = 14, to = 15),
        androidx.room.AutoMigration(from = 15, to = 16),
        androidx.room.AutoMigration(from = 16, to = 17),
        androidx.room.AutoMigration(from = 17, to = 18)
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
