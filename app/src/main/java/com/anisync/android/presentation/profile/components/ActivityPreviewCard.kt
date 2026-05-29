package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.ui.theme.LocalAvatarShape

@Composable
fun ActivityPreviewCard(
    activity: UserActivity,
    onClick: () -> Unit,
    onSubscribeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    onLikeClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. HEADER: Authors, Time, Badges, Subscribe + (own-activity) overflow menu
            ActivityHeader(
                activity = activity,
                onSubscribeClick = onSubscribeClick,
                onUserClick = onUserClick,
                onDeleteClick = onDeleteClick,
                onEditClick = onEditClick
            )

            // 2. BODY: Rich text rendered inline (parsed via AsyncRichTextRenderer)
            val rawHtml = activity.text.orEmpty()
            if (rawHtml.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                AsyncRichTextRenderer(
                    html = rawHtml,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            // 3. FOOTER: Engagement Stats
            ActivityFooter(
                activity = activity,
                onUserClick = onUserClick,
                onLastReplyClick = onLastReplyClick,
                onCommentClick = onClick,
                onLikeClick = onLikeClick
            )
        }
    }
}

@Composable
private fun ActivityHeader(
    activity: UserActivity,
    onSubscribeClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatars (Handles Single or Double for Messages)
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                url = activity.userAvatarUrl,
                contentDescription = activity.userName,
                size = 36.dp,
                modifier = Modifier.clickable { activity.userName?.let { onUserClick(it) } }
            )

            if (activity.type == ActivityType.MESSAGE && activity.recipientAvatarUrl != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.cd_to),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp)
                )
                UserAvatar(
                    url = activity.recipientAvatarUrl,
                    contentDescription = activity.recipientName,
                    size = 28.dp,
                    modifier = Modifier.clickable { activity.recipientName?.let { onUserClick(it) } }
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Author Names, Time, and Status Badges
        Column(modifier = Modifier.weight(1f)) {
            // Names Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = activity.userName.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { activity.userName?.let { onUserClick(it) } }
                )

                if (activity.type == ActivityType.MESSAGE && !activity.recipientName.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.cd_to),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = activity.recipientName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { onUserClick(activity.recipientName) }
                    )
                }

                val lockedLike = activity.isLocked || activity.isPrivate

                if (activity.isPinned) {
                    StatusBadge(
                        icon = Icons.Default.PushPin,
                        text = stringResource(R.string.pinned),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                if (lockedLike) {
                    StatusBadge(
                        icon = Icons.Default.Lock,
                        text = if (activity.isPrivate) "Private" else "Locked",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Subtitle Row: Time
            val createdAtSeconds = activity.timestamp / 1000L
            Text(
                text = formatRelativeTimeSeconds(createdAtSeconds),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Subscribe — keep default IconButton size (48dp) for accessible touch.
        IconButton(onClick = onSubscribeClick) {
            Icon(
                imageVector = if (activity.isSubscribed) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                contentDescription = if (activity.isSubscribed) "Unsubscribe" else "Subscribe",
                tint = if (activity.isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        // Overflow menu (only when this is the viewer's own activity)
        if (onDeleteClick != null) {
            ActivityOverflowMenu(
                onDeleteClick = onDeleteClick,
                onEditClick = onEditClick
            )
        }
    }
}

@Composable
private fun ActivityFooter(
    activity: UserActivity,
    onUserClick: (String) -> Unit,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    onCommentClick: () -> Unit = {},
    onLikeClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Last Reply Section (Collapses gracefully on small screens)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (activity.replyUserName != null && activity.repliedAt != null) {
                Surface(
                    onClick = {
                        val replyId = activity.lastReplyId
                        if (replyId != null) onLastReplyClick(activity.id, replyId)
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        UserAvatar(
                            url = activity.replyUserAvatarUrl,
                            contentDescription = activity.replyUserName,
                            size = 18.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Last by ${activity.replyUserName} • ${
                                formatRelativeTimeSeconds(
                                    activity.repliedAt
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Engagement Metrics — both with proper touch targets so they tap directly
        // from the card without forcing the user into the activity detail.
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActivityStatPill(
                icon = Icons.Outlined.ChatBubbleOutline,
                value = activity.replyCount,
                onClick = onCommentClick,
                contentDescription = stringResource(R.string.cd_comments)
            )
            ActivityStatPill(
                icon = if (activity.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                value = activity.likeCount,
                tint = if (activity.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onLikeClick,
                contentDescription = if (activity.isLiked) "Unlike" else "Like"
            )
        }
    }
}

// ============================================================================
// --- Reusable Micro-Components (Synced with ForumThreadCard) ---
// ============================================================================

@Composable
private fun StatusBadge(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.9f),
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}

// UserAvatar now lives in presentation/components/UserAvatar.kt (shared).

private fun formatRelativeTimeSeconds(timestampSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestampSeconds
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 604800}w ago"
        diff < 31536000 -> "${diff / 2592000}mo ago"
        else -> "${diff / 31536000}y ago"
    }
}

// ============================================================================
// --- Jetpack Compose Previews ---
// ============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun PreviewActivityCard_StandardText() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ActivityPreviewCard(
                activity = UserActivity(
                    id = 1,
                    type = ActivityType.TEXT,
                    userName = "SakuraFan99",
                    userAvatarUrl = null,
                    text = "Just finished the latest episode of Frieren! The animation was absolutely breathtaking. <br> Can't wait for next week!",
                    timestamp = System.currentTimeMillis() - 3600 * 1000 * 2, // 2 hours ago
                    replyCount = 14,
                    likeCount = 89,
                    isLiked = false,
                    isSubscribed = false,
                    isPinned = false,
                    isLocked = false,
                    isPrivate = false
                ),
                onClick = {},
                onSubscribeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun PreviewActivityCard_Message() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ActivityPreviewCard(
                activity = UserActivity(
                    id = 2,
                    type = ActivityType.MESSAGE,
                    userName = "NarutoUzumaki",
                    userAvatarUrl = null,
                    recipientName = "SasukeUchiha",
                    recipientAvatarUrl = null,
                    text = "Hey! Did you check out the new recommendations I sent you? <p>Let me know what you think!</p>",
                    timestamp = System.currentTimeMillis() - 86400 * 1000 * 3, // 3 days ago
                    replyCount = 2,
                    likeCount = 15,
                    isLiked = true,
                    isSubscribed = true,
                    isPinned = false,
                    isLocked = false,
                    isPrivate = false
                ),
                onClick = {},
                onSubscribeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun PreviewActivityCard_PinnedAndPrivate() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ActivityPreviewCard(
                activity = UserActivity(
                    id = 3,
                    type = ActivityType.TEXT,
                    userName = "AdminUser",
                    userAvatarUrl = null,
                    text = "Reminder: Please keep all seasonal spoilers tagged in the forums. Violators will be given a temporary ban. Thanks!",
                    timestamp = System.currentTimeMillis() - 86400 * 1000 * 10, // 10 days ago
                    replyCount = 105,
                    likeCount = 450,
                    isLiked = false,
                    isSubscribed = true,
                    isPinned = true,
                    isLocked = true,
                    isPrivate = true
                ),
                onClick = {},
                onSubscribeClick = {}
            )
        }
    }
}