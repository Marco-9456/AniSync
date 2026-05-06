package com.anisync.android.domain

import com.anisync.android.type.MediaType

/**
 * Domain model for an airing schedule entry.
 * Used for tracking when episodes of anime air.
 */
data class AiringSchedule(
    val id: Int,
    val episode: Int,
    val airingAt: Long,
    val mediaId: Int,
    val mediaTitle: String,
    val mediaCoverUrl: String?,
    val mediaCover: CoverImage? = null,
    val mediaType: MediaType
)
