package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.ForumThread
import com.anisync.android.presentation.forum.components.shared.AuthorRow
import com.anisync.android.presentation.forum.components.shared.StatBadge

@Composable
fun ForumThreadCard(
    thread: ForumThread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title row with optional locked indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (thread.isLocked) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))

            // Author
            AuthorRow(
                name = thread.authorName,
                avatarUrl = thread.authorAvatarUrl,
                timestampSeconds = thread.updatedAt
            )

            Spacer(Modifier.height(8.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBadge(
                    icon = Icons.Default.ChatBubbleOutline,
                    value = thread.replyCount,
                    contentDescription = "${thread.replyCount} replies"
                )
                StatBadge(
                    icon = Icons.Default.RemoveRedEye,
                    value = thread.viewCount,
                    contentDescription = "${thread.viewCount} views"
                )
                StatBadge(
                    icon = Icons.Outlined.FavoriteBorder,
                    value = thread.likeCount,
                    contentDescription = "${thread.likeCount} likes",
                    tint = if (thread.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                // Category chips (first 2 max)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    thread.categories.take(2).forEach { category ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForumThreadCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SkeletonLine(fraction = 0.85f, height = 16.dp)
            Spacer(Modifier.height(4.dp))
            SkeletonLine(fraction = 0.5f, height = 16.dp)
            Spacer(Modifier.height(12.dp))
            SkeletonLine(fraction = 0.4f, height = 12.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonLine(fraction = 0.6f, height = 12.dp)
        }
    }
}
