package com.anisync.android.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.LocalWhatsNew
import com.anisync.android.presentation.components.PinnedRail
import com.anisync.android.presentation.components.ViewModeToggle
import com.anisync.android.presentation.components.rememberActiveWhatsNew
import com.anisync.android.presentation.components.revealChip
import com.anisync.android.presentation.components.result.LibraryResultRow
import com.anisync.android.presentation.components.result.ResultGroupHeader
import com.anisync.android.presentation.discover.components.DiscoverNoResultsState
import com.anisync.android.presentation.library.components.LibraryFilterChipBar
import com.anisync.android.presentation.library.components.LibraryFilterId
import com.anisync.android.presentation.library.components.LibraryPosterCard
import com.anisync.android.presentation.library.components.LibraryRailChip
import com.anisync.android.presentation.settings.components.InfoNotice
import com.anisync.android.presentation.util.LIBRARY_ALL_TAB_ID
import com.anisync.android.presentation.util.posterGridColumns
import com.anisync.android.presentation.util.toLabel

/**
 * What library search shows: every list holding a match with how many it holds, the list picked
 * in the rail, and that list's matches.
 */
internal class LibrarySearchView(
    val categories: List<Pair<LibraryTab, Int>>,
    val active: LibraryTab,
    val entries: List<LibraryEntry>
)

/**
 * All first, then each list with matches in the viewer's tab order. A list picked earlier that no
 * longer holds a match falls back to All.
 */
internal fun librarySearchView(state: LibraryUiState): LibrarySearchView {
    val byCategory = state.searchMatchesByCategory
    val categories = buildList {
        add(LibraryTab.All to state.searchMatches.size)
        state.tabOrder.forEach { id ->
            if (id == LIBRARY_ALL_TAB_ID || id in state.hiddenListNames) return@forEach
            val matches = byCategory[id] ?: return@forEach
            LibraryTab.fromId(id, state.customListNames)?.let { add(it to matches.size) }
        }
    }
    val active = categories.firstOrNull { it.first.toId() == state.activeSearchCategory }?.first
        ?: LibraryTab.All
    val entries = if (active is LibraryTab.All) state.searchMatches else byCategory[active.toId()].orEmpty()
    return LibrarySearchView(categories, active, entries)
}

/**
 * The matches under each list they sit on, in the order the rail lists them. A run that lands on
 * one list comes back as one group, which draws without a header.
 */
internal fun groupByList(entries: List<LibraryEntry>, tabOrder: List<String>): List<Pair<LibraryStatus, List<LibraryEntry>>> {
    val byStatus = entries.groupBy { it.status }
    fun rank(status: LibraryStatus): Int =
        tabOrder.indexOf("status:${status.name}").let { if (it < 0) Int.MAX_VALUE else it }
    return byStatus.keys
        .sortedWith(compareBy<LibraryStatus> { rank(it) }.thenBy { it.ordinal })
        .map { it to byStatus.getValue(it) }
}

/**
 * The library's search: the shared filter bar, then the rail of lists with the list and grid
 * switch, then the matches as Library draws them. The list view groups by list wherever the pick
 * spans several, the grid stays one flat grid with each card's list tab.
 */
