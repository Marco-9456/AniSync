package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
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
import com.anisync.android.domain.UserActivity
import com.anisync.android.domain.url
import com.anisync.android.presentation.profile.util.formatProfileRelativeTime

@Composable
fun RecentUpdateCard(
    activity: UserActivity,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {},
    onActivityClick: (Int) -> Unit = {},
    onMediaClick: (Int) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    onLikeClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    Card(
        onClick = { onActivityClick(activity.id) },
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // LEFT: Media Cover (tap → MediaDetails when mediaId available)
            val coverModifier = Modifier
                .width(88.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(12.dp))
                .let { base ->
                    val mediaId = activity.mediaId
                    if (mediaId != null) base.clickable { onMediaClick(mediaId) } else base
                }
                .background(MaterialTheme.colorScheme.surfaceVariant)
            if (activity.mediaCoverUrl != null) {
                AsyncImage(
                    model = activity.mediaCover.url() ?: activity.mediaCoverUrl,
                    contentDescription = activity.mediaTitle,
                    modifier = coverModifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = coverModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activity.mediaTitle.take(2).ifBlank { "??" }.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // RIGHT: Author header + status text + footer
            Column(modifier = Modifier.weight(1f)) {
                UpdateHeader(
                    activity = activity,
                    onUserClick = onUserClick,
                    onDeleteClick = onDeleteClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                UpdateStatusText(activity = activity)

                Spacer(modifier = Modifier.height(12.dp))

                UpdateFooter(
                    activity = activity,
                    onUserClick = onUserClick,
                    onLastReplyClick = onLastReplyClick,
                    onLikeClick = onLikeClick,
                    onCommentClick = { onActivityClick(activity.id) }
                )
            }
        }
    }
}

@Composable
private fun UpdateHeader(
    activity: UserActivity,
    onUserClick: (String) -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            url = activity.userAvatarUrl,
            contentDescription = activity.userName,
            size = 34.dp,
            modifier = Modifier
                .clip(MaterialShapes.Clover8Leaf.toShape())
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialShapes.Clover8Leaf.toShape()
                )
                .clickable { activity.userName?.let { onUserClick(it) } }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                if (activity.mediaScore != null && activity.mediaScore > 0) {
                    StatusBadge(
                        icon = Icons.Default.Star,
                        text = activity.mediaScore.toString(),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Text(
                text = formatProfileRelativeTime(activity.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onDeleteClick != null) {
            ActivityOverflowMenu(onDeleteClick = onDeleteClick)
        }
    }
}

@Composable
private fun UpdateStatusText(activity: UserActivity) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val styledText = remember(
        activity.status,
        activity.progress,
        activity.mediaTitle,
        primaryColor
    ) {
        val statusText = (activity.status ?: "Updated")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val progressText = activity.progress.orEmpty()

        buildAnnotatedString {
            append("$statusText ")
            if (progressText.isNotEmpty()) {
                withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                    append(progressText)
                }
                append(" of ")
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(activity.mediaTitle)
            }
        }
    }

    Text(
        text = styledText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun UpdateFooter(
    activity: UserActivity,
    onUserClick: (String) -> Unit,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    onLikeClick: (() -> Unit)? = null,
    onCommentClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Last reply pill
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
                            size = 16.dp,
                            modifier = Modifier
                                .clip(MaterialShapes.Clover8Leaf.toShape())
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialShapes.Clover8Leaf.toShape()
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Last by ${activity.replyUserName} • ${
                                formatProfileRelativeTime(activity.repliedAt!! * 1000L)
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

        // Engagement metrics — pill-shaped, 22dp icons, 48dp+ touch target.
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
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
// --- Reusable Micro-Components (Synced with ForumThreadCard / ActivityPreviewCard) ---
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
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.9f
            ),
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}

@Composable
private fun UserAvatar(
    url: String?,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.7f)
            )
        }
    }
}
