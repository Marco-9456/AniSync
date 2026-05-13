package com.anisync.android.presentation.discover

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AppBarWithSearch
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.MediaTypeSelector
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.discover.components.DiscoverHeroCarousel
import com.anisync.android.presentation.discover.components.DiscoverShimmer
import com.anisync.android.presentation.discover.components.HorizontalMediaList
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
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    onUserClick: (String) -> Unit = {},
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


    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedDiscoverReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToMediaDetails: (Int) -> Unit = remember(onMediaClick) {
        { mediaId ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedDiscoverReEnter = false
            onMediaClick(mediaId)
        }
    }

    val isDiscoverEnteringFromBackStack by remember {
        derivedStateOf {
            animatedVisibilityScope.transition.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isDiscoverTargetingVisible by remember {
        derivedStateOf {
            animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isDiscoverFullyVisible by remember {
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
                isDiscoverTargetingVisible &&
                (
                    isDiscoverEnteringFromBackStack ||
                        (hasObservedDiscoverReEnter && isSharedTransitionRunning)
                    )
        }
    }
    val topBarOverlayAlpha by animatedVisibilityScope.transition.animateFloat(label = "DiscoverTopBarOverlayAlpha") { state ->
        if (state == EnterExitState.Visible) 1f else 0f
    }

    LaunchedEffect(shouldKeepTopBarOverlayForReturn, isDiscoverEnteringFromBackStack) {
        if (shouldKeepTopBarOverlayForReturn && isDiscoverEnteringFromBackStack) {
            hasObservedDiscoverReEnter = true
        }
    }

    LaunchedEffect(
        shouldKeepTopBarOverlayForReturn,
        hasObservedDiscoverReEnter,
        isDiscoverFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepTopBarOverlayForReturn &&
            hasObservedDiscoverReEnter &&
            isDiscoverFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepTopBarOverlayForReturn = false
            hasObservedDiscoverReEnter = false
        }
    }

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

    val onSearchItemClick: (Int) -> Unit = remember(navigateToMediaDetails, searchBarState, coroutineScope, keyboardController) {
        { id ->
            keyboardController?.hide()
            // Collapse the full-screen search overlay before navigating; the overlay
            // is a Popup window that otherwise persists over MediaDetails and lets
            // tap/back events keep firing onto a stale list, repeatedly re-pushing
            // the detail destination (observed on Android 16 with predictive back).
            coroutineScope.launch { searchBarState.animateToCollapsed() }
            navigateToMediaDetails(id)
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            with(sharedTransitionScope) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .renderInSharedTransitionScopeOverlay(
                            zIndexInOverlay = 1f,
                            renderInOverlay = { shouldRenderTopBarInOverlay }
                        )
                        .graphicsLayer {
                            alpha = if (shouldRenderTopBarInOverlay) topBarOverlayAlpha else 1f
                        },
                    color = MaterialTheme.colorScheme.background
                ) {
                    DiscoverTopBar(
                        scrollBehavior = scrollBehavior,
                        searchBarState = searchBarState,
                        textFieldState = textFieldState,
                        mediaType = currentMediaType,
                        coroutineScope = coroutineScope,
                        keyboardController = keyboardController,
                        onSearch = { viewModel.onAction(DiscoverAction.OnSearch(textFieldState.text.toString())) },
                        onMediaTypeChange = { viewModel.onAction(DiscoverAction.OnMediaTypeChange(it)) }
                    )
                }
            }
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
            onMediaClick = navigateToMediaDetails,
            onSectionSeeAllClick = onSectionSeeAllClick,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }

    val successState2 = uiState as? DiscoverUiState.Success
    val searchQuery = successState2?.searchQuery ?: ""
    val searchAnime = successState2?.searchAnime ?: emptyList()
    val searchManga = successState2?.searchManga ?: emptyList()
    val groupedResults = successState2?.groupedResults
        ?: com.anisync.android.domain.GroupedSearchResults()
    val isSearching = successState2?.isSearching ?: false
    val searchError = successState2?.searchError

    val onCharacterItemClick: (Int) -> Unit = remember(onCharacterClick, searchBarState, coroutineScope, keyboardController) {
        { id ->
            keyboardController?.hide()
            coroutineScope.launch { searchBarState.animateToCollapsed() }
            onCharacterClick(id)
        }
    }
    val onStaffItemClick: (Int) -> Unit = remember(onStaffClick, searchBarState, coroutineScope, keyboardController) {
        { id ->
            keyboardController?.hide()
            coroutineScope.launch { searchBarState.animateToCollapsed() }
            onStaffClick(id)
        }
    }
    val onStudioItemClick: (Int) -> Unit = remember(onStudioClick, searchBarState, coroutineScope, keyboardController) {
        { id ->
            keyboardController?.hide()
            coroutineScope.launch { searchBarState.animateToCollapsed() }
            onStudioClick(id)
        }
    }
    val onUserItemClick: (String) -> Unit = remember(onUserClick, searchBarState, coroutineScope, keyboardController) {
        { name ->
            keyboardController?.hide()
            coroutineScope.launch { searchBarState.animateToCollapsed() }
            onUserClick(name)
        }
    }

    val taxonomy by viewModel.taxonomy.collectAsStateWithLifecycle()
    val showAdultContent by viewModel.showAdultContent.collectAsStateWithLifecycle()
    val viewMode = successState2?.viewMode ?: com.anisync.android.data.DiscoverViewMode.LIST
    val activeCategory = successState2?.activeCategory ?: ResultCategory.ALL

    DiscoverSearchOverlay(
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        mediaType = currentMediaType,
        titleLanguage = titleLanguage,
        searchFilters = currentSearchFilters,
        taxonomy = taxonomy,
        showAdultContent = showAdultContent,
        coroutineScope = coroutineScope,
        keyboardController = keyboardController,
        listState = listState,
        searchQuery = searchQuery,
        searchAnime = searchAnime,
        searchManga = searchManga,
        groupedResults = groupedResults,
        isSearching = isSearching,
        searchError = searchError,
        viewMode = viewMode,
        activeCategory = activeCategory,
        onSearch = { viewModel.onAction(DiscoverAction.OnSearch(it)) },
        onFiltersChange = { viewModel.onAction(DiscoverAction.UpdateFilters(it)) },
        onLoadTaxonomy = { viewModel.onAction(DiscoverAction.LoadTaxonomy) },
        onViewModeChange = { viewModel.onAction(DiscoverAction.OnViewModeChange(it)) },
        onCategoryChange = { viewModel.onAction(DiscoverAction.OnCategoryChange(it)) },
        onSearchItemClick = onSearchItemClick,
        onCharacterClick = onCharacterItemClick,
        onStaffClick = onStaffItemClick,
        onStudioClick = onStudioItemClick,
        onUserClick = onUserItemClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoverTopBar(
    scrollBehavior: SearchBarScrollBehavior?,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    mediaType: MediaType,
    coroutineScope: CoroutineScope,
    keyboardController: SoftwareKeyboardController?,
    onSearch: () -> Unit,
    onMediaTypeChange: (MediaType) -> Unit
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
                    coroutineScope = coroutineScope,
                    keyboardController = keyboardController,
                    onSearch = onSearch
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
    coroutineScope: CoroutineScope,
    keyboardController: SoftwareKeyboardController?,
    onSearch: () -> Unit
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
                    onClearText = { textFieldState.edit { replace(0, length, "") } }
                )
            }
        }
    )
}

