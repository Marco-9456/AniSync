package com.anisync.android.presentation.discover

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.ResultCategory
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchSort
import com.anisync.android.domain.SearchSortColumn
import com.anisync.android.presentation.navigation.PresetSearch
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaType

internal val PresetSearch.resultType: MediaType
    get() = if (mediaType == MediaType.MANGA.name) MediaType.MANGA else MediaType.ANIME

internal val PresetSearch.category: ResultCategory
    get() = if (resultType == MediaType.MANGA) ResultCategory.MANGA else ResultCategory.ANIME

internal fun PresetSearch.toFilters(): SearchFilters = SearchFilters(
    sort = SearchSortColumn.entries.find { it.name == sort }?.let { SearchSort(it) } ?: SearchSort(),
    genresIncluded = setOfNotNull(genre),
    tagsIncluded = setOfNotNull(tag),
    yearRange = if (year > 0) IntRangeFilter(min = year, max = year) else IntRangeFilter(),
    season = MediaSeason.knownEntries.find { it.name == season }
)

/** The preset a [PresetSearch] page opens on. The Discover tab's own search has none. */
internal fun SavedStateHandle.presetSearch(): PresetSearch? =
    if (contains("mediaType")) toRoute<PresetSearch>() else null
