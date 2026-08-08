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

    /** Drops one account cached schedule. Other accounts keep theirs. */
    @Query("DELETE FROM airing_schedule WHERE ownerId = :ownerId")
    suspend fun clearAll(ownerId: Int)

    /**
     * Claims the rows the v23 migration left unowned for the signed in account.
     *
     * OR REPLACE because the account may already have refreshed its own rows for the same ids, in
     * which case the fresher ones win.
     */
    @Query("UPDATE OR REPLACE airing_schedule SET ownerId = :toOwnerId WHERE ownerId = :fromOwnerId")
    suspend fun reassignOwner(fromOwnerId: Int, toOwnerId: Int)

    /**
     * Get episodes airing between startTime and endTime.
     */
    @Query("SELECT * FROM airing_schedule WHERE ownerId = :ownerId AND airingAt >= :startTime AND airingAt <= :endTime ORDER BY airingAt ASC")
    suspend fun getAiringBetween(ownerId: Int, startTime: Long, endTime: Long): List<AiringScheduleEntity>

    /**
     * Episodes airing in a window, for series the account is actually watching.
     *
     * Either signal counts, they cover each other blind spots.
     *
     * isWatching comes from the mediaListEntry AniList returned when the schedule was fetched, so it
     * holds up even if the local library never synced. The join handles the other case, a series
     * added, dropped or finished in the app since that fetch, which the cached flag knows nothing
     * about. Going by the flag alone emptied the My List filter whenever a schedule refresh landed
     * before the library loaded.
     *
     * REPEATING counts with CURRENT, a rewatch is still watching.
     */
    @Query(
        """
        SELECT s.* FROM airing_schedule AS s
        WHERE s.ownerId = :ownerId
            AND s.airingAt >= :startTime AND s.airingAt <= :endTime
            AND (
                s.isWatching = 1
                OR EXISTS (
                    SELECT 1 FROM library_entries AS l
                    WHERE l.ownerId = s.ownerId AND l.mediaId = s.mediaId
                        AND l.status IN ('CURRENT', 'REPEATING')
                )
            )
        ORDER BY s.airingAt ASC
        """
    )
    suspend fun getAiringBetweenForUser(ownerId: Int, startTime: Long, endTime: Long): List<AiringScheduleEntity>

    /** Reactive version of [getAiringBetween], for screens that follow the cache. */
    @Query("SELECT * FROM airing_schedule WHERE ownerId = :ownerId AND airingAt >= :startTime AND airingAt <= :endTime ORDER BY airingAt ASC")
    fun observeAiringBetween(ownerId: Int, startTime: Long, endTime: Long): Flow<List<AiringScheduleEntity>>

    /** Reactive version of [getAiringBetweenForUser]. */
    @Query(
        """
        SELECT s.* FROM airing_schedule AS s
        WHERE s.ownerId = :ownerId
            AND s.airingAt >= :startTime AND s.airingAt <= :endTime
            AND (
                s.isWatching = 1
                OR EXISTS (
                    SELECT 1 FROM library_entries AS l
                    WHERE l.ownerId = s.ownerId AND l.mediaId = s.mediaId
                        AND l.status IN ('CURRENT', 'REPEATING')
                )
            )
        ORDER BY s.airingAt ASC
        """
    )
    fun observeAiringBetweenForUser(ownerId: Int, startTime: Long, endTime: Long): Flow<List<AiringScheduleEntity>>
}
