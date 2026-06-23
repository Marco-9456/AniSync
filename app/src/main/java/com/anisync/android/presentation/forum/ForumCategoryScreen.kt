package com.anisync.android.presentation.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateConfigs
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.forum.components.ForumThreadCard
import com.anisync.android.presentation.forum.components.ForumThreadCardSkeleton
import com.anisync.android.presentation.forum.components.SearchField
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumCategoryScreen(
    categoryId: Int,
    categoryName: String,
    onBackClick: () -> Unit,
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit,
    onThreadCommentClick: (threadId: Int, commentId: Int) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: ForumCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(categoryId) {
        viewModel.onAction(ForumCategoryAction.Load(categoryId, categoryName))
    }

    LaunchedEffect(viewModel.actions) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is ForumCategoryAction.OnThreadClick -> onThreadClick(
                    action.threadId,
                    action.threadTitle
                )

                else -> {}
            }
        }
    }

    CollapsingTopBarScaffold(
        title = categoryName,
        onBackClick = onBackClick,
        scrollableState = listState
    ) { topContentPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = rememberRateLimitedRefresh { viewModel.onAction(ForumCategoryAction.Refresh) },
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = uiState.isRefreshing,
                    state = pullToRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topContentPadding)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topContentPadding, bottom = 16.dp)
                ) {
                    item { Spacer(Modifier.height(16.dp)) }
                    items(8) {
                        ForumThreadCardSkeleton(
                            Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }

                uiState.errorMessage != null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topContentPadding)
                ) {
                    ErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.onAction(ForumCategoryAction.Refresh) }
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(top = topContentPadding, bottom = 24.dp)
                    ) {
                        // Search bar
                        item(key = "search") {
                            SearchField(
                                query = uiState.searchQuery,
                                onQueryChange = {
                                    viewModel.onAction(
                                        ForumCategoryAction.OnSearchQueryChange(
                                            it
                                        )
                                    )
                                },
                                placeholder = stringResource(R.string.forum_search_threads),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        // Thread cards or empty state
                        if (uiState.threads.isEmpty()) {
                            item(key = "empty") {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    EmptyStateConfigs.ForumSearchNoResults(
                                        query = uiState.searchQuery
                                    )
                                } else {
                                    EmptyStateConfigs.ForumNoThreads()
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = uiState.threads,
                                key = { _, t -> "thread_${t.id}" },
                                contentType = { _, _ -> "ForumThread" }
                            ) { index, thread ->

                                if (index >= uiState.threads.size - 4 && uiState.hasNextPage && !uiState.isLoading && !uiState.isPaginating) {
                                    LaunchedEffect(index) {
                                        viewModel.onAction(ForumCategoryAction.LoadMore)
                                    }
                                }

                                ForumThreadCard(
                                    thread = thread,
                                    onClick = { onThreadClick(thread.id, thread.title) },
                                    onUserClick = onUserClick,
                                    onLastReplyClick = onThreadCommentClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
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
}
