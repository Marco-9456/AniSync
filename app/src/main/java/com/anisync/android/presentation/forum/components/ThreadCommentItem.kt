package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ForumComment
import com.anisync.android.presentation.forum.components.shared.AuthorRow

/**
 * Renders a forum comment with visual nesting via colored left-border bars.
 * Child comments display "Replying to @parent" labels and increasing indentation.
 *
 * @param comment The comment to render
 * @param onLikeClick Called when the like button is tapped
 * @param onReplyClick Called when the reply button is tapped (null hides button)
 * @param parentAuthorName Name of the parent comment's author (null for top-level)
 * @param depth Nesting depth — 0 for top-level, capped at 3
 */
@Composable
fun ThreadCommentItem(
    comment: ForumComment,
    onLikeClick: () -> Unit,
    onReplyClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    parentAuthorName: String? = null,
    depth: Int = 0
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Divider between top-level comments
        if (depth == 0) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Colored nesting bars — one per depth level
            repeat(depth) { level ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(nestingBarColor(level + 1))
                )
                Spacer(Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (depth == 0) 16.dp else 4.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = 4.dp
                    )
            ) {
                // "Replying to @parent" label for nested comments
                if (depth > 0 && parentAuthorName != null) {
                    Text(
                        text = stringResource(R.string.forum_replying_to, parentAuthorName),
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                AuthorRow(
                    name = comment.authorName,
                    avatarUrl = comment.authorAvatarUrl,
                    timestampSeconds = comment.createdAt
                )

                Spacer(Modifier.height(6.dp))

                // Render body as AniList-Flavored Markdown
                HtmlText(
                    html = comment.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                // Action buttons (like + reply)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (comment.isLiked) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = comment.likeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    if (onReplyClick != null) {
                        IconButton(onClick = onReplyClick) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = "Reply",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Render child comments recursively (capped at depth 3)
        if (depth < 3) {
            comment.childComments.forEach { child ->
                ThreadCommentItem(
                    comment = child,
                    onLikeClick = onLikeClick,
                    onReplyClick = onReplyClick,
                    parentAuthorName = comment.authorName,
                    depth = depth + 1
                )
            }
        }
    }
}

/**
 * Returns a visually distinct color for each nesting depth.
 * Multiple bars stack to show the full thread ancestry.
 */
@Composable
private fun nestingBarColor(depth: Int): Color = when (depth) {
    1 -> MaterialTheme.colorScheme.primary
    2 -> MaterialTheme.colorScheme.tertiary
    3 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.outlineVariant
}
