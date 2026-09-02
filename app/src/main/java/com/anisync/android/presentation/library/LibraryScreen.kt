package com.anisync.android.presentation.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.library.components.BulkAddToListSheet
import com.anisync.android.presentation.library.components.BulkProgressDialog
import com.anisync.android.presentation.library.components.BulkScoreSheet
import com.anisync.android.presentation.library.components.BulkStatusSheet
import com.anisync.android.presentation.library.components.EditLibraryEntrySheet
import com.anisync.android.presentation.library.components.LibraryEmptyState
import com.anisync.android.presentation.library.components.LibraryBulkActionBar
import com.anisync.android.presentation.library.components.LibraryBulkMoreMenu
import com.anisync.android.presentation.library.components.LibraryFilterSheet
import com.anisync.android.presentation.library.components.LibraryOverflowMenu
import com.anisync.android.presentation.library.components.LibraryPosterCard
import com.anisync.android.presentation.library.components.LibraryQueueRow
import com.anisync.android.presentation.library.components.LibraryRail
import com.anisync.android.presentation.library.components.LibrarySearchCategoryBar
import com.anisync.android.presentation.library.components.LibrarySearchResultCard
import com.anisync.android.presentation.library.components.LibrarySelectionTopBar
import com.anisync.android.presentation.library.components.LibraryViewOptionsSheet
import com.anisync.android.presentation.library.components.ListManagementSheet
import com.anisync.android.presentation.library.components.SkeletonGrid
import com.anisync.android.presentation.library.components.SkeletonList
import com.anisync.android.presentation.library.components.SortIcon
import com.anisync.android.presentation.library.components.airedCount
import com.anisync.android.presentation.util.LIBRARY_ALL_TAB_ID
import com.anisync.android.presentation.util.LIBRARY_FAVORITES_TAB_ID
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.LocalGridColumnCount
import com.anisync.android.presentation.util.LocalGridColumnsAuto
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.posterGridColumns
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed class LibraryTab {
    /** Browse-all tab showing every status list merged (#91). Lives in the tab order like the rest. */
    object All : LibraryTab()
    data class Standard(val status: LibraryStatus) : LibraryTab()
    object Favorites : LibraryTab()
    data class Custom(val name: String) : LibraryTab()

    /** Canonical identifier matching the format used in tabOrder / AppSettings. */
    fun toId(): String = when (this) {
        is All -> LIBRARY_ALL_TAB_ID
        is Standard -> "status:${status.name}"
        is Favorites -> LIBRARY_FAVORITES_TAB_ID
        is Custom -> name
    }

    @Composable
    fun getLabel(mediaType: MediaType): String {
        return when (this) {
            is All -> stringResource(R.string.all)
            is Standard -> status.toLabel(mediaType)
            is Favorites -> "Favorites"
            is Custom -> name
        }
    }

    /**
     * Favorites come from the profile rather than the media list, so those rows carry no list entry
     * id and no bulk mutation can address them.
     */
    val supportsSelection: Boolean get() = this !is Favorites
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    kotlinx.coroutines.FlowPreview::class
)
@Composable
fun LibraryScreen(
    onMediaClick: (Int) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onBrowseDiscover: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaType = uiState.mediaType
    val sortOption = uiState.sortOption
    val isAscending = uiState.isAscending
    val titleLanguage = uiState.titleLanguage

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showOverflow by rememberSaveable { mutableStateOf(false) }
    var showViewOptions by rememberSaveable { mutableStateOf(false) }
    var showListManagement by remember { mutableStateOf(false) }
    var showBulkStatus by rememberSaveable { mutableStateOf(false) }
    var showBulkScore by rememberSaveable { mutableStateOf(false) }
    var showBulkAddToList by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<LibraryEntry?>(null) }

    val tabs = remember(uiState.tabOrder, uiState.hiddenListNames, uiState.customListNames) {
        uiState.tabOrder.mapNotNull { id ->
            if (id in uiState.hiddenListNames) return@mapNotNull null
            when {
                id == LIBRARY_ALL_TAB_ID -> LibraryTab.All
                id == LIBRARY_FAVORITES_TAB_ID -> LibraryTab.Favorites
                id.startsWith("status:") -> {
                    val statusName = id.removePrefix("status:")
                    LibraryStatus.entries.find { it.name == statusName }?.let { LibraryTab.Standard(it) }
                }

                else -> if (id in uiState.customListNames) LibraryTab.Custom(id) else null
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val currentTab = tabs.getOrNull(pagerState.currentPage)
    val currentTabId = currentTab?.toId() ?: LIBRARY_ALL_TAB_ID

    LaunchedEffect(uiState.initialTabId, tabs) {
        val targetId = uiState.initialTabId ?: return@LaunchedEffect
        if (tabs.isEmpty()) return@LaunchedEffect
        val targetIndex = tabs.indexOfFirst { it.toId() == targetId }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            pagerState.scrollToPage(targetIndex)
        }
        viewModel.onAction(LibraryAction.ConsumeInitialTab)
    }

    LaunchedEffect(pagerState, tabs) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                if (page < tabs.size) {
                    viewModel.onAction(LibraryAction.OnTabSelected(tabs[page].toId()))
                }
            }
    }

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val inputModeManager = LocalInputModeManager.current

    val isSearchQueryEmpty by remember { derivedStateOf { textFieldState.text.isEmpty() } }

    val handleIncrement = remember(viewModel) {
        { mediaId: Int -> viewModel.onAction(LibraryAction.IncrementProgress(mediaId)) }
    }
    val handleEdit = remember { { entry: LibraryEntry -> editingEntry = entry } }

    LaunchedEffect(Unit) { viewModel.onAction(LibraryAction.OnScreenVisible) }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .debounce(300.milliseconds)
            .collect { viewModel.onAction(LibraryAction.OnSearchQueryChange(it)) }
    }

    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Expanded) {
            viewModel.onAction(LibraryAction.OnSearchOpened(currentTabId))
        }
    }

    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) {
        focusManager.clearFocus()
        keyboardController?.hide()
        coroutineScope.launch { searchBarState.animateToCollapsed() }
    }

    // Back leaves the selection before it leaves the screen.
    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.onAction(LibraryAction.ClearSelection)
    }

    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedLibraryReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToMediaDetails: (Int) -> Unit = remember(onMediaClick) {
        { id ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedLibraryReEnter = false
            onMediaClick(id)
        }
    }

    val onSearchResultClick: (Int) -> Unit =
        remember(navigateToMediaDetails, searchBarState, coroutineScope) {
            { id ->
                keyboardController?.hide()
                // The overlay is a Popup that otherwise persists over MediaDetails and keeps
                // firing taps onto a stale list.
                coroutineScope.launch { searchBarState.animateToCollapsed() }
                navigateToMediaDetails(id)
            }
        }

    val isGridView = uiState.isGridView

    val inputField = @Composable {
        LibrarySearchBarInputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            isSearchQueryEmpty = isSearchQueryEmpty,
            isGridView = isGridView,
            isNonDefaultSort = sortOption != LibrarySort.AIRING_SOON || !isAscending ||
                !uiState.filters.isEmpty,
            isAscending = isAscending,
            showListManagement = showListManagement,
            onSearch = { keyboardController?.hide() },
            onBackClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                coroutineScope.launch { searchBarState.animateToCollapsed() }
            },
            onClearClick = { textFieldState.edit { replace(0, length, "") } },
            onToggleView = {
                viewModel.onAction(LibraryAction.SetGridView(!isGridView))
            },
            onSortAndFilter = { showFilterSheet = true },
            onOverflow = { showOverflow = true },
            overflowMenu = {
                LibraryOverflowMenu(
                    expanded = showOverflow,
                    showPrivateEntries = uiState.showPrivateEntries,
                    onDismiss = { showOverflow = false },
                    onOpenCalendar = onNavigateToCalendar,
                    onOpenNotes = onNavigateToNotes,
                    onTogglePrivate = { viewModel.onAction(LibraryAction.TogglePrivateVisibility(it)) },
                    onManageLists = { showListManagement = true },
                    onCardOptions = { showViewOptions = true },
                    onRefresh = { viewModel.onAction(LibraryAction.Refresh) }
                )
            }
        )
    }

    val isLibraryEnteringFromBackStack by remember {
        derivedStateOf {
            animatedVisibilityScope.transition.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isLibraryTargetingVisible by remember {
        derivedStateOf { animatedVisibilityScope.transition.targetState == EnterExitState.Visible }
    }
    val isLibraryFullyVisible by remember {
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
                isLibraryTargetingVisible &&
                (isLibraryEnteringFromBackStack || (hasObservedLibraryReEnter && isSharedTransitionRunning))
        }
    }
    val topBarOverlayAlpha by animatedVisibilityScope.transition.animateFloat(label = "TopBarOverlayAlpha") { state ->
        if (state == EnterExitState.Visible) 1f else 0f
    }

    LaunchedEffect(shouldKeepTopBarOverlayForReturn, isLibraryEnteringFromBackStack) {
        if (shouldKeepTopBarOverlayForReturn && isLibraryEnteringFromBackStack) {
            hasObservedLibraryReEnter = true
        }
    }

    LaunchedEffect(
        shouldKeepTopBarOverlayForReturn,
        hasObservedLibraryReEnter,
        isLibraryFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepTopBarOverlayForReturn &&
            hasObservedLibraryReEnter &&
            isLibraryFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepTopBarOverlayForReturn = false
            hasObservedLibraryReEnter = false
        }
    }

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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        if (uiState.isSelectionMode) {
                            LibrarySelectionTopBar(
                                count = uiState.selectedEntryIds.size,
                                listLabel = currentTab?.getLabel(mediaType).orEmpty(),
                                mediaType = mediaType,
                                onClose = { viewModel.onAction(LibraryAction.ClearSelection) },
                                onSelectAll = {
                                    val ids = entriesForTab(uiState, currentTab).map { it.id }
                                    viewModel.onAction(LibraryAction.SelectAll(ids))
                                }
                            )
                        } else {
                            // Keep the collapsed search field unfocusable in touch mode: M3 expands
                            // the bar whenever the field gains focus, and old devices (API 26 /
                            // EMUI 8, issue #51) spuriously re-focus it as the expanded dialog
                            // tears down.
                            AppBarWithSearch(
                                modifier = Modifier.focusProperties {
                                    canFocus = !showListManagement &&
                                        inputModeManager.inputMode == InputMode.Keyboard
                                },
                                scrollBehavior = scrollBehavior,
                                state = searchBarState,
                                inputField = inputField,
                                colors = SearchBarDefaults.appBarWithSearchColors(
                                    appBarContainerColor = Color.Transparent,
                                    scrolledAppBarContainerColor = Color.Transparent
                                )
                            )

                            Spacer(Modifier.height(4.dp))

                            LibraryRail(
                                tabs = tabs,
                                selectedIndex = pagerState.currentPage,
                                mediaType = mediaType,
                                counts = uiState.tabCounts,
                                onTabClick = { index ->
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                                onMediaTypeChange = {
                                    viewModel.onAction(LibraryAction.OnMediaTypeChange(it))
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> {
                    if (isGridView) SkeletonGrid(itemCount = 6) else SkeletonList(itemCount = 6)
                }

                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.onAction(LibraryAction.Refresh) }
                )

                else -> {
                    val motionScheme = MaterialTheme.motionScheme
                    val spatialSpec = remember(motionScheme) { motionScheme.defaultSpatialSpec<IntOffset>() }
                    val effectsSpec = remember(motionScheme) { motionScheme.defaultEffectsSpec<Float>() }
                    val pullToRefreshState = rememberPullToRefreshState()

                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = rememberRateLimitedRefresh {
                            viewModel.onAction(LibraryAction.Refresh)
                        },
                        state = pullToRefreshState,
                        // Pulling while ticking rows would fight the selection gesture.
                        enabled = !uiState.isSelectionMode,
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            CustomPullToRefreshIndicator(
                                isRefreshing = uiState.isRefreshing,
                                state = pullToRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                            )
                        }
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            // Swiping between lists while ticking rows loses the selection.
                            userScrollEnabled = !uiState.isSelectionMode,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIndex ->
                            if (pageIndex >= tabs.size) return@HorizontalPager
                            val tab = tabs[pageIndex]
                            val entries = entriesForTab(uiState, tab)
                            val tabId = tab.toId()
                            val tabLabel = tab.getLabel(mediaType)

                            // One state per layout. AnimatedContent composes both during the
                            // crossfade, and a LazyGridState attached to two grids at once
                            // scrambles the scroll position.
                            val gridState = rememberSaveable(
                                tabLabel, sortOption, isAscending, uiState.filters,
                                saver = LazyGridState.Saver
                            ) { LazyGridState() }
                            val rowState = rememberSaveable(
                                tabLabel, sortOption, isAscending, uiState.filters,
                                saver = LazyGridState.Saver
                            ) { LazyGridState() }

                            val hasQuickProgress = tab is LibraryTab.Standard &&
                                (tab.status == LibraryStatus.CURRENT || tab.status == LibraryStatus.REPEATING)

                            AnimatedContent(
                                targetState = isGridView,
                                transitionSpec = {
                                    (slideInVertically(spatialSpec) { if (targetState) -it / 8 else it / 8 } +
                                        fadeIn(effectsSpec)) togetherWith
                                        (slideOutVertically(spatialSpec) { if (targetState) it / 8 else -it / 8 } +
                                            fadeOut(effectsSpec))
                                },
                                label = "ViewMode"
                            ) { grid ->
                                LibraryTabContent(
                                    uiState = uiState,
                                    tab = tab,
                                    entries = entries,
                                    isGrid = grid,
                                    gridState = if (grid) gridState else rowState,
                                    hasQuickProgress = hasQuickProgress,
                                    mediaType = mediaType,
                                    titleLanguage = titleLanguage,
                                    onEntryClick = { entry ->
                                        if (uiState.isSelectionMode) {
                                            viewModel.onAction(LibraryAction.ToggleSelection(entry.id))
                                        } else {
                                            navigateToMediaDetails(entry.mediaId)
                                        }
                                    },
                                    onEntryLongPress = { entry ->
                                        if (uiState.isSelectionMode) {
                                            viewModel.onAction(LibraryAction.ToggleSelection(entry.id))
                                        } else {
                                            viewModel.onAction(
                                                LibraryAction.EnterSelection(entry.id, tabId)
                                            )
                                        }
                                    },
                                    onIncrement = handleIncrement,
                                    onEdit = handleEdit,
                                    onBrowseDiscover = onBrowseDiscover,
                                    onGoToTab = { targetId ->
                                        val index = tabs.indexOfFirst { it.toId() == targetId }
                                        if (index >= 0) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                    },
                                    onClearFilters = { viewModel.onAction(LibraryAction.ClearFilters) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.isSelectionMode,
                        enter = slideInVertically(spatialSpec) { it } + fadeIn(effectsSpec),
                        exit = slideOutVertically(spatialSpec) { it } + fadeOut(effectsSpec),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        LibraryBulkActionBar(
                            onStatus = { showBulkStatus = true },
                            onScore = { showBulkScore = true },
                            onAddToList = { showBulkAddToList = true },
                            moreMenu = { expanded, dismiss ->
                                LibraryBulkMoreMenu(
                                    expanded = expanded,
                                    onDismiss = dismiss,
                                    canEditSingle = uiState.selectedEntryIds.size == 1,
                                    onEditSingle = {
                                        val id = uiState.selectedEntryIds.firstOrNull()
                                        uiState.entries.find { it.id == id }?.let { entry ->
                                            viewModel.onAction(LibraryAction.ClearSelection)
                                            editingEntry = entry
                                        }
                                    },
                                    onSetPrivate = {
                                        viewModel.onAction(LibraryAction.BulkSetPrivate(it))
                                    },
                                    onRemove = { viewModel.onAction(LibraryAction.BulkRemove) }
                                )
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 20.dp + LocalMainNavBarInset.current)
                        )
                    }
                }
            }
        }
    }

    if (editingEntry == null) {
        ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
            LibrarySearchOverlay(
                uiState = uiState,
                isSearchQueryEmpty = isSearchQueryEmpty,
                onCategoryChange = { viewModel.onAction(LibraryAction.OnSearchCategoryChange(it)) },
                onResultClick = onSearchResultClick
            )
        }
    }

    LibraryFilterSheet(
        visible = showFilterSheet,
        sort = sortOption,
        isAscending = isAscending,
        filters = uiState.filters,
        availableGenres = uiState.availableGenres,
        availableFormats = uiState.availableFormats,
        availableAiringStatuses = uiState.availableAiringStatuses,
        resultCount = entriesForTab(uiState, currentTab).size,
        onSortChange = { sort, ascending ->
            viewModel.onAction(LibraryAction.OnSortOptionChange(sort, ascending))
        },
        onFiltersChange = { viewModel.onAction(LibraryAction.SetFilters(it)) },
        onDismiss = { showFilterSheet = false }
    )

    BulkStatusSheet(
        visible = showBulkStatus,
        count = uiState.selectedEntryIds.size,
        mediaType = mediaType,
        onPick = { status ->
            showBulkStatus = false
            viewModel.onAction(LibraryAction.BulkSetStatus(status))
        },
        onDismiss = { showBulkStatus = false }
    )

    BulkScoreSheet(
        visible = showBulkScore,
        count = uiState.selectedEntryIds.size,
        format = uiState.userScoreFormat,
        onPick = { score ->
            showBulkScore = false
            viewModel.onAction(LibraryAction.BulkSetScore(score))
        },
        onDismiss = { showBulkScore = false }
    )

    BulkAddToListSheet(
        visible = showBulkAddToList,
        lists = uiState.customListNames,
        onPick = { name ->
            showBulkAddToList = false
            viewModel.onAction(LibraryAction.BulkAddToCustomList(name))
        },
        onDismiss = { showBulkAddToList = false }
    )

    uiState.bulkOperation?.let { operation ->
        BulkProgressDialog(
            operation = operation,
            onCancel = { viewModel.onAction(LibraryAction.CancelBulkOperation) }
        )
    }

    val appSettings = LocalAppSettings.current
    LibraryViewOptionsSheet(
        visible = showViewOptions,
        isGridView = isGridView,
        autoColumns = LocalGridColumnsAuto.current,
        columnCount = LocalGridColumnCount.current,
        showScore = uiState.showScoreOnCards,
        onSetGridView = { viewModel.onAction(LibraryAction.SetGridView(it)) },
        onSetAutoColumns = { appSettings.setGridColumnsAuto(it) },
        onSetColumnCount = { appSettings.setGridColumnCount(it) },
        onSetShowScore = { appSettings.setShowScoreOnCards(it) },
        onDismiss = { showViewOptions = false }
    )

    ListManagementSheet(
        visible = showListManagement,
        onDismiss = { showListManagement = false },
        tabOrder = uiState.tabOrder,
        customLists = uiState.customListNames,
        hiddenLists = uiState.hiddenListNames,
        counts = uiState.tabCounts,
        mediaType = mediaType,
        onVisibilityChanged = { name, hidden ->
            viewModel.onAction(LibraryAction.ToggleListVisibility(name, hidden))
        },
        onReorder = { viewModel.onAction(LibraryAction.ReorderTabs(it)) },
        onDeleteList = { viewModel.onAction(LibraryAction.DeleteCustomList(it)) },
        onCreateList = { listName, type ->
            viewModel.onAction(LibraryAction.CreateCustomList(listName, type))
        }
    )

    editingEntry?.let { entry ->
        LaunchedEffect(Unit) {
            if (searchBarState.currentValue == SearchBarValue.Expanded) {
                searchBarState.animateToCollapsed()
            }
        }

        EditLibraryEntrySheet(
            entry = entry,
            titleLanguage = titleLanguage,
            scoreFormat = uiState.userScoreFormat,
            availableCustomLists = uiState.customListNames,
            advancedScoringCategories = uiState.advancedScoringCategories,
            onDismiss = { editingEntry = null },
            onSave = { updatedEntry ->
                viewModel.onAction(LibraryAction.UpdateEntry(updatedEntry))
                editingEntry = null
            },
            onDelete = {
                viewModel.onAction(LibraryAction.DeleteEntry(entry.id, entry.mediaId))
                editingEntry = null
            }
        )
    }
}

