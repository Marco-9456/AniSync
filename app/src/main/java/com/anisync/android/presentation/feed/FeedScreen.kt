package com.anisync.android.presentation.feed

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.FeedScope
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateCompact
import com.anisync.android.presentation.components.EmptyStateConfigs
import com.anisync.android.presentation.components.MarkdownComposeSheet
import com.anisync.android.presentation.feed.components.FeedFilterBar
import com.anisync.android.presentation.profile.RecentUpdateCard
import com.anisync.android.presentation.profile.components.ActivityPreviewCard
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeedScreen(
    onActivityClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    onMediaClick: (Int) -> Unit,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    onLoginClick: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val composeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Box(modifier = Modifier.padding(bottom = 85.dp)) {
                FloatingActionButton(
                    onClick = { viewModel.onAction(FeedAction.OpenCompose) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Write a status"
                    )
                }
            }
        },
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                FeedFilterBar(
                    filter = uiState.filter,
                    scope = uiState.scope,
                    mediaType = uiState.mediaType,
                    onFilterChange = { viewModel.onAction(FeedAction.OnFilterChange(it)) },
                    onScopeChange = { viewModel.onAction(FeedAction.OnScopeChange(it)) },
                    onMediaTypeChange = { viewModel.onAction(FeedAction.OnMediaTypeChange(it)) },
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(Modifier.height(4.dp))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = { viewModel.onAction(FeedAction.Refresh) },
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
                !uiState.isAuthenticated && uiState.scope == FeedScope.FOLLOWING -> {
                    EmptyStateConfigs.NotLoggedIn(
                        onLoginClick = onLoginClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.isLoading && uiState.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null && uiState.items.isEmpty() -> {
                    EmptyStateConfigs.GenericError(
                        message = uiState.errorMessage!!,
                        onRetryClick = { viewModel.onAction(FeedAction.Refresh) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.items.isEmpty() -> {
                    val emptyMsg = when (uiState.scope) {
                        FeedScope.GLOBAL -> stringResource(R.string.feed_empty_global)
                        FeedScope.FOLLOWING -> stringResource(R.string.feed_empty_following)
                    }
                    EmptyStateCompact(
                        icon = Icons.Default.DynamicFeed,
                        title = emptyMsg,
                        modifier = Modifier.fillMaxSize()
                    )
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
                            items = uiState.items,
                            key = { _, activity -> "feed_${activity.id}" },
                            contentType = { _, activity -> activity.type.name }
                        ) { index, activity ->

                            if (index >= uiState.items.size - 4 && uiState.hasNextPage && !uiState.isLoading && !uiState.isPaginating) {
                                LaunchedEffect(index) {
                                    viewModel.onAction(FeedAction.LoadMore)
                                }
                            }

                            key(activity.id) {
                                val isOwner = uiState.viewerId != null &&
                                    activity.userId == uiState.viewerId
                                val cardLike: () -> Unit = {
                                    viewModel.onAction(FeedAction.ToggleLike(activity.id))
                                }
                                val cardDelete: (() -> Unit)? = if (isOwner) {
                                    { viewModel.onAction(FeedAction.DeleteActivity(activity.id)) }
                                } else null

                                if (activity.type == ActivityType.MEDIA_LIST) {
                                    RecentUpdateCard(
                                        activity = activity,
                                        onUserClick = onUserClick,
                                        onActivityClick = onActivityClick,
                                        onMediaClick = onMediaClick,
                                        onLastReplyClick = onLastReplyClick,
                                        onLikeClick = cardLike,
                                        onDeleteClick = cardDelete
                                    )
                                } else {
                                    ActivityPreviewCard(
                                        activity = activity,
                                        onClick = { onActivityClick(activity.id) },
                                        onSubscribeClick = {
                                            viewModel.onAction(FeedAction.ToggleSubscribe(activity.id))
                                        },
                                        onUserClick = onUserClick,
                                        onLastReplyClick = onLastReplyClick,
                                        onLikeClick = cardLike,
                                        onDeleteClick = cardDelete
                                    )
                                }
                            }
                        }

                        if (uiState.isPaginating) {
                            item(key = "feed_paginating_indicator") {
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

    if (uiState.isComposeSheetVisible) {
        MarkdownComposeSheet(
            title = stringResource(R.string.feed_compose_title),
            placeholder = stringResource(R.string.feed_compose_placeholder),
            submitLabel = stringResource(R.string.feed_compose_submit),
            isSubmitting = uiState.isPostingStatus,
            onSubmit = { body -> viewModel.onAction(FeedAction.PostStatus(body)) },
            onDismiss = { viewModel.onAction(FeedAction.DismissCompose) },
            sheetState = composeSheetState
        )
    }
}
