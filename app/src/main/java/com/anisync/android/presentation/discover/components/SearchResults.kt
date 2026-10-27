package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.GroupedSearchResults
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.ResultCategory
import com.anisync.android.domain.SearchResult
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.PinnedRail
import com.anisync.android.presentation.components.RailChip
import com.anisync.android.presentation.components.revealChip
import com.anisync.android.presentation.components.ViewModeToggle
import com.anisync.android.presentation.components.result.DiscoverResultPoster
import com.anisync.android.presentation.components.result.DiscoverResultRow
import com.anisync.android.presentation.components.result.PersonResultPoster
import com.anisync.android.presentation.components.result.PersonResultRow
import com.anisync.android.presentation.components.result.ResultGroupHeader
import com.anisync.android.presentation.components.result.StudioResultCard
import com.anisync.android.presentation.components.result.StudioResultRow
import com.anisync.android.presentation.components.result.UserResultCard
import com.anisync.android.presentation.components.result.UserResultRow
import com.anisync.android.presentation.discover.SearchPaging
import com.anisync.android.presentation.discover.SearchTarget
import com.anisync.android.presentation.util.posterGridColumns

/** How many of each kind the All list previews before its See all. */
private const val PreviewCount = 3

private val RowMinWidth = 360.dp
private val PosterWidth = 150.dp

/**
 * The row under the filter bar that picks what the results show: list or grid pinned at its start,
 * the categories with results scrolling under it.
 */
@Composable
fun SearchResultsRail(
    categories: List<ResultCategory>,
    activeCategory: ResultCategory,
    isGrid: Boolean,
    onCategoryChange: (ResultCategory) -> Unit,
    onGridChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = categories.indexOf(activeCategory)
    // A category picked through See all can sit off screen, so the rail follows it there.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.revealChip(selectedIndex)
    }
    PinnedRail(
        pinned = { ViewModeToggle(isGrid = isGrid, onGridChange = onGridChange) },
        listState = listState,
        modifier = modifier
    ) {
        items(categories, key = { it.name }) { category ->
            RailChip(
                label = category.label(),
                icon = category.icon(),
                selected = category == activeCategory,
                onClick = { onCategoryChange(category) }
            )
        }
    }
}

/** Where a tapped result goes. Media can also go straight onto Planning. */
@Stable
class SearchResultActions(
    val onMedia: (Int) -> Unit,
    val onAddToList: (Int) -> Unit,
    val onCharacter: (Int) -> Unit,
    val onStaff: (Int) -> Unit,
    val onStudio: (Int) -> Unit,
    val onUser: (String) -> Unit
)

/**
 * Discover's search results.
 *
 * All previews each kind under a header whose See all opens it: three rows apiece in the list, a
 * rail of posters apiece in the grid, the way Discover home lays out. A single category is the
 * plain list or grid, paging in as it scrolls.
 */
@Composable
fun SearchResults(
    activeCategory: ResultCategory,
    isGrid: Boolean,
    anime: List<LibraryEntry>,
    manga: List<LibraryEntry>,
    grouped: GroupedSearchResults,
    paging: SearchPaging,
    titleLanguage: TitleLanguage,
    actions: SearchResultActions,
    onShowAll: (ResultCategory) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    /** The result open in the two-pane detail, which takes the selected ring. */
    selected: SearchTarget? = null
) {
    val results = SearchResultSet(anime, manga, grouped, titleLanguage, actions, selected)
    when {
        activeCategory != ResultCategory.ALL -> CategoryResults(
            category = activeCategory,
            isGrid = isGrid,
            results = results,
            hasMore = paging.hasNextFor(activeCategory),
            onLoadMore = onLoadMore,
            modifier = modifier
        )
        isGrid -> OverviewRails(results, onShowAll, modifier)
        else -> OverviewList(results, paging, onShowAll, modifier)
    }
}