/** The entries a tab shows, already sorted and filtered by the ViewModel. */
private fun entriesForTab(state: LibraryUiState, tab: LibraryTab?): List<LibraryEntry> = when (tab) {
    null -> emptyList()
    is LibraryTab.All -> state.entries
    is LibraryTab.Standard -> state.groupedEntries[tab.status] ?: emptyList()
    is LibraryTab.Favorites -> state.favoriteEntries
    is LibraryTab.Custom -> state.customListEntries[tab.name] ?: emptyList()
}

/**
 * One page of the pager.
 *
 * The sort/filter toolbar is the list's first item rather than pinned chrome, so it scrolls away
 * with the content it describes and gives the cards back the 32dp it would otherwise hold.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryTabContent(
    uiState: LibraryUiState,
    tab: LibraryTab,
    entries: List<LibraryEntry>,
    isGrid: Boolean,
    gridState: LazyGridState,
    hasQuickProgress: Boolean,
    mediaType: MediaType,
    titleLanguage: com.anisync.android.data.TitleLanguage,
    onEntryClick: (LibraryEntry) -> Unit,
    onEntryLongPress: (LibraryEntry) -> Unit,
    onIncrement: (Int) -> Unit,
    onEdit: (LibraryEntry) -> Unit,
    onBrowseDiscover: () -> Unit,
    onGoToTab: (String) -> Unit,
    onClearFilters: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    if (entries.isEmpty()) {
        // One full-height item so the empty state still emits nested scroll, otherwise
        // pull-to-refresh cannot fire on an empty or glitched tab (#35).
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize()) {
                    LibraryEmptyState(
                        tab = tab,
                        mediaType = mediaType,
                        // Only report filtering when the list would otherwise have something in it.
                        filterCount = if ((uiState.unfilteredTabCounts[tab.toId()] ?: 0) > 0) {
                            uiState.filters.activeCount
                        } else {
                            0
                        },
                        onBrowseDiscover = onBrowseDiscover,
                        onGoToTab = onGoToTab,
                        onClearFilters = onClearFilters
                    )
                }
            }
        }
        return
    }

    val groups = remember(entries, uiState.sortOption, hasQuickProgress, mediaType) {
        buildQueueGroups(entries, uiState.sortOption, hasQuickProgress, mediaType)
    }
    val selectionMode = uiState.isSelectionMode
    val selectable = tab.supportsSelection

    LazyVerticalGrid(
        columns = if (isGrid) {
            posterGridColumns(baseMinSize = 150.dp)
        } else {
            GridCells.Adaptive(minSize = 360.dp)
        },
        state = gridState,
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 12.dp,
            bottom = 24.dp + LocalMainNavBarInset.current + if (selectionMode) 96.dp else 0.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isGrid) 20.dp else 12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        groups.forEach { group ->
            if (group.title != null) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "header_${group.title}",
                    contentType = "header"
                ) {
                    QueueGroupHeader(title = group.title, count = group.entries.size)
                }
            }

            items(
                items = group.entries,
                key = { "${if (isGrid) "grid" else "row"}_${tab.toId()}_${it.mediaId}" },
                contentType = { "LibraryEntry" }
            ) { entry ->
                val selected = entry.id in uiState.selectedEntryIds
                if (isGrid) {
                    LibraryPosterCard(
                        entry = entry,
                        mediaType = mediaType,
                        titleLanguage = titleLanguage,
                        showScore = uiState.showScoreOnCards,
                        scoreFormat = uiState.userScoreFormat,
                        showListIndicator = tab is LibraryTab.All || tab is LibraryTab.Custom,
                        onClick = { onEntryClick(entry) },
                        onIncrement = if (hasQuickProgress) {
                            { onIncrement(entry.mediaId) }
                        } else {
                            null
                        },
                        onEdit = { onEdit(entry) },
                        onLongPress = if (selectable) {
                            { onEntryLongPress(entry) }
                        } else {
                            null
                        },
                        selectionMode = selectionMode,
                        selected = selected,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.animateItem()
                    )
                } else {
                    LibraryQueueRow(
                        entry = entry,
                        mediaType = mediaType,
                        titleLanguage = titleLanguage,
                        onClick = { onEntryClick(entry) },
                        onIncrement = if (hasQuickProgress) {
                            { onIncrement(entry.mediaId) }
                        } else {
                            null
                        },
                        onEdit = { onEdit(entry) },
                        onLongPress = if (selectable) {
                            { onEntryLongPress(entry) }
                        } else {
                            null
                        },
                        selectionMode = selectionMode,
                        selected = selected,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueGroupHeader(title: Int, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A run of entries under an optional header. */
