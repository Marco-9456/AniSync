@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ADULT_GENRES
import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.ComparatorMode
import com.anisync.android.domain.IntComparatorFilter
import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.MediaTag
import com.anisync.android.domain.OriginCountry
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchType
import com.anisync.android.domain.SortOption
import com.anisync.android.presentation.components.filtersheet.FilterOptionGroup
import com.anisync.android.presentation.components.filtersheet.FilterOptionRow
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaSource
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType
import java.util.Calendar

/**
 * Routes the user-tapped chip to the right filter sheet. Each sheet applies
 * edits live; the chip bar reflects state immediately.
 *
 * Unlike the previous design, `MORE` no longer routes into nested sheets —
 * its sub-filters now expand inline inside [MoreFiltersSheet], so there is
 * no `onOpenFilter` callback to plumb anymore.
 */
@Composable
fun SearchFilterSheetHost(
    openedFilter: FilterId?,
    filters: SearchFilters,
    mediaType: MediaType,
    genres: List<String>,
    tags: List<MediaTag>,
    showAdultContent: Boolean,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val effectiveMediaType = when (filters.searchType) {
        SearchType.MANGA -> MediaType.MANGA
        SearchType.ANIME -> MediaType.ANIME
        else -> mediaType
    }
    when (openedFilter) {
        FilterId.TYPE -> TypeFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.SORT -> SortFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.GENRES -> GenresFilterSheet(filters, genres, showAdultContent, onFiltersChange, onDismiss)
        FilterId.TAGS -> TagsFilterSheet(filters, tags, showAdultContent, onFiltersChange, onDismiss)
        FilterId.YEAR -> YearFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.FORMAT -> FormatFilterSheet(filters, effectiveMediaType, onFiltersChange, onDismiss)
        FilterId.MORE -> MoreFiltersSheet(filters, effectiveMediaType, onFiltersChange, onDismiss)
        // Legacy direct openings (currently unreachable from the chip bar but
        // kept addressable so deep links / future callers still resolve).
        FilterId.STATUS -> StatusFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.SEASON -> SeasonFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.SOURCE -> SourceFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.COUNTRY -> CountryFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.SCORE -> ScoreFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.EPISODES -> EpisodesFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.CHAPTERS -> ChaptersFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.ADULT -> AdultFilterSheet(filters, onFiltersChange, onDismiss)
        null -> Unit
    }
}

// =============================================================================
// TYPE
// =============================================================================

@Composable
private fun TypeFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Type",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(searchType = null)) },
        resetEnabled = filters.searchType != null
    ) {
        FilterOptionRow(
            label = "All",
            selected = filters.searchType == null,
            leading = if (filters.searchType == null) checkIcon() else null,
            onClick = { onFiltersChange(filters.copy(searchType = null)) }
        )
        SearchType.entries.forEach { type ->
            val selected = filters.searchType == type
            FilterOptionRow(
                label = type.label(),
                selected = selected,
                leading = if (selected) checkIcon() else null,
                onClick = { onFiltersChange(filters.copy(searchType = type)) }
            )
        }
    }
}

// =============================================================================
// COMPACT (chip) HELPERS used inside expanded FilterOptionGroup bodies
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        content = { content() }
    )
}

@Composable
private fun SheetFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else null
    )
}

// =============================================================================
// SORT
// =============================================================================

@Composable
private fun SortFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Sort by",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(sort = SortOption.POPULARITY_DESC)) },
        resetEnabled = filters.sort != SortOption.POPULARITY_DESC
    ) {
        SortOption.entries.forEach { option ->
            val selected = filters.sort == option
            FilterOptionRow(
                label = option.fullLabel(),
                selected = selected,
                leading = if (selected) checkIcon() else null,
                onClick = { onFiltersChange(filters.copy(sort = option)) }
            )
        }
    }
}

