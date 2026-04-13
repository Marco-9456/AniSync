package com.anisync.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "airing_schedule")
data class AiringScheduleEntity(
    @PrimaryKey val id: Int, // The unique ID of the airing schedule item
    val mediaId: Int,
    val airingAt: Long, // Unix timestamp in seconds
    val episode: Int,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val format: String?, // TV, MOVIE, etc.
    val isWatching: Boolean, // Denormalized field to filter by "My List" easily
    @ColumnInfo(name = "streamingSeriesUrl")
    val streamingSeriesUrl: String? = null
)
