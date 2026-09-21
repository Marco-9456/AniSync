package com.anisync.android.presentation.forum

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.library.components.SortIcon
import com.anisync.android.domain.ForumCategory
import com.anisync.android.domain.ForumThread
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.ScrollToTopFab
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.discover.components.DiscoverSectionHeader
import com.anisync.android.presentation.forum.components.ForumFeedMenu
import com.anisync.android.presentation.forum.components.ForumFilterId
import com.anisync.android.presentation.forum.components.ForumOverflowMenu
import com.anisync.android.presentation.forum.components.ForumReorderSectionsSheet
import com.anisync.android.presentation.forum.components.ForumMediaFilterHeader
import com.anisync.android.presentation.forum.components.ForumRail
import com.anisync.android.presentation.forum.components.ForumSearchFilterChipBar
import com.anisync.android.presentation.forum.components.ForumSearchFilterSheetHost
import com.anisync.android.presentation.forum.components.ForumSortFilterSheet
import com.anisync.android.presentation.forum.components.ForumThreadCard
import com.anisync.android.presentation.forum.components.ForumThreadCardSkeleton
import com.anisync.android.presentation.forum.components.ThreadActionsSheet
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.domain.MainTab
import com.anisync.android.presentation.util.LocalRailFabState
import com.anisync.android.presentation.util.SetRailFab
import com.anisync.android.presentation.util.bouncyClickable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.anisync.android.presentation.components.ScrollToTopOnRequest
import com.anisync.android.presentation.components.ExpandSearchOnRequest

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    FlowPreview::class
)
@Composable
fun ForumScreen(
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit,
    onThreadCommentClick: (threadId: Int, commentId: Int) -> Unit,
    onCreateThreadClick: () -> Unit,
    onCreateThreadForMedia: (mediaId: Int, title: String, coverUrl: String?) -> Unit,
    onUserClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    // The thread id open in the two-pane detail (or null); its card shows the selection ring.
    selectedThreadId: Int? = null,
    viewModel: ForumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }

    ScrollToTopOnRequest(uiState.scrollToTopRequest, listState)

    // On rail layouts the create-thread action lives in the rail header (Material 3); on compact it
    // stays a floating action button below. SetRailFab is a no-op when there is no rail.
    val hasRail = LocalRailFabState.current != null
    SetRailFab(
        MainTab.FORUM,
        Icons.Default.Add,
        stringResource(R.string.forum_create_thread),
        onCreateThreadClick
    )

    val focusManager = LocalFocusManager.current
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState(initialText = uiState.searchFilters.query)
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val inputModeManager = LocalInputModeManager.current
    var openedFilter by remember { mutableStateOf<ForumFilterId?>(null) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var feedMenuExpanded by remember { mutableStateOf(false) }

    // The search bar carries sort and filter, the way Library's does. The input field is
    // remembered without keys, so ordering has to reach it through an updated state or the icon
    // keeps drawing the sort the screen opened on.
    val hubSort = rememberUpdatedState(uiState.hubFilters.sort)
    val feedDefaultSort = rememberUpdatedState(uiState.feed.defaultSort)

    val collapseSearch: () -> Unit = remember(searchBarState, coroutineScope, focusManager) {
        {
            focusManager.clearFocus()
            coroutineScope.launch { searchBarState.animateToCollapsed() }
            Unit
        }
    }
    val onSearchThreadClick: (Int, String) -> Unit =
        remember(onThreadClick, collapseSearch) {
            { threadId, threadTitle -> collapseSearch(); onThreadClick(threadId, threadTitle) }
        }
    val onSearchCommentClick: (Int, Int) -> Unit =
        remember(onThreadCommentClick, collapseSearch) {
            { threadId, commentId -> collapseSearch(); onThreadCommentClick(threadId, commentId) }
        }
    val onSearchUserClick: (String) -> Unit =
        remember(onUserClick, collapseSearch) {
            { userName -> collapseSearch(); onUserClick(userName) }
        }
    val onSearchCreateForMedia: (Int, String, String?) -> Unit =
        remember(onCreateThreadForMedia, collapseSearch) {
            { mediaId, title, cover -> collapseSearch(); onCreateThreadForMedia(mediaId, title, cover) }
        }

    LaunchedEffect(Unit) { viewModel.onScreenVisible() }

    LaunchedEffect(viewModel.actions) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is ForumAction.OnThreadClick -> onThreadClick(action.threadId, action.threadTitle)
                is ForumAction.OnCreateThreadClick -> onCreateThreadClick()
                else -> {}
            }
        }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .debounce(300.milliseconds)
            .collect { viewModel.onAction(ForumAction.OnSearchQueryChange(it)) }
    }

    ExpandSearchOnRequest(uiState.searchOverlayRequest, searchBarState)

    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) { collapseSearch() }

    val sharedItemModifier = remember { Modifier.fillMaxWidth() }

    val inputField = remember {
        @Composable {
            val expanded = searchBarState.currentValue == SearchBarValue.Expanded
            val isSearchEmpty = textFieldState.text.isEmpty()

            SearchBarDefaults.InputField(
                // Without this the collapsed bar is sized by whatever it happens to contain, so
                // the Forum bar sat narrower than Discover's. Both fill the width instead.
                modifier = if (expanded) Modifier else Modifier.fillMaxWidth(),
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = { focusManager.clearFocus() },
                // One line always: at a raised font scale a wrapping placeholder takes the whole
                // bar with it and the bar's height starts depending on the wording.
                placeholder = {
                    Text(
                        text = stringResource(R.string.forum_search_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    if (expanded) {
                        IconButton(onClick = collapseSearch) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (expanded) {
                        if (!isSearchEmpty) {
                            IconButton(onClick = {
                                textFieldState.edit { replace(0, length, "") }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Ordering and narrowing keep a control of their own; the overflow is
                            // for the screen's own preferences, as it is on Discover.
                            // Library's treatment exactly: the arrows show the direction, and a
                            // tertiary disc shows the ordering is no longer the feed's own.
                            val sort = hubSort.value
                            val isNonDefaultSort = sort != feedDefaultSort.value
                            val sortDescription = stringResource(R.string.cd_sort_threads)
                            IconButton(
                                onClick = {
                                    viewModel.onAction(
                                        ForumAction.OpenSheet(ForumSheet.SORT_AND_FILTER)
                                    )
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = sortDescription
                                }
                            ) {
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
                                        isAscending = sort.isAscending,
                                        activeColor = if (isNonDefaultSort) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                            Box {
                            IconButton(onClick = { overflowExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more)
                                )
                            }
                            ForumOverflowMenu(
                                expanded = overflowExpanded,
                                onDismiss = { overflowExpanded = false },
                                onReorderSections = {
                                    viewModel.onAction(ForumAction.OpenReorderSections)
                                },
                                onOpenSettings = onNavigateToSettings
                            )
                            }
                        }
                    }
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = LocalMainNavBarInset.current)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScrollToTopFab(
                        visible = showScrollToTop,
                        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } }
                    )
                    if (!hasRail) {
                        FloatingActionButton(
                            onClick = onCreateThreadClick,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.forum_create_thread)
                            )
                        }
                    }
                }
            }
        },
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                // Keep the collapsed search field unfocusable in touch mode: M3 expands the bar
                // whenever the field gains focus, and old devices spuriously re-focus it as the
                // expanded dialog tears down, popping the bar back open (issue #51).
                AppBarWithSearch(
                    modifier = Modifier.focusProperties {
                        canFocus = inputModeManager.inputMode == InputMode.Keyboard
                    },
                    scrollBehavior = scrollBehavior,
                    state = searchBarState,
                    inputField = inputField,
                    colors = SearchBarDefaults.appBarWithSearchColors(
                        appBarContainerColor = Color.Transparent,
                        scrolledAppBarContainerColor = Color.Transparent
                    )
                )

                ForumRail(
                    feed = uiState.feed,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onOpenFeedPicker = { feedMenuExpanded = true },
                    onCategoryChange = { viewModel.onAction(ForumAction.OnCategoryChange(it)) },
                    feedMenu = {
                        ForumFeedMenu(
                            expanded = feedMenuExpanded,
                            selected = uiState.feed,
                            onDismiss = { feedMenuExpanded = false },
                            onSelect = { viewModel.onAction(ForumAction.OnFeedChange(it)) }
                        )
                    },
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = rememberRateLimitedRefresh { viewModel.onAction(ForumAction.Refresh) },
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = uiState.isRefreshing,
                    state = pullToRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isOverviewEmpty = uiState.feed == ForumFeed.OVERVIEW &&
                    uiState.overviewPinned.isEmpty() && uiState.overviewRecent.isEmpty()
            val isFeedEmpty = uiState.feed != ForumFeed.OVERVIEW && uiState.threads.isEmpty()

            when {
                uiState.isLoading -> ForumLoadingSkeleton()

                uiState.errorMessage != null -> EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.forum_empty_failed_title),
                    description = stringResource(R.string.forum_empty_failed_desc),
                    actionLabel = stringResource(R.string.forum_empty_retry),
                    actionIcon = Icons.Default.SwapVert,
                    onAction = { viewModel.onAction(ForumAction.Refresh) },
                    animationKey = uiState.errorMessage
                )

                isOverviewEmpty || isFeedEmpty -> ForumEmptyState(
                    state = uiState,
                    onCreateClick = onCreateThreadClick,
                    onBrowseClick = {
                        viewModel.onAction(ForumAction.OnFeedChange(ForumFeed.OVERVIEW))
                    },
                    onClearFilters = {
                        viewModel.onAction(ForumAction.ResetHubFilters)
                        viewModel.onAction(ForumAction.ApplyHubFilters)
                    }
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = systemBarsPadding.calculateBottomPadding() + 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.showsPinnedSection) {
                        item(key = "pinned_header") {
                            PinnedHeader(
                                count = uiState.pinnedThreads.size,
                                expanded = uiState.isPinnedExpanded,
                                onToggle = { viewModel.onAction(ForumAction.TogglePinnedExpanded) }
                            )
                        }
                        if (uiState.isPinnedExpanded) {
                            items(
                                items = uiState.pinnedThreads,
                                key = { "pinned_${it.id}" },
                                contentType = { "ForumThread" }
                            ) { thread ->
                                ThreadCard(
                                    thread, uiState, selectedThreadId, viewModel,
                                    onThreadClick, onUserClick, onThreadCommentClick,
                                    sharedItemModifier
                                )
                            }
                        }
                    }

                    if (uiState.feed == ForumFeed.OVERVIEW) {
                        overviewSections(
                            uiState = uiState,
                            selectedThreadId = selectedThreadId,
                            viewModel = viewModel,
                            onThreadClick = onThreadClick,
                            onUserClick = onUserClick,
                            onLastReplyClick = onThreadCommentClick,
                            itemModifier = sharedItemModifier
                        )
                    } else {
                        val rest = uiState.unpinnedThreads
                        itemsIndexed(
                            items = rest,
                            key = { _, thread -> "thread_${thread.id}" },
                            contentType = { _, _ -> "ForumThread" }
                        ) { index, thread ->
                            if (index >= rest.size - 4 && uiState.hasNextPage &&
                                !uiState.isLoading && !uiState.isPaginating
                            ) {
                                LaunchedEffect(index) { viewModel.onAction(ForumAction.LoadMore) }
                            }
                            ThreadCard(
                                thread, uiState, selectedThreadId, viewModel,
                                onThreadClick, onUserClick, onThreadCommentClick, sharedItemModifier
                            )
                        }

                        if (uiState.isPaginating) {
                            item(key = "paginating_indicator") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) { AppCircularProgressIndicator() }
                            }
                        }
                    }
                }
            }
        }
    }

    ForumReorderSectionsSheet(
        visible = uiState.isReorderSheetVisible,
        sections = uiState.overviewOrder,
        hiddenSections = uiState.hiddenOverviewSections,
        onDismiss = { viewModel.onAction(ForumAction.DismissReorderSections) },
        onReorder = { viewModel.onAction(ForumAction.ReorderOverview(it)) },
        onVisibilityChanged = { section, visible ->
            viewModel.onAction(ForumAction.SetOverviewSectionHidden(section, visible))
        },
        onReset = { viewModel.onAction(ForumAction.ResetOverviewOrder) }
    )

    if (uiState.openSheet == ForumSheet.SORT_AND_FILTER) {
        ForumSortFilterSheet(
            sort = uiState.hubFilters.sort,
            resultCount = if (uiState.feed == ForumFeed.OVERVIEW) {
                uiState.visibleOverviewSections.sumOf { uiState.overviewThreads(it).size }
            } else {
                uiState.threads.size
            },
            onSortChange = { viewModel.onAction(ForumAction.OnHubSortChange(it)) },
            onReset = { viewModel.onAction(ForumAction.ResetHubFilters) },
            onApply = { viewModel.onAction(ForumAction.ApplyHubFilters) },
            onDismiss = { viewModel.onAction(ForumAction.DismissSheet) }
        )
    }

    uiState.actionSheetThread?.let { thread ->
        if (uiState.openSheet == ForumSheet.THREAD_ACTIONS) {
            ThreadActionsSheet(
                thread = thread,
                isSaved = thread.id in uiState.savedThreadIds,
                onSubscribe = { viewModel.onAction(ForumAction.ToggleSubscribeThread(thread)) },
                onSave = { viewModel.onAction(ForumAction.ToggleSaveThread(thread)) },
                onDismiss = { viewModel.onAction(ForumAction.DismissSheet) }
            )
        }
    }

    // Fullscreen advanced-search overlay. Its filter bar is the shipped one, untouched.
    val searchFilters = uiState.searchFilters
    val searchActive = searchFilters.query.trim().length >= 2 || searchFilters.hasActiveFilters
    ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
        Column(modifier = Modifier.fillMaxSize()) {
            ForumSearchFilterChipBar(
                filters = searchFilters,
                onChipTap = { openedFilter = it },
                onToggleSubscribed = { viewModel.onAction(ForumAction.ToggleSubscribedOnly) }
            )

            searchFilters.media?.let { media ->
                ForumMediaFilterHeader(
                    media = media,
                    onCreateThread = {
                        onSearchCreateForMedia(
                            media.mediaId,
                            media.titleUserPreferred,
                            media.coverUrl
                        )
                    },
                    onClear = { viewModel.onAction(ForumAction.ClearMediaFilter) }
                )
            }

            when {
                uiState.isSearching && uiState.searchResults.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { AppCircularProgressIndicator() }

                uiState.searchError != null && uiState.searchResults.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.forum_empty_failed_title),
                    description = stringResource(R.string.forum_empty_failed_desc),
                    animationKey = uiState.searchError
                )

                !searchActive -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.forum_search_prompt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }

                uiState.searchResults.isEmpty() -> EmptyState(
                    icon = Icons.Default.Tune,
                    title = stringResource(R.string.forum_empty_filtered_title),
                    description = stringResource(
                        R.string.forum_empty_filtered_desc,
                        searchFilters.activeCount
                    ),
                    actionLabel = stringResource(R.string.forum_empty_clear_filters),
                    actionIcon = Icons.Default.Close,
                    onAction = { viewModel.onAction(ForumAction.ClearSearchFilters) },
                    // The viewer caused this and can undo it in one tap.
                    actionEmphasised = true,
                    animationKey = searchFilters
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = systemBarsPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = uiState.searchResults,
                        key = { _, thread -> "search_${thread.id}" },
                        contentType = { _, _ -> "ForumThread" }
                    ) { index, thread ->
                        if (index >= uiState.searchResults.size - 4 && uiState.searchHasNextPage &&
                            !uiState.isSearching && !uiState.searchIsPaginating
                        ) {
                            LaunchedEffect(index) { viewModel.onAction(ForumAction.LoadMoreSearch) }
                        }

                        ForumThreadCard(
                            thread = thread,
                            onClick = { onSearchThreadClick(thread.id, thread.title) },
                            onUserClick = onSearchUserClick,
                            isSaved = thread.id in uiState.savedThreadIds,
                            onSaveClick = { viewModel.onAction(ForumAction.ToggleSaveThread(thread)) },
                            onOverflowClick = {
                                viewModel.onAction(ForumAction.OpenThreadActions(thread))
                            },
                            onLastReplyClick = onSearchCommentClick,
                            modifier = sharedItemModifier
                        )
                    }

                    if (uiState.searchIsPaginating) {
                        item(key = "search_paginating_indicator") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) { AppCircularProgressIndicator() }
                        }
                    }
                }
            }
        }
    }

    ForumSearchFilterSheetHost(
        opened = openedFilter,
        filters = searchFilters,
        mediaPickerType = uiState.mediaPickerType,
        mediaPickerQuery = uiState.mediaPickerQuery,
        mediaPickerResults = uiState.mediaPickerResults,
        isMediaPickerSearching = uiState.isMediaPickerSearching,
        authorPickerQuery = uiState.authorPickerQuery,
        authorPickerResults = uiState.authorPickerResults,
        isAuthorPickerSearching = uiState.isAuthorPickerSearching,
        pickerError = uiState.pickerError,
        onSortChange = { viewModel.onAction(ForumAction.OnSortChange(it)) },
        onCategoryChange = { viewModel.onAction(ForumAction.OnCategoryFilterChange(it)) },
        onMediaPickerTypeChange = { viewModel.onAction(ForumAction.OnMediaPickerTypeChange(it)) },
        onMediaPickerQueryChange = { viewModel.onAction(ForumAction.OnMediaPickerQueryChange(it)) },
        onSelectMedia = { viewModel.onAction(ForumAction.SelectMediaFilter(it)) },
        onClearMedia = { viewModel.onAction(ForumAction.ClearMediaFilter) },
        onAuthorPickerQueryChange = { viewModel.onAction(ForumAction.OnAuthorPickerQueryChange(it)) },
        onSelectAuthor = { viewModel.onAction(ForumAction.SelectAuthorFilter(it)) },
        onClearAuthor = { viewModel.onAction(ForumAction.ClearAuthorFilter) },
        onDismiss = { openedFilter = null }
    )
}

