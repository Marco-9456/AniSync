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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Edit
import com.anisync.android.presentation.components.ScrollToTopOnRequest
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.ContentLimits
import com.anisync.android.domain.FeedFilter
import com.anisync.android.domain.FeedScope
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.components.richtext.RichTextInputSheet
import com.anisync.android.presentation.feed.components.FeedDayHeader
import com.anisync.android.presentation.feed.components.FeedEmptyState
import com.anisync.android.presentation.feed.components.FeedOfflineState
import com.anisync.android.presentation.feed.components.FeedRail
import com.anisync.android.presentation.feed.components.NewActivityPill
import com.anisync.android.presentation.feed.components.GroupedListActivityCard
import com.anisync.android.presentation.profile.components.ActivityCard
import com.anisync.android.presentation.settings.activityMergeLabel
import com.anisync.android.presentation.util.LocalMainNavBarInset
import com.anisync.android.domain.MainTab
import com.anisync.android.presentation.util.LocalRailFabState
import com.anisync.android.presentation.util.SetRailFab
import kotlinx.coroutines.launch

/** AniList answers 429 when the app asks faster than its budget allows. */
private const val RATE_LIMIT_CODE = 429

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeedScreen(
    onActivityClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    onMediaClick: (Int) -> Unit,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    onComposeStatus: () -> Unit,
    onOpenActivitySettings: () -> Unit,
    // The activity id open in the two-pane detail (or null); its card shows the selection ring.
    selectedActivityId: Int? = null,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val pullToRefreshState = rememberPullToRefreshState()
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val coroutineScope = rememberCoroutineScope()
    val atTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    ScrollToTopOnRequest(uiState.scrollToTopRequest, listState)
    val feedItems = remember(uiState.items, uiState.groupListUpdates) {
        buildFeedItems(uiState.items, uiState.groupListUpdates)
    }

    // On rail layouts the compose action lives in the rail header (Material 3); on compact it stays a
    // floating action button below. SetRailFab is a no-op when there is no rail.
    val hasRail = LocalRailFabState.current != null
    SetRailFab(
        MainTab.FEED,
        Icons.Default.Edit,
        stringResource(R.string.cd_write_status),
        onComposeStatus
    )

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }

    Scaffold(
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
                if (!hasRail) {
                    FloatingActionButton(
                        onClick = onComposeStatus,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_write_status)
                        )
                    }
                }
            }
        },
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                FeedRail(
                    scope = uiState.scope,
                    filter = uiState.filter,
                    mediaType = uiState.mediaType,
                    groupListUpdates = uiState.groupListUpdates,
                    mergeWindowLabel = activityMergeLabel(uiState.activityMergeMinutes),
                    onScopeChange = { viewModel.onAction(FeedAction.OnScopeChange(it)) },
                    onFilterChange = { viewModel.onAction(FeedAction.OnFilterChange(it)) },
                    onListTypeChange = { viewModel.onAction(FeedAction.OnListTypeChange(it)) },
                    onToggleGroupListUpdates = {
                        viewModel.onAction(FeedAction.ToggleGroupListUpdates)
                    },
                    onOpenActivitySettings = onOpenActivitySettings,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(Modifier.height(4.dp))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = rememberRateLimitedRefresh { viewModel.onAction(FeedAction.Refresh) },
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
            NewActivityPill(
                count = uiState.newActivityCount,
                onClick = {
                    viewModel.onAction(FeedAction.DismissNewActivity)
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .zIndex(1f)
            )

            // Reaching the top is the same answer the pill offers, so it stops asking.
            LaunchedEffect(atTop, uiState.newActivityCount) {
                if (atTop && uiState.newActivityCount > 0) {
                    viewModel.onAction(FeedAction.DismissNewActivity)
                }
            }

            when {
                uiState.isLoading && uiState.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null && uiState.items.isEmpty() -> {
                    PullableState {
                        FeedOfflineState(
                            rateLimited = uiState.errorCode == RATE_LIMIT_CODE,
                            onRetry = { viewModel.onAction(FeedAction.Refresh) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                uiState.items.isEmpty() -> {
                    PullableState {
                        FeedEmptyState(
                            scope = uiState.scope,
                            filter = uiState.filter,
                            mediaType = uiState.mediaType,
                            onSwitchToGlobal = {
                                viewModel.onAction(FeedAction.OnScopeChange(FeedScope.GLOBAL))
                            },
                            onClearFilters = {
                                viewModel.onAction(FeedAction.OnFilterChange(FeedFilter.ALL))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 24.dp,
                            end = 24.dp,
                            top = 8.dp,
                            bottom = systemBarsPadding.calculateBottomPadding() + 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = feedItems,
                            key = { _, item -> item.key },
                            contentType = { _, item -> item::class }
                        ) { index, item ->

                            if (index >= feedItems.size - 4 && uiState.hasNextPage && !uiState.isLoading && !uiState.isPaginating) {
                                LaunchedEffect(index) {
                                    viewModel.onAction(FeedAction.LoadMore)
                                }
                            }

                            when (item) {
                                is FeedItem.DayHeader -> FeedDayHeader(startOfDay = item.startOfDay)

                                is FeedItem.Group -> GroupedListActivityCard(
                                    activities = item.activities,
                                    onActivityClick = onActivityClick,
                                    onUserClick = onUserClick,
                                    onMediaClick = onMediaClick
                                )

                                is FeedItem.Single -> {
                                    val activity = item.activity
                                    val isOwner = uiState.viewerId != null &&
                                        activity.userId == uiState.viewerId
                                    val cardLike: () -> Unit = {
                                        viewModel.onAction(FeedAction.ToggleLike(activity.id))
                                    }
                                    val cardDelete: (() -> Unit)? = if (isOwner) {
                                        { viewModel.onAction(FeedAction.DeleteActivity(activity.id)) }
                                    } else null
                                    // Edit only on own TEXT or MESSAGE activities — never on
                                    // server-derived MEDIA_LIST entries.
                                    val cardEdit: (() -> Unit)? =
                                        if (isOwner && (activity.type == ActivityType.TEXT ||
                                                activity.type == ActivityType.MESSAGE)) {
                                            { viewModel.onAction(FeedAction.EditActivity(activity.id)) }
                                        } else null

                                    ActivityCard(
                                        activity = activity,
                                        selected = activity.id == selectedActivityId,
                                        onClick = { onActivityClick(activity.id) },
                                        onUserClick = onUserClick,
                                        onMediaClick = onMediaClick,
                                        onLastReplyClick = onLastReplyClick,
                                        onSubscribeClick = {
                                            viewModel.onAction(FeedAction.ToggleSubscribe(activity.id))
                                        },
                                        onLikeClick = cardLike,
                                        onDeleteClick = cardDelete,
                                        onEditClick = cardEdit
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
                                    AppCircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val editing = uiState.editingActivity
    if (editing != null) {
        val isMessage = editing.type == ActivityType.MESSAGE
        val bounds = if (isMessage) ContentLimits.MessageActivity else ContentLimits.TextActivity
        RichTextInputSheet(
            title = stringResource(R.string.activity_edit_status_title),
            placeholder = stringResource(R.string.feed_compose_placeholder),
            submitLabel = stringResource(R.string.activity_edit_save),
            isSubmitting = uiState.isSavingEdit,
            prefillBody = editing.bodyMarkdown ?: editing.text,
            minLength = bounds.min,
            maxLength = bounds.max,
            onSubmit = { body -> viewModel.onAction(FeedAction.SubmitEdit(body)) },
            onDismiss = { viewModel.onAction(FeedAction.DismissEdit) }
        )
    }
}

/**
 * A full-screen state the reader can still pull down on.
 *
 * Pull to refresh listens for nested scroll, and a centred column reports none, so every empty and
 * error state used to ignore the gesture its own copy told the reader to use. One item as tall as
 * the viewport keeps the state centred and hands the pull through.
 */
@Composable
private fun PullableState(content: @Composable () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(modifier = Modifier.fillParentMaxSize()) {
                content()
            }
        }
    }
}
