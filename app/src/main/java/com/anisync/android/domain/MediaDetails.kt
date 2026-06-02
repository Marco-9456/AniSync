package com.anisync.android.domain

import com.anisync.android.type.MediaType
import kotlinx.serialization.Serializable

/**
 * Type of external link.
 */
enum class ExternalLinkType {
    STREAMING,
    SOCIAL,
    INFO
}

/**
 * Represents an external link to a streaming service or other resource.
 */
@Serializable
data class ExternalLink(
    val id: Int,
    val url: String?,
    val site: String,
    val type: ExternalLinkType?,
    val color: String?,   // Hex color (e.g., "#E50914" for Netflix)
    val icon: String?,    // URL to 64x64 PNG icon
    val language: String?,
    val notes: String? = null
)

/**
 * Represents a content tag for themes, warnings, etc.
 */
@Serializable
data class Tag(
    val name: String,
    val category: String,
    val description: String? = null,
    val isMediaSpoiler: Boolean,
    val isGeneralSpoiler: Boolean,
    val rank: Int?
)

/**
 * Represents a media trailer (typically from YouTube).
 */
@Serializable
data class Trailer(
    val id: String?,
    val site: String?,
    val thumbnail: String?
)

data class MediaDetails(
    val id: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val cover: CoverImage? = null,
    /** Average cover color as a `#RRGGBB` hex string; used to tint link-preview cards. */
    val coverColor: String? = null,
    val bannerUrl: String?,
    val description: String,
    val score: Int?,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val episodes: Int?,
    val nextAiringEpisode: NextAiringEpisode? = null,
    val chapters: Int?,
    val volumes: Int?,
    val type: MediaType?,
    val status: String,
    val format: String?,
    val genres: List<String>,
    val source: String?,
    val studio: StudioRef?,
    val year: Int?,
    val startDate: String?,
    val endDate: String?,
    val season: String?,
    val seasonYear: Int?,
    val duration: Int?, // Episode duration in minutes
    val tags: List<Tag>,
    val trailer: Trailer?,
    // User's list entry (null if not in user's list)
    val listEntryId: Int?,
    val listStatus: LibraryStatus?,
    val listProgress: Int?,
    val listEntryPrivate: Boolean? = null,
    val listEntryHiddenFromStatusLists: Boolean? = null,
    // Characters
    val characters: List<CharacterInfo>,
    // Staff
    val staff: List<StaffInfo> = emptyList(),
    // Related media
    val relations: List<RelatedMedia>,
    // External and streaming links
    val externalLinks: List<ExternalLink>,
    // Recommendations
    val recommendations: List<RecommendedMedia> = emptyList(),
    // Reviews
    val reviews: List<MediaReview> = emptyList(),
    // Whether this media is in user's favorites
    val isFavourite: Boolean = false,
    // Whether this media is blocked from being recommended to/from (null = unknown)
    val isRecommendationBlocked: Boolean? = null,
    // Whether this media is blocked from being reviewed (null = unknown)
    val isReviewBlocked: Boolean? = null
)

@Serializable
data class StudioRef(
    val id: Int,
    val name: String
)

@Serializable
data class CharacterInfo(
    val id: Int,
    val nameFull: String,
    val nameNative: String?,
    val nameUserPreferred: String,
    val imageUrl: String?,
    val role: String
)

@Serializable
data class NextAiringEpisode(
    val episode: Int,
    val airingAt: Long,         // unix seconds, absolute — survives cache reads
    val timeUntilAiring: Int    // seconds, snapshot at fetch time (fallback only)
)

@Serializable
data class StaffInfo(
    val id: Int,
    val nameFull: String,
    val nameNative: String?,
    val nameUserPreferred: String,
    val imageUrl: String?,
    val role: String,
    val primaryOccupations: List<String> = emptyList()
)

@Serializable
data class RelatedMedia(
    val id: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val cover: CoverImage? = null,
    val format: String?,
    val status: String?,
    val relationType: String
)

/**
 * Represents a recommended media item from the AniList recommendations system.
 */
@Serializable
data class RecommendedMedia(
    val id: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val cover: CoverImage? = null,
    val format: String?,
    val score: Int?,
    val rating: Int, // Community recommendation rating
    val userRating: String? = null // "RATE_UP", "RATE_DOWN", or "NO_RATING"/null
)

/**
 * Represents a followed user's list entry for a given media.
 */
@Serializable
data class MediaFollowingEntry(
    val userId: Int,
    val userName: String,
    val userAvatarUrl: String?,
    val status: LibraryStatus,
    val score: Double?,
    val progress: Int?,
    val scoreFormat: ScoreFormat? = null
)

/**
 * Represents a user review for a media.
 */
@Serializable
data class MediaReview(
    val id: Int,
    val summary: String,
    val body: String? = null,
    val score: Int,
    val rating: Int,        // Upvotes
    val ratingAmount: Int,  // Total votes
    val userRating: String? = null,
    val userName: String,
    val userAvatarUrl: String?,
    val createdAt: Long,    // Unix timestamp
    val mediaTitle: String? = null,
    val mediaCoverUrl: String? = null,
    val mediaCover: CoverImage? = null,
    val mediaBannerUrl: String? = null
)
