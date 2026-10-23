@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ADULT_GENRES
import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.ComparatorMode
import com.anisync.android.domain.IntComparatorFilter
import com.anisync.android.domain.IntRangeFilter
import com.anisync.android.domain.MediaTag
import com.anisync.android.domain.OriginCountry
import com.anisync.android.domain.ResultCategory
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchSort
import com.anisync.android.domain.SearchSortColumn
import com.anisync.android.presentation.components.filtersheet.FilterOptionGroup
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.components.filtersheet.IncludeExcludeChip
import com.anisync.android.presentation.components.filtersheet.IncludeExcludeHint
import com.anisync.android.presentation.components.filtersheet.OptionPillGrid
import com.anisync.android.presentation.components.filtersheet.SegmentedChoice
import com.anisync.android.presentation.components.filtersheet.SheetFooterButton
import com.anisync.android.presentation.components.filtersheet.SheetSectionLabel
import com.anisync.android.presentation.components.filtersheet.SortSheet
import com.anisync.android.presentation.components.filtersheet.TagChoice
import com.anisync.android.presentation.components.filtersheet.TagPickerSheet
import com.anisync.android.presentation.components.filtersheet.TriState
import com.anisync.android.presentation.components.filtersheet.airingStatusTitle
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaSource
import com.anisync.android.type.MediaStatus
import java.util.Calendar

/**
 * Every sheet the Discover filter bar opens. Edits apply live, and each filter sheet closes on a
 * footer that says how many titles the filters leave in the [category] on screen.
 */
@Composable
fun SearchFilterSheetHost(
    openedFilter: FilterId?,
    filters: SearchFilters,
    category: ResultCategory,
    hasQuery: Boolean,
    matchingTitles: Int,
    genres: List<String>,
    tags: List<MediaTag>,
    showAdultContent: Boolean,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val footer: @Composable () -> Unit = {
        SheetFooterButton(
            text = pluralStringResourceOrDefault(R.plurals.library_show_results, matchingTitles, matchingTitles),
            onClick = onDismiss
        )
    }
    when (openedFilter) {
        FilterId.SORT -> SearchSortSheet(filters, category, hasQuery, onFiltersChange, onDismiss)
        FilterId.GENRES -> GenresSheet(filters, genres, showAdultContent, onFiltersChange, onDismiss, footer)
        FilterId.TAGS -> {
            val choices = remember(tags) {
                tags.map { TagChoice(name = it.name, category = it.category, isAdult = it.isAdult) }
            }
            TagPickerSheet(
                tags = choices,
                included = filters.tagsIncluded,
                excluded = filters.tagsExcluded,
                showAdult = showAdultContent,
                onSelectionChange = { included, excluded ->
                    onFiltersChange(filters.copy(tagsIncluded = included, tagsExcluded = excluded))
                },
                onDismiss = onDismiss,
                footer = footer
            )
        }
        FilterId.FORMAT -> FilterSheetScaffold(
            title = stringResource(R.string.library_filter_format),
            onDismiss = onDismiss,
            onReset = { onFiltersChange(filters.copy(formats = emptySet())) },
            resetEnabled = filters.formats.isNotEmpty(),
            footer = footer
        ) {
            OptionPillGrid(
                options = formatOptions(category),
                label = { it.toLabel() },
                isSelected = { it in filters.formats },
                onClick = { format -> onFiltersChange(filters.copy(formats = filters.formats.toggle(format))) },
                role = Role.Checkbox
            )
        }
        FilterId.STATUS -> FilterSheetScaffold(
            title = airingStatusTitle(manga = category == ResultCategory.MANGA),
            onDismiss = onDismiss,
            onReset = { onFiltersChange(filters.copy(statuses = emptySet())) },
            resetEnabled = filters.statuses.isNotEmpty(),
            footer = footer
        ) {
            OptionPillGrid(
                options = StatusOptions,
                label = { it.toLabel() },
                isSelected = { it in filters.statuses },
                onClick = { status -> onFiltersChange(filters.copy(statuses = filters.statuses.toggle(status))) },
                role = Role.Checkbox
            )
        }
        FilterId.YEAR -> YearAndSeasonSheet(filters, category, onFiltersChange, onDismiss, footer)
        FilterId.MORE -> MoreFiltersSheet(filters, category, onFiltersChange, onDismiss, footer)
        null -> Unit
    }
}

