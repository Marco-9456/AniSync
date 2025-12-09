package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anisync.android.data.local.entity.LibraryEntryEntity
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
    @Query("SELECT * FROM library_entries WHERE mediaType = :type ORDER BY title ASC")
    fun observeByType(type: MediaType): Flow<List<LibraryEntryEntity>>

    /**
     * Get all entries by type (non-reactive, for one-time reads).
     */
    @Query("SELECT * FROM library_entries WHERE mediaType = :type")
    suspend fun getByType(type: MediaType): List<LibraryEntryEntity>

    /**
     * Insert or replace entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LibraryEntryEntity>)

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
}
