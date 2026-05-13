package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchType
import com.anisync.android.domain.SortOption

/**
 * Identifies which filter the user opened from the chip bar.
 *
 * `TYPE` overrides the screen-level Anime/Manga selector when set to a
 * non-null value. When [SearchFilters.isNonMediaType] is true, the media-only
 * filters (genres/tags/year/format/more) are hidden from the bar entirely so
 * the user can't open filters that would no-op against a character/staff/
 * user/studio result set.
 */
enum class FilterId {
    TYPE, SORT, GENRES, TAGS, YEAR, FORMAT, MORE,
    STATUS, SEASON, SOURCE, COUNTRY, SCORE, EPISODES, CHAPTERS, ADULT
}

@Composable
fun SearchFilterChipBar(
    filters: SearchFilters,
    onChipTap: (FilterId) -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterBarChip(
            label = filters.typeChipLabel(),
            active = filters.searchType != null,
            onClick = { onChipTap(FilterId.TYPE) }
        )
        if (!filters.isNonMediaType) {
            FilterBarChip(
                label = "Sort · ${filters.sort.shortLabel()}",
                active = filters.sort != SortOption.POPULARITY_DESC,
                onClick = { onChipTap(FilterId.SORT) }
            )
            val genresActive = filters.genresIncluded.size + filters.genresExcluded.size
            FilterBarChip(
                label = if (genresActive == 0) "Genres" else "Genres · $genresActive",
                active = genresActive > 0,
                onClick = { onChipTap(FilterId.GENRES) }
            )
            val tagsActive = filters.tagsIncluded.size + filters.tagsExcluded.size
            FilterBarChip(
                label = if (tagsActive == 0) "Tags" else "Tags · $tagsActive",
                active = tagsActive > 0,
                onClick = { onChipTap(FilterId.TAGS) }
            )
            FilterBarChip(
                label = filters.yearChipLabel(),
                active = filters.yearRange.isActive,
                onClick = { onChipTap(FilterId.YEAR) }
            )
            FilterBarChip(
                label = filters.formatChipLabel(),
                active = filters.formats.isNotEmpty(),
                onClick = { onChipTap(FilterId.FORMAT) }
            )
            val moreCount = filters.moreCount()
            FilterBarChip(
                label = if (moreCount == 0) "More" else "More · $moreCount",
                active = moreCount > 0,
                onClick = { onChipTap(FilterId.MORE) }
            )
        }
    }
}

@Composable
private fun FilterBarChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

private fun SearchFilters.typeChipLabel(): String =
    "Type · ${searchType?.label() ?: "All"}"

internal fun SearchType.label(): String = when (this) {
    SearchType.ANIME -> "Anime"
    SearchType.MANGA -> "Manga"
    SearchType.CHARACTERS -> "Characters"
    SearchType.STAFF -> "Staff"
    SearchType.USERS -> "Users"
    SearchType.STUDIOS -> "Studios"
}

private fun SearchFilters.yearChipLabel(): String = when {
    yearRange.min != null && yearRange.max != null && yearRange.min == yearRange.max ->
        "Year · ${yearRange.min}"
    yearRange.min != null && yearRange.max != null -> "Year · ${yearRange.min}–${yearRange.max}"
    yearRange.min != null -> "Year · ${yearRange.min}+"
    yearRange.max != null -> "Year · ≤${yearRange.max}"
    else -> "Year"
}

private fun SearchFilters.formatChipLabel(): String = when (formats.size) {
    0 -> "Format"
    1 -> "Format · ${formats.first().shortName()}"
    else -> "Format · ${formats.first().shortName()} +${formats.size - 1}"
}

private fun SearchFilters.moreCount(): Int = listOf(
    statuses.isNotEmpty(),
    season != null,
    sources.isNotEmpty(),
    country != null,
    score.isActive,
    episodes.isActive,
    chapters.isActive,
    adultMode != com.anisync.android.domain.AdultMode.ANY
).count { it }

private fun SortOption.shortLabel(): String = when (this) {
    SortOption.POPULARITY_DESC -> "Popularity"
    SortOption.SCORE_DESC -> "Score"
    SortOption.TRENDING_DESC -> "Trending"
    SortOption.FAVOURITES_DESC -> "Favourites"
    SortOption.START_DATE_DESC -> "Newest"
    SortOption.START_DATE -> "Oldest"
    SortOption.UPDATED_AT_DESC -> "Updated"
    SortOption.TITLE_ROMAJI -> "Title (Romaji)"
    SortOption.TITLE_ENGLISH -> "Title (English)"
    SortOption.EPISODES_DESC -> "Episodes"
    SortOption.DURATION_DESC -> "Duration"
    SortOption.CHAPTERS_DESC -> "Chapters"
    SortOption.VOLUMES_DESC -> "Volumes"
}

private fun com.anisync.android.type.MediaFormat.shortName(): String = when (this) {
    com.anisync.android.type.MediaFormat.TV -> "TV"
    com.anisync.android.type.MediaFormat.TV_SHORT -> "TV Short"
    com.anisync.android.type.MediaFormat.MOVIE -> "Movie"
    com.anisync.android.type.MediaFormat.SPECIAL -> "Special"
    com.anisync.android.type.MediaFormat.OVA -> "OVA"
    com.anisync.android.type.MediaFormat.ONA -> "ONA"
    com.anisync.android.type.MediaFormat.MUSIC -> "Music"
    com.anisync.android.type.MediaFormat.MANGA -> "Manga"
    com.anisync.android.type.MediaFormat.NOVEL -> "Novel"
    com.anisync.android.type.MediaFormat.ONE_SHOT -> "One-shot"
    else -> name
}
