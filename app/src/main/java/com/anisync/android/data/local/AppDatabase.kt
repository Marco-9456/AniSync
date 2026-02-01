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
 * - v1: Initial schema
 * - v2: Added indices for LibraryEntryEntity (mediaType, status)
 * - v7: Added animeStatusCounts to UserProfileEntity
 */
@Database(
    entities = [
        LibraryEntryEntity::class,
        MediaDetailsEntity::class,
        UserProfileEntity::class,
        AiringScheduleEntity::class,
        TrendingEntity::class
    ],
    version = 14, // Increment version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun mediaDetailsDao(): MediaDetailsDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun airingScheduleDao(): com.anisync.android.data.local.dao.AiringScheduleDao
    abstract fun trendingDao(): com.anisync.android.data.local.dao.TrendingDao

    companion object {
        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add nextAiringEpisodeTime to LibraryEntryEntity
                db.execSQL("ALTER TABLE library_entries ADD COLUMN nextAiringEpisodeTime INTEGER")

                // Add nextAiringEpisode and nextAiringEpisodeTime to MediaDetailsEntity
                db.execSQL("ALTER TABLE media_details ADD COLUMN nextAiringEpisode INTEGER")
                db.execSQL("ALTER TABLE media_details ADD COLUMN nextAiringEpisodeTime INTEGER")
            }
        }
    }
}
