package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.ResultCategory
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchSort
import com.anisync.android.presentation.components.filtersheet.ClearFiltersChip
import com.anisync.android.presentation.components.filtersheet.FilterBarChip
import com.anisync.android.presentation.components.filtersheet.airingStatusTitle
import com.anisync.android.presentation.components.filtersheet.countChipLabel
import com.anisync.android.presentation.components.filtersheet.pickChipLabel
import com.anisync.android.presentation.util.toLabel

/** Which sheet the Discover filter bar has open. */
enum class FilterId { SORT, GENRES, TAGS, FORMAT, STATUS, YEAR, MORE }

/**
 * The filter bar over Discover search: Clear once anything is filtered, then Sort, Genres, Tags,
 * Format, Airing status, Year and More. A closed list names its pick on the chip, genres and tags
 * say how many are picked.
 */
@Composable
fun SearchFilterChipBar(
    filters: SearchFilters,
    category: ResultCategory,
    onChipTap: (FilterId) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (filters.hasNarrowingFilters) ClearFiltersChip(onClick = onClear)
        FilterBarChip(
            label = stringResource(R.string.library_filter_chip_sort, filters.sort.column.label()),
            active = filters.sort != SearchSort(),
            onClick = { onChipTap(FilterId.SORT) }
        )
        val genres = filters.genresIncluded.size + filters.genresExcluded.size
        FilterBarChip(
            label = countChipLabel(stringResource(R.string.filter_genres), genres),
            active = genres > 0,
            onClick = { onChipTap(FilterId.GENRES) }
        )
        val tags = filters.tagsIncluded.size + filters.tagsExcluded.size
        FilterBarChip(
            label = countChipLabel(stringResource(R.string.library_filter_tags), tags),
            active = tags > 0,
            onClick = { onChipTap(FilterId.TAGS) }
        )
        FilterBarChip(
            label = pickChipLabel(
                stringResource(R.string.library_filter_format),
                filters.formats.map { it.toLabel() }
            ),
            active = filters.formats.isNotEmpty(),
            onClick = { onChipTap(FilterId.FORMAT) }
        )
        FilterBarChip(
            label = pickChipLabel(
                airingStatusTitle(manga = category == ResultCategory.MANGA),
                filters.statuses.mapNotNull { status -> status.toLabel().takeIf { it.isNotEmpty() } }
            ),
            active = filters.statuses.isNotEmpty(),
            onClick = { onChipTap(FilterId.STATUS) }
        )
        val year = yearAndSeasonValue(filters)
        FilterBarChip(
            label = if (year == null) {
                stringResource(R.string.filter_year)
            } else {
                stringResource(R.string.filter_chip_value, stringResource(R.string.filter_year), year)
            },
            active = year != null,
            onClick = { onChipTap(FilterId.YEAR) }
        )
        val more = filters.moreCount()
        FilterBarChip(
            label = countChipLabel(stringResource(R.string.more), more),
            active = more > 0,
            onClick = { onChipTap(FilterId.MORE) }
        )
    }
}

/** "Fall 2024", "2020–2024", "2020+". Null when neither a season nor a year is set. */
@Composable
private fun yearAndSeasonValue(filters: SearchFilters): String? {
    val season = filters.season?.toLabel()
    val years = filters.yearRange.label()
    return listOfNotNull(season, years).joinToString(" ").ifEmpty { null }
}

internal fun IntRangeFilter.label(): String? = when {
    min != null && max != null && min == max -> "$min"
    min != null && max != null -> "$min–$max"
    min != null -> "$min+"
    max != null -> "≤$max"
    else -> null
}

/** The filters the More sheet holds, for its chip's count. */
private fun SearchFilters.moreCount(): Int = listOf(
    sources.isNotEmpty(),
    country != null,
    score.isActive,
    episodes.isActive,
    chapters.isActive,
    adultMode != AdultMode.ANY
).count { it }
