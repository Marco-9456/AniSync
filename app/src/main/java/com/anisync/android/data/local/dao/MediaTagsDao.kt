package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anisync.android.data.local.entity.MediaTagsEntity
import com.anisync.android.data.local.entity.MediaTagsStamp
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaTagsDao {
    @Query("SELECT * FROM media_tags")
    fun observeAll(): Flow<List<MediaTagsEntity>>

    @Query("SELECT mediaId, fetchedAt FROM media_tags")
    suspend fun getStamps(): List<MediaTagsStamp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<MediaTagsEntity>)

    /** Drops titles that no signed-in account has on a list any more. */
    @Query("DELETE FROM media_tags WHERE mediaId NOT IN (SELECT mediaId FROM library_entries)")
    suspend fun deleteOrphans()
}
