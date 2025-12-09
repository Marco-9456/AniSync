package com.anisync.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType

/**
 * Room entity for caching library entries.
 */
@Entity(tableName = "library_entries")
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
    val lastUpdated: Long = System.currentTimeMillis()
)
