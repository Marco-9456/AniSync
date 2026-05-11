package com.anisync.android.domain

import androidx.compose.runtime.Immutable
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaSource
import com.anisync.android.type.MediaStatus

/**
 * Origin country for `countryOfOrigin` (ISO 3166-1 alpha-2).
 */
enum class OriginCountry(val code: String, val displayName: String) {
    JAPAN("JP", "Japan"),
    SOUTH_KOREA("KR", "South Korea"),
    CHINA("CN", "China"),
    TAIWAN("TW", "Taiwan");

    companion object {
        fun fromCode(code: String?): OriginCountry? = entries.firstOrNull { it.code == code }
    }
}

@Immutable
data class IntRangeFilter(val min: Int? = null, val max: Int? = null) {
    val isActive: Boolean get() = min != null || max != null
}

@Immutable
data class SearchFilters(
    val sort: SortOption = SortOption.POPULARITY_DESC,
    val genresIncluded: Set<String> = emptySet(),
    val genresExcluded: Set<String> = emptySet(),
    val tagsIncluded: Set<String> = emptySet(),
    val tagsExcluded: Set<String> = emptySet(),
    val yearRange: IntRangeFilter = IntRangeFilter(),
    val season: MediaSeason? = null,
    val formats: Set<MediaFormat> = emptySet(),
    val statuses: Set<MediaStatus> = emptySet(),
    val sources: Set<MediaSource> = emptySet(),
    val scoreRange: IntRangeFilter = IntRangeFilter(),
    val episodesRange: IntRangeFilter = IntRangeFilter(),
    val chaptersRange: IntRangeFilter = IntRangeFilter(),
    val country: OriginCountry? = null,
    val onlyAdult: Boolean? = null
) {
    val hasActiveFilters: Boolean
        get() = activeFilterCount > 0

    val activeFilterCount: Int
        get() = listOf(
            sort != SortOption.POPULARITY_DESC,
            genresIncluded.isNotEmpty() || genresExcluded.isNotEmpty(),
            tagsIncluded.isNotEmpty() || tagsExcluded.isNotEmpty(),
            yearRange.isActive,
            season != null,
            formats.isNotEmpty(),
            statuses.isNotEmpty(),
            sources.isNotEmpty(),
            scoreRange.isActive,
            episodesRange.isActive,
            chaptersRange.isActive,
            country != null,
            onlyAdult != null
        ).count { it }
}
