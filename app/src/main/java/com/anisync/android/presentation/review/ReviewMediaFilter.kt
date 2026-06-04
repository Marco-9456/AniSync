package com.anisync.android.presentation.review

import com.anisync.android.type.MediaType

/**
 * Media-type filter for review lists. [ALL] maps to a null AniList media-type
 * filter (both anime and manga reviews).
 */
enum class ReviewMediaFilter {
    ALL,
    ANIME,
    MANGA;

    fun toMediaType(): MediaType? = when (this) {
        ALL -> null
        ANIME -> MediaType.ANIME
        MANGA -> MediaType.MANGA
    }

    companion object {
        fun fromRouteArg(arg: String?): ReviewMediaFilter = when (arg) {
            "MANGA" -> MANGA
            "ALL" -> ALL
            else -> ANIME
        }
    }
}