// =============================================================================
// SORT
// =============================================================================

@Composable
private fun SearchSortSheet(
    filters: SearchFilters,
    category: ResultCategory,
    hasQuery: Boolean,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val sort = filters.sort
    SortSheet(
        columns = sortColumns(category, hasQuery),
        label = { it.label() },
        selected = sort.column.on(category),
        isAscending = !sort.descending,
        hasDirection = sort.column.hasDirection,
        onColumnPick = { onFiltersChange(filters.copy(sort = SearchSort(it))) },
        onDirectionChange = { ascending -> onFiltersChange(filters.copy(sort = sort.copy(descending = !ascending))) },
        onReset = { onFiltersChange(filters.copy(sort = SearchSort())) },
        resetEnabled = sort != SearchSort(),
        onDismiss = onDismiss
    )
}

/**
 * Best match joins while a query is typed. Length columns only make sense for one type at a time,
 * so All leaves them out.
 */
internal fun sortColumns(category: ResultCategory, hasQuery: Boolean): List<SearchSortColumn> = buildList {
    if (hasQuery) add(SearchSortColumn.BEST_MATCH)
    addAll(
        listOf(
            SearchSortColumn.POPULARITY,
            SearchSortColumn.SCORE,
            SearchSortColumn.TRENDING,
            SearchSortColumn.FAVOURITES,
            SearchSortColumn.RELEASE_DATE,
            SearchSortColumn.UPDATED,
            SearchSortColumn.TITLE
        )
    )
    when (category) {
        ResultCategory.ANIME -> addAll(listOf(SearchSortColumn.EPISODES, SearchSortColumn.DURATION))
        ResultCategory.MANGA -> addAll(listOf(SearchSortColumn.CHAPTERS, SearchSortColumn.VOLUMES))
        else -> Unit
    }
}

/** A length column named the way [category] reads it: Chapters on manga, Episodes on anime. */
internal fun SearchSortColumn.on(category: ResultCategory): SearchSortColumn = when (category) {
    ResultCategory.ANIME -> when (this) {
        SearchSortColumn.CHAPTERS -> SearchSortColumn.EPISODES
        SearchSortColumn.VOLUMES -> SearchSortColumn.DURATION
        else -> this
    }
    ResultCategory.MANGA -> when (this) {
        SearchSortColumn.EPISODES -> SearchSortColumn.CHAPTERS
        SearchSortColumn.DURATION -> SearchSortColumn.VOLUMES
        else -> this
    }
    else -> this
}

@Composable
internal fun SearchSortColumn.label(): String = stringResource(
    when (this) {
        SearchSortColumn.BEST_MATCH -> R.string.search_sort_best_match
        SearchSortColumn.POPULARITY -> R.string.search_sort_popularity
        SearchSortColumn.SCORE -> R.string.search_sort_score
        SearchSortColumn.TRENDING -> R.string.search_sort_trending
        SearchSortColumn.FAVOURITES -> R.string.search_sort_favourites
        SearchSortColumn.RELEASE_DATE -> R.string.search_sort_release_date
        SearchSortColumn.UPDATED -> R.string.search_sort_updated
        SearchSortColumn.TITLE -> R.string.search_sort_title
        SearchSortColumn.EPISODES -> R.string.search_sort_episodes
        SearchSortColumn.DURATION -> R.string.search_sort_duration
        SearchSortColumn.CHAPTERS -> R.string.search_sort_chapters
        SearchSortColumn.VOLUMES -> R.string.search_sort_volumes
    }
)

