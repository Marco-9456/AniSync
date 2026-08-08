package com.anisync.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * One airing-schedule row, scoped to the account that cached it.
 *
 * [isWatching] is denormalised from the account library, so one AniList schedule id means different
 * things to different accounts. Hence [ownerId] in the primary key rather than a plain column.
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
    /**
     * Whether the account was watching this when the row was written, taken from mediaListEntry.
     *
     * Correct at fetch time and stale afterwards, since adding or dropping a series in the app does
     * not rewrite the schedule cache. The My List filter therefore treats this as one of two
     * signals rather than the answer. See `AiringScheduleDao.getAiringBetweenForUser`.
     */
    val isWatching: Boolean,
    @ColumnInfo(name = "streamingSeriesUrl")
    val streamingSeriesUrl: String? = null
)
