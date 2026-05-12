package com.anisync.android.domain

import androidx.compose.runtime.Immutable
import com.anisync.android.type.MediaType

@Immutable
data class StudioDetails(
    val id: Int,
    val name: String,
    val isAnimationStudio: Boolean,
    val siteUrl: String?,
    val favourites: Int,
    val isFavourite: Boolean,
    val media: List<StudioMediaEntry>,
    val hasNextPage: Boolean
)

@Immutable
data class StudioMediaEntry(
    val mediaId: Int,
    val titleUserPreferred: String,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val coverUrl: String?,
    val cover: CoverImage? = null,
    val format: String?,
    val type: MediaType?,
    val status: String?,
    val year: Int?,
    val averageScore: Int?,
    val popularity: Int?,
    val favourites: Int?,
    val isMainStudio: Boolean,
    val isOnList: Boolean
)
