package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anisync.android.data.local.entity.AiringScheduleEntity

@Dao
interface AiringScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<AiringScheduleEntity>)

    @Query("DELETE FROM airing_schedule")
    suspend fun clearAll()

    /**
     * Get episodes airing between startTime and endTime.
     */
    @Query("SELECT * FROM airing_schedule WHERE airingAt >= :startTime AND airingAt <= :endTime ORDER BY airingAt ASC")
    suspend fun getAiringBetween(startTime: Long, endTime: Long): List<AiringScheduleEntity>

    /**
     * Get episodes airing between startTime and endTime, ONLY for anime the user is watching (isWatching = 1).
     */
    @Query("SELECT * FROM airing_schedule WHERE airingAt >= :startTime AND airingAt <= :endTime AND isWatching = 1 ORDER BY airingAt ASC")
    suspend fun getAiringBetweenForUser(startTime: Long, endTime: Long): List<AiringScheduleEntity>
}