/**
 * The Overview's three previews. Each carries an expand button that switches to the feed it is
 * previewing, so no section is a dead end.
 */
private fun LazyListScope.overviewSections(
    uiState: ForumUiState,
    selectedThreadId: Int?,
    viewModel: ForumViewModel,
    onThreadClick: (Int, String) -> Unit,
    onUserClick: (String) -> Unit,
    onLastReplyClick: (Int, Int) -> Unit,
    itemModifier: Modifier
) {
    uiState.visibleOverviewSections.forEach { section ->
        val threads = uiState.overviewThreads(section)
        if (threads.isEmpty()) return@forEach

        item(key = "section_${section.name}") {
            Spacer(Modifier.height(12.dp))
            // Discover's header, so the accent bar, the type and the "See all" action are the
            // same control on both browse surfaces.
            DiscoverSectionHeader(
                title = stringResource(section.titleRes),
                onActionClick = {
                    if (section == OverviewSection.RELEASE_DISCUSSION) {
                        // The section is a category, so expanding it means that category's feed.
                        viewModel.onAction(
                            ForumAction.OnCategoryChange(RELEASE_DISCUSSION_CATEGORY_ID)
                        )
                    } else {
                        viewModel.onAction(ForumAction.OnFeedChange(section.opens))
                    }
                },
                horizontalPadding = 12.dp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(
            items = threads,
            key = { "${section.name}_${it.id}" },
            contentType = { "ForumThread" }
        ) { thread ->
            ThreadCard(
                thread, uiState, selectedThreadId, viewModel,
                onThreadClick, onUserClick, onLastReplyClick, itemModifier
            )
        }
    }
}

@Composable
private fun ThreadCard(
    thread: ForumThread,
    uiState: ForumUiState,
    selectedThreadId: Int?,
    viewModel: ForumViewModel,
    onThreadClick: (Int, String) -> Unit,
    onUserClick: (String) -> Unit,
    onLastReplyClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ForumThreadCard(
        thread = thread,
        selected = thread.id == selectedThreadId,
        onClick = { onThreadClick(thread.id, thread.title) },
        onUserClick = onUserClick,
        isSaved = thread.id in uiState.savedThreadIds,
        onSaveClick = { viewModel.onAction(ForumAction.ToggleSaveThread(thread)) },
        onOverflowClick = { viewModel.onAction(ForumAction.OpenThreadActions(thread)) },
        onLastReplyClick = onLastReplyClick,
        modifier = modifier
    )
}

/**
 * AniList keeps several threads stickied at all times, so the section defaults shut and the list
 * opens on live discussion instead.
 */
@Composable
private fun PinnedHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        label = "PinnedChevron"
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onToggle, clipShape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp, 24.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.forum_pinned),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (expanded) R.string.cd_collapse else R.string.cd_expand
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).rotate(rotation)
            )
        }
    }
}

