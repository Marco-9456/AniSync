package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anisync.android.data.local.entity.AiringScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiringScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<AiringScheduleEntity>)

    /** Drops one account's cached schedule. The other accounts' rows survive. */
    @Query("DELETE FROM airing_schedule WHERE ownerId = :ownerId")
    suspend fun clearAll(ownerId: Int)

    /**
     * Claims rows left unowned by the v23 migration for the signed-in account.
     *
     * `OR REPLACE` because the account may already have refreshed its own rows for the same
     * schedule ids, in which case the fresher ones win.
     */
    @Query("UPDATE OR REPLACE airing_schedule SET ownerId = :toOwnerId WHERE ownerId = :fromOwnerId")
    suspend fun reassignOwner(fromOwnerId: Int, toOwnerId: Int)

    /**
     * Get episodes airing between startTime and endTime.
     */
    @Query("SELECT * FROM airing_schedule WHERE ownerId = :ownerId AND airingAt >= :startTime AND airingAt <= :endTime ORDER BY airingAt ASC")
    suspend fun getAiringBetween(ownerId: Int, startTime: Long, endTime: Long): List<AiringScheduleEntity>

    /**
     * Get episodes airing between startTime and endTime, ONLY for anime the user is watching (isWatching = 1).
     */
    @Query("SELECT * FROM airing_schedule WHERE ownerId = :ownerId AND airingAt >= :startTime AND airingAt <= :endTime AND isWatching = 1 ORDER BY airingAt ASC")
    suspend fun getAiringBetweenForUser(ownerId: Int, startTime: Long, endTime: Long): List<AiringScheduleEntity>

    /** Reactive form of [getAiringBetween], for in-app screens that follow the cache. */
    @Query("SELECT * FROM airing_schedule WHERE ownerId = :ownerId AND airingAt >= :startTime AND airingAt <= :endTime ORDER BY airingAt ASC")
    fun observeAiringBetween(ownerId: Int, startTime: Long, endTime: Long): Flow<List<AiringScheduleEntity>>

    /** Reactive form of [getAiringBetweenForUser]. */
    @Query("SELECT * FROM airing_schedule WHERE ownerId = :ownerId AND airingAt >= :startTime AND airingAt <= :endTime AND isWatching = 1 ORDER BY airingAt ASC")
    fun observeAiringBetweenForUser(ownerId: Int, startTime: Long, endTime: Long): Flow<List<AiringScheduleEntity>>
}
