package com.anisync.android.domain

import com.anisync.android.type.MediaType

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
    // User's list entry (null if not in user's list)
    val listEntryId: Int?,
    val listStatus: LibraryStatus?,
    val listProgress: Int?,
    // Characters
    val characters: List<CharacterInfo>,
    // Related media
    val relations: List<RelatedMedia>
)

data class CharacterInfo(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val role: String
)

data class RelatedMedia(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val format: String?,
    val status: String?,
    val relationType: String
)