// =============================================================================
// GENRES — FlowRow of tri-state chips inside scaffold body
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenresFilterSheet(
    filters: SearchFilters,
    genres: List<String>,
    showAdult: Boolean,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val anyActive = filters.genresIncluded.isNotEmpty() || filters.genresExcluded.isNotEmpty()
    val visibleGenres = remember(genres, showAdult) {
        if (showAdult) genres else genres.filterNot { it in ADULT_GENRES }
    }
    FilterSheetScaffold(
        title = "Genres",
        onDismiss = onDismiss,
        onReset = {
            onFiltersChange(filters.copy(genresIncluded = emptySet(), genresExcluded = emptySet()))
        },
        resetEnabled = anyActive
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tap to include · Long-press to exclude",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (visibleGenres.isEmpty()) {
                Text(
                    "Loading genres…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleGenres.forEach { genre ->
                        val tri = when {
                            genre in filters.genresIncluded -> TriState.INCLUDED
                            genre in filters.genresExcluded -> TriState.EXCLUDED
                            else -> TriState.OFF
                        }
                        IncludeExcludeChip(
                            label = genre,
                            state = tri,
                            onStateChange = { newState ->
                                val newIn = if (newState == TriState.INCLUDED) filters.genresIncluded + genre
                                else filters.genresIncluded - genre
                                val newOut = if (newState == TriState.EXCLUDED) filters.genresExcluded + genre
                                else filters.genresExcluded - genre
                                onFiltersChange(
                                    filters.copy(genresIncluded = newIn, genresExcluded = newOut)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// TAGS — search field + per-category expandable group of FlowRow chips
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsFilterSheet(
    filters: SearchFilters,
    tags: List<MediaTag>,
    showAdult: Boolean,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val anyActive = filters.tagsIncluded.isNotEmpty() || filters.tagsExcluded.isNotEmpty()
    val visibleTags = remember(tags, showAdult, query) {
        val pool = if (showAdult) tags else tags.filterNot { it.isAdult }
        if (query.isBlank()) pool
        else pool.filter { it.name.contains(query, ignoreCase = true) }
    }
    val grouped = remember(visibleTags) {
        visibleTags.groupBy { it.category ?: "Other" }.toSortedMap()
    }

    FilterSheetScaffold(
        title = "Tags · ${filters.tagsIncluded.size}↑ ${filters.tagsExcluded.size}↓",
        onDismiss = onDismiss,
        onReset = {
            onFiltersChange(filters.copy(tagsIncluded = emptySet(), tagsExcluded = emptySet()))
        },
        resetEnabled = anyActive,
        scrollableBody = false
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search tags") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            grouped.forEach { (category, categoryTags) ->
                val includedInCat = categoryTags.count { it.name in filters.tagsIncluded }
                val excludedInCat = categoryTags.count { it.name in filters.tagsExcluded }
                val total = categoryTags.size
                val summary = when {
                    includedInCat == 0 && excludedInCat == 0 ->
                        "$total tag${if (total == 1) "" else "s"}"
                    excludedInCat == 0 -> "$includedInCat included"
                    includedInCat == 0 -> "$excludedInCat excluded"
                    else -> "$includedInCat included · $excludedInCat excluded"
                }
                item(key = "group-$category") {
                    FilterOptionGroup(
                        label = category,
                        summary = summary,
                        expanded = expandedCategory == category,
                        onToggle = {
                            expandedCategory = if (expandedCategory == category) null else category
                        }
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categoryTags.forEach { tag ->
                                val state = when {
                                    tag.name in filters.tagsIncluded -> TriState.INCLUDED
                                    tag.name in filters.tagsExcluded -> TriState.EXCLUDED
                                    else -> TriState.OFF
                                }
                                IncludeExcludeChip(
                                    label = tag.name,
                                    state = state,
                                    onStateChange = { newState ->
                                        val newIn = if (newState == TriState.INCLUDED) filters.tagsIncluded + tag.name
                                        else filters.tagsIncluded - tag.name
                                        val newEx = if (newState == TriState.EXCLUDED) filters.tagsExcluded + tag.name
                                        else filters.tagsExcluded - tag.name
                                        onFiltersChange(
                                            filters.copy(tagsIncluded = newIn, tagsExcluded = newEx)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (grouped.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (query.isBlank()) "No tags available"
                            else "No tags match \"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// YEAR
// =============================================================================

/**
 * Year filter sheet — Material 3 date-picker year-grid styling, restricted to
 * years (no month/day). Range: [1940, currentYear + 1].
 *
 * M3 has no public year-only picker (`androidx.compose.material3.YearPicker`
 * is private). This composable mirrors the spec/behavior of M3's internal
 * year grid: 3 columns, `bodyLarge` typography, circular cells,
 * `primary`-filled endpoints, `secondaryContainer`-filled in-range, 1dp
 * `outline` border for today's year when unselected.
 *
 * Tap state machine on the existing [IntRangeFilter] (`min == max` means
 * single-year selection):
 *   - empty → tap Y                → (Y, Y)              single year
 *   - (A, A) tap A                  → empty               deselect
 *   - (A, A) tap Y ≠ A              → (min(A,Y), max(A,Y)) grow to range
 *   - (A, B) A<B, tap A             → (B, B)              shrink to other endpoint
 *   - (A, B) A<B, tap B             → (A, A)              shrink to other endpoint
 *   - (A, B) A<B, tap any other Y   → (Y, Y)              restart with single
 */
@Composable
private fun YearFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Year",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(yearRange = IntRangeFilter())) },
        resetEnabled = filters.yearRange.isActive,
        scrollableBody = false
    ) {
        YearPickerBody(filters, onFiltersChange)
    }
}

@Composable
private fun YearPickerBody(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val maxYear = currentYear + 1
    val minYear = 1940
    val years = remember(maxYear) { (minYear..maxYear).toList().reversed() }

    val from = filters.yearRange.min
    val to = filters.yearRange.max
    val isRange = from != null && to != null && from != to
    val isSingle = from != null && to != null && from == to

    val onYearTap: (Int) -> Unit = { year ->
        val newRange = when {
            from == null || to == null -> IntRangeFilter(min = year, max = year)
            from == to && year == from -> IntRangeFilter()
            from == to -> IntRangeFilter(min = minOf(from, year), max = maxOf(from, year))
            year == from -> IntRangeFilter(min = to, max = to)
            year == to -> IntRangeFilter(min = from, max = from)
            else -> IntRangeFilter(min = year, max = year)
        }
        onFiltersChange(filters.copy(yearRange = newRange))
    }

    // Scroll initial position once: target = current selection start, else today.
    // Place target row with one row of context above when possible.
    val initialIdx = remember(maxYear) {
        val target = from ?: currentYear
        val itemIdx = (maxYear - target).coerceIn(0, years.size - 1)
        ((itemIdx / 3) - 1).coerceAtLeast(0) * 3
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIdx)

    val supporting = stringResource(
        when {
            isRange -> R.string.year_picker_hint_range
            isSingle -> R.string.year_picker_hint_single
            else -> R.string.year_picker_hint_empty
        }
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(items = years, key = { it }) { year ->
                val isEndpoint = year == from || year == to
                val inRange = from != null && to != null && from != to && year > from && year < to
                YearCell(
                    year = year,
                    isEndpoint = isEndpoint,
                    inRange = inRange,
                    isToday = year == currentYear && !isEndpoint,
                    onClick = { onYearTap(year) }
                )
            }
        }
    }
}

@Composable
private fun YearCell(
    year: Int,
    isEndpoint: Boolean,
    inRange: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val container = when {
        isEndpoint -> MaterialTheme.colorScheme.primary
        inRange -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val content = when {
        isEndpoint -> MaterialTheme.colorScheme.onPrimary
        inRange -> MaterialTheme.colorScheme.onSecondaryContainer
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border: BorderStroke? = if (isToday) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else null

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// =============================================================================
// FORMAT (row-based multi-select)
// =============================================================================

@Composable
private fun FormatFilterSheet(
    filters: SearchFilters,
    mediaType: MediaType,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Format",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(formats = emptySet())) },
        resetEnabled = filters.formats.isNotEmpty()
    ) {
        formatOptions(mediaType).forEach { format ->
            val selected = format in filters.formats
            FilterOptionRow(
                label = format.toLabel(),
                selected = selected,
                leading = if (selected) checkIcon() else null,
                onClick = {
                    onFiltersChange(
                        filters.copy(
                            formats = if (selected) filters.formats - format else filters.formats + format
                        )
                    )
                }
            )
        }
    }
}

private fun formatOptions(mediaType: MediaType): List<MediaFormat> =
    if (mediaType == MediaType.ANIME) {
        listOf(
            MediaFormat.TV, MediaFormat.TV_SHORT, MediaFormat.MOVIE,
            MediaFormat.SPECIAL, MediaFormat.OVA, MediaFormat.ONA, MediaFormat.MUSIC
        )
    } else {
        listOf(MediaFormat.MANGA, MediaFormat.NOVEL, MediaFormat.ONE_SHOT)
    }

// =============================================================================
// STATUS / SEASON / SOURCE / COUNTRY — row-based selects
// =============================================================================

@Composable
private fun StatusFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Status",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(statuses = emptySet())) },
        resetEnabled = filters.statuses.isNotEmpty()
    ) { StatusOptionList(filters, onFiltersChange) }
}

@Composable
private fun StatusOptionList(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    compact: Boolean = false
) {
    val entries = MediaStatus.entries.filter { it != MediaStatus.UNKNOWN__ }
    if (compact) {
        ChipsFlow {
            entries.forEach { status ->
                val selected = status in filters.statuses
                SheetFilterChip(
                    label = status.toLabel(),
                    selected = selected,
                    onClick = {
                        onFiltersChange(
                            filters.copy(
                                statuses = if (selected) filters.statuses - status else filters.statuses + status
                            )
                        )
                    }
                )
            }
        }
    } else {
        entries.forEach { status ->
            val selected = status in filters.statuses
            FilterOptionRow(
                label = status.toLabel(),
                selected = selected,
                leading = if (selected) checkIcon() else null,
                onClick = {
                    onFiltersChange(
                        filters.copy(
                            statuses = if (selected) filters.statuses - status else filters.statuses + status
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun SeasonFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Season",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(season = null)) },
        resetEnabled = filters.season != null
    ) { SeasonOptionList(filters, onFiltersChange) }
}

@Composable
private fun SeasonOptionList(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    compact: Boolean = false
) {
    val entries = MediaSeason.entries.filter { it != MediaSeason.UNKNOWN__ }
    if (compact) {
        ChipsFlow {
            entries.forEach { season ->
                val selected = filters.season == season
                SheetFilterChip(
                    label = season.label(),
                    selected = selected,
                    onClick = {
                        onFiltersChange(filters.copy(season = if (selected) null else season))
                    }
                )
            }
        }
    } else {
        entries.forEach { season ->
            val selected = filters.season == season
            FilterOptionRow(
                label = season.label(),
                selected = selected,
                leading = if (selected) checkIcon() else null,
                onClick = { onFiltersChange(filters.copy(season = if (selected) null else season)) }
            )
        }
    }
}

@Composable
private fun SourceFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Source",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(sources = emptySet())) },
        resetEnabled = filters.sources.isNotEmpty()
    ) { SourceOptionList(filters, onFiltersChange) }
}

@Composable
private fun SourceOptionList(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    compact: Boolean = false
) {
    val entries = MediaSource.entries.filter { it != MediaSource.UNKNOWN__ }
    if (compact) {
        ChipsFlow {
            entries.forEach { source ->
                val selected = source in filters.sources
                SheetFilterChip(
                    label = source.label(),
                    selected = selected,
                    onClick = {
                        onFiltersChange(
                            filters.copy(
                                sources = if (selected) filters.sources - source else filters.sources + source
                            )
                        )
                    }
                )
            }
        }
    } else {
        entries.forEach { source ->
            val selected = source in filters.sources
            FilterOptionRow(
                label = source.label(),
                selected = selected,
                leading = if (selected) checkIcon() else null,
                onClick = {
                    onFiltersChange(
                        filters.copy(
                            sources = if (selected) filters.sources - source else filters.sources + source
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun CountryFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Country of origin",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(country = null)) },
        resetEnabled = filters.country != null
    ) { CountryOptionList(filters, onFiltersChange) }
}

@Composable
private fun CountryOptionList(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    compact: Boolean = false
) {
    if (compact) {
        ChipsFlow {
            OriginCountry.entries.forEach { country ->
                val selected = filters.country == country
                SheetFilterChip(
                    label = country.displayName,
                    selected = selected,
                    onClick = {
                        onFiltersChange(filters.copy(country = if (selected) null else country))
                    }
                )
            }
        }
    } else {
        OriginCountry.entries.forEach { country ->
            val selected = filters.country == country
            FilterOptionRow(
                label = country.displayName,
                selected = selected,
                leading = if (selected) checkIcon() else null,
                onClick = { onFiltersChange(filters.copy(country = if (selected) null else country)) }
            )
        }
    }
}

// =============================================================================
// COMPARATOR (Score / Episodes / Chapters)
// =============================================================================

@Composable
private fun ScoreFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Mean score",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(score = IntComparatorFilter())) },
        resetEnabled = filters.score.isActive
    ) {
        ComparatorBody(
            filter = filters.score,
            valueBounds = 0..100,
            step = 5,
            defaultValue = 75,
            valueSuffix = "%",
            onChange = { onFiltersChange(filters.copy(score = it)) }
        )
    }
}

@Composable
private fun EpisodesFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Episodes",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(episodes = IntComparatorFilter())) },
        resetEnabled = filters.episodes.isActive
    ) {
        ComparatorBody(
            filter = filters.episodes,
            valueBounds = 1..200,
            step = 1,
            defaultValue = 12,
            valueSuffix = "",
            onChange = { onFiltersChange(filters.copy(episodes = it)) }
        )
    }
}

@Composable
private fun ChaptersFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Chapters",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(chapters = IntComparatorFilter())) },
        resetEnabled = filters.chapters.isActive
    ) {
        ComparatorBody(
            filter = filters.chapters,
            valueBounds = 1..1000,
            step = 1,
            defaultValue = 50,
            valueSuffix = "",
            onChange = { onFiltersChange(filters.copy(chapters = it)) }
        )
    }
}

/**
 * Comparator body: mode rows + wheel picker. The mode buttons are full-width
 * [FilterOptionRow]s (matching the rest of the sheet design) instead of the
 * old chip row.
 */
@Composable
private fun ComparatorBody(
    filter: IntComparatorFilter,
    valueBounds: IntRange,
    step: Int,
    defaultValue: Int,
    valueSuffix: String,
    onChange: (IntComparatorFilter) -> Unit,
    compact: Boolean = false
) {
    val values = remember(valueBounds, step) {
        (valueBounds.first..valueBounds.last step step).toList()
    }
    val labels = remember(values, valueSuffix) { values.map { "$it$valueSuffix" } }
    val currentValue = filter.value ?: defaultValue
    val initialIdx = values.indexOf(currentValue).coerceAtLeast(0)

    val modeLabel: (ComparatorMode) -> String = { mode ->
        when (mode) {
            ComparatorMode.ANY -> "Any"
            ComparatorMode.AT_LEAST -> "At least"
            ComparatorMode.AT_MOST -> "At most"
            ComparatorMode.EXACTLY -> "Exactly"
        }
    }
    if (compact) {
        ChipsFlow {
            ComparatorMode.entries.forEach { mode ->
                val selected = filter.mode == mode
                SheetFilterChip(
                    label = modeLabel(mode),
                    selected = selected,
                    onClick = {
                        val newValue = if (mode == ComparatorMode.ANY) null
                        else (filter.value ?: defaultValue)
                        onChange(IntComparatorFilter(mode = mode, value = newValue))
                    }
                )
            }
        }
    } else {
        ComparatorMode.entries.forEach { mode ->
            val selected = filter.mode == mode
            FilterOptionRow(
                label = modeLabel(mode),
                selected = selected,
                leading = if (selected) checkIcon() else null,
                onClick = {
                    val newValue = if (mode == ComparatorMode.ANY) null
                    else (filter.value ?: defaultValue)
                    onChange(IntComparatorFilter(mode = mode, value = newValue))
                }
            )
        }
    }
    if (filter.mode != ComparatorMode.ANY) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 12.dp else 24.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(width = 0.dp, height = if (compact) 160.dp else 200.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                WheelPicker(
                    items = labels,
                    selectedIndex = initialIdx,
                    onSelectedIndexChange = { idx -> onChange(filter.copy(value = values[idx])) }
                )
            }
        }
    }
}

// =============================================================================
// ADULT
// =============================================================================

@Composable
private fun AdultFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = "Adult content",
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(adultMode = AdultMode.ANY)) },
        resetEnabled = filters.adultMode != AdultMode.ANY
    ) {
        Text(
            text = "Doesn't reveal NSFW tags. To browse them, enable Show adult content in Settings → Look and Feel.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        AdultOptionList(filters, onFiltersChange)
    }
}

@Composable
private fun AdultOptionList(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    compact: Boolean = false
) {
    if (compact) {
        ChipsFlow {
            AdultMode.entries.forEach { mode ->
                val selected = filters.adultMode == mode
                SheetFilterChip(
                    label = mode.label(),
                    selected = selected,
                    onClick = { onFiltersChange(filters.copy(adultMode = mode)) }
                )
            }
        }
    } else {
        AdultMode.entries.forEach { mode ->
            val selected = filters.adultMode == mode
            FilterOptionRow(
                label = mode.label(),
                selected = selected,
                leading = if (selected) checkIcon() else null,
                onClick = { onFiltersChange(filters.copy(adultMode = mode)) }
            )
        }
    }
}

// =============================================================================
// MORE — single sheet, inline-expanding groups, no nested sheets
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreFiltersSheet(
    filters: SearchFilters,
    mediaType: MediaType,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val isAnime = mediaType == MediaType.ANIME
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    val anyActive = filters.statuses.isNotEmpty() ||
        filters.season != null ||
        filters.sources.isNotEmpty() ||
        filters.country != null ||
        filters.score.isActive ||
        filters.episodes.isActive ||
        filters.chapters.isActive ||
        filters.adultMode != AdultMode.ANY

    FilterSheetScaffold(
        title = "More filters",
        onDismiss = onDismiss,
        onReset = {
            onFiltersChange(
                filters.copy(
                    statuses = emptySet(),
                    season = null,
                    sources = emptySet(),
                    country = null,
                    score = IntComparatorFilter(),
                    episodes = IntComparatorFilter(),
                    chapters = IntComparatorFilter(),
                    adultMode = AdultMode.ANY
                )
            )
        },
        resetEnabled = anyActive
    ) {
        FilterOptionGroup(
            label = "Status",
            summary = filters.statusesSummary(),
            expanded = expanded == "status",
            onToggle = { expanded = if (expanded == "status") null else "status" }
        ) { StatusOptionList(filters, onFiltersChange, compact = true) }

        if (isAnime) {
            FilterOptionGroup(
                label = "Season",
                summary = filters.season?.label() ?: "Any",
                expanded = expanded == "season",
                onToggle = { expanded = if (expanded == "season") null else "season" }
            ) { SeasonOptionList(filters, onFiltersChange, compact = true) }
        }

        FilterOptionGroup(
            label = "Source",
            summary = filters.sourcesSummary(),
            expanded = expanded == "source",
            onToggle = { expanded = if (expanded == "source") null else "source" }
        ) { SourceOptionList(filters, onFiltersChange, compact = true) }

        FilterOptionGroup(
            label = "Country",
            summary = filters.country?.displayName ?: "Any",
            expanded = expanded == "country",
            onToggle = { expanded = if (expanded == "country") null else "country" }
        ) { CountryOptionList(filters, onFiltersChange, compact = true) }

        FilterOptionGroup(
            label = "Mean score",
            summary = filters.score.summary("%"),
            expanded = expanded == "score",
            onToggle = { expanded = if (expanded == "score") null else "score" }
        ) {
            ComparatorBody(
                filter = filters.score,
                valueBounds = 0..100,
                step = 5,
                defaultValue = 75,
                valueSuffix = "%",
                onChange = { onFiltersChange(filters.copy(score = it)) },
                compact = true
            )
        }

        if (isAnime) {
            FilterOptionGroup(
                label = "Episodes",
                summary = filters.episodes.summary(),
                expanded = expanded == "episodes",
                onToggle = { expanded = if (expanded == "episodes") null else "episodes" }
            ) {
                ComparatorBody(
                    filter = filters.episodes,
                    valueBounds = 1..200,
                    step = 1,
                    defaultValue = 12,
                    valueSuffix = "",
                    onChange = { onFiltersChange(filters.copy(episodes = it)) },
                    compact = true
                )
            }
        } else {
            FilterOptionGroup(
                label = "Chapters",
                summary = filters.chapters.summary(),
                expanded = expanded == "chapters",
                onToggle = { expanded = if (expanded == "chapters") null else "chapters" }
            ) {
                ComparatorBody(
                    filter = filters.chapters,
                    valueBounds = 1..1000,
                    step = 1,
                    defaultValue = 50,
                    valueSuffix = "",
                    onChange = { onFiltersChange(filters.copy(chapters = it)) },
                    compact = true
                )
            }
        }

        FilterOptionGroup(
            label = "Adult content",
            summary = filters.adultMode.label(),
            expanded = expanded == "adult",
            onToggle = { expanded = if (expanded == "adult") null else "adult" }
        ) { AdultOptionList(filters, onFiltersChange, compact = true) }
    }
}

// =============================================================================
// HELPERS
// =============================================================================

private fun checkIcon(): @Composable () -> Unit = {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

internal fun SortOption.fullLabel(): String = when (this) {
    SortOption.POPULARITY_DESC -> "Popularity"
    SortOption.SCORE_DESC -> "Highest score"
    SortOption.TRENDING_DESC -> "Trending now"
    SortOption.FAVOURITES_DESC -> "Most favourited"
    SortOption.START_DATE_DESC -> "Recently released"
    SortOption.START_DATE -> "Earliest released"
    SortOption.UPDATED_AT_DESC -> "Recently updated"
    SortOption.TITLE_ROMAJI -> "Title (Romaji A–Z)"
    SortOption.TITLE_ENGLISH -> "Title (English A–Z)"
    SortOption.EPISODES_DESC -> "Most episodes"
    SortOption.DURATION_DESC -> "Longest runtime"
    SortOption.CHAPTERS_DESC -> "Most chapters"
    SortOption.VOLUMES_DESC -> "Most volumes"
}

internal fun MediaSeason.label(): String = when (this) {
    MediaSeason.WINTER -> "Winter"
    MediaSeason.SPRING -> "Spring"
    MediaSeason.SUMMER -> "Summer"
    MediaSeason.FALL -> "Fall"
    MediaSeason.UNKNOWN__ -> ""
}

internal fun MediaSource.label(): String = when (this) {
    MediaSource.ORIGINAL -> "Original"
    MediaSource.MANGA -> "Manga"
    MediaSource.LIGHT_NOVEL -> "Light Novel"
    MediaSource.VISUAL_NOVEL -> "Visual Novel"
    MediaSource.VIDEO_GAME -> "Video Game"
    MediaSource.NOVEL -> "Novel"
    MediaSource.DOUJINSHI -> "Doujinshi"
    MediaSource.ANIME -> "Anime"
    MediaSource.WEB_NOVEL -> "Web Novel"
    MediaSource.LIVE_ACTION -> "Live Action"
    MediaSource.GAME -> "Game"
    MediaSource.COMIC -> "Comic"
    MediaSource.MULTIMEDIA_PROJECT -> "Multimedia Project"
    MediaSource.PICTURE_BOOK -> "Picture Book"
    MediaSource.OTHER -> "Other"
    MediaSource.UNKNOWN__ -> ""
}

internal fun AdultMode.label(): String = when (this) {
    AdultMode.ANY -> "Any"
    AdultMode.HIDE -> "Hide adult"
    AdultMode.ONLY -> "Adult only"
}

private fun MediaStatus.plainLabel(): String = when (this) {
    MediaStatus.RELEASING -> "Releasing"
    MediaStatus.FINISHED -> "Finished"
    MediaStatus.NOT_YET_RELEASED -> "Not yet released"
    MediaStatus.CANCELLED -> "Cancelled"
    MediaStatus.HIATUS -> "Hiatus"
    MediaStatus.UNKNOWN__ -> ""
}

private fun SearchFilters.statusesSummary(): String = when (statuses.size) {
    0 -> "Any"
    1 -> statuses.first().plainLabel()
    else -> "${statuses.first().plainLabel()} +${statuses.size - 1}"
}

private fun SearchFilters.sourcesSummary(): String = when (sources.size) {
    0 -> "Any"
    1 -> sources.first().label()
    else -> "${sources.first().label()} +${sources.size - 1}"
}

private fun IntComparatorFilter.summary(suffix: String = ""): String = when (mode) {
    ComparatorMode.ANY -> "Any"
    ComparatorMode.AT_LEAST -> "≥ ${value ?: "?"}$suffix"
    ComparatorMode.AT_MOST -> "≤ ${value ?: "?"}$suffix"
    ComparatorMode.EXACTLY -> "= ${value ?: "?"}$suffix"
}
