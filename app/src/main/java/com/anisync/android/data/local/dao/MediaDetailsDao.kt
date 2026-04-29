package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anisync.android.data.local.entity.MediaDetailsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for media details operations.
 */
@Dao
interface MediaDetailsDao {
    
    /**
     * Observe media details by ID.
     * Emits null if not cached, new value when inserted/updated.
     */
    @Query("SELECT * FROM media_details WHERE id = :id")
    fun observeById(id: Int): Flow<MediaDetailsEntity?>

    /**
     * Get media details by ID (one-time read).
     */
    @Query("SELECT * FROM media_details WHERE id = :id")
    suspend fun getById(id: Int): MediaDetailsEntity?

    /**
     * Insert or replace media details.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(details: MediaDetailsEntity)

    /**
     * Delete media details by ID.
     */
    @Query("DELETE FROM media_details WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE media_details SET isFavourite = :isFavourite WHERE id = :id")
    suspend fun updateFavouriteStatus(id: Int, isFavourite: Boolean)

    /**
     * Clear all cached media details.
     */
    @Query("DELETE FROM media_details")
    suspend fun clear()
}
