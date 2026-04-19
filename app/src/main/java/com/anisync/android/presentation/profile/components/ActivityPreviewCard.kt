package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.CardLabelStrip
import com.anisync.android.presentation.components.ChainedAuthorRow
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.forum.components.StatBadge

@Composable
fun ActivityPreviewCard(
    activity: UserActivity,
    onClick: () -> Unit,
    onSubscribeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val lockedLike = activity.isLocked || activity.isPrivate
            if (activity.isPinned || lockedLike) {
                CardLabelStrip(
                    isPinned = activity.isPinned,
                    isLocked = lockedLike
                )
                Spacer(Modifier.height(12.dp))
            }

            val createdAtSeconds = activity.timestamp / 1000L
            val subtitle = remember(createdAtSeconds) { formatRelativeTimeSeconds(createdAtSeconds) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ChainedAuthorRow(
                    leadingAvatarUrl = activity.userAvatarUrl,
                    leadingName = activity.userName.orEmpty(),
                    trailingAvatarUrl = if (activity.type == ActivityType.MESSAGE) activity.recipientAvatarUrl else null,
                    trailingName = if (activity.type == ActivityType.MESSAGE) activity.recipientName else null,
                    subtitle = subtitle,
                    modifier = Modifier.weight(1f),
                    onLeadingClick = { name -> if (name.isNotBlank()) onUserClick(name) },
                    onTrailingClick = { name -> if (name.isNotBlank()) onUserClick(name) }
                )

                IconButton(onClick = onSubscribeClick) {
                    Icon(
                        imageVector = if (activity.isSubscribed) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                        contentDescription = if (activity.isSubscribed) "Unsubscribe" else "Subscribe",
                        tint = if (activity.isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            val preview = remember(activity.text) { stripHtml(activity.text.orEmpty()) }
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatBadge(
                    icon = Icons.Default.ChatBubbleOutline,
                    value = activity.replyCount,
                    contentDescription = "${activity.replyCount} replies"
                )
                StatBadge(
                    icon = Icons.Outlined.FavoriteBorder,
                    value = activity.likeCount,
                    contentDescription = "${activity.likeCount} likes",
                    tint = if (activity.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun stripHtml(html: String): String =
    html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<.*?>"), "")
        .replace(Regex("\\n+"), " ")
        .trim()
