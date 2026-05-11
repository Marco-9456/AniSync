package com.anisync.android.domain

import androidx.compose.runtime.Immutable

/**
 * A single AniList tag fetched from `MediaTagCollection`. Tags are richer than
 * genres: each has a description, category grouping, and adult flag for NSFW
 * gating.
 */
@Immutable
data class MediaTag(
    val id: Int,
    val name: String,
    val description: String?,
    val category: String?,
    val isAdult: Boolean
)
