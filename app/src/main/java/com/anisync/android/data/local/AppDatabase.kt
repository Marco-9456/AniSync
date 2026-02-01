package com.anisync.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.dao.MediaDetailsDao
import com.anisync.android.data.local.dao.UserProfileDao
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.data.local.entity.MediaDetailsEntity
import com.anisync.android.data.local.entity.TrendingEntity
import com.anisync.android.data.local.entity.UserProfileEntity

/**
 * Room database for offline caching.
 *
 * Version History:
 * ─────────────────────────────────────────────────────────────────────────────
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
        TrendingEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun mediaDetailsDao(): MediaDetailsDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun airingScheduleDao(): com.anisync.android.data.local.dao.AiringScheduleDao
    abstract fun trendingDao(): com.anisync.android.data.local.dao.TrendingDao
}