private data class QueueGroup(val title: Int?, val entries: List<LibraryEntry>)

/**
 * Splits an airing-sorted watching list into what you can watch now and what you are waiting for.
 *
 * Only for the airing sort on a list with quick progress: under any other order the split would
 * fight the order the user chose, and on a finished list everything is ready by definition.
 */
private fun buildQueueGroups(
    entries: List<LibraryEntry>,
    sort: LibrarySort,
    hasQuickProgress: Boolean,
    mediaType: MediaType
): List<QueueGroup> {
    if (sort != LibrarySort.AIRING_SOON || !hasQuickProgress) {
        return listOf(QueueGroup(null, entries))
    }
    val ready = ArrayList<LibraryEntry>()
    val waiting = ArrayList<LibraryEntry>()
    for (entry in entries) {
        val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
        val aired = airedCount(entry, total)
        if (aired != null && entry.progress < aired) ready.add(entry) else waiting.add(entry)
    }
    if (ready.isEmpty() || waiting.isEmpty()) return listOf(QueueGroup(null, entries))
    return listOf(
        QueueGroup(R.string.library_group_ready, ready),
        QueueGroup(R.string.library_group_waiting, waiting)
    )
}

@Composable
private fun LibrarySearchOverlay(
    uiState: LibraryUiState,
    isSearchQueryEmpty: Boolean,
    onCategoryChange: (String) -> Unit,
    onResultClick: (Int) -> Unit
) {
    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        uiState.searchMatches.isEmpty() && !isSearchQueryEmpty ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

        // Blank query: keep the overlay empty until the user types (Discover parity).
        uiState.searchMatches.isEmpty() -> Box(Modifier.fillMaxSize())

        else -> {
            val byCategory = uiState.searchMatchesByCategory
            val categories = remember(uiState.searchMatches, byCategory, uiState.tabOrder, uiState.hiddenListNames) {
                buildList {
                    add(LIBRARY_ALL_TAB_ID to uiState.searchMatches.size)
                    uiState.tabOrder.forEach { id ->
                        if (id != LIBRARY_ALL_TAB_ID && id !in uiState.hiddenListNames) {
                            byCategory[id]?.let { add(id to it.size) }
                        }
                    }
                }
            }
            val categoryIds = remember(categories) { categories.mapTo(HashSet()) { it.first } }
            val effectiveCategory = if (uiState.activeSearchCategory in categoryIds) {
                uiState.activeSearchCategory
            } else {
                LIBRARY_ALL_TAB_ID
            }
            val activeList = if (effectiveCategory == LIBRARY_ALL_TAB_ID) {
                uiState.searchMatches
            } else {
                byCategory[effectiveCategory] ?: uiState.searchMatches
            }

            Column(modifier = Modifier.fillMaxSize()) {
                LibrarySearchCategoryBar(
                    activeCategory = effectiveCategory,
                    categories = categories,
                    mediaType = uiState.mediaType,
                    onCategoryChange = onCategoryChange
                )
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = activeList,
                        key = { "search_${it.mediaId}" },
                        contentType = { "SearchResult" }
                    ) { entry ->
                        LibrarySearchResultCard(
                            entry = entry,
                            mediaType = uiState.mediaType,
                            onClick = { onResultClick(entry.mediaId) },
                            titleLanguage = uiState.titleLanguage
                        )
                    }
                }
            }
        }
    }
}

