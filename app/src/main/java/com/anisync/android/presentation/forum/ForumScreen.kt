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
import com.anisync.android.presentation.util.LocalMainNavBarInset
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateConfigs
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.forum.components.ForumFeedSelector
import com.anisync.android.presentation.forum.components.ForumThreadCard
import com.anisync.android.presentation.forum.components.ForumThreadCardSkeleton
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.delay
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
    onUserClick: (String) -> Unit,
    viewModel: ForumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState(initialText = uiState.searchQuery)
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

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

    // Stop the M3 expressive search bar from reopening itself. As it collapses, M3 can
    // re-expand it via animateToExpanded() when the collapsed InputField catches a stray
    // PressInteraction.Release (or focus) while the full-screen overlay tears down — the
    // bar pops back open on slow devices (API 26 / EMUI 8, issue #51). Rather than disable
    // the field (which kills the keyboard and the close), detect the reopen and undo it:
    // while a collapse is in flight, treat any re-expansion as the self-reopen and snap
    // back to collapsed. Leaves focus, keyboard and close untouched.
    var searchClosing by remember { mutableStateOf(false) }
    LaunchedEffect(searchBarState.targetValue) {
        if (searchBarState.targetValue == SearchBarValue.Collapsed) {
            searchClosing = true
            delay(400L)
            searchClosing = false
        } else if (searchClosing) {
            searchBarState.animateToCollapsed()
        }
    }
    LaunchedEffect(searchBarState.currentValue) {
        if (searchBarState.currentValue == SearchBarValue.Collapsed) {
            focusManager.clearFocus()
        }
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
        },
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                AppBarWithSearch(
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
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen search overlay
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField
    ) {
        when {
            uiState.isLoading -> ForumLoadingSkeleton(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = systemBarsPadding.calculateBottomPadding() + 16.dp
                )
            )

            uiState.threads.isEmpty() && textFieldState.text.isNotEmpty() -> {
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
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = systemBarsPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = uiState.threads,
                        key = { _, thread -> "search_${thread.id}" },
                        contentType = { _, _ -> "ForumThread" }
                    ) { index, thread ->

                        if (index >= uiState.threads.size - 4 && uiState.hasNextPage && !uiState.isLoading && !uiState.isPaginating) {
                            LaunchedEffect(index) {
                                viewModel.onAction(ForumAction.LoadMore)
                            }
                        }

                        ForumThreadCard(
                            thread = thread,
                            onClick = {
                                focusManager.clearFocus()
                                onThreadClick(thread.id, thread.title)
                            },
                            onUserClick = onUserClick,
                            isSaved = thread.id in uiState.savedThreadIds,
                            onSaveClick = { viewModel.onAction(ForumAction.ToggleSaveThread(thread)) },
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

                    if (uiState.isPaginating) {
                        item(key = "search_paginating_indicator") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
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
