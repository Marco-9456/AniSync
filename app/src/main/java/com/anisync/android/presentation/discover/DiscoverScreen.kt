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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.MediaTypeSelector
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.discover.components.DiscoverHeroCarousel
import com.anisync.android.presentation.discover.components.DiscoverShimmer
import com.anisync.android.presentation.discover.components.HorizontalMediaList
import com.anisync.android.presentation.discover.components.SearchFilterDialog
import com.anisync.android.presentation.discover.components.SearchResultItem
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.StarGold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

private const val TAG = "DiscoverScreen"

private val TrendingIconColor = Color(0xFFFF5722)
private val TbaIconColor = Color(0xFF9E9E9E)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    FlowPreview::class
)
@Composable
fun DiscoverScreen(
    onMediaClick: (Int) -> Unit,
    onSectionSeeAllClick: (title: String, sectionType: String, mediaType: MediaType) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    DisposableEffect(Unit) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "DiscoverScreen: Composition started")
        onDispose {
            Log.d(TAG, "DiscoverScreen: Disposed after ${System.currentTimeMillis() - startTime}ms")
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val titleLanguage by viewModel.titleLanguage.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val searchBarState = rememberSearchBarState()

    val initialQuery = rememberSaveable { (uiState as? DiscoverUiState.Success)?.searchQuery ?: "" }
    val textFieldState = rememberTextFieldState(initialText = initialQuery)

    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    val pullToRefreshState = rememberPullToRefreshState()

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val currentMediaType = (uiState as? DiscoverUiState.Success)?.mediaType ?: MediaType.ANIME

    // Separate scroll state memory for Anime vs Manga tabs
    val mainListState =
        rememberSaveable(currentMediaType, saver = LazyListState.Saver) { LazyListState() }

    var showFilterDialog by rememberSaveable { mutableStateOf(false) }

    val trendingTitle = stringResource(R.string.section_trending_now)
    val popularTitle = stringResource(R.string.section_all_time_popular)
    val upcomingTitle = stringResource(R.string.section_upcoming_season)
    val tbaTitle = stringResource(R.string.section_tba)

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { viewModel.onAction(DiscoverAction.OnSearchQueryChange(it)) }
    }

    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Collapsed) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    val onSearchItemClick: (Int) -> Unit = remember(onMediaClick, keyboardController) {
        { id ->
            keyboardController?.hide()
            onMediaClick(id)
        }
    }

    val onRefresh: () -> Unit =
        remember(viewModel) { { viewModel.onAction(DiscoverAction.Refresh) } }

    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) {
        keyboardController?.hide()
        coroutineScope.launch { searchBarState.animateToCollapsed() }
    }

    val currentSearchFilters = (uiState as? DiscoverUiState.Success)?.searchFilters
        ?: com.anisync.android.domain.SearchFilters()

    if (showFilterDialog) {
        SearchFilterDialog(
            filters = currentSearchFilters,
            mediaType = currentMediaType,
            onFiltersChanged = { viewModel.onAction(DiscoverAction.UpdateFilters(it)) },
            onDismiss = { showFilterDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DiscoverTopBar(
                scrollBehavior = scrollBehavior,
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                mediaType = currentMediaType,
                searchFilters = currentSearchFilters,
                coroutineScope = coroutineScope,
                keyboardController = keyboardController,
                onSearch = { viewModel.onAction(DiscoverAction.OnSearch(textFieldState.text.toString())) },
                onMediaTypeChange = { viewModel.onAction(DiscoverAction.OnMediaTypeChange(it)) },
                onShowFilterDialog = { showFilterDialog = true }
            )
        }
    ) { paddingValues ->
        val successState = uiState as? DiscoverUiState.Success

        DiscoverContent(
            isLoading = uiState is DiscoverUiState.Loading,
            errorMessage = (uiState as? DiscoverUiState.Error)?.message,
            trending = successState?.trending ?: emptyList(),
            popular = successState?.popular ?: emptyList(),
            upcoming = successState?.upcoming ?: emptyList(),
            tba = successState?.tba ?: emptyList(),
            mediaType = currentMediaType,
            titleLanguage = titleLanguage,
            isRefreshing = successState?.isRefreshing ?: false,
            mainListState = mainListState,
            pullToRefreshState = pullToRefreshState,
            paddingValues = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 80.dp
            ),
            trendingTitle = trendingTitle,
            popularTitle = popularTitle,
            upcomingTitle = upcomingTitle,
            tbaTitle = tbaTitle,
            onRefresh = onRefresh,
            onMediaClick = onMediaClick,
            onSectionSeeAllClick = onSectionSeeAllClick,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }

    val successState2 = uiState as? DiscoverUiState.Success
    val searchQuery = successState2?.searchQuery ?: ""
    val searchResults = successState2?.searchResults ?: emptyList()
    val isSearching = successState2?.isSearching ?: false
    val searchError = successState2?.searchError

    DiscoverSearchOverlay(
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        mediaType = currentMediaType,
        titleLanguage = titleLanguage,
        searchFilters = currentSearchFilters,
        coroutineScope = coroutineScope,
        keyboardController = keyboardController,
        listState = listState,
        searchQuery = searchQuery,
        searchResults = searchResults,
        isSearching = isSearching,
        searchError = searchError,
        onSearch = { viewModel.onAction(DiscoverAction.OnSearch(it)) },
        onSearchItemClick = onSearchItemClick,
        onShowFilterDialog = { showFilterDialog = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoverTopBar(
    scrollBehavior: SearchBarScrollBehavior?,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    mediaType: MediaType,
    searchFilters: com.anisync.android.domain.SearchFilters,
    coroutineScope: CoroutineScope,
    keyboardController: SoftwareKeyboardController?,
    onSearch: () -> Unit,
    onMediaTypeChange: (MediaType) -> Unit,
    onShowFilterDialog: () -> Unit
) {
    Column(
        modifier = Modifier.statusBarsPadding()
    ) {
        AppBarWithSearch(
            scrollBehavior = scrollBehavior,
            state = searchBarState,
            inputField = {
                SearchInputField(
                    searchBarState = searchBarState,
                    textFieldState = textFieldState,
                    mediaType = mediaType,
                    searchFilters = searchFilters,
                    coroutineScope = coroutineScope,
                    keyboardController = keyboardController,
                    onSearch = onSearch,
                    onShowFilterDialog = onShowFilterDialog
                )
            },
            colors = SearchBarDefaults.appBarWithSearchColors(
                appBarContainerColor = Color.Transparent,
                scrolledAppBarContainerColor = Color.Transparent
            )
        )

        MediaTypeSelector(
            selected = mediaType,
            onSelect = onMediaTypeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchInputField(
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    mediaType: MediaType,
    searchFilters: com.anisync.android.domain.SearchFilters,
    coroutineScope: CoroutineScope,
    keyboardController: SoftwareKeyboardController?,
    onSearch: () -> Unit,
    onShowFilterDialog: () -> Unit
) {
    val isExpanded = searchBarState.currentValue == SearchBarValue.Expanded
    val hasText by remember { derivedStateOf { textFieldState.text.isNotEmpty() } }

    val placeholderTextRes by remember(mediaType) {
        derivedStateOf {
            if (mediaType == MediaType.ANIME) {
                R.string.search_anime_placeholder
            } else {
                R.string.search_manga_placeholder
            }
        }
    }

    SearchBarDefaults.InputField(
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        onSearch = {
            onSearch()
            keyboardController?.hide()
        },
        placeholder = {
            if (!isExpanded) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(placeholderTextRes),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(stringResource(placeholderTextRes))
            }
        },
        leadingIcon = {
            if (isExpanded) {
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
            if (isExpanded) {
                SearchTrailingIcons(
                    hasText = hasText,
                    searchFilters = searchFilters,
                    onClearText = { textFieldState.edit { replace(0, length, "") } },
                    onShowFilterDialog = onShowFilterDialog
                )
            }
        }
    )
}

@Composable
private fun SearchTrailingIcons(
    hasText: Boolean,
    searchFilters: com.anisync.android.domain.SearchFilters,
    onClearText: () -> Unit,
    onShowFilterDialog: () -> Unit
) {
    Row {
        if (hasText) {
            IconButton(onClick = onClearText) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear)
                )
            }
        }
        IconButton(onClick = onShowFilterDialog) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun DiscoverContent(
    isLoading: Boolean,
    errorMessage: String?,
    trending: List<LibraryEntry>,
    popular: List<LibraryEntry>,
    upcoming: List<LibraryEntry>,
    tba: List<LibraryEntry>,
    mediaType: MediaType,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    isRefreshing: Boolean,
    mainListState: LazyListState,
    pullToRefreshState: PullToRefreshState,
    paddingValues: PaddingValues,
    trendingTitle: String,
    popularTitle: String,
    upcomingTitle: String,
    tbaTitle: String,
    onRefresh: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onSectionSeeAllClick: (title: String, sectionType: String, mediaType: MediaType) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
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
            when {
                isLoading -> {
                    item(key = "shimmer", contentType = "shimmer") { DiscoverShimmer() }
                }

                errorMessage != null -> {
                    item(key = "error", contentType = "error") {
                        ErrorContent(message = errorMessage)
                    }
                }

                else -> {
                    item(key = "trending_header", contentType = "section_header") {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(
                            title = trendingTitle,
                            iconColor = TrendingIconColor,
                            onActionClick = {
                                onSectionSeeAllClick(
                                    trendingTitle,
                                    "trending",
                                    mediaType
                                )
                            },
                            level = HeaderLevel.Section
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item(key = "trending_carousel", contentType = "hero_carousel") {
                        val trendingItems = remember(trending) { trending.take(10) }
                        DiscoverHeroCarousel(
                            items = trendingItems,
                            onItemClick = onMediaClick,
                            titleLanguage = titleLanguage,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item(key = "popular_header", contentType = "section_header") {
                        Spacer(modifier = Modifier.height(48.dp))
                        SectionHeader(
                            title = popularTitle,
                            iconColor = StarGold,
                            onActionClick = {
                                onSectionSeeAllClick(
                                    popularTitle,
                                    "popular",
                                    mediaType
                                )
                            },
                            level = HeaderLevel.Section
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item(key = "popular_list", contentType = "media_list") {
                        HorizontalMediaList(
                            items = popular,
                            onItemClick = onMediaClick,
                            titleLanguage = titleLanguage,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (mediaType == MediaType.ANIME) {
                        item(key = "upcoming_header", contentType = "section_header") {
                            Spacer(modifier = Modifier.height(48.dp))
                            SectionHeader(
                                title = upcomingTitle,
                                iconColor = MaterialTheme.colorScheme.primary,
                                onActionClick = {
                                    onSectionSeeAllClick(
                                        upcomingTitle,
                                        "upcoming",
                                        mediaType
                                    )
                                },
                                level = HeaderLevel.Section
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item(key = "upcoming_list", contentType = "media_list") {
                            val upcomingItems = remember(upcoming) { upcoming.take(10) }
                            HorizontalMediaList(
                                items = upcomingItems,
                                onItemClick = onMediaClick,
                                titleLanguage = titleLanguage,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    item(key = "tba_header", contentType = "section_header") {
                        Spacer(modifier = Modifier.height(48.dp))
                        SectionHeader(
                            title = tbaTitle,
                            iconColor = TbaIconColor,
                            onActionClick = { onSectionSeeAllClick(tbaTitle, "tba", mediaType) },
                            level = HeaderLevel.Section
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item(key = "tba_list", contentType = "media_list") {
                        val tbaItems = remember(tba) { tba.take(10) }
                        HorizontalMediaList(
                            items = tbaItems,
                            onItemClick = onMediaClick,
                            titleLanguage = titleLanguage,
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

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.error_failed_to_load),
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoverSearchOverlay(
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    mediaType: MediaType,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    searchFilters: com.anisync.android.domain.SearchFilters,
    coroutineScope: CoroutineScope,
    keyboardController: SoftwareKeyboardController?,
    listState: LazyListState,
    searchQuery: String,
    searchResults: List<LibraryEntry>,
    isSearching: Boolean,
    searchError: String?,
    onSearch: (String) -> Unit,
    onSearchItemClick: (Int) -> Unit,
    onShowFilterDialog: () -> Unit
) {
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = {
            SearchInputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                mediaType = mediaType,
                searchFilters = searchFilters,
                coroutineScope = coroutineScope,
                keyboardController = keyboardController,
                onSearch = { onSearch(textFieldState.text.toString()) },
                onShowFilterDialog = onShowFilterDialog
            )
        }
    ) {
        SearchResultsContent(
            isSearching = isSearching,
            searchResults = searchResults,
            searchQuery = searchQuery,
            searchError = searchError,
            titleLanguage = titleLanguage,
            listState = listState,
            onSearchItemClick = onSearchItemClick
        )
    }
}

@Composable
private fun SearchResultsContent(
    isSearching: Boolean,
    searchResults: List<LibraryEntry>,
    searchQuery: String,
    searchError: String?,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    listState: LazyListState,
    onSearchItemClick: (Int) -> Unit
) {
    when {
        isSearching -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        searchError != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.error_failed_to_load),
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = searchError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        searchResults.isEmpty() && searchQuery.isNotEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> {
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
                    val onClick = remember(item.mediaId, onSearchItemClick) {
                        { onSearchItemClick(item.mediaId) }
                    }
                    SearchResultItem(
                        item = item,
                        onClick = onClick,
                        titleLanguage = titleLanguage
                    )
                }
            }
        }
    }
}