// =============================================================================
// GENRES
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenresSheet(
    filters: SearchFilters,
    genres: List<String>,
    showAdult: Boolean,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
    footer: @Composable () -> Unit
) {
    val visibleGenres = remember(genres, showAdult) {
        if (showAdult) genres else genres.filterNot { it in ADULT_GENRES }
    }
    FilterSheetScaffold(
        title = stringResource(R.string.filter_genres),
        onDismiss = onDismiss,
        onReset = {
            onFiltersChange(filters.copy(genresIncluded = emptySet(), genresExcluded = emptySet()))
        },
        resetEnabled = filters.genresIncluded.isNotEmpty() || filters.genresExcluded.isNotEmpty(),
        footer = footer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IncludeExcludeHint(counts = null)
            if (visibleGenres.isEmpty()) {
                Text(
                    text = stringResource(R.string.filter_loading_genres),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleGenres.forEach { genre ->
                        IncludeExcludeChip(
                            label = genre,
                            state = when (genre) {
                                in filters.genresIncluded -> TriState.INCLUDED
                                in filters.genresExcluded -> TriState.EXCLUDED
                                else -> TriState.OFF
                            },
                            onStateChange = { state ->
                                onFiltersChange(
                                    filters.copy(
                                        genresIncluded = filters.genresIncluded.with(genre, state == TriState.INCLUDED),
                                        genresExcluded = filters.genresExcluded.with(genre, state == TriState.EXCLUDED)
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
// FORMAT and STATUS
// =============================================================================

private val AnimeFormats = listOf(
    MediaFormat.TV, MediaFormat.TV_SHORT, MediaFormat.MOVIE,
    MediaFormat.SPECIAL, MediaFormat.OVA, MediaFormat.ONA, MediaFormat.MUSIC
)
private val MangaFormats = listOf(MediaFormat.MANGA, MediaFormat.NOVEL, MediaFormat.ONE_SHOT)

private fun formatOptions(category: ResultCategory): List<MediaFormat> = when (category) {
    ResultCategory.ANIME -> AnimeFormats
    ResultCategory.MANGA -> MangaFormats
    else -> AnimeFormats + MangaFormats
}

/** The order the Library lists them in, running before finished before not yet out. */
private val StatusOptions = listOf(
    MediaStatus.RELEASING,
    MediaStatus.FINISHED,
    MediaStatus.NOT_YET_RELEASED,
    MediaStatus.HIATUS,
    MediaStatus.CANCELLED
)

// =============================================================================
// YEAR AND SEASON
// =============================================================================

private val Seasons = listOf(MediaSeason.WINTER, MediaSeason.SPRING, MediaSeason.SUMMER, MediaSeason.FALL)

/**
 * The season sits with the year it belongs to. It is anime only, so manga opens on the years.
 */
@Composable
private fun YearAndSeasonSheet(
    filters: SearchFilters,
    category: ResultCategory,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
    footer: @Composable () -> Unit
) {
    val showSeason = category != ResultCategory.MANGA
    FilterSheetScaffold(
        title = stringResource(R.string.filter_year_and_season),
        onDismiss = onDismiss,
        onReset = { onFiltersChange(filters.copy(season = null, yearRange = IntRangeFilter())) },
        resetEnabled = filters.season != null || filters.yearRange.isActive,
        scrollableBody = false,
        footer = footer
    ) {
        if (showSeason) {
            SheetSectionLabel(stringResource(R.string.filter_season))
            Spacer(Modifier.height(10.dp))
            OptionPillGrid(
                options = Seasons,
                label = { it.toLabel().orEmpty() },
                isSelected = { it == filters.season },
                onClick = { season ->
                    onFiltersChange(filters.copy(season = season.takeIf { it != filters.season }))
                }
            )
            Spacer(Modifier.height(10.dp))
        }
        SheetSectionLabel(stringResource(R.string.filter_year))
        Spacer(Modifier.height(10.dp))
        YearPickerBody(filters, onFiltersChange)
    }
}

/**
 * A year grid in the style of Material 3's own year picker, restricted to years. M3 keeps its year
 * picker private, so this mirrors its spec: 3 columns, bodyLarge, circular cells, primary-filled
 * endpoints, secondaryContainer in between, and a 1dp outline on this year when it is unpicked.
 *
 * Taps edit the [IntRangeFilter] (`min == max` is a single year):
 *   - empty, tap Y                → (Y, Y)
 *   - (A, A), tap A               → empty
 *   - (A, A), tap Y ≠ A           → (min(A, Y), max(A, Y))
 *   - (A, B), tap A or B          → the other endpoint alone
 *   - (A, B), tap any other Y     → (Y, Y)
 */
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

    // Open on the picked start, else this year, with one row of context above it.
    val initialIdx = remember(maxYear) {
        val target = from ?: currentYear
        val itemIdx = (maxYear - target).coerceIn(0, years.size - 1)
        ((itemIdx / 3) - 1).coerceAtLeast(0) * 3
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIdx)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                when {
                    isRange -> R.string.year_picker_hint_range
                    isSingle -> R.string.year_picker_hint_single
                    else -> R.string.year_picker_hint_empty
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = years, key = { it }) { year ->
                val isEndpoint = year == from || year == to
                YearCell(
                    year = year,
                    isEndpoint = isEndpoint,
                    inRange = from != null && to != null && from != to && year > from && year < to,
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
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = when {
            isEndpoint -> MaterialTheme.colorScheme.primary
            inRange -> MaterialTheme.colorScheme.secondaryContainer
            else -> Color.Transparent
        },
        contentColor = when {
            isEndpoint -> MaterialTheme.colorScheme.onPrimary
            inRange -> MaterialTheme.colorScheme.onSecondaryContainer
            isToday -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = year.toString(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// =============================================================================
// MORE
// =============================================================================

/** The filters too rare for a chip of their own, as groups that open in place, one at a time. */
@Composable
private fun MoreFiltersSheet(
    filters: SearchFilters,
    category: ResultCategory,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
    footer: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    fun toggle(key: String) {
        expanded = if (expanded == key) null else key
    }
    val any = stringResource(R.string.filter_any)
    val groupPill = MaterialTheme.colorScheme.surfaceContainerHighest

    FilterSheetScaffold(
        title = stringResource(R.string.filter_more_title),
        onDismiss = onDismiss,
        onReset = {
            onFiltersChange(
                filters.copy(
                    sources = emptySet(),
                    country = null,
                    score = IntComparatorFilter(),
                    episodes = IntComparatorFilter(),
                    chapters = IntComparatorFilter(),
                    adultMode = AdultMode.ANY
                )
            )
        },
        resetEnabled = filters.sources.isNotEmpty() || filters.country != null ||
            filters.score.isActive || filters.episodes.isActive || filters.chapters.isActive ||
            filters.adultMode != AdultMode.ANY,
        footer = footer
    ) {
        val sourceLabels = filters.sources.mapNotNull { it.toLabel() }
        FilterOptionGroup(
            label = stringResource(R.string.filter_source),
            summary = when (sourceLabels.size) {
                0 -> any
                1 -> sourceLabels.first()
                else -> stringResource(R.string.filter_value_more, sourceLabels.first(), sourceLabels.size - 1)
            },
            expanded = expanded == "source",
            onToggle = { toggle("source") }
        ) {
            OptionPillGrid(
                options = SourceOptions,
                label = { it.toLabel().orEmpty() },
                isSelected = { it in filters.sources },
                onClick = { source -> onFiltersChange(filters.copy(sources = filters.sources.toggle(source))) },
                horizontalPadding = 0.dp,
                idleColor = groupPill,
                role = Role.Checkbox
            )
        }

        FilterOptionGroup(
            label = stringResource(R.string.filter_country),
            summary = filters.country?.toLabel() ?: any,
            expanded = expanded == "country",
            onToggle = { toggle("country") }
        ) {
            OptionPillGrid(
                options = OriginCountry.entries,
                label = { it.toLabel() },
                isSelected = { it == filters.country },
                onClick = { country ->
                    onFiltersChange(filters.copy(country = country.takeIf { it != filters.country }))
                },
                horizontalPadding = 0.dp,
                idleColor = groupPill
            )
        }

        FilterOptionGroup(
            label = stringResource(R.string.filter_mean_score),
            summary = filters.score.summary(suffix = "%") ?: any,
            expanded = expanded == "score",
            onToggle = { toggle("score") }
        ) {
            ThresholdBody(
                filter = filters.score,
                values = (0..100 step 5).toList(),
                defaultValue = 75,
                suffix = "%",
                onChange = { onFiltersChange(filters.copy(score = it)) }
            )
        }

        if (category != ResultCategory.MANGA) {
            FilterOptionGroup(
                label = stringResource(R.string.filter_episodes),
                summary = filters.episodes.summary() ?: any,
                expanded = expanded == "episodes",
                onToggle = { toggle("episodes") }
            ) {
                ThresholdBody(
                    filter = filters.episodes,
                    values = (1..200).toList(),
                    defaultValue = 12,
                    onChange = { onFiltersChange(filters.copy(episodes = it)) }
                )
            }
        }

        if (category != ResultCategory.ANIME) {
            FilterOptionGroup(
                label = stringResource(R.string.filter_chapters),
                summary = filters.chapters.summary() ?: any,
                expanded = expanded == "chapters",
                onToggle = { toggle("chapters") }
            ) {
                ThresholdBody(
                    filter = filters.chapters,
                    values = (1..1000).toList(),
                    defaultValue = 50,
                    onChange = { onFiltersChange(filters.copy(chapters = it)) }
                )
            }
        }

        FilterOptionGroup(
            label = stringResource(R.string.filter_adult_content),
            summary = filters.adultMode.label(),
            expanded = expanded == "adult",
            onToggle = { toggle("adult") }
        ) {
            OptionPillGrid(
                options = AdultMode.entries,
                label = { it.label() },
                isSelected = { it == filters.adultMode },
                onClick = { onFiltersChange(filters.copy(adultMode = it)) },
                horizontalPadding = 0.dp,
                idleColor = groupPill
            )
        }
    }
}

private val SourceOptions = MediaSource.entries.filter { it != MediaSource.UNKNOWN__ }

/**
 * A threshold's mode on the connected selector, then its value on the wheel. The wheel only shows
 * once there is a value to pick.
 */
@Composable
private fun ThresholdBody(
    filter: IntComparatorFilter,
    values: List<Int>,
    defaultValue: Int,
    onChange: (IntComparatorFilter) -> Unit,
    suffix: String = ""
) {
    val modes = ComparatorMode.entries
    SegmentedChoice(
        labels = modes.map { it.label() },
        selectedIndex = modes.indexOf(filter.mode),
        onSelect = { index ->
            val mode = modes[index]
            onChange(
                IntComparatorFilter(
                    mode = mode,
                    value = if (mode == ComparatorMode.ANY) null else filter.value ?: defaultValue
                )
            )
        },
        idleColor = MaterialTheme.colorScheme.surfaceContainerHighest
    )
    if (filter.mode != ComparatorMode.ANY) {
        val labels = remember(values, suffix) { values.map { "$it$suffix" } }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
        ) {
            WheelPicker(
                items = labels,
                selectedIndex = values.indexOf(filter.value ?: defaultValue).coerceAtLeast(0),
                onSelectedIndexChange = { index -> onChange(filter.copy(value = values[index])) },
                itemHeight = 32.dp
            )
        }
    }
}

@Composable
private fun ComparatorMode.label(): String = stringResource(
    when (this) {
        ComparatorMode.ANY -> R.string.filter_any
        ComparatorMode.AT_LEAST -> R.string.filter_at_least
        ComparatorMode.AT_MOST -> R.string.filter_at_most
        ComparatorMode.EXACTLY -> R.string.filter_exactly
    }
)

/** "At least 75%". Null while the threshold is off. */
@Composable
private fun IntComparatorFilter.summary(suffix: String = ""): String? {
    val shown = value?.let { "$it$suffix" } ?: return null
    return when (mode) {
        ComparatorMode.ANY -> null
        ComparatorMode.AT_LEAST -> stringResource(R.string.filter_at_least_value, shown)
        ComparatorMode.AT_MOST -> stringResource(R.string.filter_at_most_value, shown)
        ComparatorMode.EXACTLY -> stringResource(R.string.filter_exactly_value, shown)
    }
}

@Composable
private fun AdultMode.label(): String = stringResource(
    when (this) {
        AdultMode.ANY -> R.string.filter_any
        AdultMode.HIDE -> R.string.filter_adult_hide
        AdultMode.ONLY -> R.string.filter_adult_only
    }
)

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

private fun Set<String>.with(item: String, present: Boolean): Set<String> =
    if (present) this + item else this - item
