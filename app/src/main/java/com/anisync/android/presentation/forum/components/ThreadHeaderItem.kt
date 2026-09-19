package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.anisync.android.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.parser.ParsedRichText
import com.anisync.android.presentation.components.AnimatedFavoriteButton
import com.anisync.android.presentation.components.RichTextRenderer
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.forum.components.shared.formatCount
import com.anisync.android.presentation.forum.components.shared.toRelativeTime

/**
 * Renders the top portion of the thread including the title, author, tags, and locked banner.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThreadHeaderTop(
    thread: ForumThread,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {},
    /** The title's measured height, so the bar knows when the title has scrolled under it. */
    onTitleHeight: (Int) -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(
                top = 16.dp,
                start = 20.dp,
                end = 20.dp,
                bottom = 8.dp
            )
        ) {
            // The title reads at full length here rather than inside a collapsing hero, which
            // wrapped it to two lines anyway and then spent 200dp doing it.
            Text(
                text = thread.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.onSizeChanged { onTitleHeight(it.height) }
            )

            Spacer(Modifier.height(16.dp))

            ThreadAuthorBlock(
                name = thread.authorName,
                avatarUrl = thread.authorAvatarUrl,
                createdAt = thread.createdAt,
                onUserClick = onUserClick
            )

            if (thread.categories.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    thread.categories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (thread.isLocked) {
                Spacer(Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.thread_locked_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Author over date, not beside it. A display name can run the width of the column on its own, and
 * sharing a line with the date meant the date decided how much of the name you got to read.
 */
@Composable
private fun ThreadAuthorBlock(
    name: String,
    avatarUrl: String?,
    createdAt: Long,
    onUserClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onUserClick(name) }
    ) {
        UserAvatar(
            url = avatarUrl,
            contentDescription = stringResource(R.string.a11y_user_avatar, name),
            size = 32.dp
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = createdAt.toRelativeTime(LocalResources.current),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Renders the main HTML body content of the thread.
 */
@Composable
fun ThreadBodyItem(
    body: ParsedRichText,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        RichTextRenderer(
            parsedData = body,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

/**
 * Renders the stats block (views, replies, likes) at the bottom of the thread content.
 */
@Composable
fun ThreadHeaderStats(
    thread: ForumThread,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLikeCountClick: (() -> Unit)? = null
) {
    ContentStatsBar(
        replyCount = thread.replyCount,
        viewCount = thread.viewCount,
        likeCount = thread.likeCount,
        isLiked = thread.isLiked,
        onLikeClick = onLikeClick,
        onLikeCountClick = onLikeCountClick,
        modifier = modifier
    )
}

/**
 * Replies, views and the like control, in that order, at the end of the thread body.
 *
 * Only one of the three does anything. Replies and views are readouts, so they carry no container:
 * a filled pill is the app's word for "tappable" everywhere else, and spending it on a number the
 * user cannot act on made two thirds of this row a dead button.
 */
@Composable
fun ContentStatsBar(
    replyCount: Int,
    viewCount: Int?,
    likeCount: Int,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLikeCountClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatReadout(
                icon = Icons.Default.ChatBubbleOutline,
                value = replyCount,
                contentDescription = stringResource(R.string.a11y_reply_count, replyCount)
            )
            if (viewCount != null) {
                StatReadout(
                    icon = Icons.Default.RemoveRedEye,
                    value = viewCount,
                    contentDescription = stringResource(R.string.a11y_view_count, viewCount)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isLiked) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    )
                    .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
            ) {
                AnimatedFavoriteButton(
                    isFavorite = isLiked,
                    onClick = onLikeClick,
                    iconSize = 22.dp
                )
                val countModifier = if (onLikeCountClick != null && likeCount > 0) {
                    Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onLikeCountClick)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                } else {
                    Modifier
                }
                Text(
                    text = likeCount.formatCount(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isLiked) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = countModifier
                )
            }
        }
    }
}

/** A number the thread is reporting, not a control. Icon and value, no container, no click. */
@Composable
private fun StatReadout(
    icon: ImageVector,
    value: Int,
    contentDescription: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value.formatCount(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
