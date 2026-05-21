package com.anisync.android.presentation.profile

import com.anisync.android.domain.url

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.components.ActivityOverflowMenu
import com.anisync.android.presentation.profile.components.ActivityPreviewCard
import com.anisync.android.presentation.profile.components.ActivityStatPill
import com.anisync.android.presentation.profile.components.RecentUpdateCard
import com.anisync.android.presentation.profile.util.formatProfileRelativeTime

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
                            onSubscribeClick = { onSubscribeClick(activity.id) },
                            onUserClick = onUserClick,
                            onLastReplyClick = onLastReplyClick,
                            onLikeClick = cardLike,
                            onDeleteClick = cardDelete,
                            onEditClick = cardEdit
                        )
                    }
                }
            }
        }
    }
}

