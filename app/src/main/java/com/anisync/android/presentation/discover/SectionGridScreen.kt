@file:OptIn(ExperimentalMaterial3Api::class)

package com.anisync.android.presentation.discover

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.PosterCard
import com.anisync.android.presentation.discover.components.FormatFilterRow
import com.anisync.android.presentation.util.AppMotion
import kotlinx.coroutines.launch

/**
 * Grid screen for displaying all media items from a Discover section.
 * Reuses the card pattern from LibraryScreen but without controls.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items, key = { it.mediaId }) { item ->
                            // Optimization: Use stable callback
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

@OptIn(ExperimentalSharedTransitionApi::class)
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
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val hasNextPage by viewModel.hasNextPage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val mediaType by viewModel.mediaType.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle(initialValue = TitleLanguage.ROMAJI)
    
    // Grid state - we'll reset it manually when filter changes
    // This prevents duplicate key crashes when switching between filters during fling animations
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    
    // Reset scroll position when filter changes to prevent key conflicts
    LaunchedEffect(selectedFormat) {
        gridState.scrollToItem(0, 0)
    }
    
    // Infinite scroll detection using snapshotFlow for proper reactivity
    LaunchedEffect(gridState) {
        snapshotFlow {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 6 && totalItems > 0
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !viewModel.isLoadingMore.value && viewModel.hasNextPage.value && !viewModel.isLoading.value) {
                viewModel.loadNextPage()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Format filter row
            FormatFilterRow(
                mediaType = mediaType,
                selectedFormat = selectedFormat,
                onFormatSelected = { format ->
                    // Stop any ongoing scroll/fling animation before changing filter
                    // This prevents race conditions where animation tries to use stale keys
                    coroutineScope.launch {
                        gridState.scrollToItem(0, 0) // Reset scroll to stop ongoing animations
                        viewModel.setFormatFilter(format)
                    }
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    items.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No items found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        val placementSpec = AppMotion.rememberOffsetSpatialSpec()
                        val fadeSpec = AppMotion.rememberEffectsSpec()
                        
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                            state = gridState
                        ) {
                            items(items, key = { it.mediaId }) { item ->
                                // Optimization: Use stable callback
                                PosterCard(
                                    item = item,
                                    titleLanguage = titleLanguage,
                                    onClick = { onMediaClick(item.mediaId) },
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
                            
                            // Loading indicator at bottom
                            if (isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
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
    
    val items = if (uiState is com.anisync.android.presentation.profile.ProfileUiState.Success) {
        (uiState as com.anisync.android.presentation.profile.ProfileUiState.Success).profile.favoriteAnime
    } else emptyList()

    MediaGridContent(
        title = sectionTitle,
        items = items,
        isLoading = uiState is com.anisync.android.presentation.profile.ProfileUiState.Loading,
        titleLanguage = titleLanguage,
        errorMessage = (uiState as? com.anisync.android.presentation.profile.ProfileUiState.Error)?.message,
        onBackClick = onBackClick,
        onMediaClick = onMediaClick,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}
