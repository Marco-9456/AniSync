package com.anisync.android.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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

@Composable
fun RecentUpdatesSection(
    activities: List<UserActivity>,
    modifier: Modifier = Modifier,
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
            padding = PaddingValues(bottom = 20.dp)
        )

        val displayedActivities = remember(activities) { activities.take(5) }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            displayedActivities.forEach { activity ->
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

                    ActivityCard(
                        activity = activity,
                        onClick = { onActivityClick(activity.id) },
                        onUserClick = onUserClick,
                        onMediaClick = onMediaClick,
                        onLastReplyClick = onLastReplyClick,
                        onSubscribeClick = { onSubscribeClick(activity.id) },
                        onLikeClick = cardLike,
                        onDeleteClick = cardDelete,
                        onEditClick = cardEdit
                    )
                }
            }
        }
    }
}

