package com.anisync.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * One airing-schedule row, scoped to the account that cached it.
 *
 * [isWatching] is denormalised from the account's library, so the same AniList schedule id means
 * different things to different accounts. That is why [ownerId] is part of the primary key rather
 * than a plain column.
 */
@Entity(
    tableName = "airing_schedule",
    primaryKeys = ["id", "ownerId"],
    indices = [Index(value = ["ownerId", "airingAt"])]
)
data class AiringScheduleEntity(
    val id: Int, // The unique ID of the airing schedule item
    /** AniList user id the row was cached for, -1 when signed out, as with `library_entries`. */
    val ownerId: Int,
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
