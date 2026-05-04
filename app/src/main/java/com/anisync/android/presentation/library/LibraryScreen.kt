package com.anisync.android.presentation.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.components.CompletedCardConfig
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.LibraryMediaCard
import com.anisync.android.presentation.components.MediaTypeSelector
import com.anisync.android.presentation.components.WatchingCardConfig
import com.anisync.android.presentation.library.components.EditLibraryEntrySheet
import com.anisync.android.presentation.library.components.EmptyLibraryTabState
import com.anisync.android.presentation.library.components.LibraryListCard
import com.anisync.android.presentation.library.components.LibrarySearchResultCard
import com.anisync.android.presentation.library.components.ListManagementSheet
import com.anisync.android.presentation.library.components.SkeletonGrid
import com.anisync.android.presentation.library.components.SkeletonList
import com.anisync.android.presentation.library.components.SortBottomSheet
import com.anisync.android.presentation.library.components.SortIcon
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed class LibraryTab {
    data class Standard(val status: LibraryStatus) : LibraryTab()
    object Favorites : LibraryTab()
    data class Custom(val name: String) : LibraryTab()

    @Composable
    fun getLabel(mediaType: MediaType): String {
        return when (this) {
            is Standard -> status.toLabel(mediaType)
            is Favorites -> "Favorites"
            is Custom -> name
        }
    }
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
    viewModel: LibraryViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaType = uiState.mediaType
    val sortOption = uiState.sortOption
    val isAscending = uiState.isAscending
    val titleLanguage = uiState.titleLanguage

    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isGridView by rememberSaveable { mutableStateOf(true) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }

    val tabs = remember(uiState.tabOrder, uiState.hiddenListNames, uiState.customListNames) {
        uiState.tabOrder.mapNotNull { id ->
            if (id in uiState.hiddenListNames) return@mapNotNull null

            when {
                id == "status:FAVORITES" -> LibraryTab.Favorites
                id.startsWith("status:") -> {
                    val statusName = id.removePrefix("status:")
                    LibraryStatus.entries.find { it.name == statusName }?.let { LibraryTab.Standard(it) }
                }
                else -> {
                    // Custom list — only show if it exists
                    if (id in uiState.customListNames) {
                        LibraryTab.Custom(id)
                    } else null
                }
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    var editingEntry by remember { mutableStateOf<LibraryEntry?>(null) }
    var showListManagement by remember { mutableStateOf(false) }

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    val isSearchQueryEmpty by remember {
        derivedStateOf { textFieldState.text.isEmpty() }
    }

    val handleIncrement =
        remember(viewModel) {
            { mediaId: Int ->
                viewModel.onAction(
                    LibraryAction.IncrementProgress(
                        mediaId
                    )
                )
            }
        }
    val handleDecrement =
        remember(viewModel) {
            { mediaId: Int ->
                viewModel.onAction(
                    LibraryAction.DecrementProgress(
                        mediaId
                    )
                )
            }
        }
    val handleEdit = remember { { entry: LibraryEntry -> editingEntry = entry } }

    LaunchedEffect(Unit) {
        viewModel.onAction(LibraryAction.OnScreenVisible)
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .debounce(300.milliseconds)
            .collect { viewModel.onAction(LibraryAction.OnSearchQueryChange(it)) }
    }

    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Collapsed) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) {
        keyboardController?.hide()
        coroutineScope.launch { searchBarState.animateToCollapsed() }
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

    val onSearchResultClick: (Int) -> Unit = remember(navigateToMediaDetails, searchBarState, coroutineScope) {
        { id ->
            keyboardController?.hide()
            // Collapse the full-screen search overlay before navigating; the overlay
            // is a Popup window that otherwise persists over MediaDetails and lets
            // tap events keep firing onto a stale list, repeatedly re-pushing the
            // detail destination until the app is force-closed.
            coroutineScope.launch { searchBarState.animateToCollapsed() }
            navigateToMediaDetails(id)
        }
    }

    // OPTIMIZATION: Fixed focus loss. Extracting the input field to a standalone composable
    // function stops Compose from destroying and recreating the node state when captured values change.
    val inputField = @Composable {
        LibrarySearchBarInputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            isSearchQueryEmpty = isSearchQueryEmpty,
            isGridView = isGridView,
            isAscending = isAscending,
            showListManagement = showListManagement,
            onSearch = { keyboardController?.hide() },
            onBackClick = {
                keyboardController?.hide()
                coroutineScope.launch { searchBarState.animateToCollapsed() }
            },
            onClearClick = { textFieldState.edit { replace(0, length, "") } },
            onToggleView = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isGridView = !isGridView
            },
            onToggleSort = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showSortMenu = true
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
        derivedStateOf {
            animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
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
                (
                    isLibraryEnteringFromBackStack ||
                        (hasObservedLibraryReEnter && isSharedTransitionRunning)
                    )
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
                        AppBarWithSearch(
                            modifier = Modifier.focusProperties { canFocus = !showListManagement },
                            scrollBehavior = scrollBehavior,
                            state = searchBarState,
                            inputField = inputField,
                            colors = SearchBarDefaults.appBarWithSearchColors(
                                appBarContainerColor = Color.Transparent,
                                scrolledAppBarContainerColor = Color.Transparent
                            ),
                        )

                        MediaTypeSelector(
                            selected = mediaType,
                            onSelect = { viewModel.onAction(LibraryAction.OnMediaTypeChange(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PrimaryScrollableTabRow(
                                modifier = Modifier.weight(1f),
                                selectedTabIndex = pagerState.currentPage.coerceAtMost(
                                    tabs.lastIndex.coerceAtLeast(
                                        0
                                    )
                                ),
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                edgePadding = 16.dp,
                                indicator = {},
                                divider = {}
                            ) {
                                tabs.forEachIndexed { index, tab ->
                                    val statusIcon = when (tab) {
                                        is LibraryTab.Standard -> {
                                            when (tab.status) {
                                                LibraryStatus.CURRENT -> if (mediaType == MediaType.ANIME) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook
                                                LibraryStatus.PAUSED -> Icons.Default.Pause
                                                LibraryStatus.COMPLETED -> Icons.Default.Done
                                                LibraryStatus.PLANNING -> Icons.Default.CalendarMonth
                                                LibraryStatus.DROPPED -> Icons.Default.Close
                                                else -> Icons.Default.Inbox
                                            }
                                        }

                                        is LibraryTab.Favorites -> Icons.Default.Favorite
                                        is LibraryTab.Custom -> Icons.AutoMirrored.Filled.List
                                    }

                                    AnimatedTab(
                                        index = index,
                                        selectedIndex = pagerState.currentPage,
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        icon = statusIcon,
                                        label = tab.getLabel(mediaType)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showListManagement = true },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Manage Lists"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 80.dp)
        ) {
            when {
                uiState.isLoading -> {
                    if (isGridView) SkeletonGrid(itemCount = 6) else SkeletonList(itemCount = 6)
                }

                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.onAction(LibraryAction.Refresh) })

                else -> {
                    val motionScheme = MaterialTheme.motionScheme
                    val spatialSpec =
                        remember(motionScheme) { motionScheme.defaultSpatialSpec<IntOffset>() }
                    val effectsSpec =
                        remember(motionScheme) { motionScheme.defaultEffectsSpec<Float>() }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        if (pageIndex >= tabs.size) return@HorizontalPager
                        val tab = tabs[pageIndex]

                        val entries = when (tab) {
                            is LibraryTab.Standard -> uiState.groupedEntries[tab.status]
                                ?: emptyList()

                            is LibraryTab.Favorites -> uiState.favoriteEntries
                            is LibraryTab.Custom -> uiState.customListEntries[tab.name]
                                ?: emptyList()
                        }

                        val tabLabel = tab.getLabel(mediaType)
                        val gridState = rememberSaveable(
                            tabLabel,
                            sortOption,
                            isAscending,
                            saver = LazyGridState.Saver
                        ) { LazyGridState() }
                        val listState = rememberSaveable(
                            tabLabel,
                            sortOption,
                            isAscending,
                            saver = LazyListState.Saver
                        ) { LazyListState() }

                        val cardConfig =
                            if (tab is LibraryTab.Standard && tab.status == LibraryStatus.CURRENT) WatchingCardConfig else CompletedCardConfig

                        if (entries.isEmpty()) {
                            val emptyStatus = if (tab is LibraryTab.Standard) tab.status else null
                            EmptyLibraryTabState(emptyStatus, mediaType)
                        } else {
                            AnimatedContent(
                                targetState = isGridView,
                                transitionSpec = {
                                    (slideInVertically(spatialSpec) { if (targetState) -it / 8 else it / 8 } + fadeIn(
                                        effectsSpec
                                    )) togetherWith
                                            (slideOutVertically(spatialSpec) { if (targetState) it / 8 else -it / 8 } + fadeOut(
                                                effectsSpec
                                            ))
                                },
                                label = "ViewMode"
                            ) { isGrid ->
                                if (isGrid) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        state = gridState,
                                        contentPadding = PaddingValues(24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = entries,
                                            key = { "grid_${tabLabel}_${it.mediaId}" },
                                            contentType = { "LibraryEntry" }
                                        ) { entry ->
                                            LibraryMediaCard(
                                                entry = entry,
                                                mediaType = mediaType,
                                                onClick = { navigateToMediaDetails(entry.mediaId) },
                                                onIncrement = if (tab is LibraryTab.Standard && tab.status == LibraryStatus.CURRENT) {
                                                    { handleIncrement(entry.mediaId) }
                                                } else null,
                                                onDecrement = if (tab is LibraryTab.Standard && tab.status == LibraryStatus.CURRENT) {
                                                    { handleDecrement(entry.mediaId) }
                                                } else null,
                                                onEdit = { handleEdit(entry) },
                                                config = cardConfig,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                titleLanguage = titleLanguage,
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = effectsSpec,
                                                    fadeOutSpec = effectsSpec,
                                                    placementSpec = spatialSpec
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        contentPadding = PaddingValues(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = entries,
                                            key = { "list_${tabLabel}_${it.mediaId}" },
                                            contentType = { "LibraryEntry" }
                                        ) { entry ->
                                            LibraryListCard(
                                                entry = entry,
                                                mediaType = mediaType,
                                                onClick = { navigateToMediaDetails(entry.mediaId) },
                                                onIncrement = if (tab is LibraryTab.Standard && tab.status == LibraryStatus.CURRENT) {
                                                    { handleIncrement(entry.mediaId) }
                                                } else null,
                                                onDecrement = if (tab is LibraryTab.Standard && tab.status == LibraryStatus.CURRENT) {
                                                    { handleDecrement(entry.mediaId) }
                                                } else null,
                                                onEdit = { handleEdit(entry) },
                                                config = cardConfig,
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                titleLanguage = titleLanguage,
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = effectsSpec,
                                                    fadeOutSpec = effectsSpec,
                                                    placementSpec = spatialSpec
                                                )
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

    if (editingEntry == null) {
        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = inputField
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.loading),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                uiState.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                else -> {
                    if (uiState.entries.isEmpty() && !isSearchQueryEmpty) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.search_no_results),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.entries,
                                key = { "search_${it.id}" },
                                contentType = { "SearchResult" }
                            ) { entry ->
                                LibrarySearchResultCard(
                                    entry = entry,
                                    mediaType = mediaType,
                                    onClick = { onSearchResultClick(entry.mediaId) },
                                    titleLanguage = titleLanguage
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    SortBottomSheet(
        visible = showSortMenu,
        onDismiss = { showSortMenu = false },
        options = LibrarySort.entries.toList(),
        selectedOption = sortOption,
        isAscending = isAscending,
        onOptionSelected = { sort, ascending ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.onAction(LibraryAction.OnSortOptionChange(sort, ascending))
        }
    )


    ListManagementSheet(
        visible = showListManagement,
        onDismiss = { showListManagement = false },
        tabOrder = uiState.tabOrder,
        customLists = uiState.customListNames,
        hiddenLists = uiState.hiddenListNames,
        mediaType = mediaType,
        onVisibilityChanged = { name, hidden ->
            viewModel.onAction(LibraryAction.ToggleListVisibility(name, hidden))
        },
        onReorder = { viewModel.onAction(LibraryAction.ReorderTabs(it)) },
        onDeleteList = { viewModel.onAction(LibraryAction.DeleteCustomList(it)) },
        onCreateList = { listName, type ->
            viewModel.onAction(
                LibraryAction.CreateCustomList(
                    listName,
                    type
                )
            )
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
            scoreFormat = uiState.userScoreFormat,
            availableCustomLists = uiState.customListNames,
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

/**
 * Isolated Component to fix Compose identity loss and input field focus drops during typing/toggling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySearchBarInputField(
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    isSearchQueryEmpty: Boolean,
    isGridView: Boolean,
    isAscending: Boolean,
    showListManagement: Boolean,
    onSearch: () -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    onToggleView: () -> Unit,
    onToggleSort: () -> Unit
) {
    SearchBarDefaults.InputField(
        enabled = !showListManagement,
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        onSearch = { onSearch() },
        placeholder = {
            if (searchBarState.currentValue == SearchBarValue.Collapsed) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.search_library_placeholder),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(stringResource(R.string.search_library_placeholder))
            }
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
        } else null,
        trailingIcon = {
            if (searchBarState.currentValue == SearchBarValue.Expanded && !isSearchQueryEmpty) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear)
                    )
                }
            } else if (searchBarState.currentValue == SearchBarValue.Collapsed) {
                Row {
                    IconButton(onClick = onToggleView) {
                        Icon(
                            imageVector = if (isGridView) Icons.Outlined.GridView else Icons.Outlined.ViewAgenda,
                            contentDescription = stringResource(R.string.toggle_view)
                        )
                    }

                    IconButton(onClick = onToggleSort) {
                        SortIcon(isAscending = isAscending)
                    }
                }
            }
        }
    )
}