/** Everything one search returned, and how each kind draws as a row and as a poster. */
@Stable
private class SearchResultSet(
    val anime: List<LibraryEntry>,
    val manga: List<LibraryEntry>,
    val grouped: GroupedSearchResults,
    val titleLanguage: TitleLanguage,
    val actions: SearchResultActions,
    val selected: SearchTarget?
) {
    /** The kinds with results, in rail order. */
    val populated: List<ResultCategory>
        get() = ResultCategory.entries.filter { it != ResultCategory.ALL && size(it) > 0 }

    fun size(category: ResultCategory): Int = items(category).size

    fun items(category: ResultCategory): List<Any> = when (category) {
        ResultCategory.ANIME -> anime
        ResultCategory.MANGA -> manga
        ResultCategory.CHARACTERS -> grouped.characters
        ResultCategory.STAFF -> grouped.staff
        ResultCategory.USERS -> grouped.users
        ResultCategory.STUDIOS -> grouped.studios
        ResultCategory.ALL -> emptyList()
    }

    fun key(item: Any): String = when (item) {
        is LibraryEntry -> "m_${item.mediaId}"
        is SearchResult.CharacterResult -> "c_${item.id}"
        is SearchResult.StaffResult -> "s_${item.id}"
        is SearchResult.UserResult -> "u_${item.id}"
        is SearchResult.StudioResult -> "st_${item.id}"
        else -> item.hashCode().toString()
    }

    @Composable
    fun ResultRow(item: Any, modifier: Modifier = Modifier) {
        when (item) {
            is LibraryEntry -> DiscoverResultRow(
                entry = item,
                titleLanguage = titleLanguage,
                onClick = { actions.onMedia(item.mediaId) },
                onAddToList = { actions.onAddToList(item.mediaId) },
                selected = selected == SearchTarget.Media(item.mediaId),
                modifier = modifier
            )
            is SearchResult.CharacterResult -> PersonResultRow(
                name = item.displayName,
                detail = item.nativeName,
                imageUrl = item.imageUrl,
                favourites = item.favourites,
                onClick = { actions.onCharacter(item.id) },
                selected = selected == SearchTarget.Character(item.id),
                modifier = modifier
            )
            is SearchResult.StaffResult -> PersonResultRow(
                name = item.displayName,
                detail = item.primaryOccupations.firstOrNull() ?: item.nativeName,
                imageUrl = item.imageUrl,
                favourites = item.favourites,
                onClick = { actions.onStaff(item.id) },
                selected = selected == SearchTarget.Staff(item.id),
                modifier = modifier
            )
            is SearchResult.UserResult -> UserResultRow(
                name = item.displayName,
                avatarUrl = item.imageUrl,
                onClick = { actions.onUser(item.displayName) },
                modifier = modifier
            )
            is SearchResult.StudioResult -> StudioResultRow(
                name = item.displayName,
                favourites = item.favourites,
                onClick = { actions.onStudio(item.id) },
                selected = selected == SearchTarget.Studio(item.id),
                modifier = modifier
            )
        }
    }

    @Composable
    fun ResultPoster(item: Any, modifier: Modifier = Modifier) {
        when (item) {
            is LibraryEntry -> DiscoverResultPoster(
                entry = item,
                titleLanguage = titleLanguage,
                onClick = { actions.onMedia(item.mediaId) },
                onAddToList = { actions.onAddToList(item.mediaId) },
                selected = selected == SearchTarget.Media(item.mediaId),
                modifier = modifier
            )
            is SearchResult.CharacterResult -> PersonResultPoster(
                name = item.displayName,
                detail = item.nativeName,
                imageUrl = item.imageUrl,
                favourites = item.favourites,
                onClick = { actions.onCharacter(item.id) },
                selected = selected == SearchTarget.Character(item.id),
                modifier = modifier
            )
            is SearchResult.StaffResult -> PersonResultPoster(
                name = item.displayName,
                detail = item.primaryOccupations.firstOrNull() ?: item.nativeName,
                imageUrl = item.imageUrl,
                favourites = item.favourites,
                onClick = { actions.onStaff(item.id) },
                selected = selected == SearchTarget.Staff(item.id),
                modifier = modifier
            )
            is SearchResult.UserResult -> UserResultCard(
                name = item.displayName,
                avatarUrl = item.imageUrl,
                onClick = { actions.onUser(item.displayName) },
                modifier = modifier
            )
            is SearchResult.StudioResult -> StudioResultCard(
                name = item.displayName,
                favourites = item.favourites,
                onClick = { actions.onStudio(item.id) },
                selected = selected == SearchTarget.Studio(item.id),
                modifier = modifier
            )
        }
    }
}

