package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.anisync.android.R
import com.anisync.android.domain.ForumComment
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.forum.components.shared.AuthorRow
import com.anisync.android.presentation.util.rememberHapticFeedback

/**
 * Modern Reddit-style comment item supporting curved tree branches and strict top-level boundaries.
 */
@Composable
fun ThreadCommentItem(
    comment: ForumComment,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    descendantCount: Int,
    onLikeClick: (commentId: Int, currentLiked: Boolean) -> Unit,
    onReplyClick: ((commentId: Int, authorName: String) -> Unit)?,
    modifier: Modifier = Modifier,
    threadAuthorId: Int = 0,
    depth: Int = 0
) {
    val haptic = rememberHapticFeedback()
    val basePadding = 16.dp

    // Geometrically matched constants to guarantee valid Canvas Paths and no measurement crashes
    val indentSize = 22.dp
    val avatarRadius = 12.dp
    val curvePadding = 4.dp

    // Cap visual nesting to prevent layout constraints exceeding screen bounds (like Reddit)
    val maxVisualDepth = 5
    val displayDepth = depth.coerceAtMost(maxVisualDepth)

    // Distinct tree level colors
    val lineColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )

    Column(modifier = modifier.fillMaxWidth()) {

        // Strict boundary separating independent top-level comment threads
        if (depth == 0) {
            HorizontalDivider(
                thickness = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (depth == 0) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent)
                // drawBehind powers the Reddit-style continuous commit graph / tree lines
                .drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
                    val cornerRadius = 6.dp.toPx()
                    // Target center of the avatar based on paddings (6dp top + 4dp inner top padding + 12dp radius)
                    val avatarCenterY = 22.dp.toPx()

                    val basePx = basePadding.toPx()
                    val indentPx = indentSize.toPx()
                    val avatarRadiusPx = avatarRadius.toPx()
                    val curvePaddingPx = curvePadding.toPx()

                    // 1. Draw continuous vertical timeline lines for all ancestor depths
                    for (i in 0 until displayDepth) {
                        val x = basePx + (i * indentPx) + avatarRadiusPx
                        drawLine(
                            color = lineColors[i % lineColors.size].copy(alpha = 0.25f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = strokeWidth
                        )
                    }

                    // 2. Draw the L-shaped branch curving into THIS comment's avatar (or vertical trunk if maxed)
                    if (depth > 0) {
                        val parentVisualDepth = (depth - 1).coerceAtMost(maxVisualDepth)

                        if (displayDepth > parentVisualDepth) {
                            val parentX = basePx + (parentVisualDepth * indentPx) + avatarRadiusPx
                            val childX = basePx + (displayDepth * indentPx) + avatarRadiusPx

                            val path = Path().apply {
                                moveTo(parentX, 0f)
                                lineTo(parentX, avatarCenterY - cornerRadius)
                                // Elegant curve mimicking modern mobile thread graphs
                                quadraticBezierTo(
                                    parentX,
                                    avatarCenterY,
                                    parentX + cornerRadius,
                                    avatarCenterY
                                )
                                // Connect perfectly to the edge of the avatar
                                lineTo(childX - avatarRadiusPx - curvePaddingPx, avatarCenterY)
                            }

                            drawPath(
                                path = path,
                                color = lineColors[parentVisualDepth % lineColors.size].copy(alpha = 0.8f), // Highlighted color
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            // Max visual depth reached: draw vertical trunk dropping from parent
                            val myX = basePx + (displayDepth * indentPx) + avatarRadiusPx
                            drawLine(
                                color = lineColors[displayDepth % lineColors.size].copy(alpha = 0.25f),
                                start = Offset(myX, 0f),
                                end = Offset(myX, avatarCenterY - avatarRadiusPx - curvePaddingPx),
                                strokeWidth = strokeWidth
                            )
                        }
                    }

                    // 3. Draw a downward line sprouting from THIS comment if it has children and isn't collapsed
                    if (descendantCount > 0 && !isCollapsed) {
                        val myX = basePx + (displayDepth * indentPx) + avatarRadiusPx
                        drawLine(
                            color = lineColors[displayDepth % lineColors.size].copy(alpha = 0.25f),
                            start = Offset(myX, avatarCenterY + avatarRadiusPx + 4.dp.toPx()),
                            end = Offset(myX, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                }
                .padding(
                    start = basePadding + (displayDepth * indentSize),
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 6.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleCollapse()
                    }
                    .background(if (isCollapsed && depth > 0) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent)
                    .padding(vertical = 4.dp, horizontal = 4.dp)
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
                        avatarSize = avatarRadius * 2,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (threadAuthorId != 0 && comment.authorId == threadAuthorId) {
                        Text(
                            text = "OP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isCollapsed && descendantCount > 0) {
                    Text(
                        text = "+$descendantCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!isCollapsed) {
                Spacer(Modifier.height(6.dp))

                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    HtmlText(
                        html = comment.body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AnimatedFavoriteButton(
                                isFavorite = comment.isLiked,
                                onClick = { onLikeClick(comment.id, comment.isLiked) },
                                iconSize = 18.dp
                            )
                            if (comment.likeCount > 0) {
                                Text(
                                    text = comment.likeCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (comment.isLiked) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        if (onReplyClick != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onReplyClick(comment.id, comment.authorName)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Reply,
                                    contentDescription = "Reply",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.forum_reply),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}