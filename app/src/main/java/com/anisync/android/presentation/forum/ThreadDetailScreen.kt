package com.anisync.android.presentation.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.components.StaggeredAnimatedVisibility
import com.anisync.android.presentation.forum.components.ReplyBottomSheetContent
import com.anisync.android.presentation.forum.components.ThreadCommentItem
import com.anisync.android.presentation.forum.components.ThreadHeaderItem
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailScreen(
    threadId: Int,
    threadTitle: String,
    onBackClick: () -> Unit,
    viewModel: ThreadDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val replySheetState = rememberModalBottomSheetState()
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(threadId) {
        viewModel.onAction(ThreadDetailAction.Load(threadId))
    }

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is ThreadDetailAction.ShowSnackbar -> snackbarHostState.showSnackbar(action.message)
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
                        text = uiState.thread?.title ?: threadTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            val thread = uiState.thread
            if (thread != null && !thread.isLocked) {
                StaggeredAnimatedVisibility(key = "thread_fab", index = 10) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.onAction(ThreadDetailAction.OpenReply(null, null))
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.forum_reply))
                    }
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.thread != null,
            state = pullToRefreshState,
            onRefresh = { viewModel.onAction(ThreadDetailAction.Load(threadId)) },
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = uiState.isLoading && uiState.thread != null,
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.thread == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.onAction(ThreadDetailAction.Load(threadId)) }
                )

                else -> {
                    val thread = uiState.thread ?: return@PullToRefreshBox

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Thread header (body + stats)
                        item(key = "thread_header") {
                            StaggeredAnimatedVisibility(key = "header_item", index = 0) {
                                ThreadHeaderItem(
                                    thread = thread,
                                    onLikeClick = {
                                        viewModel.onAction(
                                            ThreadDetailAction.ToggleLike(
                                                isThread = true,
                                                id = thread.id,
                                                currentLiked = thread.isLiked
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Comments section header
                        item(key = "comments_header") {
                            StaggeredAnimatedVisibility(key = "comments_title", index = 1) {
                                SectionHeader(
                                    title = stringResource(R.string.forum_comments),
                                    level = HeaderLevel.Section
                                )
                            }
                        }

                        // Comment items or empty state
                        if (uiState.comments.isEmpty() && !uiState.isLoadingMoreComments) {
                            item(key = "empty_comments") {
                                StaggeredAnimatedVisibility(key = "empty_comments", index = 2) {
                                    EmptyStateConfigs.ForumNoComments()
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = uiState.comments,
                                key = { _, c -> "comment_${c.id}" },
                                contentType = { _, _ -> "Comment" }
                            ) { index, comment ->
                                StaggeredAnimatedVisibility(key = "comment_${comment.id}", index = index + 2) {
                                    ThreadCommentItem(
                                        comment = comment,
                                        onLikeClick = { commentId, currentLiked ->
                                            viewModel.onAction(
                                                ThreadDetailAction.ToggleLike(
                                                    isThread = false,
                                                    id = commentId,
                                                    currentLiked = currentLiked
                                                )
                                            )
                                        },
                                        onReplyClick = if (!thread.isLocked) {
                                            { commentId, authorName ->
                                                viewModel.onAction(
                                                    ThreadDetailAction.OpenReply(commentId, authorName)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Load more comments button
                        if (uiState.hasMoreComments) {
                            item(key = "load_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMoreComments) {
                                        CircularProgressIndicator()
                                    } else {
                                        FilledTonalButton(
                                            onClick = { viewModel.onAction(ThreadDetailAction.LoadMoreComments) },
                                            shape = RoundedCornerShape(percent = 50)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.forum_load_more_comments),
                                                fontWeight = FontWeight.SemiBold
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

    // Reply bottom sheet
    if (uiState.isReplySheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(ThreadDetailAction.CloseReply) },
            sheetState = replySheetState
        ) {
            ReplyBottomSheetContent(
                replyingToAuthor = uiState.replyTargetAuthorName,
                isSubmitting = uiState.isSubmittingReply,
                onSubmit = { body ->
                    viewModel.onAction(ThreadDetailAction.SubmitReply(threadId, body))
                },
                onDismiss = { viewModel.onAction(ThreadDetailAction.CloseReply) }
            )
        }
    }
}
