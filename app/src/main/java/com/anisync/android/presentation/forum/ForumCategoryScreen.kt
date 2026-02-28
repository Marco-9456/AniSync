package com.anisync.android.presentation.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateConfigs
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
    viewModel: ForumCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val pullToRefreshState = rememberPullToRefreshState()

    // Infinite scroll trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
    }

    LaunchedEffect(categoryId) {
        viewModel.onAction(ForumCategoryAction.Load(categoryId, categoryName))
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.onAction(ForumCategoryAction.LoadMore)
    }

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is ForumCategoryAction.ShowSnackbar -> snackbarHostState.showSnackbar(action.message)
                is ForumCategoryAction.OnThreadClick -> onThreadClick(action.threadId, action.threadTitle)
                else -> {}
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = categoryName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = { viewModel.onAction(ForumCategoryAction.Refresh) },
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
                uiState.isLoading -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item { Spacer(Modifier.height(16.dp)) }
                    items(8) { ForumThreadCardSkeleton(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                }

                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.onAction(ForumCategoryAction.Refresh) }
                )

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Search bar
                        item(key = "search") {
                            SearchField(
                                query = uiState.searchQuery,
                                onQueryChange = { viewModel.onAction(ForumCategoryAction.OnSearchQueryChange(it)) },
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
