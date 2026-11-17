package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.LocalWhatsNew
import com.anisync.android.presentation.components.RailChip
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.WhatsNewPill
import com.anisync.android.presentation.components.rememberActiveWhatsNew
import com.anisync.android.presentation.profile.ProfileActivityFilter
import com.anisync.android.presentation.profile.components.ActivityCard
import com.anisync.android.presentation.profile.components.ActivityUpdateRow
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.profile.components.ProfileTabRail
import com.anisync.android.presentation.profile.components.RailScroll
import com.anisync.android.presentation.profile.components.rememberRailScroll
import com.anisync.android.presentation.profile.components.updateRowShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

fun LazyListScope.profileActivityTab(
    profile: UserProfile,
    selectedFilter: ProfileActivityFilter,
    onUserClick: (String) -> Unit = {},
    onActivityClick: (Int) -> Unit = {},
    onMediaClick: (Int) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    onSubscribeClick: (Int) -> Unit = {},
    onLikeActivity: ((Int) -> Unit)? = null,
    onDeleteActivity: ((Int) -> Unit)? = null,
    onEditActivity: ((Int) -> Unit)? = null,
    viewerId: Int? = null,
    activitiesHasNextPage: Boolean = false,
    isActivitiesPaginating: Boolean = false,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
    onCompose: (() -> Unit)? = null,
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {}
) {
    if (onCompose != null && (selectedFilter == ProfileActivityFilter.ALL || selectedFilter == ProfileActivityFilter.STATUS)) {
        item(key = "activity_composer", contentType = "activity_composer") {
            StatusComposerCard(avatarUrl = profile.avatarUrl, onClick = onCompose, expanded = true)
        }
    }
    val filteredActivities = when (selectedFilter) {
        ProfileActivityFilter.ALL -> profile.activities
        ProfileActivityFilter.STATUS -> profile.activities.filter { it.type == ActivityType.TEXT }
        ProfileActivityFilter.MESSAGES -> profile.activities.filter { it.type == ActivityType.MESSAGE }
        ProfileActivityFilter.LISTS -> profile.activities.filter { it.type == ActivityType.MEDIA_LIST }
        ProfileActivityFilter.REVIEWS, ProfileActivityFilter.THREADS, ProfileActivityFilter.COMMENTS -> emptyList()
    }

    if (loadFailed && profile.activities.isEmpty()) {
        item(key = "activity_failed", contentType = "empty") {
            EmptyState(
                icon = Icons.Outlined.ErrorOutline,
                title = stringResource(R.string.profile_activity_failed),
                description = stringResource(R.string.profile_activity_failed_body),
                actionLabel = stringResource(R.string.profile_try_again),
                actionIcon = Icons.Default.Refresh,
                onAction = onRetry,
                modifier = Modifier.height(360.dp)
            )
        }
    } else if (filteredActivities.isEmpty() && onCompose != null && selectedFilter == ProfileActivityFilter.STATUS) {
        item(key = "activity_empty", contentType = "empty") {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = stringResource(R.string.profile_no_statuses),
                description = stringResource(R.string.profile_no_statuses_body),
                modifier = Modifier.height(360.dp)
            )
        }
    } else if (filteredActivities.isEmpty()) {
        item(key = "activity_empty", contentType = "empty") {
            PlaceholderTabContent(
                message = stringResource(
                    R.string.profile_no_activity_for_filter,
                    stringResource(selectedFilter.labelRes)
                ),
                modifier = modifier
            )
        }
    } else {
        val zone = ZoneId.systemDefault()
        filteredActivities.forEachIndexed { index, activity ->
            val day = activity.dayIn(zone)
            val previous = filteredActivities.getOrNull(index - 1)
            if (!activity.isPinned && (previous == null || previous.isPinned || previous.dayIn(zone) != day)) {
                item(key = "activity_day_${index}_$day", contentType = "activity_day") {
                    ActivityDayHeader(day, start = 20.dp, top = 12.dp, bottom = 8.dp)
                }
            }
            val update = activity.isGroupedUpdate()
            val joinsAbove = update && previous?.isGroupedUpdate() == true && previous.dayIn(zone) == day
            val next = filteredActivities.getOrNull(index + 1)
            val joinsBelow = update && next?.isGroupedUpdate() == true && next.dayIn(zone) == day
            item(key = "activity_${activity.id}", contentType = if (update) "activity_update" else "activity_item") {
            val canDelete = viewerId != null && (
                activity.userId == viewerId ||
                    (activity.type == ActivityType.MESSAGE && activity.recipientId == viewerId && !activity.isAuthorMod)
            )
            // Edit only on own TEXT or MESSAGE — never on server-derived MEDIA_LIST.
            val canEdit = viewerId != null &&
                activity.userId == viewerId &&
                (activity.type == ActivityType.TEXT || activity.type == ActivityType.MESSAGE)
            val cardLike = onLikeActivity?.let { cb -> { cb(activity.id) } }
            val cardDelete = if (canDelete) {
                onDeleteActivity?.let { cb -> { cb(activity.id) } }
            } else null
            val cardEdit = if (canEdit) {
                onEditActivity?.let { cb -> { cb(activity.id) } }
            } else null

            if (update) {
                // A day's updates join into one card.
                ActivityUpdateRow(
                    activity = activity,
                    shape = updateRowShape(top = !joinsAbove, bottom = !joinsBelow),
                    onClick = { onActivityClick(activity.id) },
                    onMediaClick = onMediaClick,
                    onLikeClick = cardLike,
                    extraTop = if (joinsAbove) 0.dp else 4.dp,
                    extraBottom = if (joinsBelow) 0.dp else 4.dp,
                    modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = if (joinsBelow) 0.dp else 8.dp)
                )
            } else {
                ActivityCard(
                    activity = activity,
                    onClick = { onActivityClick(activity.id) },
                    modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    onUserClick = onUserClick,
                    onMediaClick = onMediaClick,
                    onLastReplyClick = onLastReplyClick,
                    onSubscribeClick = { onSubscribeClick(activity.id) },
                    onLikeClick = cardLike,
                    onDeleteClick = cardDelete,
                    onEditClick = cardEdit,
                    showAuthor = false,
                    pinnedLabel = true,
                    timeText = activityTime(activity)
                )
            }
            }
        }

        // Infinite-scroll trigger and footer loader, unified into one item. It exists only
        // while older pages remain and, being the last item, is composed only when the user
        // scrolls near the end of the filtered list. Driving the fetch from this single item
        // (instead of a per-row LaunchedEffect) avoids the stampede of duplicate requests the
        // old per-row trigger fired — every visible tail row requested the same next page.
        //
        // The effect is keyed on the raw loaded count (profile.activities.size), which grows
        // by a whole page on every successful fetch. That walks forward through pages when a
        // filter's latest page added no matching rows (so filteredActivities didn't grow),
        // yet it does NOT re-fire after a failed fetch (count unchanged) — a network error
        // can't spin a retry storm; the user re-triggers by scrolling.
        //
        // The spinner is gated on isActivitiesPaginating so it shows only while a page is
        // actually in flight and never lingers once loading settles (#89). Bare default size,
        // like the app's other wavy loaders — a height/size constraint squishes the wave into
        // a malformed, never-completing arc.
        if (activitiesHasNextPage) {
            item(key = "activity_paginating", contentType = "paginating") {
                LaunchedEffect(profile.activities.size) {
                    if (!isActivitiesPaginating) onLoadMore()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActivitiesPaginating) {
                        AppCircularProgressIndicator()
                    }
                }
            }
        }

        item(key = "activity_bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

internal fun UserActivity.dayIn(zone: ZoneId): Long =
    Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().toEpochDay()

internal fun UserActivity.isGroupedUpdate(): Boolean = type == ActivityType.MEDIA_LIST && !isPinned

/** Under a day's header a card only needs its time, and the pinned post, above the days, its date. */
@Composable
private fun activityTime(activity: UserActivity): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(activity.timestamp, activity.isPinned, locale) {
        val date = Date(activity.timestamp)
        if (activity.isPinned) {
            val day = Instant.ofEpochMilli(activity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            val skeleton = if (day.year == LocalDate.now().year) "MMMd" else "MMMdyyyy"
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            DateTimeFormatter.ofPattern(pattern, locale).format(day)
        } else {
            android.text.format.DateFormat.getTimeFormat(context).format(date)
        }
    }
}

@Composable
internal fun ActivityDayHeader(epochDay: Long, start: Dp = 32.dp, top: Dp = 16.dp, bottom: Dp = 6.dp) {
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()
    val day = LocalDate.ofEpochDay(epochDay)
    val label = when (day) {
        today -> stringResource(R.string.feed_day_today)
        today.minusDays(1) -> stringResource(R.string.feed_day_yesterday)
        else -> {
            val skeleton = if (day.year == today.year) "EEEEMMMd" else "EEEEMMMdyyyy"
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
            DateTimeFormatter.ofPattern(pattern, locale).format(day)
        }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = start, end = 16.dp, top = top, bottom = bottom)
    )
}

