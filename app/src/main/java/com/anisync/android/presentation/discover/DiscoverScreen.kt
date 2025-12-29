package com.anisync.android.presentation.discover

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.anisync.android.R
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

private const val TAG = "DiscoverScreen"

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun DiscoverScreen(
    onMediaClick: (Int) -> Unit,
    onSectionSeeAllClick: (title: String, sectionType: String, mediaType: MediaType) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // Add lifecycle logging for performance monitoring
    DisposableEffect(Unit) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "DiscoverScreen: Composition started")
        onDispose {
            Log.d(TAG, "DiscoverScreen: Disposed after ${System.currentTimeMillis() - startTime}ms")
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchFilters by viewModel.searchFilters.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()

    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    val listState =
        rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val mainListState =
        rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    var showFilterDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { viewModel.onSearchQueryChange(it) }
    }

    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Collapsed) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    // Optimization: Memoize search result click to avoid recreation in lazy list
    val onSearchItemClick: (Int) -> Unit = remember(onMediaClick, keyboardController) {
        { id ->
            keyboardController?.hide()
            onMediaClick(id)
        }
    }

    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) {
        keyboardController?.hide()
        coroutineScope.launch { searchBarState.animateToCollapsed() }
    }

    // Optimization: remember inputField to avoid recreation on every recomposition
    val inputField = remember(searchBarState.currentValue, mediaType, searchQuery, searchFilters) {
        @Composable {
            SearchBarDefaults.InputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = {
                    viewModel.onSearch(textFieldState.text.toString())
                    keyboardController?.hide()
                },
                placeholder = {
                    val textRes = if (mediaType == MediaType.ANIME)
                        R.string.search_anime_placeholder
                    else
                        R.string.search_manga_placeholder

                    if (searchBarState.currentValue == SearchBarValue.Collapsed) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(textRes),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(stringResource(textRes))
                    }
                },
                leadingIcon = {
                    if (searchBarState.currentValue == SearchBarValue.Expanded) {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            coroutineScope.launch { searchBarState.animateToCollapsed() }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (searchBarState.currentValue == SearchBarValue.Expanded) {
                        Row {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    textFieldState.edit { replace(0, length, "") }
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.clear)
                                    )
                                }
                            }
                            IconButton(onClick = { showFilterDialog = true }) {
                                if (searchFilters.hasActiveFilters) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(searchFilters.activeFilterCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.FilterList,
                                            contentDescription = stringResource(R.string.filter)
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = stringResource(R.string.filter)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    if (showFilterDialog) {
        SearchFilterDialog(
            filters = searchFilters,
            mediaType = mediaType,
            onFiltersChanged = { viewModel.updateFilters(it) },
            onDismiss = { showFilterDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                AppBarWithSearch(
                    scrollBehavior = scrollBehavior,
                    state = searchBarState,
                    inputField = inputField,
                    windowInsets = WindowInsets(0),
                    colors = SearchBarDefaults.appBarWithSearchColors(
                        appBarContainerColor = Color.Transparent,
                        scrolledAppBarContainerColor = Color.Transparent
                    )
                )

                MediaTypeSelector(
                    selected = mediaType,
                    onSelect = viewModel::onMediaTypeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    ) { paddingValues ->
        val pullToRefreshState =
            rememberPullToRefreshState()

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
                        item(contentType = "shimmer") { DiscoverShimmer() }
                    }

                    is DiscoverUiState.Error -> {
                        item(contentType = "error") {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.error_failed_to_load),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is DiscoverUiState.Success -> {
                        // Trending Section
                        item(contentType = "section_header") {
                            val trendingTitle = stringResource(R.string.section_trending_now)
                            Spacer(modifier = Modifier.height(24.dp))
                            SectionHeader(
                                title = trendingTitle,
                                iconColor = Color(0xFFFF5722),

                                onActionClick = remember(mediaType, onSectionSeeAllClick) {
                                    {
                                        onSectionSeeAllClick(
                                            trendingTitle,
                                            "trending",
                                            mediaType
                                        )
                                    }
                                },
                                level = HeaderLevel.Section
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item(contentType = "hero_carousel") {
                            // Optimization: Slice the list outside of the composable parameters if possible, 
                            // though here it's simple enough inside item.
                            CinematicHeroCarousel(
                                items = remember(state.trending) { state.trending.take(10) },
                                onItemClick = onMediaClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Popular Section
                        item(contentType = "section_header") {
                            val popularTitle = stringResource(R.string.section_all_time_popular)
                            Spacer(modifier = Modifier.height(48.dp))
                            SectionHeader(
                                title = popularTitle,
                                iconColor = StarGold,
                                onActionClick = remember(mediaType, onSectionSeeAllClick) {
                                    {
                                        onSectionSeeAllClick(
                                            popularTitle,
                                            "popular",
                                            mediaType
                                        )
                                    }
                                },
                                level = HeaderLevel.Section
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item(contentType = "media_list") {
                            HorizontalMediaList(
                                items = state.popular,
                                onItemClick = onMediaClick,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Upcoming Section
                        if (mediaType == MediaType.ANIME) {
                            item(contentType = "section_header") {
                                val upcomingTitle = stringResource(R.string.section_upcoming_season)
                                Spacer(modifier = Modifier.height(48.dp))
                                SectionHeader(
                                    title = upcomingTitle,
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    onActionClick = remember(mediaType, onSectionSeeAllClick) {
                                        {
                                            onSectionSeeAllClick(
                                                upcomingTitle,
                                                "upcoming",
                                                mediaType
                                            )
                                        }
                                    },
                                    level = HeaderLevel.Section
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            item(contentType = "media_list") {
                                HorizontalMediaList(
                                    items = remember(state.upcoming) { state.upcoming.take(10) },
                                    onItemClick = onMediaClick,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // TBA Section
                        item(contentType = "section_header") {
                            val tbaTitle = stringResource(R.string.section_tba)
                            Spacer(modifier = Modifier.height(48.dp))
                            SectionHeader(
                                title = tbaTitle,
                                iconColor = Color(0xFF9E9E9E),
                                onActionClick = remember(mediaType, onSectionSeeAllClick) {
                                    {
                                        onSectionSeeAllClick(
                                            tbaTitle,
                                            "tba",
                                            mediaType
                                        )
                                    }
                                },
                                level = HeaderLevel.Section
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item(contentType = "media_list") {
                            HorizontalMediaList(
                                items = remember(state.tba) { state.tba.take(10) },
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

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField
    ) {
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
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                items(
                    items = searchResults,
                    key = { it.mediaId },
                    contentType = { "search_result" }
                ) { item ->
                    SearchResultItem(
                        item = item,
                        onClick = { onSearchItemClick(item.mediaId) }
                    )
                }
            }
        }
    }
}