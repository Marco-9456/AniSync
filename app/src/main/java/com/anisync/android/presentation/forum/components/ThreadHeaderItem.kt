package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.ForumThread
import com.anisync.android.presentation.forum.components.shared.AuthorRow
import com.anisync.android.presentation.forum.components.shared.StatBadge

@Composable
fun ThreadHeaderItem(
    thread: ForumThread,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        // Title
        Text(
            text = thread.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        // Author and timestamp
        AuthorRow(
            name = thread.authorName,
            avatarUrl = thread.authorAvatarUrl,
            timestampSeconds = thread.createdAt
        )

        // Category chips
        if (thread.categories.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                thread.categories.forEach { cat ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Locked banner
        if (thread.isLocked) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "This thread is locked",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Body rendered from HTML
        thread.body?.let { body ->
            HtmlText(
                html = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
        }

        HorizontalDivider()

        Spacer(Modifier.height(8.dp))

        // Stats row with like toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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

            Spacer(Modifier.weight(1f))

            IconToggleButton(
                checked = thread.isLiked,
                onCheckedChange = { onLikeClick() }
            ) {
                Icon(
                    imageVector = if (thread.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (thread.isLiked) "Unlike" else "Like",
                    tint = if (thread.isLiked) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = thread.likeCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
