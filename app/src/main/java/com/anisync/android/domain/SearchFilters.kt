package com.anisync.android.domain

import androidx.compose.runtime.Immutable
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaStatus

/**
 * Data class representing search filter options.
 * Used to filter search results by various criteria.
 */
@Immutable
data class SearchFilters(
    val genres: Set<String> = emptySet(),
    val year: Int? = null,
    val season: MediaSeason? = null,
    val formats: Set<MediaFormat> = emptySet(),
    val status: MediaStatus? = null
) {
    /**
     * Returns true if any filter is currently active.
     */
    val hasActiveFilters: Boolean
        get() = genres.isNotEmpty() || year != null || season != null || 
                formats.isNotEmpty() || status != null
    
    /**
     * Returns a count of active filter categories.
     */
    val activeFilterCount: Int
        get() = listOf(
            genres.isNotEmpty(),
            year != null,
            season != null,
            formats.isNotEmpty(),
            status != null
        ).count { it }
}

/**
 * Available genres from AniList.
 */
val AVAILABLE_GENRES = listOf(
    "Action",
    "Adventure", 
    "Comedy",
    "Drama",
    "Ecchi",
    "Fantasy",
    "Horror",
    "Mahou Shoujo",
    "Mecha",
    "Music",
    "Mystery",
    "Psychological",
    "Romance",
    "Sci-Fi",
    "Slice of Life",
    "Sports",
    "Supernatural",
    "Thriller"
)