/**
 * Where you post a status. Overview shows the one-line pill, the Activity tab the fuller card with
 * the format bar the composer opens with. Either way a tap opens the composer.
 */
@Composable
internal fun StatusComposerCard(avatarUrl: String?, onClick: () -> Unit, expanded: Boolean = false) {
    if (expanded) {
        val label = stringResource(R.string.profile_post_status)
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            ) {
                Row(modifier = Modifier.height(88.dp)) {
                    UserAvatar(contentDescription = null, size = 40.dp, url = avatarUrl)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ComposerFormatIcons.forEach { icon ->
                        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onClick, enabled = false) {
                        Text(stringResource(R.string.post))
                    }
                }
            }
        }
        return
    }
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(56.dp)
                .padding(start = 8.dp, end = 16.dp)
        ) {
            UserAvatar(contentDescription = null, size = 40.dp, url = avatarUrl)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.profile_post_status),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val ComposerFormatIcons = listOf(
    Icons.Outlined.AttachFile,
    Icons.Default.FormatBold,
    Icons.Default.FormatItalic,
    Icons.Default.FormatStrikethrough,
    Icons.Default.Code,
    Icons.Default.Link,
    Icons.Default.VisibilityOff
)

/** The Activity tab's filters, docked with the profile tabs. */
@Composable
internal fun ActivityFilterRail(
    selected: ProfileActivityFilter,
    scroll: RailScroll = rememberRailScroll(),
    onSelect: (ProfileActivityFilter) -> Unit
) {
    val whatsNewStore = LocalWhatsNew.current
    val postsMoved = WhatsNewItem.PROFILE_POSTS_MOVED in rememberActiveWhatsNew()
    LaunchedEffect(selected) {
        if (!selected.isFeed) whatsNewStore.markSeen(WhatsNewItem.PROFILE_POSTS_MOVED)
    }
    ProfileTabRail(ProfileActivityFilter.entries, selected, scroll = scroll) { filter, isSelected ->
        RailChip(
            label = stringResource(filter.labelRes),
            icon = activityFilterIcon(filter),
            selected = isSelected,
            onClick = { onSelect(filter) },
            badge = if (filter == ProfileActivityFilter.REVIEWS && postsMoved) {
                { WhatsNewPill(WhatsNewItem.Kind.MOVED) }
            } else {
                null
            }
        )
    }
}

private fun activityFilterIcon(filter: ProfileActivityFilter): ImageVector {
    return when (filter) {
        ProfileActivityFilter.ALL -> Icons.Outlined.ViewAgenda
        ProfileActivityFilter.STATUS -> Icons.AutoMirrored.Filled.Chat
        ProfileActivityFilter.MESSAGES -> Icons.Default.Mail
        ProfileActivityFilter.LISTS -> Icons.AutoMirrored.Filled.PlaylistAddCheck
        ProfileActivityFilter.REVIEWS -> Icons.Default.RateReview
        ProfileActivityFilter.THREADS -> Icons.Default.Forum
        ProfileActivityFilter.COMMENTS -> Icons.AutoMirrored.Filled.Comment
    }
}

