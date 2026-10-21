package com.anisync.android.presentation.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.filtersheet.FilterBarChip
import com.anisync.android.presentation.components.filtersheet.FilterOptionRow
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.components.filtersheet.IncludeExcludeChip
import com.anisync.android.presentation.components.filtersheet.TagChoice
import com.anisync.android.presentation.components.filtersheet.TagPickerSheet
import com.anisync.android.presentation.library.LibraryFilters
import com.anisync.android.presentation.library.LibrarySort
import com.anisync.android.presentation.library.LibraryTagOption
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat

/** Which sheet the library search's filter bar has open. */
enum class LibraryFilterId { SORT, GENRES, TAGS, FORMAT, AIRING_STATUS }

/**
 * The filter bar at the top of library search, laid out like Discover's: one chip per filter, each
 * opening its own sheet, with how much is picked written on the chip.
 */
@Composable
fun LibraryFilterChipBar(
    sort: LibrarySort,
    isAscending: Boolean,
    filters: LibraryFilters,
    onChipTap: (LibraryFilterId) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterBarChip(
            label = stringResource(R.string.library_filter_chip_sort, sort.label()),
            active = sort != LibrarySort.AIRING_SOON || !isAscending,
            onClick = { onChipTap(LibraryFilterId.SORT) }
        )
        val genres = filters.genresIncluded.size + filters.genresExcluded.size
        FilterBarChip(
            label = chipLabel(stringResource(R.string.filter_genres), genres),
            active = genres > 0,
            onClick = { onChipTap(LibraryFilterId.GENRES) }
        )
        val tags = filters.tagsIncluded.size + filters.tagsExcluded.size
        FilterBarChip(
            label = chipLabel(stringResource(R.string.library_filter_tags), tags),
            active = tags > 0,
            onClick = { onChipTap(LibraryFilterId.TAGS) }
        )
        FilterBarChip(
            label = chipLabel(stringResource(R.string.library_filter_format), filters.formats.size),
            active = filters.formats.isNotEmpty(),
            onClick = { onChipTap(LibraryFilterId.FORMAT) }
        )
        FilterBarChip(
            label = chipLabel(
                stringResource(R.string.library_filter_airing_status),
                filters.airingStatuses.size
            ),
            active = filters.airingStatuses.isNotEmpty(),
            onClick = { onChipTap(LibraryFilterId.AIRING_STATUS) }
        )
    }
}

@Composable
private fun chipLabel(name: String, count: Int): String =
    if (count == 0) name else stringResource(R.string.library_filter_chip_count, name, count)

/** Every sheet the filter bar can open. Edits apply live, like the rest of the app's filters. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterSheetHost(
    openedFilter: LibraryFilterId?,
    sort: LibrarySort,
    isAscending: Boolean,
    filters: LibraryFilters,
    availableGenres: List<String>,
    availableTags: List<LibraryTagOption>,
    availableFormats: List<MediaFormat>,
    availableAiringStatuses: List<String>,
    showAdultContent: Boolean,
    onSortChange: (LibrarySort, Boolean) -> Unit,
    onFiltersChange: (LibraryFilters) -> Unit,
    onDismiss: () -> Unit
) {
    when (openedFilter) {
        LibraryFilterId.SORT -> LibrarySortSheet(
            visible = true,
            sort = sort,
            isAscending = isAscending,
            onSortChange = onSortChange,
            onDismiss = onDismiss
        )
        LibraryFilterId.GENRES -> GenresSheet(filters, availableGenres, onFiltersChange, onDismiss)
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
                onDismiss = onDismiss
            )
        }
        LibraryFilterId.FORMAT -> OptionsSheet(
            title = stringResource(R.string.library_filter_format),
            options = availableFormats,
            label = { it.toLabel() },
            selected = filters.formats,
            onSelectedChange = { onFiltersChange(filters.copy(formats = it)) },
            onDismiss = onDismiss
        )
        LibraryFilterId.AIRING_STATUS -> OptionsSheet(
            title = stringResource(R.string.library_filter_airing_status),
            options = availableAiringStatuses,
            label = { it.airingStatusLabel() },
            selected = filters.airingStatuses,
            onSelectedChange = { onFiltersChange(filters.copy(airingStatuses = it)) },
            onDismiss = onDismiss
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
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = stringResource(R.string.filter_genres),
        onDismiss = onDismiss,
        onReset = {
            onFiltersChange(filters.copy(genresIncluded = emptySet(), genresExcluded = emptySet()))
        },
        resetEnabled = filters.genresIncluded.isNotEmpty() || filters.genresExcluded.isNotEmpty()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.library_filter_exclude_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionsSheet(
    title: String,
    options: List<T>,
    label: @Composable (T) -> String,
    selected: Set<T>,
    onSelectedChange: (Set<T>) -> Unit,
    onDismiss: () -> Unit
) {
    FilterSheetScaffold(
        title = title,
        onDismiss = onDismiss,
        onReset = { onSelectedChange(emptySet()) },
        resetEnabled = selected.isNotEmpty()
    ) {
        options.forEach { option ->
            val isSelected = option in selected
            FilterOptionRow(
                label = label(option),
                selected = isSelected,
                leading = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    null
                },
                onClick = {
                    onSelectedChange(if (isSelected) selected - option else selected + option)
                }
            )
        }
    }
}

@Composable
private fun String.airingStatusLabel(): String = when (this) {
    "RELEASING" -> stringResource(R.string.media_status_releasing)
    "FINISHED" -> stringResource(R.string.media_status_finished)
    "NOT_YET_RELEASED" -> stringResource(R.string.media_status_not_yet_released)
    "HIATUS" -> stringResource(R.string.media_status_hiatus)
    "CANCELLED" -> stringResource(R.string.media_status_cancelled)
    else -> this
}
