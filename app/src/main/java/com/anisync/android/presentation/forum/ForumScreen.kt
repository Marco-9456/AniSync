package com.anisync.android.presentation.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.HorizontalStaggeredAnimatedVisibility
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.StaggeredAnimatedVisibility
import com.anisync.android.presentation.forum.components.ForumCategoryChip
import com.anisync.android.presentation.forum.components.ForumThreadCard
import com.anisync.android.presentation.forum.components.ForumThreadCardSkeleton
import kotlinx.coroutines.flow.collectLatest

private data class FeedTabInfo(
    val feed: ForumFeed,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForumScreen(
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit,
    onCategoryClick: (categoryId: Int, categoryName: String) -> Unit,
    onCreateThreadClick: () -> Unit,
    viewModel: ForumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

    val feedTabs = remember {
        listOf(
            FeedTabInfo(ForumFeed.OVERVIEW, "Overview", Icons.Default.ViewList),
            FeedTabInfo(ForumFeed.RECENT, "Recent", Icons.Default.Schedule),
            FeedTabInfo(ForumFeed.NEW, "New", Icons.Default.NewReleases)
        )
    }
    val selectedTabIndex = remember(uiState.selectedFeed) {
        feedTabs.indexOfFirst { it.feed == uiState.selectedFeed }.coerceAtLeast(0)
    }

    // Detect when user scrolls near end for infinite scroll
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.onAction(ForumAction.LoadMore)
    }

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is ForumAction.ShowSnackbar -> snackbarHostState.showSnackbar(action.message)
                is ForumAction.OnThreadClick -> onThreadClick(action.threadId, action.threadTitle)
                is ForumAction.OnCategoryClick -> onCategoryClick(action.category.id, action.category.name)
                is ForumAction.OnCreateThreadClick -> onCreateThreadClick()
                else -> {}
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            StaggeredAnimatedVisibility(key = "fab", index = 4) {
                FloatingActionButton(
                    onClick = onCreateThreadClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.forum_create_thread))
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = { viewModel.onAction(ForumAction.Refresh) },
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = uiState.isRefreshing,
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
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
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        // Screen header
                        item(key = "header") {
                            StaggeredAnimatedVisibility(key = "forum_header", index = 0) {
                                Column(modifier = Modifier.statusBarsPadding()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    SectionHeader(
                                        title = stringResource(R.string.nav_forum),
                                        level = HeaderLevel.Screen,
                                        icon = Icons.Default.Forum,
                                        iconColor = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // Feed tabs
                        item(key = "feed_tabs") {
                            StaggeredAnimatedVisibility(key = "forum_tabs", index = 1) {
                                PrimaryScrollableTabRow(
                                    selectedTabIndex = selectedTabIndex,
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    edgePadding = 16.dp,
                                    indicator = {},
                                    divider = {}
                                ) {
                                    feedTabs.forEachIndexed { index, tab ->
                                        AnimatedTab(
                                            index = index,
                                            selectedIndex = selectedTabIndex,
                                            selected = selectedTabIndex == index,
                                            onClick = {
                                                viewModel.onAction(ForumAction.OnFeedChange(tab.feed))
                                            },
                                            icon = tab.icon,
                                            label = tab.label
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // Category chips row
                        item(key = "categories") {
                            StaggeredAnimatedVisibility(key = "forum_categories", index = 2) {
                                Column {
                                    SectionHeader(
                                        title = stringResource(R.string.forum_browse_categories),
                                        level = HeaderLevel.Section
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        itemsIndexed(defaultCategories, key = { _, c -> "cat_${c.id}" }) { index, category ->
                                            HorizontalStaggeredAnimatedVisibility(
                                                key = "cat_chip_${category.id}",
                                                index = index
                                            ) {
                                                ForumCategoryChip(
                                                    category = category,
                                                    onClick = { onCategoryClick(category.id, category.name) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // Threads section header
                        item(key = "threads_header") {
                            StaggeredAnimatedVisibility(key = "forum_threads_header", index = 3) {
                                SectionHeader(
                                    title = when (uiState.selectedFeed) {
                                        ForumFeed.OVERVIEW -> stringResource(R.string.forum_recent_discussions)
                                        ForumFeed.RECENT -> stringResource(R.string.forum_recently_active)
                                        ForumFeed.NEW -> stringResource(R.string.forum_new_threads)
                                    },
                                    level = HeaderLevel.Section
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // Thread cards
                        itemsIndexed(
                            items = uiState.threads,
                            key = { _, thread -> "thread_${thread.id}" },
                            contentType = { _, _ -> "ForumThread" }
                        ) { index, thread ->
                            StaggeredAnimatedVisibility(
                                key = "thread_${thread.id}",
                                index = index + 4
                            ) {
                                ForumThreadCard(
                                    thread = thread,
                                    onClick = { onThreadClick(thread.id, thread.title) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumLoadingSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item { Spacer(Modifier.height(80.dp)) }
        items(8) { ForumThreadCardSkeleton(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
    }
}
