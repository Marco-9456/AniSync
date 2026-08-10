package com.anisync.android.presentation.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Settings
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Unread rows are always the newest run, so the split is a prefix rather than a filter.
    val newEntries = remember(uiState.entries) { uiState.entries.takeWhile { it.isUnread } }
    val earlierEntries = remember(uiState.entries, newEntries) {
        uiState.entries.drop(newEntries.size)
    }

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

    val notificationCard: @Composable (NotificationEntry) -> Unit = { entry ->
        NotificationGroupCard(
            entry = entry,
            onMediaClick = onMediaClick,
            onUserClick = onUserClick,
            onActivityClick = onActivityClick,
            onThreadClick = onThreadClick,
            selectedTarget = selectedTarget,
            isUnread = entry.isUnread
        )
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
        titleTrailing = if (newEntries.isNotEmpty()) {
            { NewCountPill(count = newEntries.size) }
        } else {
            null
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
                        if (newEntries.isNotEmpty()) {
                            item(key = "section_new") {
                                NotificationSectionHeader(
                                    label = stringResource(R.string.notifications_section_new),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    MarkAllReadButton(
                                        onClick = {
                                            viewModel.onAction(NotificationsAction.MarkAllRead)
                                        }
                                    )
                                }
                            }
                            items(newEntries, key = { it.key }) { entry ->
                                notificationCard(entry)
                            }
                            if (earlierEntries.isNotEmpty()) {
                                item(key = "section_earlier") {
                                    NotificationSectionHeader(
                                        label = stringResource(
                                            R.string.notifications_section_earlier
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        items(earlierEntries, key = { it.key }) { entry ->
                            notificationCard(entry)
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

/** "2 new" beside the screen title. Counts the rows on screen, so filtering narrows it. */
@Composable
private fun NewCountPill(count: Int) {
    val amount = if (count > 99) {
        stringResource(R.string.notifications_new_count_overflow)
    } else {
        count.toString()
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = stringResource(R.string.notifications_new_count, amount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/**
 * NEW / EARLIER divider. The 10dp above and below lands on the design's 20dp once the list's own
 * 10dp item spacing is added.
 */
@Composable
private fun NotificationSectionHeader(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = color,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        trailing?.invoke()
    }
}

/** Sits next to what it acts on rather than in the app bar, and only exists while a NEW run does. */
@Composable
private fun MarkAllReadButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.notifications_mark_all_read),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
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
