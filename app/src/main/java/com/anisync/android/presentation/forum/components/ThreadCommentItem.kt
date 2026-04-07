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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.anisync.android.R
import com.anisync.android.domain.ForumComment
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.components.AsyncRichTextRenderer
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
    depth: Int = 0,
    onUserClick: (String) -> Unit = {}
) {
    val haptic = rememberHapticFeedback()
    val basePadding = 16.dp

    val indentSize = 32.dp
    val avatarRadius = 14.dp
    val curvePadding = 6.dp
    val headerHorizontalPadding = 4.dp

    val contentIndent = 32.dp

    val maxVisualDepth = 5
    val displayDepth = depth.coerceAtMost(maxVisualDepth)

    val branchPath = remember { Path() }

    val density = LocalDensity.current
    val strokeStyle = remember(density) {
        Stroke(
            width = with(density) { 2.dp.toPx() },
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }

    val colorScheme = MaterialTheme.colorScheme
    val lineColors = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary,
            colorScheme.outlineVariant,
            colorScheme.primaryContainer
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (depth == 0) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val cornerRadius = 12.dp.toPx()
                    val avatarCenterY = 26.dp.toPx()

                    val basePx = basePadding.toPx()
                    val indentPx = indentSize.toPx()
                    val avatarRadiusPx = avatarRadius.toPx()
                    val curvePaddingPx = curvePadding.toPx()
                    val headerHorizontalPaddingPx = headerHorizontalPadding.toPx()

                    val getAvatarCenterX = { d: Int ->
                        basePx + (d * indentPx) + headerHorizontalPaddingPx + avatarRadiusPx
                    }

                    for (i in 0 until displayDepth) {
                        val x = getAvatarCenterX(i)
                        drawLine(
                            color = lineColors[i % lineColors.size].copy(alpha = 0.25f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = strokeStyle.width,
                            cap = StrokeCap.Round
                        )
                    }

                    if (depth > 0) {
                        val parentVisualDepth = (depth - 1).coerceAtMost(maxVisualDepth)

                        if (displayDepth > parentVisualDepth) {
                            val parentX = getAvatarCenterX(parentVisualDepth)
                            val childX = getAvatarCenterX(displayDepth)
                            val targetX = childX - avatarRadiusPx - curvePaddingPx

                            branchPath.reset()
                            branchPath.moveTo(parentX, 0f)
                            branchPath.lineTo(parentX, avatarCenterY - cornerRadius)
                            branchPath.quadraticTo(
                                parentX, avatarCenterY,
                                parentX + cornerRadius, avatarCenterY
                            )
                            branchPath.lineTo(targetX, avatarCenterY)

                            drawPath(
                                path = branchPath,
                                color = lineColors[parentVisualDepth % lineColors.size].copy(alpha = 0.6f),
                                style = strokeStyle
                            )
                        } else {
                            val myX = getAvatarCenterX(displayDepth)
                            drawLine(
                                color = lineColors[displayDepth % lineColors.size].copy(alpha = 0.6f),
                                start = Offset(myX, 0f),
                                end = Offset(myX, avatarCenterY - avatarRadiusPx - curvePaddingPx),
                                strokeWidth = strokeStyle.width,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    if (descendantCount > 0 && !isCollapsed) {
                        val myX = getAvatarCenterX(displayDepth)
                        drawLine(
                            color = lineColors[displayDepth % lineColors.size].copy(alpha = 0.25f),
                            start = Offset(myX, avatarCenterY + avatarRadiusPx + 4.dp.toPx()),
                            end = Offset(myX, size.height),
                            strokeWidth = strokeStyle.width,
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
                        if (isCollapsed) MaterialTheme.colorScheme.surfaceContainerHighest
                        else Color.Transparent
                    )
                    .padding(vertical = 6.dp, horizontal = headerHorizontalPadding)
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
                        modifier = Modifier.weight(1f, fill = false),
                        onUserClick = onUserClick
                    )

                    if (threadAuthorId != 0 && comment.authorId == threadAuthorId) {
                        Text(
                            text = "OP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(100)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isCollapsed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(100)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (descendantCount > 0) {
                            Text(
                                text = "+$descendantCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (!isCollapsed) {
                Spacer(Modifier.height(4.dp))

                Column(
                    modifier = Modifier.padding(
                        start = headerHorizontalPadding + contentIndent,
                        end = headerHorizontalPadding,
                        bottom = 4.dp
                    )
                ) {
                    AsyncRichTextRenderer(
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(end = 12.dp, start = 4.dp, top = 4.dp, bottom = 4.dp)
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

                        if (onReplyClick != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onReplyClick(comment.id, comment.authorName)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
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