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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AppBarWithSearch
import com.anisync.android.presentation.components.AppCircularProgressIndicator
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.MediaReview
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.MediaTypeSelector
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.discover.components.DiscoverHeroCarousel
import com.anisync.android.presentation.discover.components.DiscoverShimmer
import com.anisync.android.presentation.discover.components.HorizontalMediaList
import com.anisync.android.presentation.discover.components.RecentReviewsRow
import com.anisync.android.presentation.navigation.TwoPaneListDetailScaffold
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.StarGold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DiscoverScreen"

private val TrendingIconColor = Color(0xFFFF5722)
private val NewlyAddedIconColor = Color(0xFF26C6DA)
private val RecentReviewsIconColor = Color(0xFFAB47BC)
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
    onReviewClick: (Int) -> Unit = {},
    onRecentReviewsSeeAllClick: (MediaType) -> Unit = {},
    // App nav controller, threaded only so the wide (expanded) search overlay can host its results in a
    // two-pane list-detail. Null on compact/medium (and previews), where search push-navigates instead.
    navController: NavHostController? = null,
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
    val newlyAddedTitle = stringResource(R.string.section_newly_added)
    val recentReviewsTitle = stringResource(R.string.section_recent_reviews)
    val tbaTitle = stringResource(R.string.section_tba)

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { viewModel.onAction(DiscoverAction.OnSearchQueryChange(it)) }
    }

    // External preset-filter search request (ranking cards, genre/tag chips on media
    // details): the viewmodel has already applied the filters; clear the query field
    // and open the overlay so the filtered results are what the user lands on.
    // The expand animation can be cancelled while the tab-switch transition is still
    // settling (the effect often fires on the destination's very first frame), so
    // keep nudging until the bar actually reports Expanded.
    val searchOverlayRequest = (uiState as? DiscoverUiState.Success)?.searchOverlayRequest ?: 0L
    LaunchedEffect(searchOverlayRequest) {
        if (searchOverlayRequest > 0L) {
            textFieldState.clearText()
            var attempts = 0
            while (searchBarState.currentValue != SearchBarValue.Expanded && attempts < 10) {
                runCatching { searchBarState.animateToExpanded() }
                attempts++
                if (searchBarState.currentValue != SearchBarValue.Expanded) delay(100)
            }
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
        rememberRateLimitedRefresh { viewModel.onAction(DiscoverAction.Refresh) }

    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) {
        // Clear focus up front: the M3 expressive InputField re-expands while it stays
        // focused during the collapse animation, so the bar reopens itself on some
        // devices (observed on API 26 / EMUI 8, issue #51) unless focus is dropped first.
        focusManager.clearFocus()
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
            newlyAdded = successState?.newlyAdded ?: emptyList(),
            tba = successState?.tba ?: emptyList(),
            recentReviews = successState?.recentReviews ?: emptyList(),
            mediaType = currentMediaType,
            titleLanguage = titleLanguage,
            isRefreshing = successState?.isRefreshing ?: false,
            mainListState = mainListState,
            pullToRefreshState = pullToRefreshState,
            paddingValues = PaddingValues(
                top = paddingValues.calculateTopPadding()
            ),
            trendingTitle = trendingTitle,
            popularTitle = popularTitle,
            upcomingTitle = upcomingTitle,
            newlyAddedTitle = newlyAddedTitle,
            recentReviewsTitle = recentReviewsTitle,
            tbaTitle = tbaTitle,
            onRefresh = onRefresh,
            onMediaClick = navigateToMediaDetails,
            onSectionSeeAllClick = onSectionSeeAllClick,
            onReviewClick = onReviewClick,
            onRecentReviewsSeeAllClick = onRecentReviewsSeeAllClick,
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
    val searchPaging = successState2?.searchPaging ?: SearchPaging()

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
        navController = navController,
        searchQuery = searchQuery,
        searchAnime = searchAnime,
        searchManga = searchManga,
        groupedResults = groupedResults,
        isSearching = isSearching,
        searchError = searchError,
        viewMode = viewMode,
        activeCategory = activeCategory,
        searchPaging = searchPaging,
        onLoadMore = { viewModel.onAction(DiscoverAction.LoadMoreResults) },
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
    val inputModeManager = LocalInputModeManager.current
    Column(
        modifier = Modifier.statusBarsPadding()
    ) {
        // Keep the collapsed search field unfocusable in touch mode: M3 expands the bar
        // whenever the field gains focus, and old devices (API 26 / EMUI 8, issue #51)
        // spuriously re-focus it as the expanded search dialog tears down — popping the
        // bar back open (and again on tab switches while the stale focus lingers).
        // Tap-to-open still works via the press path, and the expanded field lives in
        // its own dialog window, unaffected. Keyboard-mode focus stays allowed;
        // expansion there is key-driven, not focus-driven.
        AppBarWithSearch(
            modifier = Modifier.focusProperties {
                canFocus = inputModeManager.inputMode == InputMode.Keyboard
            },
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
    val focusManager = LocalFocusManager.current

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
                    focusManager.clearFocus()
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
    newlyAdded: List<LibraryEntry>,
    tba: List<LibraryEntry>,
    recentReviews: List<MediaReview>,
    mediaType: MediaType,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    isRefreshing: Boolean,
    mainListState: LazyListState,
    pullToRefreshState: PullToRefreshState,
    paddingValues: PaddingValues,
    trendingTitle: String,
    popularTitle: String,
    upcomingTitle: String,
    newlyAddedTitle: String,
    recentReviewsTitle: String,
    tbaTitle: String,
    onRefresh: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onSectionSeeAllClick: (title: String, sectionType: String, mediaType: MediaType) -> Unit,
    onReviewClick: (Int) -> Unit,
    onRecentReviewsSeeAllClick: (MediaType) -> Unit,
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
            contentPadding = PaddingValues(bottom = 24.dp + LocalMainNavBarInset.current)
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

                    item(key = "newly_added_header", contentType = "section_header") {
                        Spacer(modifier = Modifier.height(48.dp))
                        SectionHeader(
                            title = newlyAddedTitle,
                            iconColor = NewlyAddedIconColor,
                            onActionClick = {
                                onSectionSeeAllClick(
                                    newlyAddedTitle,
                                    "newly_added",
                                    mediaType
                                )
                            },
                            level = HeaderLevel.Section
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item(key = "newly_added_list", contentType = "media_list") {
                        val newlyAddedItems = remember(newlyAdded) { newlyAdded.take(10) }
                        HorizontalMediaList(
                            items = newlyAddedItems,
                            onItemClick = onMediaClick,
                            titleLanguage = titleLanguage,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (recentReviews.isNotEmpty()) {
                        item(key = "recent_reviews_header", contentType = "section_header") {
                            Spacer(modifier = Modifier.height(48.dp))
                            SectionHeader(
                                title = recentReviewsTitle,
                                iconColor = RecentReviewsIconColor,
                                onActionClick = { onRecentReviewsSeeAllClick(mediaType) },
                                level = HeaderLevel.Section
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item(key = "recent_reviews_row", contentType = "review_row") {
                            RecentReviewsRow(
                                reviews = recentReviews.take(10),
                                onReviewClick = onReviewClick
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
    navController: NavHostController?,
    searchQuery: String,
    searchAnime: List<LibraryEntry>,
    searchManga: List<LibraryEntry>,
    groupedResults: com.anisync.android.domain.GroupedSearchResults,
    isSearching: Boolean,
    searchError: String?,
    viewMode: com.anisync.android.data.DiscoverViewMode,
    activeCategory: ResultCategory,
    searchPaging: SearchPaging,
    onLoadMore: () -> Unit,
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
                navController = navController,
                isSearching = isSearching,
                searchAnime = searchAnime,
                searchManga = searchManga,
                groupedResults = groupedResults,
                searchQuery = searchQuery,
                searchError = searchError,
                titleLanguage = titleLanguage,
                viewMode = viewMode,
                activeCategory = activeCategory,
                searchPaging = searchPaging,
                onLoadMore = onLoadMore,
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
    navController: NavHostController?,
    isSearching: Boolean,
    searchAnime: List<LibraryEntry>,
    searchManga: List<LibraryEntry>,
    groupedResults: com.anisync.android.domain.GroupedSearchResults,
    searchQuery: String,
    searchError: String?,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    viewMode: com.anisync.android.data.DiscoverViewMode,
    activeCategory: ResultCategory,
    searchPaging: SearchPaging,
    onLoadMore: () -> Unit,
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
                AppCircularProgressIndicator()
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
            // The same panel board on every width (phone and tablet read identically). Expanded widths
            // wrap it in the two-pane list-detail (tap → on-demand detail pane); compact/medium render it
            // directly and push the detail full screen on tap.
            val isWideSearch = LocalAdaptiveInfo.current.supportsTwoPane && navController != null
            Column(modifier = Modifier.fillMaxSize()) {
                com.anisync.android.presentation.discover.components.SearchResultsHeader(
                    activeCategory = activeCategory,
                    availableCategories = availableCategories,
                    viewMode = viewMode,
                    onCategoryChange = onCategoryChange,
                    onViewModeChange = onViewModeChange,
                    // Results lay themselves out as adaptive panels now, so the old list/grid toggle is gone.
                    showViewToggle = false
                )
                if (isWideSearch) {
                    TwoPaneListDetailScaffold(
                        modifier = Modifier.weight(1f),
                        selectionSaver = SearchTargetSaver,
                        gutterPadding = PaddingValues(16.dp),
                        listPane = { selectedTarget, onSelect ->
                            com.anisync.android.presentation.discover.components.SearchResultsPanels(
                                activeCategory = activeCategory,
                                searchAnime = searchAnime,
                                searchManga = searchManga,
                                groupedResults = groupedResults,
                                titleLanguage = titleLanguage,
                                onShowAll = onCategoryChange,
                                // Media/character/staff/studio open in the detail pane; users open full screen.
                                onMediaClick = { onSelect(SearchTarget.Media(it)) },
                                onCharacterClick = { onSelect(SearchTarget.Character(it)) },
                                onStaffClick = { onSelect(SearchTarget.Staff(it)) },
                                onStudioClick = { onSelect(SearchTarget.Studio(it)) },
                                onUserClick = onUserClick,
                                selectedTarget = selectedTarget,
                                hasMoreResults = searchPaging.hasNextFor(activeCategory),
                                onLoadMore = onLoadMore
                            )
                        },
                        detailPane = { target, onClose ->
                            SearchDetailPane(
                                target = target,
                                navController = navController!!,
                                onClose = onClose
                            )
                        }
                    )
                } else {
                    com.anisync.android.presentation.discover.components.SearchResultsPanels(
                        modifier = Modifier.weight(1f),
                        activeCategory = activeCategory,
                        searchAnime = searchAnime,
                        searchManga = searchManga,
                        groupedResults = groupedResults,
                        titleLanguage = titleLanguage,
                        onShowAll = onCategoryChange,
                        onMediaClick = onSearchItemClick,
                        onCharacterClick = onCharacterClick,
                        onStaffClick = onStaffClick,
                        onStudioClick = onStudioClick,
                        onUserClick = onUserClick,
                        hasMoreResults = searchPaging.hasNextFor(activeCategory),
                        onLoadMore = onLoadMore
                    )
                }
            }
        }
    }
}