/**
 * Every reason the hub has nothing to show, on the app's shared block. The primary-filled action is
 * reserved for the one the viewer caused and can undo in a tap.
 */
@Composable
private fun ForumEmptyState(
    state: ForumUiState,
    onCreateClick: () -> Unit,
    onBrowseClick: () -> Unit,
    onClearFilters: () -> Unit
) {
    when {
        state.feed == ForumFeed.SAVED -> EmptyState(
            icon = Icons.Default.Bookmark,
            title = stringResource(R.string.forum_empty_saved_title),
            description = stringResource(R.string.forum_empty_saved_desc),
            actionLabel = stringResource(R.string.forum_empty_browse_action),
            actionIcon = Icons.Default.Forum,
            onAction = onBrowseClick,
            animationKey = "saved"
        )

        state.feed == ForumFeed.SUBSCRIBED -> EmptyState(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.forum_empty_subscribed_title),
            description = stringResource(R.string.forum_empty_subscribed_desc),
            actionLabel = stringResource(R.string.forum_empty_browse_action),
            actionIcon = Icons.Default.Forum,
            onAction = onBrowseClick,
            animationKey = "subscribed"
        )

        state.hubFilterCount > 0 -> EmptyState(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.forum_empty_filtered_title),
            description = stringResource(
                R.string.forum_empty_filtered_desc,
                state.hubFilterCount
            ),
            actionLabel = stringResource(R.string.forum_empty_clear_filters),
            actionIcon = Icons.Default.Close,
            onAction = onClearFilters,
            actionEmphasised = true,
            animationKey = state.hubFilterCount
        )

        else -> EmptyState(
            icon = Icons.Default.Forum,
            title = stringResource(R.string.forum_empty_browse_title),
            description = stringResource(R.string.forum_empty_browse_desc),
            actionLabel = stringResource(R.string.forum_empty_start_thread),
            actionIcon = Icons.Default.Add,
            onAction = onCreateClick,
            animationKey = "browse"
        )
    }
}

@Composable
private fun ForumLoadingSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(8) { ForumThreadCardSkeleton(Modifier.fillMaxWidth()) }
    }
}
