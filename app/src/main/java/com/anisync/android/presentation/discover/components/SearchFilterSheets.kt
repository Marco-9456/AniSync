package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.ComparatorMode
import com.anisync.android.domain.IntComparatorFilter
import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.MediaTag
import com.anisync.android.domain.OriginCountry
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SortOption
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaSource
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType
import java.util.Calendar

/**
 * Routes the user-tapped chip to the right ModalBottomSheet. Caller owns
 * `openedFilter` state and clears it (via [onDismiss]) when the sheet closes.
 * Each sheet applies edits live; the chip bar reflects state immediately.
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
    onOpenFilter: (FilterId?) -> Unit,
    onDismiss: () -> Unit
) {
    when (openedFilter) {
        FilterId.SORT -> SortFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.GENRES -> GenresFilterSheet(filters, genres, onFiltersChange, onDismiss)
        FilterId.TAGS -> TagsFilterSheet(filters, tags, showAdultContent, onFiltersChange, onDismiss)
        FilterId.YEAR -> YearFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.FORMAT -> FormatFilterSheet(filters, mediaType, onFiltersChange, onDismiss)
        FilterId.STATUS -> StatusFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.SEASON -> SeasonFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.SOURCE -> SourceFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.COUNTRY -> CountryFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.SCORE -> ScoreFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.EPISODES -> EpisodesFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.CHAPTERS -> ChaptersFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.ADULT -> AdultFilterSheet(filters, onFiltersChange, onDismiss)
        FilterId.MORE -> MoreFiltersSheet(filters, mediaType, onOpenFilter, onDismiss)
        null -> Unit
    }
}

// =============================================================================
// SHARED SHEET CHROME
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    title: String,
    onReset: (() -> Unit)?,
    resetEnabled: Boolean,
    onDismiss: () -> Unit,
    bodyPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (onReset != null) {
                    TextButton(
                        onClick = onReset,
                        enabled = resetEnabled
                    ) {
                        Text(
                            "Reset",
                            color = if (resetEnabled) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Box(modifier = Modifier.padding(bodyPadding)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheetTall(
    title: String,
    onReset: (() -> Unit)?,
    resetEnabled: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (onReset != null) {
                    TextButton(
                        onClick = onReset,
                        enabled = resetEnabled
                    ) {
                        Text(
                            "Reset",
                            color = if (resetEnabled) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            content()
        }
    }
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
    FilterSheet(
        title = "Sort by",
        onReset = { onFiltersChange(filters.copy(sort = SortOption.POPULARITY_DESC)) },
        resetEnabled = filters.sort != SortOption.POPULARITY_DESC,
        onDismiss = onDismiss,
        bodyPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column {
            SortOption.entries.forEach { option ->
                val selected = filters.sort == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFiltersChange(filters.copy(sort = option)) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = option.fullLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (selected) Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// =============================================================================
// GENRES
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenresFilterSheet(
    filters: SearchFilters,
    genres: List<String>,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val anyActive = filters.genresIncluded.isNotEmpty() || filters.genresExcluded.isNotEmpty()
    FilterSheet(
        title = "Genres",
        onReset = {
            onFiltersChange(
                filters.copy(genresIncluded = emptySet(), genresExcluded = emptySet())
            )
        },
        resetEnabled = anyActive,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tap to include · Long-press to exclude",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (genres.isEmpty()) {
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
                    genres.forEach { genre ->
                        val tri = when {
                            genre in filters.genresIncluded -> TriState.INCLUDED
                            genre in filters.genresExcluded -> TriState.EXCLUDED
                            else -> TriState.OFF
                        }
                        IncludeExcludeChip(
                            label = genre,
                            state = tri,
                            onStateChange = { newState ->
                                val newIn = when (newState) {
                                    TriState.INCLUDED -> filters.genresIncluded + genre
                                    else -> filters.genresIncluded - genre
                                }
                                val newOut = when (newState) {
                                    TriState.EXCLUDED -> filters.genresExcluded + genre
                                    else -> filters.genresExcluded - genre
                                }
                                onFiltersChange(
                                    filters.copy(
                                        genresIncluded = newIn,
                                        genresExcluded = newOut
                                    )
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
// TAGS
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
    var query by remember { mutableStateOf("") }
    val anyActive = filters.tagsIncluded.isNotEmpty() || filters.tagsExcluded.isNotEmpty()
    val visibleTags = remember(tags, showAdult, query) {
        val pool = if (showAdult) tags else tags.filterNot { it.isAdult }
        if (query.isBlank()) pool
        else pool.filter { it.name.contains(query, ignoreCase = true) }
    }
    val grouped = remember(visibleTags) {
        visibleTags.groupBy { it.category ?: "Other" }.toSortedMap()
    }

    FilterSheetTall(
        title = "Tags · ${filters.tagsIncluded.size}↑ ${filters.tagsExcluded.size}↓",
        onReset = {
            onFiltersChange(filters.copy(tagsIncluded = emptySet(), tagsExcluded = emptySet()))
        },
        resetEnabled = anyActive,
        onDismiss = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                grouped.forEach { (category, categoryTags) ->
                    item(key = "header-$category") {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item(key = "flow-$category") {
                        FlowRow(
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
                                        val newIn = when (newState) {
                                            TriState.INCLUDED -> filters.tagsIncluded + tag.name
                                            else -> filters.tagsIncluded - tag.name
                                        }
                                        val newEx = when (newState) {
                                            TriState.EXCLUDED -> filters.tagsExcluded + tag.name
                                            else -> filters.tagsExcluded - tag.name
                                        }
                                        onFiltersChange(
                                            filters.copy(
                                                tagsIncluded = newIn,
                                                tagsExcluded = newEx
                                            )
                                        )
                                    }
                                )
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
}

// =============================================================================
// YEAR
// =============================================================================

@Composable
private fun YearFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val maxYear = currentYear + 1
    val minYear = 1940
    val years = remember { (minYear..maxYear).toList() }
    val labels = remember(years) { years.map { it.toString() } }

    val fromIdx = (filters.yearRange.min ?: minYear) - minYear
    val toIdx = (filters.yearRange.max ?: maxYear) - minYear

    FilterSheet(
        title = "Year",
        onReset = { onFiltersChange(filters.copy(yearRange = IntRangeFilter())) },
        resetEnabled = filters.yearRange.isActive,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                YearWheelColumn(
                    label = "From",
                    items = labels,
                    selectedIndex = fromIdx.coerceIn(0, years.lastIndex),
                    onSelect = { idx ->
                        val newMin = years[idx]
                        val newMax = filters.yearRange.max?.coerceAtLeast(newMin) ?: filters.yearRange.max
                        onFiltersChange(filters.copy(yearRange = filters.yearRange.copy(min = newMin, max = newMax)))
                    }
                )
                YearWheelColumn(
                    label = "To",
                    items = labels,
                    selectedIndex = toIdx.coerceIn(0, years.lastIndex),
                    onSelect = { idx ->
                        val newMax = years[idx]
                        val newMin = filters.yearRange.min?.coerceAtMost(newMax) ?: filters.yearRange.min
                        onFiltersChange(filters.copy(yearRange = filters.yearRange.copy(min = newMin, max = newMax)))
                    }
                )
            }
        }
    }
}

@Composable
private fun YearWheelColumn(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.size(width = 120.dp, height = 200.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            WheelPicker(
                items = items,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = onSelect
            )
        }
    }
}

// =============================================================================
// FORMAT, STATUS, SEASON, SOURCE, COUNTRY  (chip-based)
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatFilterSheet(
    filters: SearchFilters,
    mediaType: MediaType,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val available = if (mediaType == MediaType.ANIME) {
        listOf(
            MediaFormat.TV, MediaFormat.TV_SHORT, MediaFormat.MOVIE,
            MediaFormat.SPECIAL, MediaFormat.OVA, MediaFormat.ONA, MediaFormat.MUSIC
        )
    } else {
        listOf(MediaFormat.MANGA, MediaFormat.NOVEL, MediaFormat.ONE_SHOT)
    }
    FilterSheet(
        title = "Format",
        onReset = { onFiltersChange(filters.copy(formats = emptySet())) },
        resetEnabled = filters.formats.isNotEmpty(),
        onDismiss = onDismiss
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            available.forEach { format ->
                val selected = format in filters.formats
                FilterChip(
                    selected = selected,
                    onClick = {
                        onFiltersChange(
                            filters.copy(
                                formats = if (selected) filters.formats - format
                                else filters.formats + format
                            )
                        )
                    },
                    label = { Text(format.toLabel()) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheet(
        title = "Status",
        onReset = { onFiltersChange(filters.copy(statuses = emptySet())) },
        resetEnabled = filters.statuses.isNotEmpty(),
        onDismiss = onDismiss
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaStatus.entries.filter { it != MediaStatus.UNKNOWN__ }.forEach { status ->
                val selected = status in filters.statuses
                FilterChip(
                    selected = selected,
                    onClick = {
                        onFiltersChange(
                            filters.copy(
                                statuses = if (selected) filters.statuses - status
                                else filters.statuses + status
                            )
                        )
                    },
                    label = { Text(status.toLabel()) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheet(
        title = "Season",
        onReset = { onFiltersChange(filters.copy(season = null)) },
        resetEnabled = filters.season != null,
        onDismiss = onDismiss
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaSeason.entries.filter { it != MediaSeason.UNKNOWN__ }.forEach { season ->
                val selected = filters.season == season
                FilterChip(
                    selected = selected,
                    onClick = {
                        onFiltersChange(filters.copy(season = if (selected) null else season))
                    },
                    label = { Text(season.label()) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheet(
        title = "Source",
        onReset = { onFiltersChange(filters.copy(sources = emptySet())) },
        resetEnabled = filters.sources.isNotEmpty(),
        onDismiss = onDismiss
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaSource.entries.filter { it != MediaSource.UNKNOWN__ }.forEach { source ->
                val selected = source in filters.sources
                FilterChip(
                    selected = selected,
                    onClick = {
                        onFiltersChange(
                            filters.copy(
                                sources = if (selected) filters.sources - source
                                else filters.sources + source
                            )
                        )
                    },
                    label = { Text(source.label()) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountryFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheet(
        title = "Country of origin",
        onReset = { onFiltersChange(filters.copy(country = null)) },
        resetEnabled = filters.country != null,
        onDismiss = onDismiss
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OriginCountry.entries.forEach { country ->
                val selected = filters.country == country
                FilterChip(
                    selected = selected,
                    onClick = {
                        onFiltersChange(filters.copy(country = if (selected) null else country))
                    },
                    label = { Text(country.displayName) }
                )
            }
        }
    }
}

// =============================================================================
// SCORE / EPISODES / CHAPTERS  (comparator + wheel)
// =============================================================================

@Composable
private fun ScoreFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    ComparatorWheelSheet(
        title = "Mean score",
        filter = filters.score,
        valueBounds = 0..100,
        step = 5,
        defaultValue = 75,
        valueSuffix = "%",
        onChange = { onFiltersChange(filters.copy(score = it)) },
        onDismiss = onDismiss
    )
}

@Composable
private fun EpisodesFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    ComparatorWheelSheet(
        title = "Episodes",
        filter = filters.episodes,
        valueBounds = 1..200,
        step = 1,
        defaultValue = 12,
        valueSuffix = "",
        onChange = { onFiltersChange(filters.copy(episodes = it)) },
        onDismiss = onDismiss
    )
}

@Composable
private fun ChaptersFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    ComparatorWheelSheet(
        title = "Chapters",
        filter = filters.chapters,
        valueBounds = 1..1000,
        step = 1,
        defaultValue = 50,
        valueSuffix = "",
        onChange = { onFiltersChange(filters.copy(chapters = it)) },
        onDismiss = onDismiss
    )
}

@Composable
private fun ComparatorWheelSheet(
    title: String,
    filter: IntComparatorFilter,
    valueBounds: IntRange,
    step: Int,
    defaultValue: Int,
    valueSuffix: String,
    onChange: (IntComparatorFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val values = remember(valueBounds, step) {
        (valueBounds.first..valueBounds.last step step).toList()
    }
    val labels = remember(values, valueSuffix) {
        values.map { "$it$valueSuffix" }
    }
    val currentValue = filter.value ?: defaultValue
    val initialIdx = values.indexOf(currentValue).coerceAtLeast(0)

    FilterSheet(
        title = title,
        onReset = { onChange(IntComparatorFilter()) },
        resetEnabled = filter.isActive,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ComparatorChipRow(
                mode = filter.mode,
                onModeChange = { newMode ->
                    val newValue = if (newMode == ComparatorMode.ANY) null else (filter.value ?: defaultValue)
                    onChange(IntComparatorFilter(mode = newMode, value = newValue))
                },
                valueLabel = "${filter.value ?: defaultValue}$valueSuffix"
            )
            if (filter.mode != ComparatorMode.ANY) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(width = 0.dp, height = 200.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    WheelPicker(
                        items = labels,
                        selectedIndex = initialIdx,
                        onSelectedIndexChange = { idx ->
                            onChange(filter.copy(value = values[idx]))
                        }
                    )
                }
            } else {
                Text(
                    text = "Any score is accepted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =============================================================================
// ADULT
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdultFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheet(
        title = "Adult content",
        onReset = { onFiltersChange(filters.copy(adultMode = AdultMode.ANY)) },
        resetEnabled = filters.adultMode != AdultMode.ANY,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Doesn't reveal NSFW tags. To browse them, enable Show adult content in Settings → Look and Feel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdultMode.entries.forEach { mode ->
                    FilterChip(
                        selected = filters.adultMode == mode,
                        onClick = { onFiltersChange(filters.copy(adultMode = mode)) },
                        label = { Text(mode.label()) }
                    )
                }
            }
        }
    }
}

// =============================================================================
// MORE (overflow router)
// =============================================================================

@Composable
private fun MoreFiltersSheet(
    filters: SearchFilters,
    mediaType: MediaType,
    onOpenFilter: (FilterId?) -> Unit,
    onDismiss: () -> Unit
) {
    val isAnime = mediaType == MediaType.ANIME
    val rows = buildList {
        add(MoreRow(FilterId.STATUS, "Status", filters.statusesSummary()))
        add(MoreRow(FilterId.SEASON, "Season", filters.season?.label() ?: "Any"))
        add(MoreRow(FilterId.SOURCE, "Source", filters.sourcesSummary()))
        add(MoreRow(FilterId.COUNTRY, "Country", filters.country?.displayName ?: "Any"))
        add(MoreRow(FilterId.SCORE, "Mean score", filters.score.summary("%")))
        if (isAnime) {
            add(MoreRow(FilterId.EPISODES, "Episodes", filters.episodes.summary()))
        } else {
            add(MoreRow(FilterId.CHAPTERS, "Chapters", filters.chapters.summary()))
        }
        add(MoreRow(FilterId.ADULT, "Adult content", filters.adultMode.label()))
    }

    FilterSheet(
        title = "More filters",
        onReset = null,
        resetEnabled = false,
        onDismiss = onDismiss,
        bodyPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenFilter(row.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.padding(end = 12.dp)) {
                        Text(
                            text = row.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = row.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class MoreRow(val id: FilterId, val title: String, val summary: String)

// =============================================================================
// LABEL HELPERS
// =============================================================================

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
