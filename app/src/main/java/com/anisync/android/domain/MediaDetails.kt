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

data class MediaDetails(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val bannerUrl: String?,
    val description: String,
    val score: Int?,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val type: MediaType?,
    val status: String,
    val format: String?,
    val genres: List<String>,
    val studio: String?,
    val year: Int?,
    val startDate: String?,
    val season: String?,
    val seasonYear: Int?,
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
    val name: String,
    val imageUrl: String?,
    val role: String
)

@Serializable
data class RelatedMedia(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val format: String?,
    val status: String?,
    val relationType: String
)
