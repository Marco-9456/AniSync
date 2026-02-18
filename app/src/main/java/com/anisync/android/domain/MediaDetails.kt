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
    val bannerUrl: String?,
    val description: String,
    val score: Int?,
    val episodes: Int?,
    val nextAiringEpisode: Int? = null,
    val chapters: Int?,
    val volumes: Int?,
    val type: MediaType?,
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
    // User's list entry (null if not in user's list)
    val listEntryId: Int?,
    val listStatus: LibraryStatus?,
    val listProgress: Int?,
    // Characters
    val characters: List<CharacterInfo>,
    // Related media
    val relations: List<RelatedMedia>,
    // External and streaming links
    val externalLinks: List<ExternalLink>,
    // Whether this media is in user's favorites
    val isFavourite: Boolean = false
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
data class RelatedMedia(
    val id: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val titleUserPreferred: String,
    val coverUrl: String?,
    val format: String?,
    val status: String?,
    val relationType: String
)
