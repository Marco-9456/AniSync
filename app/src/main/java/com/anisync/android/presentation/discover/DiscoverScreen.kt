package com.anisync.android.presentation.discover

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.anisync.android.R
import com.anisync.android.domain.DiscoverSection
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.discover.components.AiringTimeline
import com.anisync.android.presentation.discover.components.BrowseChip
import com.anisync.android.presentation.discover.components.DiscoverAllHiddenState
import com.anisync.android.presentation.discover.components.DiscoverBrowseRail
import com.anisync.android.presentation.discover.components.DiscoverNoResultsState
import com.anisync.android.presentation.discover.components.DiscoverOfflineState
import com.anisync.android.presentation.discover.components.DiscoverOverflowMenu
import com.anisync.android.presentation.discover.components.DiscoverSectionHeader
import com.anisync.android.presentation.discover.components.DiscoverSpotlight
import com.anisync.android.presentation.discover.components.MediaMarkerRail
import com.anisync.android.presentation.discover.components.ReorderSectionsSheet
import com.anisync.android.presentation.discover.components.SectionErrorCard
import com.anisync.android.presentation.discover.components.SectionSkeletonRail
import com.anisync.android.presentation.discover.components.TrendingRankRail
import com.anisync.android.presentation.discover.components.titleRes
import com.anisync.android.presentation.discover.components.DiscoverShimmer
import com.anisync.android.presentation.discover.components.HorizontalMediaList
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.discover.components.RecentReviewsRow
import com.anisync.android.presentation.navigation.TwoPaneListDetailScaffold
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaSeason
import com.anisync.android.type.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "DiscoverScreen"


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    FlowPreview::class
)
@Composable
fun DiscoverScreen(
    // sourceSection = TransitionKeys.DISCOVER_* prefix of the section the tap came from; must
    // reach MediaDetails.sourceScreen so the return morph targets the exact card tapped (the
    // same media can sit in several Discover sections at once).
    onMediaClick: (mediaId: Int, sourceSection: String) -> Unit,
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onSectionSeeAllClick: (title: String, sectionType: String, mediaType: MediaType) -> Unit,
    onReviewClick: (Int) -> Unit = {},
    onRecentReviewsSeeAllClick: (MediaType) -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
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

    val initialQuery = rememberSaveable { uiState.searchQuery }
    val textFieldState = rememberTextFieldState(initialText = initialQuery)

    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    val pullToRefreshState = rememberPullToRefreshState()

    val currentMediaType = uiState.mediaType

    // Separate scroll state memory for Anime vs Manga tabs
    val mainListState =
        rememberSaveable(currentMediaType, saver = LazyListState.Saver) { LazyListState() }


    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedDiscoverReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToMediaDetails: (Int, String) -> Unit = remember(onMediaClick) {
        { mediaId, sourceSection ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedDiscoverReEnter = false
            onMediaClick(mediaId, sourceSection)
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

    var showOverflow by remember { mutableStateOf(false) }
    var showReorderSheet by rememberSaveable { mutableStateOf(false) }

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
    // The request counter lives in the ViewModel and survives tab switches, while this
    // composition (and its LaunchedEffect) is recreated on every return to Discover —
    // so remember the last consumed request in saveable state, or the relaunched effect
    // would keep re-opening the overlay on every re-entry for as long as the ViewModel lives.
    val searchOverlayRequest = uiState.searchOverlayRequest
    var consumedSearchOverlayRequest by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(searchOverlayRequest) {
        if (searchOverlayRequest > consumedSearchOverlayRequest) {
            consumedSearchOverlayRequest = searchOverlayRequest
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
            navigateToMediaDetails(id, TransitionKeys.DISCOVER)
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

    val currentSearchFilters = uiState.searchFilters

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
                        overflowExpanded = showOverflow,
                        onSearch = { viewModel.onAction(DiscoverAction.OnSearch(textFieldState.text.toString())) },
                        onMediaTypeChange = { viewModel.onAction(DiscoverAction.OnMediaTypeChange(it)) },
                        onOverflowClick = { showOverflow = true },
                        onOverflowDismiss = { showOverflow = false },
                        onReorderSections = { showReorderSheet = true },
                        onOpenCalendar = onNavigateToCalendar,
                        onRefresh = onRefresh,
                        onOpenSettings = onNavigateToSettings,
                        onBrowseChip = { chip ->
                            if (chip == BrowseChip.SCHEDULE) onNavigateToCalendar()
                            else viewModel.onAction(DiscoverAction.OnBrowseChip(chip))
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        DiscoverContent(
            state = uiState,
            titleLanguage = titleLanguage,
            mainListState = mainListState,
            pullToRefreshState = pullToRefreshState,
            paddingValues = PaddingValues(top = paddingValues.calculateTopPadding()),
            onRefresh = onRefresh,
            onMediaClick = navigateToMediaDetails,
            onSectionSeeAllClick = onSectionSeeAllClick,
            onReviewClick = onReviewClick,
            onRecentReviewsSeeAllClick = onRecentReviewsSeeAllClick,
            onOpenCalendar = onNavigateToCalendar,
            onAddToPlanning = { viewModel.onAction(DiscoverAction.AddToPlanning(it)) },
            onRetrySection = { viewModel.onAction(DiscoverAction.RetrySection(it)) },
            onReorderSections = { showReorderSheet = true },
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }

    ReorderSectionsSheet(
        visible = showReorderSheet,
        sections = remember(uiState.sectionOrder, uiState.hiddenSections, uiState.mediaType) {
            viewModel.sectionsForReorder()
        },
        hiddenSections = uiState.hiddenSections,
        onDismiss = { showReorderSheet = false },
        onReorder = { viewModel.onAction(DiscoverAction.ReorderSections(it)) },
        onVisibilityChanged = { section, visible ->
            viewModel.onAction(DiscoverAction.SetSectionVisible(section, visible))
        },
        onReset = { viewModel.onAction(DiscoverAction.ResetSectionOrder) }
    )

    val searchQuery = uiState.searchQuery
    val searchAnime = uiState.searchAnime
    val searchManga = uiState.searchManga
    val groupedResults = uiState.groupedResults
    val isSearching = uiState.isSearching
    val searchError = uiState.searchError
    val searchPaging = uiState.searchPaging

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
    val viewMode = uiState.viewMode
    val activeCategory = uiState.activeCategory

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
        onClearFilters = { viewModel.onAction(DiscoverAction.ClearFilters) },
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
    overflowExpanded: Boolean,
    onSearch: () -> Unit,
    onMediaTypeChange: (MediaType) -> Unit,
    onOverflowClick: () -> Unit,
    onOverflowDismiss: () -> Unit,
    onReorderSections: () -> Unit,
    onOpenCalendar: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onBrowseChip: (BrowseChip) -> Unit
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
                    onSearch = onSearch,
                    collapsedTrailing = {
                        Box {
                            IconButton(onClick = onOverflowClick) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more)
                                )
                            }
                            DiscoverOverflowMenu(
                                expanded = overflowExpanded,
                                mediaType = mediaType,
                                onDismiss = onOverflowDismiss,
                                onReorderSections = onReorderSections,
                                onOpenCalendar = onOpenCalendar,
                                onRefresh = onRefresh,
                                onOpenSettings = onOpenSettings
                            )
                        }
                    }
                )
            },
            colors = SearchBarDefaults.appBarWithSearchColors(
                appBarContainerColor = Color.Transparent,
                scrolledAppBarContainerColor = Color.Transparent
            )
        )

        // One 34dp rail carries the media-type switch and every browse entry point, in place of
        // the full-width segmented control that used to own a row on its own.
        DiscoverBrowseRail(
            mediaType = mediaType,
            onMediaTypeChange = onMediaTypeChange,
            onChipClick = onBrowseChip,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
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
    onSearch: () -> Unit,
    collapsedTrailing: (@Composable () -> Unit)? = null
) {
    val isExpanded = searchBarState.currentValue == SearchBarValue.Expanded
    val hasText by remember { derivedStateOf { textFieldState.text.isNotEmpty() } }
    val focusManager = LocalFocusManager.current

    // The overlay searches characters, staff and users too, so the placeholder says so rather
    // than promising only titles. Studios are an anime-side entity, so the manga wording drops them.
    val placeholderTextRes by remember(mediaType) {
        derivedStateOf {
            if (mediaType == MediaType.ANIME) {
                R.string.discover_search_placeholder_anime
            } else {
                R.string.discover_search_placeholder_manga
            }
        }
    }

    SearchBarDefaults.InputField(
        // Without this the collapsed bar is sized by whatever it happens to contain, so Discover
        // (leading icon, longer placeholder) sat at the cap while Library sat under it and the two
        // screens showed visibly different bars. Both fill the width instead.
        modifier = if (isExpanded) Modifier else Modifier.fillMaxWidth(),
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        onSearch = {
            onSearch()
            keyboardController?.hide()
        },
        // One line always: at a raised font scale the placeholder wrapped and took the whole
        // search bar with it, so the bar's height depended on the wording.
        placeholder = {
            Text(
                text = stringResource(placeholderTextRes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            } else {
                collapsedTrailing?.invoke()
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
    state: DiscoverUiState,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    mainListState: LazyListState,
    pullToRefreshState: PullToRefreshState,
    paddingValues: PaddingValues,
    onRefresh: () -> Unit,
    onMediaClick: (mediaId: Int, sourceSection: String) -> Unit,
    onSectionSeeAllClick: (title: String, sectionType: String, mediaType: MediaType) -> Unit,
    onReviewClick: (Int) -> Unit,
    onRecentReviewsSeeAllClick: (MediaType) -> Unit,
    onOpenCalendar: () -> Unit,
    onAddToPlanning: (Int) -> Unit,
    onRetrySection: (DiscoverSection) -> Unit,
    onReorderSections: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        indicator = {
            CustomPullToRefreshIndicator(
                isRefreshing = state.isRefreshing,
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
            if (state.feeds.isInitialLoad) {
                item(key = "shimmer", contentType = "shimmer") { DiscoverShimmer() }
                return@LazyColumn
            }
            // Two ways to end up with an empty feed, and they need different answers: every
            // request failed, or the viewer switched every rail off.
            if (state.sectionOrder.isEmpty()) {
                item(key = "all_hidden", contentType = "empty_state") {
                    DiscoverAllHiddenState(
                        onReorder = onReorderSections,
                        modifier = Modifier.fillParentMaxHeight()
                    )
                }
                return@LazyColumn
            }
            if (state.hasNothingToShow) {
                item(key = "offline", contentType = "empty_state") {
                    DiscoverOfflineState(
                        onRetry = onRefresh,
                        modifier = Modifier.fillParentMaxHeight()
                    )
                }
                return@LazyColumn
            }
            state.sectionOrder.forEach { section ->
                discoverSection(
                    section = section,
                    state = state,
                    titleLanguage = titleLanguage,
                    onMediaClick = onMediaClick,
                    onSectionSeeAllClick = onSectionSeeAllClick,
                    onReviewClick = onReviewClick,
                    onRecentReviewsSeeAllClick = onRecentReviewsSeeAllClick,
                    onOpenCalendar = onOpenCalendar,
                    onAddToPlanning = onAddToPlanning,
                    onRetrySection = onRetrySection,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }
}

/**
 * One rail, header included.
 *
 * A rail that failed or came back empty emits nothing at all. That is the point of giving every
 * section its own [SectionFeed]: the screen used to be replaced wholesale by a single centred
 * error whenever any one of five requests failed.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private fun LazyListScope.discoverSection(
    section: DiscoverSection,
    state: DiscoverUiState,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    onMediaClick: (mediaId: Int, sourceSection: String) -> Unit,
    onSectionSeeAllClick: (title: String, sectionType: String, mediaType: MediaType) -> Unit,
    onReviewClick: (Int) -> Unit,
    onRecentReviewsSeeAllClick: (MediaType) -> Unit,
    onOpenCalendar: () -> Unit,
    onAddToPlanning: (Int) -> Unit,
    onRetrySection: (DiscoverSection) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val feeds = state.feeds
    val mediaType = state.mediaType
    val hasContent = when (section) {
        DiscoverSection.TRENDING -> feeds.trending.items.isNotEmpty()
        DiscoverSection.AIRING_TODAY -> feeds.airingToday.items.isNotEmpty()
        DiscoverSection.RELEASING_NOW -> feeds.releasing.items.isNotEmpty()
        DiscoverSection.POPULAR -> feeds.popular.items.isNotEmpty()
        DiscoverSection.NOT_YET_RELEASED -> feeds.notYetReleased.items.isNotEmpty()
        DiscoverSection.NEWLY_ADDED -> feeds.newlyAdded.items.isNotEmpty()
        DiscoverSection.REVIEWS -> feeds.reviews.items.isNotEmpty()
    }
    // A rail that failed keeps its header and says so; one that is simply empty says nothing,
    // because an empty schedule is not a problem to report. A rail still waiting on a retry keeps
    // its place too, or tapping Retry would look like it had deleted the section.
    val feed = feeds.of(section)
    val failed = feed.hasFailed
    val reloading = feed.isLoading && !hasContent
    if (!hasContent && !failed && !reloading) return

    item(key = "${section.id}_header", contentType = "section_header") {
        val title = stringResource(section.titleRes())
        Spacer(Modifier.height(32.dp))
        DiscoverSectionHeader(
            title = title,
            actionLabel = if (section == DiscoverSection.AIRING_TODAY) {
                stringResource(R.string.discover_browse_schedule)
            } else {
                null
            },
            onActionClick = {
                when (section) {
                    DiscoverSection.AIRING_TODAY -> onOpenCalendar()
                    DiscoverSection.REVIEWS -> onRecentReviewsSeeAllClick(mediaType)
                    else -> onSectionSeeAllClick(title, section.gridSectionType(), mediaType)
                }
            }
        )
        Spacer(Modifier.height(12.dp))
    }

    if (failed) {
        item(key = "${section.id}_error", contentType = "section_error") {
            SectionErrorCard(onRetry = { onRetrySection(section) })
        }
        return
    }

    if (reloading) {
        item(key = "${section.id}_skeleton", contentType = "section_skeleton") {
            SectionSkeletonRail(cardWidth = section.skeletonCardWidth())
        }
        return
    }

    when (section) {
        DiscoverSection.TRENDING -> {
            val items = feeds.trending.items
            item(key = "trending_spotlight", contentType = "spotlight") {
                DiscoverSpotlight(
                    item = items.first(),
                    onClick = {
                        onMediaClick(items.first().mediaId, TransitionKeys.DISCOVER_SPOTLIGHT)
                    },
                    onAddClick = { onAddToPlanning(items.first().mediaId) },
                    titleLanguage = titleLanguage,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
            if (items.size > 1) {
                item(key = "trending_rank_rail", contentType = "rank_rail") {
                    Spacer(Modifier.height(12.dp))
                    TrendingRankRail(
                        items = items.drop(1),
                        startRank = 2,
                        titleLanguage = titleLanguage,
                        onItemClick = { onMediaClick(it, TransitionKeys.DISCOVER_TRENDING) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        }

        DiscoverSection.AIRING_TODAY -> item(key = "airing_timeline", contentType = "timeline") {
            AiringTimeline(
                episodes = feeds.airingToday.items,
                titleLanguage = titleLanguage,
                onItemClick = { onMediaClick(it, TransitionKeys.DISCOVER_AIRING) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        DiscoverSection.RELEASING_NOW -> item(key = "releasing_rail", contentType = "media_rail") {
            MediaMarkerRail(
                items = feeds.releasing.items,
                cardWidth = 132.dp,
                transitionPrefix = TransitionKeys.DISCOVER_RELEASING,
                titleLanguage = titleLanguage,
                onItemClick = { onMediaClick(it, TransitionKeys.DISCOVER_RELEASING) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        DiscoverSection.POPULAR -> item(key = "popular_rail", contentType = "media_list") {
            HorizontalMediaList(
                items = feeds.popular.items,
                onItemClick = { onMediaClick(it, TransitionKeys.DISCOVER_POPULAR) },
                titleLanguage = titleLanguage,
                transitionPrefix = TransitionKeys.DISCOVER_POPULAR,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        DiscoverSection.NOT_YET_RELEASED -> item(
            key = "not_yet_released_rail",
            contentType = "media_rail"
        ) {
            val tba = stringResource(R.string.discover_tba_marker)
            val seasonLabels = seasonLabels()
            MediaMarkerRail(
                items = feeds.notYetReleased.items,
                cardWidth = 148.dp,
                transitionPrefix = TransitionKeys.DISCOVER_UPCOMING,
                titleLanguage = titleLanguage,
                onItemClick = { onMediaClick(it, TransitionKeys.DISCOVER_UPCOMING) },
                marker = { entry -> entry.releaseMarker(seasonLabels, tba) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        DiscoverSection.NEWLY_ADDED -> item(key = "newly_added_rail", contentType = "media_rail") {
            MediaMarkerRail(
                items = feeds.newlyAdded.items,
                cardWidth = 148.dp,
                transitionPrefix = TransitionKeys.DISCOVER_NEWLY_ADDED,
                titleLanguage = titleLanguage,
                onItemClick = { onMediaClick(it, TransitionKeys.DISCOVER_NEWLY_ADDED) },
                marker = { entry -> entry.catalogueMarker() },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        DiscoverSection.REVIEWS -> item(key = "reviews_rail", contentType = "review_row") {
            RecentReviewsRow(
                reviews = feeds.reviews.items.take(10),
                onReviewClick = onReviewClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

/** The card width a rail's placeholder should reserve, so the layout does not jump on reload. */
private fun DiscoverSection.skeletonCardWidth(): Dp = when (this) {
    DiscoverSection.TRENDING -> 112.dp
    DiscoverSection.AIRING_TODAY, DiscoverSection.RELEASING_NOW -> 132.dp
    DiscoverSection.POPULAR -> 171.dp
    DiscoverSection.NOT_YET_RELEASED, DiscoverSection.NEWLY_ADDED -> 148.dp
    DiscoverSection.REVIEWS -> 310.dp
}

/** Which paginated grid a rail's See all opens. */
private fun DiscoverSection.gridSectionType(): String = when (this) {
    DiscoverSection.TRENDING -> "trending"
    DiscoverSection.POPULAR -> "popular"
    DiscoverSection.NOT_YET_RELEASED -> "not_yet_released"
    DiscoverSection.NEWLY_ADDED -> "newly_added"
    DiscoverSection.RELEASING_NOW -> "releasing"
    // Neither of these reaches the grid: the timeline opens the calendar and reviews have a
    // screen of their own.
    DiscoverSection.AIRING_TODAY, DiscoverSection.REVIEWS -> "trending"
}

@Composable
private fun seasonLabels(): Map<MediaSeason, String> = mapOf(
    MediaSeason.WINTER to stringResource(R.string.season_winter),
    MediaSeason.SPRING to stringResource(R.string.season_spring),
    MediaSeason.SUMMER to stringResource(R.string.season_summer),
    MediaSeason.FALL to stringResource(R.string.season_fall)
)

/**
 * "Spring 2026" when AniList gave the entry a season, TBA when it did not.
 *
 * Manga almost never carries a season, so on that tab this is TBA nearly every time, which is the
 * honest reading of a NOT_YET_RELEASED manga rather than a date the API never returned.
 */
private fun LibraryEntry.releaseMarker(seasonLabels: Map<MediaSeason, String>, tba: String): String {
    val season = season?.let { seasonLabels[it] } ?: return tba
    return seasonYear?.let { "$season $it" } ?: season
}

/**
 * Format, plus the length or the year. ID_DESC returns entries somebody catalogued today, so
 * `averageScore` is usually null on them and no star is drawn at all rather than an empty one.
 */
@Composable
private fun LibraryEntry.catalogueMarker(): String? {
    val format = format?.toLabel()
    val detail = totalChapters?.let { "$it ch" } ?: seasonYear?.toString()
    return listOfNotNull(format, detail).joinToString(" · ").ifEmpty { null }
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
    onClearFilters: () -> Unit,
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
                searchFilters = searchFilters,
                onLoadMore = onLoadMore,
                onClearFilters = onClearFilters,
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

    // The filter sheets scope to the whole search overlay (which covers both panes on expanded
    // widths), and this host sits OUTSIDE the overlay popup — never anchor them to the feed pane
    // hidden behind it.
    com.anisync.android.presentation.util.WindowModalSheetScope {
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
    searchFilters: com.anisync.android.domain.SearchFilters,
    onLoadMore: () -> Unit,
    onClearFilters: () -> Unit,
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
            DiscoverNoResultsState(
                hasFilters = searchFilters.hasActiveFilters,
                onClearFilters = onClearFilters
            )
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
                    // The All overview is always the fixed panel board; the toggle
                    // applies to single-category results only.
                    showViewToggle = activeCategory != ResultCategory.ALL
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
                                onLoadMore = onLoadMore,
                                viewMode = viewMode
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
                        onLoadMore = onLoadMore,
                        viewMode = viewMode
                    )
                }
            }
        }
    }
}
