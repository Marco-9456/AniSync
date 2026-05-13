package com.anisync.android.domain

import androidx.compose.runtime.Immutable
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaSource
import com.anisync.android.type.MediaStatus

/**
 * Genre names AniList classifies as adult. Hidden from the genre picker
 * unless [com.anisync.android.data.AppSettings.showAdultContent] is on.
 */
val ADULT_GENRES = setOf("Hentai")

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

/** Adult-content quick filter shown in the chip bar (alongside the global pref). */
enum class AdultMode { ANY, HIDE, ONLY }

/**
 * Entity bucket the user wants the search overlay to target.
 *
 * Null means "follow the discover screen's Anime/Manga selector"; an explicit
 * value overrides it. Non-media values (CHARACTERS/STAFF/USERS/STUDIOS) cause
 * media-only filters (genres, year, format, etc.) to be hidden in the chip
 * bar, and the viewmodel skips the heavy `Search.graphql` call in favor of
 * the multi-entity `SearchAll.graphql` projection.
 */
enum class SearchType { ANIME, MANGA, CHARACTERS, STAFF, USERS, STUDIOS }

/** Closed year range; both bounds inclusive. Null bound = open ended. */
@Immutable
data class IntRangeFilter(val min: Int? = null, val max: Int? = null) {
    val isActive: Boolean get() = min != null || max != null
}

/**
 * Comparator-based numeric filter ("at least 12", "at most 12", "exactly 12").
 * Picked over a range slider for fields where users think in single thresholds
 * (score, episodes, chapters).
 */
enum class ComparatorMode { ANY, AT_LEAST, AT_MOST, EXACTLY }

@Immutable
data class IntComparatorFilter(
    val mode: ComparatorMode = ComparatorMode.ANY,
    val value: Int? = null
) {
    val isActive: Boolean get() = mode != ComparatorMode.ANY && value != null
}

@Immutable
data class SearchFilters(
    val sort: SortOption = SortOption.POPULARITY_DESC,
    val searchType: SearchType? = null,
    val genresIncluded: Set<String> = emptySet(),
    val genresExcluded: Set<String> = emptySet(),
    val tagsIncluded: Set<String> = emptySet(),
    val tagsExcluded: Set<String> = emptySet(),
    val yearRange: IntRangeFilter = IntRangeFilter(),
    val season: MediaSeason? = null,
    val formats: Set<MediaFormat> = emptySet(),
    val statuses: Set<MediaStatus> = emptySet(),
    val sources: Set<MediaSource> = emptySet(),
    val score: IntComparatorFilter = IntComparatorFilter(),
    val episodes: IntComparatorFilter = IntComparatorFilter(),
    val chapters: IntComparatorFilter = IntComparatorFilter(),
    val country: OriginCountry? = null,
    val adultMode: AdultMode = AdultMode.ANY
) {
    /** True when [searchType] forces the overlay onto a non-media entity. */
    val isNonMediaType: Boolean
        get() = searchType != null && searchType != SearchType.ANIME && searchType != SearchType.MANGA

    val hasActiveFilters: Boolean get() = activeFilterCount > 0

    val activeFilterCount: Int
        get() = listOf(
            sort != SortOption.POPULARITY_DESC,
            searchType != null,
            genresIncluded.isNotEmpty() || genresExcluded.isNotEmpty(),
            tagsIncluded.isNotEmpty() || tagsExcluded.isNotEmpty(),
            yearRange.isActive,
            season != null,
            formats.isNotEmpty(),
            statuses.isNotEmpty(),
            sources.isNotEmpty(),
            score.isActive,
            episodes.isActive,
            chapters.isActive,
            country != null,
            adultMode != AdultMode.ANY
        ).count { it }
}
