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
enum class OriginCountry(val code: String) {
    JAPAN("JP"),
    SOUTH_KOREA("KR"),
    CHINA("CN"),
    TAIWAN("TW")
}

/** Adult-content quick filter shown in the chip bar (alongside the global pref). */
enum class AdultMode { ANY, HIDE, ONLY }

/**
 * The kinds of result Discover search returns, in the order its rail lists them. The rail picks
 * which of them is on screen, and a preset from another screen can open straight onto one.
 */
enum class ResultCategory { ALL, ANIME, MANGA, CHARACTERS, STAFF, USERS, STUDIOS }

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
    val sort: SearchSort = SearchSort(),
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
    /** A changed sort counts: ordering everything by score with nothing typed is a search. */
    val hasActiveFilters: Boolean get() = sort != SearchSort() || hasNarrowingFilters

    /** Anything that takes titles out of the results, which is what the Clear chip clears. */
    val hasNarrowingFilters: Boolean get() = copy(sort = SearchSort()) != SearchFilters()
}
