package com.anisync.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anisync.android.domain.CharacterInfo
import com.anisync.android.domain.ExternalLink
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.RecommendedMedia
import com.anisync.android.domain.RelatedMedia
import com.anisync.android.domain.Tag
import com.anisync.android.domain.Trailer
import com.anisync.android.type.MediaType

/**
 * Room entity for caching media details.
 */
@Entity(tableName = "media_details")
data class MediaDetailsEntity(
    @PrimaryKey val id: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val bannerUrl: String?,
    val description: String,
    val score: Int?,
    val episodes: Int?,
    val nextAiringEpisode: Int? = null,
    val nextAiringEpisodeTime: Long? = null,
    val chapters: Int?,
    val volumes: Int?,
    val mediaType: MediaType?,
    val status: String,
    val format: String?,
    val genres: List<String>,
    val source: String?,
    val studio: String?,
    val year: Int?,
    val startDate: String?,
    val endDate: String?,
    val season: String?,
    val seasonYear: Int?,
    val duration: Int?, // Episode duration in minutes
    val tags: List<Tag>,
    val trailer: Trailer?,
    val listEntryId: Int?,
    val listStatus: LibraryStatus?,
    val listProgress: Int?,
    val characters: List<CharacterInfo>,
    val relations: List<RelatedMedia>,
    val externalLinks: List<ExternalLink>,
    @ColumnInfo(defaultValue = "[]")
    val recommendations: List<RecommendedMedia> = emptyList(),
    @ColumnInfo(defaultValue = "[]")
    val reviews: List<MediaReview> = emptyList(),
    val isFavourite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