@Composable
private fun SearchTrailingIcons(
    hasText: Boolean,
    onClearText: () -> Unit
) {
    if (hasText) {
        IconButton(onClick = onClearText) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.clear)
            )
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
    taxonomy: SearchTaxonomy,
    showAdultContent: Boolean,
    coroutineScope: CoroutineScope,
    keyboardController: SoftwareKeyboardController?,
    listState: LazyListState,
    searchQuery: String,
    searchAnime: List<LibraryEntry>,
    searchManga: List<LibraryEntry>,
    groupedResults: com.anisync.android.domain.GroupedSearchResults,
    isSearching: Boolean,
    searchError: String?,
    viewMode: com.anisync.android.data.DiscoverViewMode,
    activeCategory: ResultCategory,
    onSearch: (String) -> Unit,
    onFiltersChange: (com.anisync.android.domain.SearchFilters) -> Unit,
    onLoadTaxonomy: () -> Unit,
    onViewModeChange: (com.anisync.android.data.DiscoverViewMode) -> Unit,
    onCategoryChange: (ResultCategory) -> Unit,
    onSearchItemClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit
) {
    var openedFilter by remember {
        mutableStateOf<com.anisync.android.presentation.discover.components.FilterId?>(null)
    }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = {
            SearchInputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                mediaType = mediaType,
                coroutineScope = coroutineScope,
                keyboardController = keyboardController,
                onSearch = { onSearch(textFieldState.text.toString()) }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            com.anisync.android.presentation.discover.components.SearchFilterChipBar(
                filters = searchFilters,
                onChipTap = { filterId ->
                    onLoadTaxonomy()
                    openedFilter = filterId
                }
            )
            SearchResultsContent(
                isSearching = isSearching,
                searchAnime = searchAnime,
                searchManga = searchManga,
                groupedResults = groupedResults,
                searchQuery = searchQuery,
                searchError = searchError,
                titleLanguage = titleLanguage,
                listState = listState,
                viewMode = viewMode,
                activeCategory = activeCategory,
                onViewModeChange = onViewModeChange,
                onCategoryChange = onCategoryChange,
                onSearchItemClick = onSearchItemClick,
                onCharacterClick = onCharacterClick,
                onStaffClick = onStaffClick,
                onStudioClick = onStudioClick,
                onUserClick = onUserClick
            )
        }
    }

    com.anisync.android.presentation.discover.components.SearchFilterSheetHost(
        openedFilter = openedFilter,
        filters = searchFilters,
        mediaType = mediaType,
        genres = taxonomy.genres,
        tags = taxonomy.tags,
        showAdultContent = showAdultContent,
        onFiltersChange = onFiltersChange,
        onDismiss = { openedFilter = null }
    )
}

