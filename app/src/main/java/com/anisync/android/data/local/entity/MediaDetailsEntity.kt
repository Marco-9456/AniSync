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
    @ColumnInfo(defaultValue = "NULL")
    val coverMedium: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val coverLarge: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val coverExtraLarge: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val coverColor: String? = null,
    val bannerUrl: String?,
    val description: String,
    val score: Int?,
    @ColumnInfo(defaultValue = "NULL")
    val popularity: Int? = null,
    @ColumnInfo(defaultValue = "NULL")
    val favourites: Int? = null,
    val episodes: Int?,
    val nextAiringEpisode: Int? = null,
    val nextAiringEpisodeTime: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val nextAiringTimeUntil: Int? = null,
    val chapters: Int?,
    val volumes: Int?,
    val mediaType: MediaType?,
    val status: String,
    val format: String?,
    val genres: List<String>,
    val source: String?,
    val studio: String?,
    @ColumnInfo(defaultValue = "NULL")
    val studioId: Int? = null,
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
    val listEntryPrivate: Boolean? = null,
    val listEntryHiddenFromStatusLists: Boolean? = null,
    val characters: List<CharacterInfo>,
    @ColumnInfo(defaultValue = "[]")
    val staff: List<com.anisync.android.domain.StaffInfo> = emptyList(),
    val relations: List<RelatedMedia>,
    val externalLinks: List<ExternalLink>,
    @ColumnInfo(defaultValue = "[]")
    val recommendations: List<RecommendedMedia> = emptyList(),
    @ColumnInfo(defaultValue = "[]")
    val reviews: List<MediaReview> = emptyList(),
    val isFavourite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
