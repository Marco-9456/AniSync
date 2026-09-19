package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.forum.components.shared.formatCount
import com.anisync.android.presentation.util.selectedPaneItem

/**
 * A thread as four bands, each owning the full width of the card.
 *
 * The shipped card put the author above the title, which read as the wrong priority, but the reason
 * behind it was sound: usernames and category lists are both variable-length, so anything sharing a
 * line with them gets squashed. The band order is kept and the metrics are tightened instead — a
 * 28dp avatar rather than 40, no 76dp trailing action strip, and no filled last-reply pill — which
 * brings a typical card from 218dp down to about 140dp.
 *
 * 1. author, optional pinned and locked marks, age; save and overflow pinned at the end
 * 2. the thread title, the thing being scanned for
 * 3. the categories, in a [FlowRow] so two or three wrap instead of truncating
 * 4. who replied last, and the reply and like counts
 */
@Composable
fun ForumThreadCard(
    thread: ForumThread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // When this thread is the one open in the two-pane detail, the card shows the Material 3
    // selection ring (two-pane only; null/false elsewhere).
    selected: Boolean = false,
    isSaved: Boolean = false,
    onSaveClick: (() -> Unit)? = null,
    onOverflowClick: (() -> Unit)? = null,
    onUserClick: (String) -> Unit = {},
    onLastReplyClick: (threadId: Int, commentId: Int) -> Unit = { _, _ -> }
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .selectedPaneItem(selected, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            AuthorBand(
                thread = thread,
                isSaved = isSaved,
                onSaveClick = onSaveClick,
                onOverflowClick = onOverflowClick,
                onUserClick = onUserClick
            )

            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = thread.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (thread.categories.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        CategoryTags(thread)
                    }
                }

                if (thread.mediaCoverUrl != null) {
                    Spacer(Modifier.width(12.dp))
                    AsyncImage(
                        model = thread.mediaCover.url() ?: thread.mediaCoverUrl,
                        contentDescription = thread.mediaTitle
                            ?: stringResource(R.string.cd_media_cover),
                        modifier = Modifier
                            .width(40.dp)
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            ActivityBand(thread = thread, onLastReplyClick = onLastReplyClick)
        }
    }
}

@Composable
private fun AuthorBand(
    thread: ForumThread,
    isSaved: Boolean,
    onSaveClick: (() -> Unit)?,
    onOverflowClick: (() -> Unit)?,
    onUserClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            url = thread.authorAvatarUrl,
            contentDescription = null,
            size = 28.dp
        )
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = thread.authorName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onUserClick(thread.authorName) }
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            )
            if (thread.isSticky) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = stringResource(R.string.pinned),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (thread.isLocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = stringResource(R.string.forum_locked_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = "· " + formatRelativeTimeSeconds(LocalResources.current, thread.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }

        if (onSaveClick != null) {
            CardIconButton(
                icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(
                    if (isSaved) R.string.cd_unsave else R.string.save
                ),
                tint = if (isSaved) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = onSaveClick
            )
        }
        if (onOverflowClick != null) {
            CardIconButton(
                icon = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onOverflowClick
            )
        }
    }
}

/**
 * A 40dp target around a 20dp glyph. The shipped card used a 36dp box with a 4dp gap, so both
 * trailing buttons fell under the 48dp minimum and a miss opened the thread instead.
 */
@Composable
private fun CardIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTags(thread: ForumThread) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        thread.categories.forEach { category ->
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun ActivityBand(
    thread: ForumThread,
    onLastReplyClick: (threadId: Int, commentId: Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = true)
        ) {
            val commentId = thread.replyCommentId
            when {
                thread.replyUserName != null && thread.repliedAt != null -> {
                    // Only offer the tap when there is a comment to land on. The shipped pill lit
                    // up either way and silently did nothing half the time.
                    val rowModifier = if (commentId != null) {
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onLastReplyClick(thread.id, commentId) }
                            .padding(horizontal = 2.dp)
                    } else {
                        Modifier
                    }
                    Row(
                        modifier = rowModifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            url = thread.replyUserAvatarUrl,
                            contentDescription = null,
                            size = 20.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.forum_last_by,
                                thread.replyUserName,
                                formatRelativeTimeSeconds(
                                    LocalResources.current,
                                    thread.repliedAt
                                )
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                thread.isLocked -> MutedLine(stringResource(R.string.forum_closed_for_replies))
                else -> MutedLine(stringResource(R.string.forum_no_replies_yet))
            }
        }

        Spacer(Modifier.width(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                value = thread.replyCount,
                contentDescription = stringResource(
                    R.string.a11y_reply_count,
                    thread.replyCount
                )
            )
            StatItem(
                icon = if (thread.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                value = thread.likeCount,
                contentDescription = stringResource(R.string.a11y_like_count, thread.likeCount),
                tint = if (thread.isLiked) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun MutedLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: Int,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = value.formatCount(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1
        )
    }
}

/**
 * Placeholder in the shape the real card settles into, so the list does not jump on load.
 */
@Composable
fun ForumThreadCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(width = 28.dp, height = 28.dp, shape = CircleShape)
                Spacer(Modifier.width(10.dp))
                SkeletonBox(width = 96.dp, height = 14.dp)
                Spacer(Modifier.weight(1f))
                SkeletonBox(width = 20.dp, height = 20.dp, shape = CircleShape)
            }
            Spacer(Modifier.height(10.dp))
            SkeletonLineBox(fraction = 0.92f)
            Spacer(Modifier.height(6.dp))
            SkeletonLineBox(fraction = 0.55f)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBox(width = 56.dp, height = 22.dp, shape = RoundedCornerShape(8.dp))
                SkeletonBox(width = 88.dp, height = 22.dp, shape = RoundedCornerShape(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(width = 20.dp, height = 20.dp, shape = CircleShape)
                    Spacer(Modifier.width(8.dp))
                    SkeletonBox(width = 104.dp, height = 12.dp)
                }
                SkeletonBox(width = 76.dp, height = 12.dp)
            }
        }
    }
}

/** A full-width skeleton line, sized by fraction rather than by an infinite width. */
@Composable
private fun SkeletonLineBox(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(18.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

@Composable
private fun SkeletonBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(percent = 50)
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}
