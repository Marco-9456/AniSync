package com.anisync.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trending_media")
data class TrendingEntity(
    @PrimaryKey val id: Int,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val averageScore: Int?,
    val rank: Int // Determining the order to display in the grid
)
