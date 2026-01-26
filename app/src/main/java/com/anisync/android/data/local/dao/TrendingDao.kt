package com.anisync.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anisync.android.data.local.entity.TrendingEntity

@Dao
interface TrendingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<TrendingEntity>)

    @Query("DELETE FROM trending_media")
    suspend fun clearAll()

    @Query("SELECT * FROM trending_media ORDER BY rank ASC LIMIT :limit")
    suspend fun getTopTrending(limit: Int): List<TrendingEntity>
}
