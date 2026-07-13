package de.mrxxxxx.anisyncplus.calendar.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AniWorldSnapshotEntity::class,
        AniWorldReleaseEntity::class,
        AniWorldMediaMappingEntity::class,
        AniWorldSyncStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AniWorldCalendarDatabase : RoomDatabase() {
    abstract fun calendarDao(): AniWorldCalendarDao

    companion object {
        const val DATABASE_NAME = "anisync_plus_calendar.db"
    }
}
