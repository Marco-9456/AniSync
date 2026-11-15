package com.anisync.android.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.components.ActivityCard
import com.anisync.android.presentation.profile.components.ActivityUpdateRow
import com.anisync.android.presentation.profile.components.updateRowShape
import com.anisync.android.presentation.profile.sections.ActivityDayHeader
import com.anisync.android.presentation.profile.sections.dayIn
import com.anisync.android.presentation.profile.sections.isGroupedUpdate
import java.time.ZoneId

@Composable
fun RecentUpdatesSection(
    activities: List<UserActivity>,
    modifier: Modifier = Modifier,
    maxItems: Int = 5,
    /** When set, each card caps its body to ~this many lines with a fade (compact teaser). */
    maxBodyLines: Int? = null,
    onActionClick: (() -> Unit)? = null,
    onUserClick: (String) -> Unit = {},
    onActivityClick: (Int) -> Unit = {},
    onMediaClick: (Int) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    onSubscribeClick: (Int) -> Unit = {},
    onLikeClick: ((activityId: Int) -> Unit)? = null,
    onDeleteClick: ((activityId: Int) -> Unit)? = null,
    onEditClick: ((activityId: Int) -> Unit)? = null,
    viewerId: Int? = null
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.section_recent_updates),
            level = HeaderLevel.Section,
            padding = PaddingValues(bottom = 20.dp),
            onActionClick = onActionClick
        )

        val displayedActivities = remember(activities, maxItems) { activities.take(maxItems) }

        val zone = remember { ZoneId.systemDefault() }
        Column {
            displayedActivities.forEachIndexed { index, activity ->
                val day = activity.dayIn(zone)
                val previous = displayedActivities.getOrNull(index - 1)
                val next = displayedActivities.getOrNull(index + 1)
                if (!activity.isPinned && (previous == null || previous.isPinned || previous.dayIn(zone) != day)) {
                    ActivityDayHeader(day, start = 16.dp)
                }
                val update = activity.isGroupedUpdate()
                val joinsAbove = update && previous?.isGroupedUpdate() == true && previous.dayIn(zone) == day
                val joinsBelow = update && next?.isGroupedUpdate() == true && next.dayIn(zone) == day
                key(activity.id) {
                    val canDelete = viewerId != null && (
                        activity.userId == viewerId ||
                            (activity.type == ActivityType.MESSAGE && activity.recipientId == viewerId && !activity.isAuthorMod)
                    )
                    // Edit allowed only for the author of TEXT or MESSAGE activity.
                    // MEDIA_LIST is server-derived and not editable.
                    val canEdit = viewerId != null &&
                        activity.userId == viewerId &&
                        (activity.type == ActivityType.TEXT || activity.type == ActivityType.MESSAGE)
                    val cardLike = onLikeClick?.let { cb -> { cb(activity.id) } }
                    val cardDelete = if (canDelete) {
                        onDeleteClick?.let { cb -> { cb(activity.id) } }
                    } else null
                    val cardEdit = if (canEdit) {
                        onEditClick?.let { cb -> { cb(activity.id) } }
                    } else null

                    if (update) {
                        ActivityUpdateRow(
                            activity = activity,
                            shape = updateRowShape(top = !joinsAbove, bottom = !joinsBelow),
                            onClick = { onActivityClick(activity.id) },
                            onMediaClick = onMediaClick,
                            onLikeClick = cardLike,
                            modifier = Modifier.padding(bottom = if (joinsBelow) 2.dp else 12.dp)
                        )
                    } else {
                        ActivityCard(
                            activity = activity,
                            onClick = { onActivityClick(activity.id) },
                            onUserClick = onUserClick,
                            onMediaClick = onMediaClick,
                            onLastReplyClick = onLastReplyClick,
                            onSubscribeClick = { onSubscribeClick(activity.id) },
                            onLikeClick = cardLike,
                            onDeleteClick = cardDelete,
                            onEditClick = cardEdit,
                            maxBodyLines = maxBodyLines,
                            showAuthor = false,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

