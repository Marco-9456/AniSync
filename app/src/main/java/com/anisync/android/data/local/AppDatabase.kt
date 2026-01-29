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
    version = 12, // Increment version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun mediaDetailsDao(): MediaDetailsDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun airingScheduleDao(): com.anisync.android.data.local.dao.AiringScheduleDao
    abstract fun trendingDao(): com.anisync.android.data.local.dao.TrendingDao
}
