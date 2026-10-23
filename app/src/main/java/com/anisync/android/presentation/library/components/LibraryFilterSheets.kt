package com.anisync.android.presentation.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.filtersheet.ClearFiltersChip
import com.anisync.android.presentation.components.filtersheet.FilterBarChip
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.components.filtersheet.IncludeExcludeChip
import com.anisync.android.presentation.components.filtersheet.IncludeExcludeHint
import com.anisync.android.presentation.components.filtersheet.OptionPillGrid
import com.anisync.android.presentation.components.filtersheet.SheetFooterButton
import com.anisync.android.presentation.components.filtersheet.TagChoice
import com.anisync.android.presentation.components.filtersheet.TagPickerSheet
import com.anisync.android.presentation.components.filtersheet.airingStatusTitle
import com.anisync.android.presentation.components.filtersheet.countChipLabel
import com.anisync.android.presentation.components.filtersheet.pickChipLabel
import com.anisync.android.presentation.library.LibraryFilters
import com.anisync.android.presentation.library.LibrarySort
import com.anisync.android.presentation.library.LibraryTagOption
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaStatus
import com.anisync.android.type.MediaType

/** Which sheet the library search's filter bar has open. */
enum class LibraryFilterId { SORT, GENRES, TAGS, FORMAT, AIRING_STATUS }

/**
 * The filter bar at the top of library search, the same bar Discover's search draws: Clear once
 * anything is filtered, then one chip per filter, each opening its own sheet.
 */
@Composable
fun LibraryFilterChipBar(
    sort: LibrarySort,
    isAscending: Boolean,
    filters: LibraryFilters,
    mediaType: MediaType,
    onChipTap: (LibraryFilterId) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!filters.isEmpty) ClearFiltersChip(onClick = onClear)
        FilterBarChip(
            label = stringResource(R.string.library_filter_chip_sort, sort.label()),
            active = sort != LibrarySort.AIRING_SOON || !isAscending,
            onClick = { onChipTap(LibraryFilterId.SORT) }
        )
        val genres = filters.genresIncluded.size + filters.genresExcluded.size
        FilterBarChip(
            label = countChipLabel(stringResource(R.string.filter_genres), genres),
            active = genres > 0,
            onClick = { onChipTap(LibraryFilterId.GENRES) }
        )
        val tags = filters.tagsIncluded.size + filters.tagsExcluded.size
        FilterBarChip(
            label = countChipLabel(stringResource(R.string.library_filter_tags), tags),
            active = tags > 0,
            onClick = { onChipTap(LibraryFilterId.TAGS) }
        )
        FilterBarChip(
            label = pickChipLabel(
                stringResource(R.string.library_filter_format),
                filters.formats.map { it.toLabel() }
            ),
            active = filters.formats.isNotEmpty(),
            onClick = { onChipTap(LibraryFilterId.FORMAT) }
        )
        FilterBarChip(
            label = pickChipLabel(
                airingStatusTitle(manga = mediaType == MediaType.MANGA),
                filters.airingStatuses.map { it.airingStatusLabel() }
            ),
            active = filters.airingStatuses.isNotEmpty(),
            onClick = { onChipTap(LibraryFilterId.AIRING_STATUS) }
        )
    }
}

/**
 * Every sheet the filter bar can open. Edits apply live, like the rest of the app's filters, and
 * each filter sheet closes on a footer that says how many titles the filters leave.
 */
@Composable
fun LibraryFilterSheetHost(
    openedFilter: LibraryFilterId?,
    sort: LibrarySort,
    isAscending: Boolean,
    filters: LibraryFilters,
    mediaType: MediaType,
    matchingTitles: Int,
    availableGenres: List<String>,
    availableTags: List<LibraryTagOption>,
    availableFormats: List<MediaFormat>,
    availableAiringStatuses: List<String>,
    showAdultContent: Boolean,
    onSortChange: (LibrarySort, Boolean) -> Unit,
    onFiltersChange: (LibraryFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val footer: @Composable () -> Unit = {
        SheetFooterButton(
            text = pluralStringResourceOrDefault(R.plurals.library_show_results, matchingTitles, matchingTitles),
            onClick = onDismiss
        )
    }
    when (openedFilter) {
        LibraryFilterId.SORT -> LibrarySortSheet(
            visible = true,
            sort = sort,
            isAscending = isAscending,
            onSortChange = onSortChange,
            onDismiss = onDismiss
        )
        LibraryFilterId.GENRES -> GenresSheet(filters, availableGenres, onFiltersChange, onDismiss, footer)
        LibraryFilterId.TAGS -> {
            val choices = remember(availableTags) {
                availableTags.map {
                    TagChoice(
                        name = it.name,
                        category = it.category.ifBlank { null },
                        isAdult = it.isAdult,
                        isSpoiler = it.isSpoiler
                    )
                }
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
        LibraryFilterId.FORMAT -> OptionsSheet(
            title = stringResource(R.string.library_filter_format),
            options = availableFormats,
            label = { it.toLabel() },
            selected = filters.formats,
            onSelectedChange = { onFiltersChange(filters.copy(formats = it)) },
            onDismiss = onDismiss,
            footer = footer
        )
        LibraryFilterId.AIRING_STATUS -> OptionsSheet(
            title = airingStatusTitle(manga = mediaType == MediaType.MANGA),
            options = availableAiringStatuses,
            label = { it.airingStatusLabel() },
            selected = filters.airingStatuses,
            onSelectedChange = { onFiltersChange(filters.copy(airingStatuses = it)) },
            onDismiss = onDismiss,
            footer = footer
        )
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GenresSheet(
    filters: LibraryFilters,
    genres: List<String>,
    onFiltersChange: (LibraryFilters) -> Unit,
    onDismiss: () -> Unit,
    footer: @Composable () -> Unit
) {
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { genre ->
                    IncludeExcludeChip(
                        label = genre,
                        state = filters.genreState(genre),
                        onStateChange = { onFiltersChange(filters.withGenre(genre, it)) }
                    )
                }
            }
        }
    }
}

/** A closed list the library holds, as the shared pills, any number picked at once. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionsSheet(
    title: String,
    options: List<T>,
    label: @Composable (T) -> String,
    selected: Set<T>,
    onSelectedChange: (Set<T>) -> Unit,
    onDismiss: () -> Unit,
    footer: @Composable () -> Unit
) {
    FilterSheetScaffold(
        title = title,
        onDismiss = onDismiss,
        onReset = { onSelectedChange(emptySet()) },
        resetEnabled = selected.isNotEmpty(),
        footer = footer
    ) {
        OptionPillGrid(
            options = options,
            label = label,
            isSelected = { it in selected },
            onClick = { option ->
                onSelectedChange(if (option in selected) selected - option else selected + option)
            },
            role = Role.Checkbox
        )
    }
}

@Composable
private fun String.airingStatusLabel(): String = MediaStatus.safeValueOf(this).toLabel().ifEmpty { this }