/**
 * Isolated so Compose keeps the field's node state when captured values change; inlining it drops
 * focus mid-typing.
 *
 * The collapsed field carries two trailing icons rather than four. Calendar and Notes navigate away
 * from the library, so they belong in the overflow, not on the search field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySearchBarInputField(
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    isSearchQueryEmpty: Boolean,
    isGridView: Boolean,
    isNonDefaultSort: Boolean,
    isAscending: Boolean,
    showListManagement: Boolean,
    onSearch: () -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    onToggleView: () -> Unit,
    onSortAndFilter: () -> Unit,
    onOverflow: () -> Unit,
    overflowMenu: @Composable () -> Unit
) {
    SearchBarDefaults.InputField(
        // Matches Discover: a collapsed bar sized by its content differs screen to screen.
        modifier = if (searchBarState.currentValue == SearchBarValue.Expanded) {
            Modifier
        } else {
            Modifier.fillMaxWidth()
        },
        enabled = !showListManagement,
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        onSearch = { onSearch() },
        placeholder = {
            Text(
                text = stringResource(R.string.search_library_placeholder),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = if (searchBarState.currentValue == SearchBarValue.Expanded) {
            {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        } else {
            null
        },
        trailingIcon = {
            if (searchBarState.currentValue == SearchBarValue.Expanded && !isSearchQueryEmpty) {
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                }
            } else if (searchBarState.currentValue == SearchBarValue.Collapsed) {
                Row {
                    IconButton(onClick = onToggleView) {
                        Icon(
                            imageVector = if (isGridView) Icons.Outlined.GridView else Icons.Outlined.ViewAgenda,
                            contentDescription = stringResource(R.string.toggle_view)
                        )
                    }
                    IconButton(onClick = onSortAndFilter) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (isNonDefaultSort) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.tertiaryContainer,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            SortIcon(
                                isAscending = isAscending,
                                activeColor = if (isNonDefaultSort) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = onOverflow) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more)
                            )
                        }
                        overflowMenu()
                    }
                }
            }
        }
    )
}
