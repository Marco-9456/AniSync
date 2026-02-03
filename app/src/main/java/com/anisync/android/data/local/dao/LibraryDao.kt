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
     */
    @Transaction
    suspend fun replaceByType(type: MediaType, entries: List<LibraryEntryEntity>) {
        deleteByType(type)
        insertAll(entries)
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
