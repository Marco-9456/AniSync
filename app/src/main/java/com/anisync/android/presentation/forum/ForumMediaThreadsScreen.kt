package com.anisync.android.presentation.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.forum.components.ForumThreadCard
import com.anisync.android.presentation.forum.components.ForumThreadCardSkeleton

/**
 * Full, paginated list of forum threads that have a given media as a
 * `mediaCategory`. Reached from the media-detail Discussions section's
 * "View all". The FAB starts a thread pre-attached to this media.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumMediaThreadsScreen(
    mediaTitle: String,
    onBackClick: () -> Unit,
    onThreadClick: (threadId: Int, threadTitle: String) -> Unit,
    onThreadCommentClick: (threadId: Int, commentId: Int) -> Unit,
    onUserClick: (String) -> Unit,
    onCreateThread: () -> Unit,
    viewModel: ForumMediaThreadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.forum_discussions_title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (mediaTitle.isNotBlank()) {
                            Text(
                                text = mediaTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateThread,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.forum_discussions_start)
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = rememberRateLimitedRefresh { viewModel.refresh() },
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
                uiState.isLoading -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(6) { ForumThreadCardSkeleton(Modifier.fillMaxWidth()) }
                    }
                }

                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.refresh() }
                )

                uiState.threads.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.forum_discussions_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = systemBarsPadding.calculateBottomPadding() + 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.threads,
                            key = { _, thread -> "media_thread_${thread.id}" },
                            contentType = { _, _ -> "ForumThread" }
                        ) { index, thread ->
                            if (index >= uiState.threads.size - 4 && uiState.hasNextPage && !uiState.isLoading && !uiState.isPaginating) {
                                LaunchedEffect(index) { viewModel.loadMore() }
                            }
                            ForumThreadCard(
                                thread = thread,
                                onClick = { onThreadClick(thread.id, thread.title) },
                                onUserClick = onUserClick,
                                isSaved = thread.id in uiState.savedThreadIds,
                                onSaveClick = { viewModel.toggleSave(thread) },
                                isSubscribed = thread.isSubscribed,
                                onSubscribeClick = { viewModel.toggleSubscribe(thread) },
                                onLastReplyClick = onThreadCommentClick,
                                modifier = Modifier.fillMaxWidth()
                            )
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