@Composable
private fun SearchResultsContent(
    isSearching: Boolean,
    searchAnime: List<LibraryEntry>,
    searchManga: List<LibraryEntry>,
    groupedResults: com.anisync.android.domain.GroupedSearchResults,
    searchQuery: String,
    searchError: String?,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    listState: LazyListState,
    viewMode: com.anisync.android.data.DiscoverViewMode,
    activeCategory: ResultCategory,
    onViewModeChange: (com.anisync.android.data.DiscoverViewMode) -> Unit,
    onCategoryChange: (ResultCategory) -> Unit,
    onSearchItemClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit
) {
    val hasAnyResults = searchAnime.isNotEmpty() || searchManga.isNotEmpty() || !groupedResults.isEmpty

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

        !hasAnyResults && searchQuery.isNotEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> {
            val availableCategories = remember(searchAnime, searchManga, groupedResults) {
                buildSet {
                    add(ResultCategory.ALL)
                    if (searchAnime.isNotEmpty()) add(ResultCategory.ANIME)
                    if (searchManga.isNotEmpty()) add(ResultCategory.MANGA)
                    if (groupedResults.characters.isNotEmpty()) add(ResultCategory.CHARACTERS)
                    if (groupedResults.staff.isNotEmpty()) add(ResultCategory.STAFF)
                    if (groupedResults.users.isNotEmpty()) add(ResultCategory.USERS)
                    if (groupedResults.studios.isNotEmpty()) add(ResultCategory.STUDIOS)
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                com.anisync.android.presentation.discover.components.SearchResultsHeader(
                    activeCategory = activeCategory,
                    availableCategories = availableCategories,
                    viewMode = viewMode,
                    onCategoryChange = onCategoryChange,
                    onViewModeChange = onViewModeChange
                )
                if (viewMode == com.anisync.android.data.DiscoverViewMode.LIST) {
                    SearchResultsList(
                        searchAnime = searchAnime,
                        searchManga = searchManga,
                        groupedResults = groupedResults,
                        activeCategory = activeCategory,
                        titleLanguage = titleLanguage,
                        listState = listState,
                        onSearchItemClick = onSearchItemClick,
                        onCharacterClick = onCharacterClick,
                        onStaffClick = onStaffClick,
                        onStudioClick = onStudioClick,
                        onUserClick = onUserClick
                    )
                } else {
                    SearchResultsGrid(
                        searchAnime = searchAnime,
                        searchManga = searchManga,
                        groupedResults = groupedResults,
                        activeCategory = activeCategory,
                        titleLanguage = titleLanguage,
                        onSearchItemClick = onSearchItemClick,
                        onCharacterClick = onCharacterClick,
                        onStaffClick = onStaffClick,
                        onStudioClick = onStudioClick,
                        onUserClick = onUserClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    searchAnime: List<LibraryEntry>,
    searchManga: List<LibraryEntry>,
    groupedResults: com.anisync.android.domain.GroupedSearchResults,
    activeCategory: ResultCategory,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    listState: LazyListState,
    onSearchItemClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit
) {
    val showAll = activeCategory == ResultCategory.ALL
    val showAnime = showAll || activeCategory == ResultCategory.ANIME
    val showManga = showAll || activeCategory == ResultCategory.MANGA
    val showCharacters = showAll || activeCategory == ResultCategory.CHARACTERS
    val showStaff = showAll || activeCategory == ResultCategory.STAFF
    val showUsers = showAll || activeCategory == ResultCategory.USERS
    val showStudios = showAll || activeCategory == ResultCategory.STUDIOS

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
        if (showAnime && searchAnime.isNotEmpty()) {
            if (showAll) {
                item(key = "header_anime") {
                    SearchSectionHeader(title = "Anime")
                }
            }
            items(
                items = searchAnime,
                key = { "anime_${it.mediaId}" },
                contentType = { "search_result" }
            ) { item ->
                val onClick = remember(item.mediaId, onSearchItemClick) {
                    { onSearchItemClick(item.mediaId) }
                }
                SearchResultItem(item = item, onClick = onClick, titleLanguage = titleLanguage)
            }
        }
        if (showManga && searchManga.isNotEmpty()) {
            if (showAll) {
                item(key = "header_manga") {
                    SearchSectionHeader(title = "Manga")
                }
            }
            items(
                items = searchManga,
                key = { "manga_${it.mediaId}" },
                contentType = { "search_result" }
            ) { item ->
                val onClick = remember(item.mediaId, onSearchItemClick) {
                    { onSearchItemClick(item.mediaId) }
                }
                SearchResultItem(item = item, onClick = onClick, titleLanguage = titleLanguage)
            }
        }
        if (showCharacters && groupedResults.characters.isNotEmpty()) {
            if (showAll) {
                item(key = "header_characters") {
                    SearchSectionHeader(title = stringResource(R.string.search_header_characters))
                }
            }
            items(
                items = groupedResults.characters,
                key = { "char_${it.id}" },
                contentType = { "character_result" }
            ) { character ->
                GenericSearchResultItem(
                    name = character.displayName,
                    subtitle = character.nativeName,
                    imageUrl = character.imageUrl,
                    onClick = { onCharacterClick(character.id) }
                )
            }
        }
        if (showStaff && groupedResults.staff.isNotEmpty()) {
            if (showAll) {
                item(key = "header_staff") {
                    SearchSectionHeader(title = stringResource(R.string.search_header_staff))
                }
            }
            items(
                items = groupedResults.staff,
                key = { "staff_${it.id}" },
                contentType = { "staff_result" }
            ) { staff ->
                GenericSearchResultItem(
                    name = staff.displayName,
                    subtitle = staff.primaryOccupations.firstOrNull() ?: staff.nativeName,
                    imageUrl = staff.imageUrl,
                    onClick = { onStaffClick(staff.id) }
                )
            }
        }
        if (showUsers && groupedResults.users.isNotEmpty()) {
            if (showAll) {
                item(key = "header_users") {
                    SearchSectionHeader(title = stringResource(R.string.search_header_users))
                }
            }
            items(
                items = groupedResults.users,
                key = { "user_${it.id}" },
                contentType = { "user_result" }
            ) { user ->
                GenericSearchResultItem(
                    name = user.displayName,
                    subtitle = null,
                    imageUrl = user.imageUrl,
                    onClick = { onUserClick(user.displayName) }
                )
            }
        }
        if (showStudios && groupedResults.studios.isNotEmpty()) {
            if (showAll) {
                item(key = "header_studios") {
                    SearchSectionHeader(title = stringResource(R.string.search_header_studios))
                }
            }
            items(
                items = groupedResults.studios,
                key = { "studio_${it.id}" },
                contentType = { "studio_result" }
            ) { studio ->
                GenericSearchResultItem(
                    name = studio.displayName,
                    subtitle = studio.favourites?.let { stringResource(R.string.search_favourites_count, it) },
                    imageUrl = null,
                    onClick = { onStudioClick(studio.id) }
                )
            }
        }
    }
}

/**
 * Grid layout for search results. Media takes adaptive grid cells with
 * poster-style cards; non-media (characters/staff/users/studios) stays as
 * full-width list rows even in grid mode — their data is name + (sometimes)
 * avatar, which doesn't fill a card meaningfully.
 */
@Composable
private fun SearchResultsGrid(
    searchAnime: List<LibraryEntry>,
    searchManga: List<LibraryEntry>,
    groupedResults: com.anisync.android.domain.GroupedSearchResults,
    activeCategory: ResultCategory,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    onSearchItemClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
    onUserClick: (String) -> Unit
) {
    val showAll = activeCategory == ResultCategory.ALL
    val showAnime = showAll || activeCategory == ResultCategory.ANIME
    val showManga = showAll || activeCategory == ResultCategory.MANGA
    val showCharacters = showAll || activeCategory == ResultCategory.CHARACTERS
    val showStaff = showAll || activeCategory == ResultCategory.STAFF
    val showUsers = showAll || activeCategory == ResultCategory.USERS
    val showStudios = showAll || activeCategory == ResultCategory.STUDIOS

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (showAnime && searchAnime.isNotEmpty()) {
            if (showAll) {
                item(
                    key = "header_anime",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    SearchSectionHeader(title = "Anime")
                }
            }
            gridItems(
                items = searchAnime,
                key = { "anime_${it.mediaId}" },
                contentType = { "search_result_grid" }
            ) { item ->
                val onClick = remember(item.mediaId, onSearchItemClick) {
                    { onSearchItemClick(item.mediaId) }
                }
                com.anisync.android.presentation.discover.components.DiscoverMediaCard(
                    item = item,
                    style = com.anisync.android.presentation.discover.components.CardStyle.Grid(),
                    onClick = onClick,
                    titleLanguage = titleLanguage,
                    transitionPrefix = "search_grid"
                )
            }
        }
        if (showManga && searchManga.isNotEmpty()) {
            if (showAll) {
                item(
                    key = "header_manga",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    SearchSectionHeader(title = "Manga")
                }
            }
            gridItems(
                items = searchManga,
                key = { "manga_${it.mediaId}" },
                contentType = { "search_result_grid" }
            ) { item ->
                val onClick = remember(item.mediaId, onSearchItemClick) {
                    { onSearchItemClick(item.mediaId) }
                }
                com.anisync.android.presentation.discover.components.DiscoverMediaCard(
                    item = item,
                    style = com.anisync.android.presentation.discover.components.CardStyle.Grid(),
                    onClick = onClick,
                    titleLanguage = titleLanguage,
                    transitionPrefix = "search_grid"
                )
            }
        }
        if (showCharacters && groupedResults.characters.isNotEmpty()) {
            if (showAll) {
                item(
                    key = "header_characters",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    SearchSectionHeader(title = stringResource(R.string.search_header_characters))
                }
            }
            gridItems(
                items = groupedResults.characters,
                key = { "char_${it.id}" },
                contentType = { "character_result_grid" },
                span = { GridItemSpan(maxLineSpan) }
            ) { character ->
                GenericSearchResultItem(
                    name = character.displayName,
                    subtitle = character.nativeName,
                    imageUrl = character.imageUrl,
                    onClick = { onCharacterClick(character.id) }
                )
            }
        }
        if (showStaff && groupedResults.staff.isNotEmpty()) {
            if (showAll) {
                item(
                    key = "header_staff",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    SearchSectionHeader(title = stringResource(R.string.search_header_staff))
                }
            }
            gridItems(
                items = groupedResults.staff,
                key = { "staff_${it.id}" },
                contentType = { "staff_result_grid" },
                span = { GridItemSpan(maxLineSpan) }
            ) { staff ->
                GenericSearchResultItem(
                    name = staff.displayName,
                    subtitle = staff.primaryOccupations.firstOrNull() ?: staff.nativeName,
                    imageUrl = staff.imageUrl,
                    onClick = { onStaffClick(staff.id) }
                )
            }
        }
        if (showUsers && groupedResults.users.isNotEmpty()) {
            if (showAll) {
                item(
                    key = "header_users",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    SearchSectionHeader(title = stringResource(R.string.search_header_users))
                }
            }
            gridItems(
                items = groupedResults.users,
                key = { "user_${it.id}" },
                contentType = { "user_result_grid" },
                span = { GridItemSpan(maxLineSpan) }
            ) { user ->
                GenericSearchResultItem(
                    name = user.displayName,
                    subtitle = null,
                    imageUrl = user.imageUrl,
                    onClick = { onUserClick(user.displayName) }
                )
            }
        }
        if (showStudios && groupedResults.studios.isNotEmpty()) {
            if (showAll) {
                item(
                    key = "header_studios",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    SearchSectionHeader(title = stringResource(R.string.search_header_studios))
                }
            }
            gridItems(
                items = groupedResults.studios,
                key = { "studio_${it.id}" },
                contentType = { "studio_result_grid" },
                span = { GridItemSpan(maxLineSpan) }
            ) { studio ->
                GenericSearchResultItem(
                    name = studio.displayName,
                    subtitle = studio.favourites?.let { stringResource(R.string.search_favourites_count, it) },
                    imageUrl = null,
                    onClick = { onStudioClick(studio.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun GenericSearchResultItem(
    name: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
