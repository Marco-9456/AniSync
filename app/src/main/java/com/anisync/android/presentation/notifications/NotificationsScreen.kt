package com.anisync.android.presentation.notifications

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.NotificationFilter
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.CustomPullToRefreshIndicator
import com.anisync.android.presentation.components.EmptyStateConfigs
import com.anisync.android.presentation.components.alert.rememberRateLimitedRefresh
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.notifications.components.NotificationGroupCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    onActivityClick: (Int) -> Unit,
    onThreadClick: (threadId: Int, commentId: Int?) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    // The target open in the two-pane detail (or null); the matching notification card shows the ring.
    selectedTarget: NotificationTarget? = null,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= uiState.entries.size - 4
        }
    }
    LaunchedEffect(shouldLoadMore, uiState.entries.size, uiState.hasNextPage) {
        if (shouldLoadMore && uiState.entries.isNotEmpty() && uiState.hasNextPage) {
            viewModel.onAction(NotificationsAction.LoadNextPage)
        }
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.notifications_title),
        onBackClick = onBackClick,
        modifier = modifier,
        scrollableState = listState,
        enableEnterAnimation = true,
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_notifications)
                )
            }
        },
        belowBar = {
            FilterChipsRow(
                selected = uiState.filter,
                onSelect = { viewModel.onAction(NotificationsAction.SetFilter(it)) }
            )
        }
    ) { topContentPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = rememberRateLimitedRefresh { viewModel.onAction(NotificationsAction.Refresh) },
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                CustomPullToRefreshIndicator(
                    isRefreshing = uiState.isRefreshing,
                    state = pullState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topContentPadding)
                )
            }
        ) {
            when {
                uiState.isLoading && uiState.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) { AppCircularProgressIndicator() }
                }
                uiState.errorMessage != null && uiState.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding)
                    ) {
                        ErrorState(
                            message = uiState.errorMessage ?: "",
                            onRetry = { viewModel.onAction(NotificationsAction.Retry) }
                        )
                    }
                }
                uiState.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding)
                    ) {
                        EmptyStateConfigs.NoNotifications()
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = topContentPadding + 8.dp,
                            bottom = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.entries, key = { it.key }) { entry ->
                            NotificationGroupCard(
                                entry = entry,
                                onMediaClick = onMediaClick,
                                onUserClick = onUserClick,
                                onActivityClick = onActivityClick,
                                onThreadClick = onThreadClick,
                                selectedTarget = selectedTarget
                            )
                        }
                        if (uiState.isPaginating) {
                            item(key = "paginating") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppCircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selected: NotificationFilter,
    onSelect: (NotificationFilter) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NotificationFilter.values().forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.labelRes())) }
            )
        }
    }
}

private fun NotificationFilter.labelRes(): Int = when (this) {
    NotificationFilter.ALL -> R.string.notifications_filter_all
    NotificationFilter.AIRING -> R.string.notifications_filter_airing
    NotificationFilter.STATUS -> R.string.notifications_filter_status
    NotificationFilter.MESSAGES -> R.string.notifications_filter_messages
    NotificationFilter.FORUM -> R.string.notifications_filter_forum
    NotificationFilter.FOLLOWS -> R.string.notifications_filter_follows
    NotificationFilter.MEDIA -> R.string.notifications_filter_media
}
