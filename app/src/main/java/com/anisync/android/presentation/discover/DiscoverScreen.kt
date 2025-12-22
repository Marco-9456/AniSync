package com.anisync.android.presentation.discover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anisync.android.R
import com.anisync.android.domain.SearchFilters
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.MediaTypeSelector
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.discover.components.CinematicHeroCarousel
import com.anisync.android.presentation.discover.components.DiscoverShimmer
import com.anisync.android.presentation.discover.components.HorizontalMediaList
import com.anisync.android.presentation.discover.components.SearchFilterDialog
import com.anisync.android.presentation.discover.components.SearchResultItem
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.StarGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreen(
    onMediaClick: (Int) -> Unit,
    onSectionSeeAllClick: (title: String, sectionType: String, mediaType: MediaType) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchFilters by viewModel.searchFilters.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // Search Bar State with persistence
    val isSearchExpanded = rememberSaveable { mutableStateOf(false) }
    val searchBarState = rememberSearchBarState(
        initialValue = if (isSearchExpanded.value) SearchBarValue.Expanded else SearchBarValue.Collapsed
    )
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) { androidx.compose.foundation.lazy.LazyListState() }
    // Main content scroll state (separate from search results)
    val mainListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) { androidx.compose.foundation.lazy.LazyListState() }

    // Filter dialog state
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }

    // Sync textFieldState changes with ViewModel
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { viewModel.onSearchQueryChange(it) }
    }

    // Sync searchBarState changes back to isSearchExpanded to persist state
    LaunchedEffect(searchBarState.currentValue) {
        isSearchExpanded.value = searchBarState.currentValue == SearchBarValue.Expanded
        
        // Clear keyboard and focus when search bar is collapsing
        if (searchBarState.currentValue == SearchBarValue.Collapsed) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    // Handle back press to close search
    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) {
        keyboardController?.hide()
        coroutineScope.launch { searchBarState.animateToCollapsed() }
    }

    // ExpandedFullScreenSearchBar displays the search results in a full-screen overlay
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = {
            SearchBarDefaults.InputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = {
                    viewModel.onSearch(textFieldState.text.toString())
                    keyboardController?.hide()
                },
                placeholder = { Text(if (mediaType == MediaType.ANIME) stringResource(R.string.search_anime_placeholder) else stringResource(R.string.search_manga_placeholder)) },
                leadingIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        coroutineScope.launch { searchBarState.animateToCollapsed() }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                textFieldState.edit { replace(0, length, "") }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }
                        // Filter button with badge when filters are active
                        IconButton(onClick = { showFilterDialog = true }) {
                            if (searchFilters.hasActiveFilters) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(searchFilters.activeFilterCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
                                }
                            } else {
                                Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
                            }
                        }
                    }
                }
            )
        }
    ) {
        // Search Results Content
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                items(searchResults, key = { it.mediaId }) { item ->
                    SearchResultItem(
                        item = item,
                        onClick = {
                            keyboardController?.hide()
                            onMediaClick(item.mediaId)
                        }
                    )
                }
            }
        }
    }

    // Search Filter Dialog
    if (showFilterDialog) {
        SearchFilterDialog(
            filters = searchFilters,
            mediaType = mediaType,
            onFiltersChanged = { viewModel.updateFilters(it) },
            onDismiss = { showFilterDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // AppBarWithSearch integrates with Scaffold's topBar for proper window insets handling
            // Custom Header instead of AppBarWithSearch
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 16.dp)
            ) {
                SectionHeader(
                    title = stringResource(R.string.nav_discover),
                    level = HeaderLevel.Screen,
                    actionIcon = Icons.Default.Search,
                    onActionClick = {
                        keyboardController?.hide()
                        coroutineScope.launch { searchBarState.animateToExpanded() }
                    },
                    modifier = Modifier.padding(horizontal = 24.dp),
                    padding = PaddingValues(0.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                MediaTypeSelector(
                    selected = mediaType,
                    onSelect = viewModel::onMediaTypeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { paddingValues ->
        val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

        // Main Content
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = isRefreshing,
                    state = pullToRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
        ) {
            LazyColumn(
                state = mainListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                when (val state = uiState) {
                    is DiscoverUiState.Loading -> {
                        item { DiscoverShimmer() }
                    }
                    is DiscoverUiState.Error -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = stringResource(R.string.error_failed_to_load), color = MaterialTheme.colorScheme.error)
                                    Text(text = state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    is DiscoverUiState.Success -> {
                        // MediaTypeSelector moved to sticky header

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            val trendingTitle = stringResource(R.string.section_trending_now)
                            SectionHeader(
                                title = trendingTitle,
                                iconColor = Color(0xFFFF5722),
                                onActionClick = { onSectionSeeAllClick(trendingTitle, "trending", mediaType) },
                                level = HeaderLevel.Section
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CinematicHeroCarousel(
                                items = state.trending.take(10),
                                onItemClick = onMediaClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(48.dp))
                            val popularTitle = stringResource(R.string.section_all_time_popular)
                            SectionHeader(
                                title = popularTitle,
                                iconColor = StarGold,
                                onActionClick = { onSectionSeeAllClick(popularTitle, "popular", mediaType) },
                                level = HeaderLevel.Section
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalMediaList(
                                items = state.popular,
                                onItemClick = onMediaClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Upcoming section only shown for Anime
                        if (mediaType == MediaType.ANIME) {
                            item {
                                Spacer(modifier = Modifier.height(48.dp))
                                val upcomingTitle = stringResource(R.string.section_upcoming_season)
                                SectionHeader(
                                    title = upcomingTitle,
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    onActionClick = { onSectionSeeAllClick(upcomingTitle, "upcoming", mediaType) },
                                    level = HeaderLevel.Section
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalMediaList(
                                    items = state.upcoming.take(10),
                                    onItemClick = onMediaClick,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(48.dp))
                            val tbaTitle = stringResource(R.string.section_tba)
                            SectionHeader(
                                title = tbaTitle,
                                iconColor = Color(0xFF9E9E9E), // Gray for TBA
                                onActionClick = { onSectionSeeAllClick(tbaTitle, "tba", mediaType) },
                                level = HeaderLevel.Section
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalMediaList(
                                items = state.tba.take(10),
                                onItemClick = onMediaClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

