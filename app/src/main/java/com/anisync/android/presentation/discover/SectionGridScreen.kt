package com.anisync.android.presentation.discover

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.anisync.android.presentation.util.posterGridColumns
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.PosterCard
import com.anisync.android.presentation.discover.components.SearchFiltersRow
import com.anisync.android.presentation.util.AppMotion
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun MediaGridContent(
    title: String,
    items: List<LibraryEntry>,
    isLoading: Boolean,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val gridState = rememberLazyGridState()

    CollapsingTopBarScaffold(
        title = title,
        onBackClick = onBackClick,
        scrollableState = gridState,
        scrolledContainerColor = MaterialTheme.colorScheme.background
    ) { topContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = posterGridColumns(baseMinSize = 150.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = topContentPadding + 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items,
                            key = { index, item -> "grid_${item.mediaId}_$index" }
                        ) { _, item ->
                            PosterCard(
                                item = item,
                                titleLanguage = titleLanguage,
                                onClick = { onMediaClick(item.mediaId) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                transitionPrefix = "sectiongrid"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SectionGridScreen(
    sectionTitle: String,
    sectionType: String,
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    viewModel: SectionGridViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle(initialValue = TitleLanguage.ROMAJI)

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // Efficiently compute when to load more items by caching the threshold state
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && uiState.hasNextPage && !uiState.isLoading) {
            viewModel.onAction(SectionGridAction.LoadNextPage)
        }
    }

    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedGridReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToMediaDetails: (Int) -> Unit = remember(onMediaClick) {
        { mediaId ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedGridReEnter = false
            onMediaClick(mediaId)
        }
    }

    val isGridEnteringFromBackStack by remember {
        derivedStateOf {
            animatedVisibilityScope.transition.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isGridTargetingVisible by remember {
        derivedStateOf {
            animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isGridFullyVisible by remember {
        derivedStateOf {
            animatedVisibilityScope.transition.currentState == EnterExitState.Visible &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isSharedTransitionRunning by remember {
        derivedStateOf { sharedTransitionScope.isTransitionActive }
    }
    val shouldRenderTopBarInOverlay by remember {
        derivedStateOf {
            shouldKeepTopBarOverlayForReturn &&
                isGridTargetingVisible &&
                (
                    isGridEnteringFromBackStack ||
                        (hasObservedGridReEnter && isSharedTransitionRunning)
                    )
        }
    }
    val topBarOverlayAlpha by animatedVisibilityScope.transition.animateFloat(label = "SectionGridTopBarOverlayAlpha") { state ->
        if (state == EnterExitState.Visible) 1f else 0f
    }

    val filtersOverlayModifier = with(sharedTransitionScope) {
        Modifier
            .fillMaxWidth()
            .renderInSharedTransitionScopeOverlay(
                zIndexInOverlay = 2f,
                renderInOverlay = { shouldRenderTopBarInOverlay }
            )
            .graphicsLayer {
                alpha = if (shouldRenderTopBarInOverlay) topBarOverlayAlpha else 1f
            }
    }

    LaunchedEffect(shouldKeepTopBarOverlayForReturn, isGridEnteringFromBackStack) {
        if (shouldKeepTopBarOverlayForReturn && isGridEnteringFromBackStack) {
            hasObservedGridReEnter = true
        }
    }

    LaunchedEffect(
        shouldKeepTopBarOverlayForReturn,
        hasObservedGridReEnter,
        isGridFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepTopBarOverlayForReturn &&
            hasObservedGridReEnter &&
            isGridFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepTopBarOverlayForReturn = false
            hasObservedGridReEnter = false
        }
    }

    CollapsingTopBarScaffold(
        title = sectionTitle,
        onBackClick = onBackClick,
        scrollableState = gridState,
        topBarModifier = with(sharedTransitionScope) {
            Modifier
                .renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = 1f,
                    renderInOverlay = { shouldRenderTopBarInOverlay }
                )
                .graphicsLayer {
                    alpha = if (shouldRenderTopBarInOverlay) topBarOverlayAlpha else 1f
                }
        },
        belowBar = {
            Box(modifier = filtersOverlayModifier) {
                SearchFiltersRow(
                    mediaType = uiState.mediaType,
                    selectedFormat = uiState.selectedFormat,
                    onFormatSelected = { format ->
                        coroutineScope.launch {
                            gridState.scrollToItem(0, 0) // Reset scroll immediately on UI filter click
                            viewModel.onAction(SectionGridAction.SetFormatFilter(format))
                        }
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    ) { topContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topContentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            AppCircularProgressIndicator()
                        }
                    }

                    uiState.errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topContentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    uiState.items.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topContentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_no_items),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        val placementSpec = AppMotion.rememberOffsetSpatialSpec()
                        val fadeSpec = AppMotion.rememberEffectsSpec()

                        LazyVerticalGrid(
                            columns = posterGridColumns(baseMinSize = 150.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = topContentPadding + 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                            state = gridState
                        ) {
                            itemsIndexed(
                                uiState.items,
                                key = { index, item -> "grid_${item.mediaId}_$index" }
                            ) { _, item ->
                                PosterCard(
                                    item = item,
                                    titleLanguage = titleLanguage,
                                    onClick = { navigateToMediaDetails(item.mediaId) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    transitionPrefix = "sectiongrid",
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = fadeSpec,
                                        fadeOutSpec = fadeSpec,
                                        placementSpec = placementSpec
                                    )
                                )
                            }

                            if (uiState.isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppCircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesGridScreen(
    sectionTitle: String, // Likely "Favorites"
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    viewModel: com.anisync.android.presentation.profile.ProfileViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle(initialValue = TitleLanguage.ROMAJI)

    val items = uiState.profile?.favoriteAnime.orEmpty()

    MediaGridContent(
        title = sectionTitle,
        items = items,
        isLoading = uiState.isLoading,
        titleLanguage = titleLanguage,
        errorMessage = uiState.errorMessage,
        onBackClick = onBackClick,
        onMediaClick = onMediaClick,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}
