package com.anisync.android.presentation.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.ErrorState
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
                        style = MaterialTheme.typography.titleMedium
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
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
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
                val thread = uiState.thread ?: return@Scaffold

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Thread header (body + stats)
                    item(key = "thread_header") {
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

                    // Comments section header
                    item(key = "comments_header") {
                        Text(
                            text = stringResource(R.string.forum_comments),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Comment items
                    items(
                        items = uiState.comments,
                        key = { "comment_${it.id}" },
                        contentType = { "Comment" }
                    ) { comment ->
                        ThreadCommentItem(
                            comment = comment,
                            onLikeClick = {
                                viewModel.onAction(
                                    ThreadDetailAction.ToggleLike(
                                        isThread = false,
                                        id = comment.id,
                                        currentLiked = comment.isLiked
                                    )
                                )
                            },
                            onReplyClick = if (!thread.isLocked) {
                                {
                                    viewModel.onAction(
                                        ThreadDetailAction.OpenReply(comment.id, comment.authorName)
                                    )
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                    Button(onClick = { viewModel.onAction(ThreadDetailAction.LoadMoreComments) }) {
                                        Text(stringResource(R.string.forum_load_more_comments))
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
