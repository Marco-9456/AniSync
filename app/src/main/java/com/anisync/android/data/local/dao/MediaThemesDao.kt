package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anisync.android.data.local.entity.MediaThemesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaThemesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MediaThemesEntity)

    @Query("SELECT * FROM media_themes WHERE mediaId = :mediaId")
    fun observe(mediaId: Int): Flow<MediaThemesEntity?>

    @Query("SELECT * FROM media_themes WHERE mediaId = :mediaId")
    suspend fun get(mediaId: Int): MediaThemesEntity?
}
