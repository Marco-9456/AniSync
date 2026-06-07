package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anisync.android.data.local.entity.SavedForumThreadEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for saved (bookmarked) forum threads.
 */
@Dao
interface SavedForumThreadDao {

    @Query("SELECT * FROM saved_forum_threads ORDER BY savedAt DESC")
    fun getAll(): Flow<List<SavedForumThreadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thread: SavedForumThreadEntity)

    @Query("DELETE FROM saved_forum_threads WHERE threadId = :threadId")
    suspend fun delete(threadId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_forum_threads WHERE threadId = :threadId)")
    suspend fun exists(threadId: Int): Boolean

    /** Delete every saved thread. Used when switching accounts (bookmarks are per-account). */
    @Query("DELETE FROM saved_forum_threads")
    suspend fun deleteAll()
}
