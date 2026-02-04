package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.Flow

/**
 * DAO for library entry operations with reactive Flow support.
 */
@Dao
interface LibraryDao {
    
    /**
     * Observe all library entries by media type.
     * Emits new list whenever data changes.
     */
    @Query("SELECT * FROM library_entries WHERE mediaType = :type ORDER BY titleUserPreferred ASC")
    fun observeByType(type: MediaType): Flow<List<LibraryEntryEntity>>

    /**
     * Get all entries by type (non-reactive, for one-time reads).
     */
    @Query("SELECT * FROM library_entries WHERE mediaType = :type")
    suspend fun getByType(type: MediaType): List<LibraryEntryEntity>

    /**
     * Get "Up Next" entries: Watching status and not completed.
     * Sorted by last updated to show most recently watched first.
     */
    @Query("SELECT * FROM library_entries WHERE status = 'CURRENT' AND (totalEpisodes IS NULL OR progress < totalEpisodes) ORDER BY lastUpdated DESC")
    suspend fun getUpNext(): List<LibraryEntryEntity>

    @Query("SELECT * FROM library_entries WHERE status = 'CURRENT' AND (totalEpisodes IS NULL OR progress < totalEpisodes) ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getMostRecentWatching(): LibraryEntryEntity?

    /**
     * Get a single entry by mediaId.
     */
    @Query("SELECT * FROM library_entries WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getEntry(mediaId: Int): LibraryEntryEntity?

    /**
     * Insert or replace entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LibraryEntryEntity>)

    /**
     * Insert or replace a single entry.
     * Used when adding new media to library from details screen.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entry: LibraryEntryEntity)

    /**
     * Delete all entries by media type.
     */
    @Query("DELETE FROM library_entries WHERE mediaType = :type")
    suspend fun deleteByType(type: MediaType)

    /**
     * Update progress for a specific media.
     */
    @Query("UPDATE library_entries SET progress = :progress, lastUpdated = :timestamp WHERE mediaId = :mediaId")
    suspend fun updateProgress(mediaId: Int, progress: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Atomic transaction: delete old entries and insert new ones.
     * Prevents UI flicker from empty state between delete and insert.
     * @deprecated Use smartMergeByType instead to preserve locally-added entries
     */
    @Transaction
    suspend fun replaceByType(type: MediaType, entries: List<LibraryEntryEntity>) {
        deleteByType(type)
        insertAll(entries)
    }

    /**
     * Smart merge: preserves locally-added entries while syncing with API.
     * - Upserts all entries from API (updates existing, inserts new)
     * - Removes entries that were deleted externally (not in API response)
     * - Preserves recent local entries (added within last 5 minutes) that API may not have yet
     */
    @Transaction
    suspend fun smartMergeByType(type: MediaType, apiEntries: List<LibraryEntryEntity>) {
        val localEntries = getByType(type)
        val apiMediaIds = apiEntries.map { it.mediaId }.toSet()
        val now = System.currentTimeMillis()
        val recentThreshold = 5 * 60 * 1000L // 5 minutes grace period for API sync delay
        
        android.util.Log.d("LibraryDao", "smartMergeByType: type=$type, localCount=${localEntries.size}, apiCount=${apiEntries.size}")
        
        // Find entries to preserve (added recently and not in API yet)
        val toPreserve = localEntries.filter { local ->
            local.mediaId !in apiMediaIds && 
            local.createdAt != null && (now - local.createdAt) <= recentThreshold
        }
        android.util.Log.d("LibraryDao", "Entries to preserve: ${toPreserve.map { "${it.mediaId}:${it.titleUserPreferred}" }}")
        
        // Delete entries that are no longer in API, unless they were added very recently
        val toDelete = localEntries.filter { local ->
            local.mediaId !in apiMediaIds && 
            (local.createdAt == null || (now - local.createdAt) > recentThreshold)
        }
        android.util.Log.d("LibraryDao", "Entries to delete: ${toDelete.map { "${it.mediaId}:${it.titleUserPreferred}" }}")
        toDelete.forEach { deleteByMediaId(it.mediaId) }
        
        // Upsert all API entries (REPLACE strategy handles updates)
        insertAll(apiEntries)
        
        // Re-insert preserved entries to ensure they aren't overwritten
        if (toPreserve.isNotEmpty()) {
            android.util.Log.d("LibraryDao", "Re-inserting preserved entries")
            insertAll(toPreserve)
        }
    }

    /**
     * Update status and progress for a specific media entry.
     * Used when status is changed from DetailsScreen.
     */
    @Query("UPDATE library_entries SET status = :status, progress = :progress, lastUpdated = :timestamp WHERE mediaId = :mediaId")
    suspend fun updateStatusAndProgress(mediaId: Int, status: LibraryStatus, progress: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Delete a specific entry by mediaId.
     * Used when an entry is removed from the user's list.
     */
    @Query("DELETE FROM library_entries WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: Int)

    /**
     * Update an entire entry.
     */
    @androidx.room.Update
    suspend fun updateEntry(entry: LibraryEntryEntity)

    /**
     * Update status, progress, and completedAt when media is completed.
     */
    @Query("UPDATE library_entries SET status = :status, progress = :progress, completedAt = :completedAt, lastUpdated = :timestamp WHERE mediaId = :mediaId")
    suspend fun updateStatusProgressAndCompletedAt(
        mediaId: Int,
        status: LibraryStatus,
        progress: Int,
        completedAt: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Update status and startedAt when starting to watch/read.
     */
    @Query("UPDATE library_entries SET status = :status, startedAt = :startedAt, lastUpdated = :timestamp WHERE mediaId = :mediaId")
    suspend fun updateStatusAndStartedAt(
        mediaId: Int,
        status: LibraryStatus,
        startedAt: Long,
        timestamp: Long = System.currentTimeMillis()
    )
}
