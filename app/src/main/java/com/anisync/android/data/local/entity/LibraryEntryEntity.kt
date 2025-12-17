package com.anisync.android.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType

/**
 * Room entity for caching library entries.
 * 
 * Indices are added for frequently-queried columns:
 * - mediaType: Used for filtering Anime vs Manga
 * - status: Used for filtering by library status (Watching, Completed, etc.)
 * - Composite index for combined queries
 */
@Entity(
    tableName = "library_entries",
    indices = [
        Index(value = ["mediaType"]),
        Index(value = ["status"]),
        Index(value = ["mediaType", "status"])
    ]
)
data class LibraryEntryEntity(
    @PrimaryKey val id: Int,                   // MediaList ID
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val progress: Int,
    val totalEpisodes: Int?,
    val totalChapters: Int?,
    val totalVolumes: Int?,
    val mediaType: MediaType?,
    val status: LibraryStatus,
    val nextAiringEpisode: Int?,
    val timeUntilAiring: Int?,
    val mediaStatus: String?,
    val score: Double? = 0.0,
    val rewatches: Int = 0,
    val notes: String? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
