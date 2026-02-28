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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ForumComment
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.forum.components.shared.AuthorRow
import com.anisync.android.presentation.util.rememberHapticFeedback

/**
 * Renders a forum comment with visual nesting via colored left-border bars.
 * Child comments display "Replying to @parent" labels and increasing indentation.
 *
 * @param comment The comment to render
 * @param onLikeClick Called when the like button is tapped, with the comment's ID and current liked state
 * @param onReplyClick Called when the reply button is tapped, with the comment's ID and author name (null hides button)
 * @param parentAuthorName Name of the parent comment's author (null for top-level)
 * @param depth Nesting depth — 0 for top-level, capped at 3
 */
@Composable
fun ThreadCommentItem(
    comment: ForumComment,
    onLikeClick: (commentId: Int, currentLiked: Boolean) -> Unit,
    onReplyClick: ((commentId: Int, authorName: String) -> Unit)?,
    modifier: Modifier = Modifier,
    parentAuthorName: String? = null,
    depth: Int = 0
) {
    val haptic = rememberHapticFeedback()

    Column(modifier = modifier.fillMaxWidth()) {
        // Divider between top-level comments
        if (depth == 0) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .then(
                    if (depth == 0) {
                        Modifier.background(
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            // Colored nesting bars — one per depth level, styled with rounded corners
            repeat(depth) { level ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(nestingBarColor(level + 1))
                )
                Spacer(Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (depth == 0) 16.dp else 4.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    )
            ) {
                // "Replying to @parent" label for nested comments
                if (depth > 0 && parentAuthorName != null) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.forum_replying_to, parentAuthorName),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                AuthorRow(
                    name = comment.authorName,
                    avatarUrl = comment.authorAvatarUrl,
                    timestampSeconds = comment.createdAt,
                    avatarSize = 24.dp
                )

                Spacer(Modifier.height(8.dp))

                // Render body as AniList-Flavored Markdown
                HtmlText(
                    html = comment.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.1f
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                // Action buttons (animated like + reply)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedFavoriteButton(
                        isFavorite = comment.isLiked,
                        onClick = { onLikeClick(comment.id, comment.isLiked) },
                        iconSize = 18.dp
                    )
                    if (comment.likeCount > 0) {
                        Text(
                            text = comment.likeCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                    } else {
                        Spacer(Modifier.width(4.dp))
                    }

                    if (onReplyClick != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onReplyClick(comment.id, comment.authorName)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = "Reply",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
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
    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    2 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
    3 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
    else -> MaterialTheme.colorScheme.outlineVariant
}
