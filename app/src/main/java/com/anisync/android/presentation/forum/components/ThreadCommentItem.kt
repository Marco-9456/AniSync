package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
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
 * Child comments display a refined "↳ Replying to..." prefix and indentation.
 *
 * @param comment The comment to render
 * @param onLikeClick Called when the like button is tapped, with the comment's ID and current liked state
 * @param onReplyClick Called when the reply button is tapped, with the comment's ID and author name (null hides button)
 * @param threadAuthorId ID of the main thread author to display OP badges
 * @param parentAuthorName Name of the parent comment's author (null for top-level)
 * @param depth Nesting depth — 0 for top-level, capped at 3
 */
@Composable
fun ThreadCommentItem(
    comment: ForumComment,
    onLikeClick: (commentId: Int, currentLiked: Boolean) -> Unit,
    onReplyClick: ((commentId: Int, authorName: String) -> Unit)?,
    modifier: Modifier = Modifier,
    threadAuthorId: Int = 0,
    parentAuthorName: String? = null,
    depth: Int = 0
) {
    val haptic = rememberHapticFeedback()

    Column(modifier = modifier.fillMaxWidth()) {
        // Subtle divider separating top-level threads
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
                .background(
                    if (depth == 0) MaterialTheme.colorScheme.surfaceContainerLowest
                    else Color.Transparent
                )
        ) {
            // Refined Thread Context Lines (Pills)
            if (depth > 0) {
                Spacer(Modifier.width(12.dp)) // Initial indent offset
                for (level in 1..depth.coerceAtMost(3)) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp) // Creates visual separation between connected bars
                            .clip(CircleShape)        // Rounded caps for a modern feel
                            .background(nestingBarColor(level))
                    )
                    Spacer(Modifier.width(10.dp))
                }
            }

            // Main Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (depth == 0) 16.dp else 4.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 12.dp
                    )
            ) {
                // Inline "Replying to" context
                if (depth > 0 && parentAuthorName != null) {
                    Text(
                        text = "↳ ${stringResource(R.string.forum_replying_to, parentAuthorName)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Header Row: Author Info & OP Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        AuthorRow(
                            name = comment.authorName,
                            avatarUrl = comment.authorAvatarUrl,
                            timestampSeconds = comment.createdAt,
                            avatarSize = 28.dp, // Slightly prominent avatar
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // OP Badge styled softly with M3 Container colors
                        if (threadAuthorId != 0 && comment.authorId == threadAuthorId) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.forum_op_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Markdown / HTML Body with enhanced line height
                HtmlText(
                    html = comment.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Like Button Group
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnimatedFavoriteButton(
                            isFavorite = comment.isLiked,
                            onClick = { onLikeClick(comment.id, comment.isLiked) },
                            iconSize = 20.dp
                        )
                        if (comment.likeCount > 0) {
                            Text(
                                text = comment.likeCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (comment.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (comment.isLiked) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Reply Button
                    if (onReplyClick != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onReplyClick(comment.id, comment.authorName)
                            },
                            modifier = Modifier.size(28.dp) // Compact click target
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = "Reply",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recursive Child Comments Rendering
        if (depth < 3) {
            comment.childComments.forEach { child ->
                ThreadCommentItem(
                    comment = child,
                    onLikeClick = onLikeClick,
                    onReplyClick = onReplyClick,
                    threadAuthorId = threadAuthorId,
                    parentAuthorName = comment.authorName,
                    depth = depth + 1
                )
            }
        }
    }
}

/**
 * Provides a thematic cohesive color for depth indicator bars using alpha channels.
 */
@Composable
private fun nestingBarColor(depth: Int): Color {
    val colors = MaterialTheme.colorScheme
    return when (depth) {
        1 -> colors.primary.copy(alpha = 0.6f)
        2 -> colors.tertiary.copy(alpha = 0.6f)
        3 -> colors.secondary.copy(alpha = 0.6f)
        else -> colors.outlineVariant.copy(alpha = 0.6f)
    }
}