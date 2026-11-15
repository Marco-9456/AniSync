package com.anisync.android.presentation.profile.lists

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatScore
import com.anisync.android.domain.max
import com.anisync.android.presentation.components.SearchField
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.components.filtersheet.OptionPillGrid
import com.anisync.android.presentation.components.filtersheet.SheetFooterButton
import com.anisync.android.presentation.components.filtersheet.SheetSectionLabel
import com.anisync.android.presentation.components.filtersheet.SortSheet
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import java.text.NumberFormat

@Composable
internal fun ProfileListToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    view: ProfileListView,
    onToggleView: () -> Unit,
    activeFilters: Int,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Local text: the strip is drawn twice, and the round trip through the ViewModel drops fast keys.
        var text by rememberSaveable { mutableStateOf(query) }
        var focused by remember { mutableStateOf(false) }
        LaunchedEffect(query) { if (!focused && query != text) text = query }
        SearchField(
            query = text,
            onQueryChange = {
                text = it
                onQueryChange(it)
            },
            placeholder = stringResource(R.string.profile_list_search_hint),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused }
        )
        IconButton(onClick = onToggleView) {
            Icon(
                imageVector = if (view == ProfileListView.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                contentDescription = stringResource(
                    if (view == ProfileListView.LIST) R.string.profile_list_show_grid else R.string.profile_list_show_rows
                )
            )
        }
        IconButton(onClick = onOpenFilters) {
            BadgedBox(badge = { if (activeFilters > 0) Badge { Text(activeFilters.toString()) } }) {
                Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.profile_list_filters))
            }
        }
    }
}

@Composable
internal fun ProfileListSummary(
    results: ProfileListResults,
    tools: ProfileListTools,
    isOwnProfile: Boolean,
    onOpenSort: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numbers = remember { NumberFormat.getIntegerInstance() }
    val count = results.items.size
    val query = tools.query.trim()
    val text = when {
        query.isNotEmpty() -> pluralStringResourceOrDefault(R.plurals.profile_list_results_for, count, numbers.format(count), query)
        tools.filters.isActive -> stringResource(R.string.profile_list_count_of, numbers.format(count), numbers.format(results.inScope))
        else -> pluralStringResourceOrDefault(R.plurals.profile_list_titles, count, numbers.format(count))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (tools.filters.isActive) {
            TextButton(onClick = onClearFilters) { Text(stringResource(R.string.profile_list_clear_filters)) }
        }
        TextButton(onClick = onOpenSort) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(sortLabel(tools.sort, isOwnProfile))
        }
    }
}

@Composable
internal fun sortLabel(sort: ProfileListSort, isOwnProfile: Boolean): String =
    stringResource(if (isOwnProfile && sort == ProfileListSort.THEIR_SCORE) R.string.profile_list_sort_score else sort.labelRes)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileListSortSheet(
    tools: ProfileListTools,
    isOwnProfile: Boolean,
    onSortChange: (ProfileListSort, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val columns = if (isOwnProfile) ProfileListSort.entries - ProfileListSort.YOUR_SCORE else ProfileListSort.entries
    SortSheet(
        columns = columns,
        label = { sortLabel(it, isOwnProfile) },
        selected = tools.sort,
        isAscending = tools.ascending,
        hasDirection = true,
        onColumnPick = { onSortChange(it, it == ProfileListSort.TITLE) },
        onDirectionChange = { onSortChange(tools.sort, it) },
        onReset = { onSortChange(ProfileListSort.UPDATED, false) },
        resetEnabled = tools.sort != ProfileListSort.UPDATED || tools.ascending,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileListFilterSheet(
    entries: List<LibraryEntry>,
    status: LibraryStatus?,
    tools: ProfileListTools,
    viewer: Map<Int, LibraryEntry>,
    titleLanguage: TitleLanguage,
    ownerScoreFormat: ScoreFormat,
    isOwnProfile: Boolean,
    onFiltersChange: (ProfileListFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val filters = tools.filters
    val matching = remember(entries, status, tools, viewer, titleLanguage) {
        applyListTools(entries, status, tools, viewer, titleLanguage).items.size
    }
    val formats = remember(entries) {
        entries.mapNotNull { it.format }.filter { it != MediaFormat.UNKNOWN__ }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    }
    val genres = remember(entries) {
        entries.flatMap { it.genres }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    }
    FilterSheetScaffold(
        title = stringResource(R.string.profile_list_filters),
        onDismiss = onDismiss,
        onReset = { onFiltersChange(ProfileListFilters()) },
        resetEnabled = filters.isActive,
        footer = {
            SheetFooterButton(
                text = pluralStringResourceOrDefault(R.plurals.library_show_results, matching, matching),
                onClick = onDismiss
            )
        }
    ) {
        if (!isOwnProfile) {
            SheetSectionLabel(stringResource(R.string.profile_list_your_list))
            Spacer(Modifier.height(8.dp))
            OptionPillGrid(
                options = YourListFilter.entries,
                label = { stringResource(it.labelRes) },
                isSelected = { it == filters.yourList },
                onClick = { onFiltersChange(filters.copy(yourList = it)) }
            )
            Spacer(Modifier.height(12.dp))
        }
        if (formats.size > 1) {
            SheetSectionLabel(stringResource(R.string.library_filter_format))
            Spacer(Modifier.height(8.dp))
            OptionPillGrid(
                options = formats,
                label = { it.toLabel() },
                isSelected = { it in filters.formats },
                onClick = { format -> onFiltersChange(filters.copy(formats = filters.formats.toggled(format))) },
                role = Role.Checkbox
            )
            Spacer(Modifier.height(12.dp))
        }
        if (genres.isNotEmpty()) {
            SheetSectionLabel(stringResource(R.string.filter_genres))
            Spacer(Modifier.height(8.dp))
            OptionPillGrid(
                options = genres,
                label = { it },
                isSelected = { it in filters.genres },
                onClick = { genre -> onFiltersChange(filters.copy(genres = filters.genres.toggled(genre))) },
                role = Role.Checkbox
            )
            Spacer(Modifier.height(12.dp))
        }
        SheetSectionLabel(stringResource(if (isOwnProfile) R.string.profile_list_sort_score else R.string.profile_list_sort_their_score))
        Spacer(Modifier.height(8.dp))
        OptionPillGrid(
            options = listOf<Int?>(null) + scoreFloors(ownerScoreFormat) + ProfileListFilters.UNSCORED,
            label = { floorLabel(it, ownerScoreFormat) },
            isSelected = { it == filters.scoreFloor },
            onClick = { onFiltersChange(filters.copy(scoreFloor = it)) }
        )
    }
}

@Composable
private fun floorLabel(floor: Int?, format: ScoreFormat): String = when (floor) {
    null -> stringResource(R.string.filter_any)
    ProfileListFilters.UNSCORED -> stringResource(R.string.profile_list_not_scored)
    100 -> formatScore(format.max, format)
    else -> stringResource(R.string.profile_list_score_and_up, formatScore(floor / 100.0 * format.max, format))
}

private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item
