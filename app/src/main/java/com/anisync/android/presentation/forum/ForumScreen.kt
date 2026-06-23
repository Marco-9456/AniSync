package com.anisync.android.presentation.forum

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Announcement
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AppBarWithSearch
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateConfigs
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.ScrollToTopFab
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.forum.components.ForumFeedSelector
import com.anisync.android.presentation.forum.components.ForumFilterId
import com.anisync.android.presentation.forum.components.ForumMediaFilterHeader
import com.anisync.android.presentation.forum.components.ForumSearchFilterChipBar
import com.anisync.android.presentation.forum.components.ForumSearchFilterSheetHost
import com.anisync.android.presentation.forum.components.ForumThreadCard
import com.anisync.android.presentation.forum.components.ForumThreadCardSkeleton
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.presentation.util.LocalRailFabState
import com.anisync.android.presentation.util.SetRailFab
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Category tabs definition: null = "All", then each defaultCategory with an icon.
 */
private data class CategoryTab(
    val id: Int?,
    val label: String,
    val icon: ImageVector
)

private val categoryTabs = listOf(
    CategoryTab(null, "All", Icons.Default.Forum),
    CategoryTab(1, "Anime", Icons.Default.PlayArrow),
    CategoryTab(2, "Manga", Icons.AutoMirrored.Filled.MenuBook),
    CategoryTab(3, "Light Novels", Icons.AutoMirrored.Filled.LibraryBooks),
    CategoryTab(4, "Visual Novels", Icons.Default.VisibilityOff),
    CategoryTab(5, "Release Discussion", Icons.Default.RateReview),
    CategoryTab(7, "General", Icons.Default.Public),
    CategoryTab(8, "News", Icons.Default.Newspaper),
    CategoryTab(9, "Music", Icons.Default.MusicNote),
    CategoryTab(10, "Gaming", Icons.Default.Gamepad),
    CategoryTab(11, "Site Feedback", Icons.Default.Feedback),
    CategoryTab(12, "Bug Reports", Icons.Default.BugReport),
    CategoryTab(13, "Announcements", Icons.AutoMirrored.Filled.Announcement),
    CategoryTab(15, "Recommendations", Icons.Default.ThumbUp),
    CategoryTab(16, "Forum Games", Icons.Default.Casino),
    CategoryTab(17, "Misc", Icons.Default.Commute),
    CategoryTab(18, "AniList Apps", Icons.Default.Apps)
)

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
    // The thread id open in the two-pane detail (or null); its card shows the selection ring.
    selectedThreadId: Int? = null,
    viewModel: ForumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }

    // On rail layouts the create-thread action lives in the rail header (Material 3); on compact it
    // stays a floating action button below. SetRailFab is a no-op when there is no rail.
    val hasRail = LocalRailFabState.current != null
    SetRailFab(Icons.Default.Add, stringResource(R.string.forum_create_thread), onCreateThreadClick)

    val focusManager = LocalFocusManager.current
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState(initialText = uiState.searchFilters.query)
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val inputModeManager = LocalInputModeManager.current
    var openedFilter by remember { mutableStateOf<ForumFilterId?>(null) }

    // Collapse the full-screen search bar (and drop focus) before navigating away
    // from a result. Leaving the expanded overlay mounted across navigation makes
    // the M3 search bar re-dispatch the pending click on return, which re-opens the
    // destination in a loop. Mirrors DiscoverScreen's result-click handling.
    val onSearchThreadClick: (Int, String) -> Unit =
        remember(onThreadClick, searchBarState, coroutineScope, focusManager) {
            { threadId, threadTitle ->
                focusManager.clearFocus()
                coroutineScope.launch { searchBarState.animateToCollapsed() }
                onThreadClick(threadId, threadTitle)
            }
        }
    val onSearchCommentClick: (Int, Int) -> Unit =
        remember(onThreadCommentClick, searchBarState, coroutineScope, focusManager) {
            { threadId, commentId ->
                focusManager.clearFocus()
                coroutineScope.launch { searchBarState.animateToCollapsed() }
                onThreadCommentClick(threadId, commentId)
            }
        }
    val onSearchUserClick: (String) -> Unit =
        remember(onUserClick, searchBarState, coroutineScope, focusManager) {
            { userName ->
                focusManager.clearFocus()
                coroutineScope.launch { searchBarState.animateToCollapsed() }
                onUserClick(userName)
            }
        }
    val onSearchCreateForMedia: (Int, String, String?) -> Unit =
        remember(onCreateThreadForMedia, searchBarState, coroutineScope, focusManager) {
            { mediaId, title, cover ->
                focusManager.clearFocus()
                coroutineScope.launch { searchBarState.animateToCollapsed() }
                onCreateThreadForMedia(mediaId, title, cover)
            }
        }

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

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

    BackHandler(enabled = searchBarState.currentValue == SearchBarValue.Expanded) {
        focusManager.clearFocus()
        coroutineScope.launch { searchBarState.animateToCollapsed() }
    }

    val selectedCategoryIndex = remember(uiState.selectedCategoryId) {
        categoryTabs.indexOfFirst { it.id == uiState.selectedCategoryId }.coerceAtLeast(0)
    }

    val sharedItemModifier = remember { Modifier.fillMaxWidth() }

    val inputField = remember {
        @Composable {
            val currentSearchBarValue = searchBarState.currentValue
            val isSearchEmpty = textFieldState.text.isEmpty()

            SearchBarDefaults.InputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = { focusManager.clearFocus() },
                placeholder = {
                    Text(
                        text = stringResource(R.string.forum_search_placeholder),
                        modifier = if (currentSearchBarValue == SearchBarValue.Collapsed) sharedItemModifier else Modifier,
                        textAlign = if (currentSearchBarValue == SearchBarValue.Collapsed) TextAlign.Center else TextAlign.Start
                    )
                },
                leadingIcon = if (currentSearchBarValue == SearchBarValue.Expanded) {
                    {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            coroutineScope.launch { searchBarState.animateToCollapsed() }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                } else null,
                trailingIcon = {
                    if (currentSearchBarValue == SearchBarValue.Expanded && !isSearchEmpty) {
                        IconButton(onClick = {
                            textFieldState.edit { replace(0, length, "") }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear)
                            )
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
            // Offset above the main bottom nav bar using the real insets — the system
            // navigation inset (gesture/3-button) plus the bar's own height — instead
            // of a fixed dp. A hardcoded value overlapped the bar on devices with a
            // taller gesture inset / edge-to-edge enforcement (e.g. Android 16). (#34)
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
                // Keep the collapsed search field unfocusable in touch mode: M3 expands the
                // bar whenever the field gains focus, and old devices (API 26 / EMUI 8,
                // issue #51) spuriously re-focus it as the expanded search dialog tears
                // down — popping the bar back open (and again on tab switches while the
                // stale focus lingers). Tap-to-open still works via the press path, and
                // the expanded field lives in its own dialog window, unaffected.
                // Keyboard-mode focus stays allowed; expansion there is key-driven, not
                // focus-driven.
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

                ForumFeedSelector(
                    selected = uiState.selectedFeed,
                    onSelect = { feed ->
                        viewModel.onAction(ForumAction.OnFeedChange(feed))
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(Modifier.height(4.dp))

                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedCategoryIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    edgePadding = 16.dp,
                    indicator = {},
                    divider = {}
                ) {
                    categoryTabs.forEachIndexed { index, tab ->
                        AnimatedTab(
                            index = index,
                            selectedIndex = selectedCategoryIndex,
                            selected = selectedCategoryIndex == index,
                            onClick = {
                                viewModel.onAction(ForumAction.OnCategoryChange(tab.id))
                            },
                            icon = tab.icon,
                            label = tab.label
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
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
            when {
                uiState.isLoading -> ForumLoadingSkeleton()
                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.onAction(ForumAction.Refresh) }
                )

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = systemBarsPadding.calculateBottomPadding() + 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "threads_header") {
                            SectionHeader(
                                title = when (uiState.selectedFeed) {
                                    ForumFeed.OVERVIEW -> stringResource(R.string.forum_recent_discussions)
                                    ForumFeed.RECENT -> stringResource(R.string.forum_recently_active)
                                    ForumFeed.NEW -> stringResource(R.string.forum_new_threads)
                                    ForumFeed.SUBSCRIBED -> stringResource(R.string.forum_subscribed_threads)
                                    ForumFeed.SAVED -> stringResource(R.string.forum_saved_threads)
                                },
                                level = HeaderLevel.Section,
                                padding = PaddingValues(vertical = 4.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (uiState.threads.isEmpty()) {
                            item(key = "empty_threads") {
                                EmptyStateConfigs.ForumNoThreads(
                                    onCreateClick = onCreateThreadClick
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = uiState.threads,
                                key = { _, thread -> "thread_${thread.id}" },
                                contentType = { _, _ -> "ForumThread" }
                            ) { index, thread ->

                                if (index >= uiState.threads.size - 4 && uiState.hasNextPage && !uiState.isLoading && !uiState.isPaginating) {
                                    LaunchedEffect(index) {
                                        viewModel.onAction(ForumAction.LoadMore)
                                    }
                                }

                                ForumThreadCard(
                                    thread = thread,
                                    selected = thread.id == selectedThreadId,
                                    onClick = {
                                        focusManager.clearFocus(); onThreadClick(
                                        thread.id,
                                        thread.title
                                    )
                                    },
                                    onUserClick = onUserClick,
                                    isSaved = thread.id in uiState.savedThreadIds,
                                    onSaveClick = {
                                        viewModel.onAction(
                                            ForumAction.ToggleSaveThread(
                                                thread
                                            )
                                        )
                                    },
                                    isSubscribed = thread.isSubscribed,
                                    onSubscribeClick = {
                                        viewModel.onAction(
                                            ForumAction.ToggleSubscribeThread(
                                                thread
                                            )
                                        )
                                    },
                                    onLastReplyClick = onThreadCommentClick,
                                    modifier = sharedItemModifier
                                )
                            }
                        }

                        if (uiState.isPaginating) {
                            item(key = "paginating_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppCircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen advanced-search overlay
    val searchFilters = uiState.searchFilters
    val searchActive = searchFilters.query.trim().length >= 2 || searchFilters.hasActiveFilters
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField
    ) {
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
                uiState.isSearching && uiState.searchResults.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AppCircularProgressIndicator()
                    }
                }

                uiState.searchError != null && uiState.searchResults.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.searchError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }

                !searchActive -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.forum_search_prompt),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }

                uiState.searchResults.isEmpty() -> {
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
                            top = 8.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = systemBarsPadding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = uiState.searchResults,
                            key = { _, thread -> "search_${thread.id}" },
                            contentType = { _, _ -> "ForumThread" }
                        ) { index, thread ->

                            if (index >= uiState.searchResults.size - 4 && uiState.searchHasNextPage && !uiState.isSearching && !uiState.searchIsPaginating) {
                                LaunchedEffect(index) {
                                    viewModel.onAction(ForumAction.LoadMoreSearch)
                                }
                            }

                            ForumThreadCard(
                                thread = thread,
                                onClick = { onSearchThreadClick(thread.id, thread.title) },
                                onUserClick = onSearchUserClick,
                                isSaved = thread.id in uiState.savedThreadIds,
                                onSaveClick = {
                                    viewModel.onAction(
                                        ForumAction.ToggleSaveThread(
                                            thread
                                        )
                                    )
                                },
                                isSubscribed = thread.isSubscribed,
                                onSubscribeClick = {
                                    viewModel.onAction(
                                        ForumAction.ToggleSubscribeThread(
                                            thread
                                        )
                                    )
                                },
                                onLastReplyClick = onSearchCommentClick,
                                modifier = sharedItemModifier
                            )
                        }

                        if (uiState.searchIsPaginating) {
                            item(key = "search_paginating_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppCircularProgressIndicator()
                                }
                            }
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

@Composable
private fun ForumLoadingSkeleton(
    contentPadding: PaddingValues? = null
) {
    val actualPadding = contentPadding ?: PaddingValues(
        start = 16.dp,
        end = 16.dp,
        bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 96.dp
    )

    val sharedSkeletonModifier = remember { Modifier.fillMaxWidth() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = actualPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) } // Mimics the header spacing
        items(8) {
            ForumThreadCardSkeleton(sharedSkeletonModifier)
        }
    }
}