@Composable
internal fun LibrarySearchOverlay(
    uiState: LibraryUiState,
    view: LibrarySearchView,
    isSearchQueryEmpty: Boolean,
    onFilterChipTap: (LibraryFilterId) -> Unit,
    onClearFilters: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onGridChange: (Boolean) -> Unit,
    onResultClick: (Int) -> Unit,
    onIncrement: (Int) -> Unit,
    onEdit: (LibraryEntry) -> Unit,
    modifier: Modifier = Modifier,
    resultsModifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LibraryFilterChipBar(
            sort = uiState.sortOption,
            isAscending = uiState.isAscending,
            filters = uiState.filters,
            mediaType = uiState.mediaType,
            onChipTap = onFilterChipTap,
            onClear = onClearFilters
        )
        if (WhatsNewItem.LIBRARY_SEARCH_FILTERS in rememberActiveWhatsNew()) {
            val whatsNewStore = LocalWhatsNew.current
            InfoNotice(
                text = stringResource(R.string.whats_new_library_filters),
                onDismiss = { whatsNewStore.markSeen(WhatsNewItem.LIBRARY_SEARCH_FILTERS) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }

            uiState.searchMatches.isEmpty() && (!isSearchQueryEmpty || !uiState.filters.isEmpty) ->
                DiscoverNoResultsState(
                    hasFilters = !uiState.filters.isEmpty,
                    onClearFilters = onClearFilters
                )

            // Nothing typed and nothing filtered: keep the overlay empty, as Discover does.
            uiState.searchMatches.isEmpty() -> Box(Modifier.fillMaxSize())

            else -> {
                LibrarySearchRail(
                    view = view,
                    uiState = uiState,
                    onCategoryChange = onCategoryChange,
                    onGridChange = onGridChange
                )
                LibrarySearchResults(
                    uiState = uiState,
                    view = view,
                    onResultClick = onResultClick,
                    onIncrement = onIncrement,
                    onEdit = onEdit,
                    modifier = resultsModifier
                )
            }
        }
    }
}

@Composable
private fun LibrarySearchRail(
    view: LibrarySearchView,
    uiState: LibraryUiState,
    onCategoryChange: (String) -> Unit,
    onGridChange: (Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    val selectedIndex = view.categories.indexOfFirst { it.first == view.active }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.revealChip(selectedIndex)
    }
    PinnedRail(
        pinned = { ViewModeToggle(isGrid = uiState.isGridView, onGridChange = onGridChange) },
        listState = listState,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        items(view.categories, key = { it.first.toId() }) { (tab, count) ->
            LibraryRailChip(
                tab = tab,
                mediaType = uiState.mediaType,
                count = count,
                selected = tab == view.active,
                onClick = { onCategoryChange(tab.toId()) }
            )
        }
    }
}

@Composable
private fun LibrarySearchResults(
    uiState: LibraryUiState,
    view: LibrarySearchView,
    onResultClick: (Int) -> Unit,
    onIncrement: (Int) -> Unit,
    onEdit: (LibraryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaType = uiState.mediaType
    fun LibraryEntry.canIncrement() = status == LibraryStatus.CURRENT || status == LibraryStatus.REPEATING
    val padding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp)

    if (uiState.isGridView) {
        // One flat grid: headers left half-empty poster rows, and each card's tab already says
        // which list it is on.
        val showListTab = view.active is LibraryTab.All || view.active is LibraryTab.Custom
        LazyVerticalGrid(
            columns = posterGridColumns(baseMinSize = 150.dp),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(view.entries, key = { "search_${it.mediaId}" }, contentType = { "LibraryEntry" }) { entry ->
                LibraryPosterCard(
                    entry = entry,
                    mediaType = mediaType,
                    titleLanguage = uiState.titleLanguage,
                    showScore = uiState.showScoreOnCards,
                    scoreFormat = uiState.userScoreFormat,
                    showListIndicator = showListTab,
                    showPriority = true,
                    onClick = { onResultClick(entry.mediaId) },
                    onIncrement = if (entry.canIncrement()) {
                        { onIncrement(entry.mediaId) }
                    } else {
                        null
                    },
                    onEdit = { onEdit(entry) }
                )
            }
        }
    } else {
        val groups = remember(view.entries, uiState.tabOrder) { groupByList(view.entries, uiState.tabOrder) }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 360.dp),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            groups.forEach { (status, entries) ->
                if (groups.size > 1) {
                    item(key = "header_${status.name}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        ResultGroupHeader(title = status.toLabel(mediaType), count = entries.size)
                    }
                }
                items(entries, key = { "search_${it.mediaId}" }, contentType = { "LibraryEntry" }) { entry ->
                    LibraryResultRow(
                        entry = entry,
                        mediaType = mediaType,
                        titleLanguage = uiState.titleLanguage,
                        showScore = uiState.showScoreOnCards,
                        scoreFormat = uiState.userScoreFormat,
                        onClick = { onResultClick(entry.mediaId) },
                        onIncrement = { onIncrement(entry.mediaId) },
                        onEdit = { onEdit(entry) }
                    )
                }
            }
        }
    }
}
