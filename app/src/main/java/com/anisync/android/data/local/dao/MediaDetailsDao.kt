package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anisync.android.data.local.entity.MediaDetailsEntity
import com.anisync.android.domain.LibraryStatus
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

    /** The viewer's entry as the page shows it when the library holds no row for the title. */
    @Query(
        "UPDATE media_details SET listEntryId = :entryId, listStatus = :status, " +
            "listProgress = :progress, listNotes = :notes, listEntryPrivate = :isPrivate, " +
            "listEntryHiddenFromStatusLists = :hiddenFromStatusLists WHERE id = :id"
    )
    suspend fun updateListEntry(
        id: Int,
        entryId: Int?,
        status: LibraryStatus?,
        progress: Int?,
        notes: String?,
        isPrivate: Boolean?,
        hiddenFromStatusLists: Boolean?
    )

    /** Takes the viewer's entry off every cached title, since it belongs to whoever fetched it. */
    @Query(
        "UPDATE media_details SET listEntryId = NULL, listStatus = NULL, listProgress = NULL, " +
            "listNotes = NULL, listEntryPrivate = NULL, listEntryHiddenFromStatusLists = NULL"
    )
    suspend fun clearListEntries()

    /**
     * Clear all cached media details.
     */
    @Query("DELETE FROM media_details")
    suspend fun clear()
}