/**
 * Three of each kind, or as many as fill whole lines of three on a wide window, under a header
 * whose See all opens the kind when there is more to it.
 */
@Composable
private fun OverviewList(
    results: SearchResultSet,
    paging: SearchPaging,
    onShowAll: (ResultCategory) -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = ((maxWidth - 48.dp + 16.dp) / (RowMinWidth + 16.dp)).toInt().coerceAtLeast(1)
        val cap = columns * ((PreviewCount + columns - 1) / columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = ResultsPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            results.populated.forEach { category ->
                val items = results.items(category)
                item(key = "header_${category.name}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    ResultGroupHeader(
                        title = category.label(),
                        onSeeAll = if (items.size > cap || paging.hasNextFor(category)) {
                            { onShowAll(category) }
                        } else {
                            null
                        }
                    )
                }
                items(items.take(cap), key = { results.key(it) }, contentType = { category.name }) { item ->
                    results.ResultRow(item)
                }
            }
        }
    }
}

/** Discover home's shape: a header and a rail of posters for each kind. */
@Composable
private fun OverviewRails(
    results: SearchResultSet,
    onShowAll: (ResultCategory) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        results.populated.forEach { category ->
            item(key = "header_${category.name}", contentType = "header") {
                ResultGroupHeader(
                    title = category.label(),
                    onSeeAll = { onShowAll(category) },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            item(key = "rail_${category.name}", contentType = "rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results.items(category), key = { results.key(it) }) { item ->
                        results.ResultPoster(item, Modifier.width(PosterWidth))
                    }
                }
            }
        }
    }
}

/** One kind on its own: rows tiling at 360dp or posters at 150dp, paging in at the end. */
@Composable
private fun CategoryResults(
    category: ResultCategory,
    isGrid: Boolean,
    results: SearchResultSet,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier
) {
    val items = results.items(category)
    LazyVerticalGrid(
        columns = if (isGrid) posterGridColumns(PosterWidth) else GridCells.Adaptive(RowMinWidth),
        contentPadding = ResultsPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isGrid) 20.dp else 12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(items, key = { results.key(it) }, contentType = { category.name }) { item ->
            if (isGrid) results.ResultPoster(item) else results.ResultRow(item)
        }
        loadMoreFooter(hasMore, items.size, onLoadMore)
    }
}

private val ResultsPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp)

/**
 * Endless scroll: the footer only composes once scrolled into view, and asks for the next page
 * then. Keying on the loaded count re-arms it after every append until there are no more pages.
 */
private fun LazyGridScope.loadMoreFooter(hasMore: Boolean, loadedCount: Int, onLoadMore: () -> Unit) {
    if (!hasMore) return
    item(key = "load_more", span = { GridItemSpan(maxLineSpan) }) {
        LaunchedEffect(loadedCount) { onLoadMore() }
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AppCircularProgressIndicator()
        }
    }
}

@Composable
private fun ResultCategory.label(): String = stringResource(
    when (this) {
        ResultCategory.ALL -> R.string.all
        ResultCategory.ANIME -> R.string.media_type_anime
        ResultCategory.MANGA -> R.string.media_type_manga
        ResultCategory.CHARACTERS -> R.string.search_header_characters
        ResultCategory.STAFF -> R.string.search_header_staff
        ResultCategory.USERS -> R.string.search_header_users
        ResultCategory.STUDIOS -> R.string.search_header_studios
    }
)

private fun ResultCategory.icon(): ImageVector = when (this) {
    ResultCategory.ALL -> Icons.Default.AllInclusive
    ResultCategory.ANIME -> Icons.Default.Tv
    ResultCategory.MANGA -> Icons.AutoMirrored.Filled.MenuBook
    ResultCategory.CHARACTERS -> Icons.Default.Face
    ResultCategory.STAFF -> Icons.Default.Person
    ResultCategory.USERS -> Icons.Default.Group
    ResultCategory.STUDIOS -> Icons.Default.Domain
}
