package com.anisync.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.type.MediaType

/**
 * Room entity for caching media details.
 */
@Entity(tableName = "media_details")
data class MediaDetailsEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val coverUrl: String?,
    val bannerUrl: String?,
    val description: String,
    val score: Int?,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val mediaType: MediaType?,
    val status: String,
    val format: String?,
    val genres: List<String>,
    val studio: String?,
    val year: Int?,
    val listEntryId: Int?,
    val listStatus: LibraryStatus?,
    val listProgress: Int?,
    val characters: List<CharacterInfo>,
    val relations: List<RelatedMedia>,
    val lastUpdated: Long = System.currentTimeMillis()
)
