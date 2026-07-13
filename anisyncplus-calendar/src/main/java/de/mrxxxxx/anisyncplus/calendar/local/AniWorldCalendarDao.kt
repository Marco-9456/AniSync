package de.mrxxxxx.anisyncplus.calendar.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AniWorldCalendarDao {
    @Transaction
    @Query(
        """
        SELECT * FROM aniworld_snapshots
        WHERE snapshotId = (
            SELECT activeSnapshotId FROM aniworld_sync_state WHERE singletonId = 1
        )
        LIMIT 1
        """
    )
    abstract fun observeActiveSnapshot(): Flow<AniWorldSnapshotWithReleases?>

    @Query("SELECT * FROM aniworld_sync_state WHERE singletonId = 1")
    abstract fun observeSyncState(): Flow<AniWorldSyncStateEntity?>

    @Query("SELECT * FROM aniworld_media_mappings")
    abstract fun observeMappings(): Flow<List<AniWorldMediaMappingEntity>>

    @Query("SELECT * FROM aniworld_media_mappings WHERE sourceSeriesKey = :sourceSeriesKey")
    abstract suspend fun mapping(sourceSeriesKey: String): AniWorldMediaMappingEntity?

    @Query("SELECT * FROM aniworld_sync_state WHERE singletonId = 1")
    abstract suspend fun syncState(): AniWorldSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertSnapshot(snapshot: AniWorldSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertReleases(releases: List<AniWorldReleaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertMapping(mapping: AniWorldMediaMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertMappings(mappings: List<AniWorldMediaMappingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSyncState(syncState: AniWorldSyncStateEntity)

    @Query("DELETE FROM aniworld_snapshots WHERE snapshotId != :activeSnapshotId")
    protected abstract suspend fun deleteInactiveSnapshots(activeSnapshotId: String)

    @Query("SELECT COUNT(*) FROM aniworld_snapshots")
    abstract suspend fun snapshotCount(): Int

    @Query("SELECT COUNT(*) FROM aniworld_release_entries WHERE snapshotId = :snapshotId")
    abstract suspend fun releaseCount(snapshotId: String): Int

    @Transaction
    open suspend fun activateSnapshot(
        snapshot: AniWorldSnapshotEntity,
        releases: List<AniWorldReleaseEntity>,
        syncState: AniWorldSyncStateEntity
    ) {
        require(syncState.activeSnapshotId == snapshot.snapshotId)
        require(snapshot.entryCount == releases.size)
        insertSnapshot(snapshot)
        insertReleases(releases)
        upsertSyncState(syncState)
        deleteInactiveSnapshots(snapshot.snapshotId)
    }
}
