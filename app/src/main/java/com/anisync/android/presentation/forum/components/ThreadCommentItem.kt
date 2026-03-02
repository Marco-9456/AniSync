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
import androidx.compose.material.icons.automirrored.filled.Reply
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.anisync.android.R
import com.anisync.android.domain.ForumComment
import com.anisync.android.presentation.components.AniListHtmlRenderer
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.forum.components.shared.AuthorRow
import com.anisync.android.presentation.util.rememberHapticFeedback

/**
 * Modern Reddit-style comment item supporting curved tree branches and strict top-level boundaries.
 * Redesigned for Material Design 3 Expressive (Softer curves, prominent pills, thicker paths).
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

    // Geometrically matched constants for expressive, softer tree branches
    val indentSize = 26.dp // Wider indent for MD3 breathing room
    val avatarRadius = 14.dp
    val curvePadding = 6.dp

    val maxVisualDepth = 5
    val displayDepth = depth.coerceAtMost(maxVisualDepth)

    // Vibrant, distinct tree level colors to make nesting visually delightful
    val lineColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.outline
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        // Expressive Top-Level Separator: Thicker, softer spacing instead of harsh lines
        if (depth == 0) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Color.Transparent)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // MD3 Expressive: Thicker, friendlier lines with rounded caps
                    val strokeWidth = 2.5.dp.toPx()
                    val cornerRadius = 16.dp.toPx() // Much softer bezier curve
                    val avatarCenterY = 24.dp.toPx() // Adjusted for new paddings

                    val basePx = basePadding.toPx()
                    val indentPx = indentSize.toPx()
                    val avatarRadiusPx = avatarRadius.toPx()
                    val curvePaddingPx = curvePadding.toPx()

                    val strokeStyle = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )

                    // 1. Draw continuous vertical timeline lines for all ancestor depths
                    for (i in 0 until displayDepth) {
                        val x = basePx + (i * indentPx) + avatarRadiusPx
                        drawLine(
                            color = lineColors[i % lineColors.size].copy(alpha = 0.2f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // 2. Draw the branch curving into THIS comment's avatar
                    if (depth > 0) {
                        val parentVisualDepth = (depth - 1).coerceAtMost(maxVisualDepth)

                        if (displayDepth > parentVisualDepth) {
                            val parentX = basePx + (parentVisualDepth * indentPx) + avatarRadiusPx
                            val childX = basePx + (displayDepth * indentPx) + avatarRadiusPx

                            val path = Path().apply {
                                moveTo(parentX, 0f)
                                lineTo(parentX, avatarCenterY - cornerRadius)
                                // Soft, expressive organic curve
                                quadraticTo(
                                    parentX,
                                    avatarCenterY,
                                    parentX + cornerRadius,
                                    avatarCenterY
                                )
                                lineTo(childX - avatarRadiusPx - curvePaddingPx, avatarCenterY)
                            }

                            drawPath(
                                path = path,
                                color = lineColors[parentVisualDepth % lineColors.size].copy(alpha = 0.9f),
                                style = strokeStyle
                            )
                        } else {
                            // Max visual depth trunk
                            val myX = basePx + (displayDepth * indentPx) + avatarRadiusPx
                            drawLine(
                                color = lineColors[displayDepth % lineColors.size].copy(alpha = 0.3f),
                                start = Offset(myX, 0f),
                                end = Offset(myX, avatarCenterY - avatarRadiusPx - curvePaddingPx),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 3. Draw downward line sprouting from THIS comment if applicable
                    if (descendantCount > 0 && !isCollapsed) {
                        val myX = basePx + (displayDepth * indentPx) + avatarRadiusPx
                        drawLine(
                            color = lineColors[displayDepth % lineColors.size].copy(alpha = 0.3f),
                            start = Offset(myX, avatarCenterY + avatarRadiusPx + 6.dp.toPx()),
                            end = Offset(myX, size.height),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
                .padding(
                    start = basePadding + (displayDepth * indentSize),
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
        ) {
            // Interactive header area
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleCollapse()
                    }
                    .background(
                        if (isCollapsed && depth > 0) MaterialTheme.colorScheme.surfaceContainer
                        else Color.Transparent
                    )
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AuthorRow(
                        name = comment.authorName,
                        avatarUrl = comment.authorAvatarUrl,
                        timestampSeconds = comment.createdAt,
                        avatarSize = avatarRadius * 2,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Expressive OP Badge: Pill shaped, prominent contrast
                    if (threadAuthorId != 0 && comment.authorId == threadAuthorId) {
                        Text(
                            text = "OP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(100) // Pill shape
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Expressive Collapsed Indicator
                if (isCollapsed && descendantCount > 0) {
                    Text(
                        text = "+$descendantCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(100)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (!isCollapsed) {
                Spacer(Modifier.height(6.dp))

                Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)) {
                    AniListHtmlRenderer(
                        html = comment.body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.35f
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Likes Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .padding(end = 8.dp)
                        ) {
                            AnimatedFavoriteButton(
                                isFavorite = comment.isLiked,
                                onClick = { onLikeClick(comment.id, comment.isLiked) },
                                iconSize = 20.dp
                            )
                            if (comment.likeCount > 0) {
                                Text(
                                    text = comment.likeCount.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (comment.isLiked) FontWeight.Black else FontWeight.Bold
                                )
                            }
                        }

                        // Expressive Reply Button
                        if (onReplyClick != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onReplyClick(comment.id, comment.authorName)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "Reply",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.forum_reply),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
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