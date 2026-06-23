package com.anisync.android.presentation.profile.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.ActivityMediaType
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserActivity
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.ReadMoreToggle
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.util.selectedPaneItem
import com.anisync.android.presentation.util.shareActivity

/**
 * Single card for every activity type — status ([ActivityType.TEXT]), message
 * ([ActivityType.MESSAGE]) and list ([ActivityType.MEDIA_LIST]). They share the
 * same chrome (author header + subscribe/share/overflow, engagement footer); only
 * the body differs, so the type switch lives in [ActivityCardBody]:
 * - MEDIA_LIST → media cover + "Watched episode … of <title>" status line
 * - TEXT / MESSAGE → inline rich-text body
 */
@Composable
fun ActivityCard(
    activity: UserActivity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {},
    onMediaClick: (Int) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    onSubscribeClick: (() -> Unit)? = null,
    onLikeClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    // When this activity is the one open in the two-pane detail (Feed), the card shows the Material 3
    // selection ring (two-pane only; null/false in the profile feed and on compact).
    selected: Boolean = false,
    /**
     * When set, the body is capped to roughly this many lines of text and its bottom edge fades
     * out, turning the card into a compact teaser (used by the profile Overview). The whole card
     * stays clickable, so a tap opens the full activity. Null renders the body in full.
     */
    maxBodyLines: Int? = null
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .selectedPaneItem(selected, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        // Smoothly animate any height change — an embedded image/video finishing load, or an
        // expand/collapse — so the card grows/shrinks gracefully instead of snapping, which is what
        // made the lazy list lurch between posts mid-scroll. Collapsed cards are already a fixed
        // height, so this only animates the residual (sub-cap images, the Read more toggle).
        Column(modifier = Modifier
            .animateContentSize()
            .padding(16.dp)
        ) {
            ActivityCardHeader(
                activity = activity,
                onSubscribeClick = onSubscribeClick,
                onUserClick = onUserClick,
                onDeleteClick = onDeleteClick,
                onEditClick = onEditClick
            )

            val isTextual = activity.type == ActivityType.TEXT || activity.type == ActivityType.MESSAGE
            when {
                // Overview teaser: a fixed line cap that opens the full activity on tap (non-interactive).
                maxBodyLines != null -> {
                    ClampedActivityBody(maxLines = maxBodyLines, fadeColor = containerColor) {
                        ActivityCardBody(
                            activity = activity,
                            onMediaClick = onMediaClick
                        )
                    }
                }
                // Full feed: cap long status/message bodies to a readable height with inline expand.
                // Bounding the height also keeps the card from re-growing as embedded images/videos
                // load, which is what made the list snap between posts mid-scroll.
                isTextual -> {
                    CollapsibleActivityBody(
                        collapsedMaxHeight = ACTIVITY_BODY_COLLAPSED_MAX,
                        fadeColor = containerColor,
                        // While collapsed, the visible preview is a teaser: a tap anywhere in it
                        // (including on a peeking image or embedded link) opens the activity rather
                        // than firing the image viewer / following the link.
                        onBodyClick = onClick
                    ) {
                        ActivityCardBody(
                            activity = activity,
                            onMediaClick = onMediaClick
                        )
                    }
                }
                // List activity: already compact (cover + status line), render in full.
                else -> {
                    ActivityCardBody(
                        activity = activity,
                        onMediaClick = onMediaClick
                    )
                }
            }

            ActivityCardFooter(
                activity = activity,
                onLastReplyClick = onLastReplyClick,
                onCommentClick = onClick,
                onLikeClick = onLikeClick
            )
        }
    }
}

/**
 * Caps [content] (an activity body) to roughly [maxLines] lines of body text and fades its bottom
 * edge out once the content overflows — turning a long status post into a compact teaser without
 * touching the block-based rich-text renderer. Sizing is derived from the body line height so it
 * tracks the user's font scale. Overflow is detected in the [layout] block (it sees the unclamped
 * height) and the fade is drawn only when actually clipped.
 */
@Composable
private fun ClampedActivityBody(
    maxLines: Int,
    fadeColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
    // Body text renders with 1.25× line spacing (see ActivityCardBody); match it so the clamp maps
    // to ~maxLines of rendered text rather than tight metric lines.
    val maxHeightPx = with(density) {
        val lineDp = if (lineHeight.isSp) lineHeight.toDp() else 20.dp
        ((lineDp * 1.25f).toPx() * maxLines).toInt()
    }
    val fadeHeightPx = with(density) { 28.dp.toPx() }
    var clipped by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .drawWithContent {
                drawContent()
                if (clipped) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, fadeColor),
                            startY = size.height - fadeHeightPx,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, size.height - fadeHeightPx),
                        size = Size(size.width, fadeHeightPx)
                    )
                }
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val overflow = placeable.height > maxHeightPx
                clipped = overflow
                val targetHeight = if (overflow) maxHeightPx else placeable.height
                layout(placeable.width, targetHeight) {
                    placeable.place(0, 0)
                }
            }
    ) {
        content()
    }
}

/**
 * Collapsed height for a status/message body in the full feed. ~15 lines of body text, or a peek of
 * an embedded image — enough to judge the post without letting one long post dominate the scroll.
 * Bodies taller than this collapse behind a "Read more" toggle; shorter ones render in full.
 */
private val ACTIVITY_BODY_COLLAPSED_MAX = 340.dp

/**
 * Caps a status/message body to [collapsedMaxHeight] with an inline "Read more"/"Show less" toggle,
 * fading the clipped edge. Two wins: long posts no longer dominate the feed, and — because the
 * collapsed height is fixed — the card stops re-growing as embedded images/videos finish loading,
 * which is what made the lazy list snap from one post to another mid-scroll. Short bodies that fit
 * under the cap render in full with no toggle. Expand state is keyed by the lazy item, so it
 * survives scrolling the card off and back on.
 */
@Composable
private fun CollapsibleActivityBody(
    collapsedMaxHeight: Dp,
    fadeColor: Color,
    onBodyClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val maxHeightPx = with(density) { collapsedMaxHeight.toPx() }
    val fadeHeightPx = with(density) { 36.dp.toPx() }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .drawWithContent {
                    drawContent()
                    if (overflow && !expanded) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, fadeColor),
                                startY = size.height - fadeHeightPx,
                                endY = size.height
                            ),
                            topLeft = Offset(0f, size.height - fadeHeightPx),
                            size = Size(size.width, fadeHeightPx)
                        )
                    }
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val isOver = placeable.height > maxHeightPx
                    if (isOver != overflow) overflow = isOver
                    val targetHeight =
                        if (isOver && !expanded) maxHeightPx.toInt() else placeable.height
                    layout(placeable.width, targetHeight) {
                        placeable.place(0, 0)
                    }
                }
        ) {
            content()
            // Collapsed teaser: a transparent layer over the visible preview swallows taps on
            // peeking images / links and routes them to the card (open the activity). Drawn after
            // the content so it sits on top; clipped to the collapsed bounds, so hidden content
            // below the cut is untouched. Removed once expanded, restoring normal image/link taps.
            if (overflow && !expanded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onBodyClick() }
                )
            }
        }

        if (overflow) {
            ReadMoreToggle(
                expanded = expanded,
                onToggle = { expanded = !expanded },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ActivityCardHeader(
    activity: UserActivity,
    onSubscribeClick: (() -> Unit)?,
    onUserClick: (String) -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Sender Avatar Only (Simplified for all cards, including messages)
        UserAvatar(
            url = activity.userAvatarUrl,
            contentDescription = activity.userName,
            size = 40.dp,
            modifier = Modifier.clickable { activity.userName?.let { onUserClick(it) } }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = activity.userName.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { activity.userName?.let { onUserClick(it) } }
                )

                // Icon-only status markers
                if (activity.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = stringResource(R.string.pinned),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (activity.isLocked || activity.isPrivate) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = if (activity.isPrivate) "Private" else "Locked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = formatRelativeTimeSeconds(activity.timestamp / 1000L),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Action Buttons Grouped (Notifications, Share, MoreVert)
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onSubscribeClick != null) {
                IconButton(onClick = onSubscribeClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (activity.isSubscribed) Icons.Filled.Notifications
                        else Icons.Outlined.NotificationsNone,
                        contentDescription = if (activity.isSubscribed) "Unsubscribe" else "Subscribe",
                        tint = if (activity.isSubscribed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            val context = LocalContext.current
            IconButton(
                onClick = { shareActivity(context, activity.id) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (onDeleteClick != null) {
                ActivityOverflowMenu(
                    onDeleteClick = onDeleteClick,
                    onEditClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun ActivityCardBody(
    activity: UserActivity,
    onMediaClick: (Int) -> Unit
) {
    when (activity.type) {
        ActivityType.MEDIA_LIST -> {
            Spacer(Modifier.height(12.dp))
            // Tint the body with the media's cover accent color; fall back to the theme
            // container when AniList returns no color or an unparseable value.
            val coverAccent = remember(activity.mediaCoverColor) {
                activity.mediaCoverColor?.parseHexColor()
            } ?: MaterialTheme.colorScheme.primaryContainer
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = coverAccent.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = coverAccent.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(10.dp)
            ) {
                val coverModifier = Modifier
                    .width(64.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .let { base ->
                        val mediaId = activity.mediaId
                        if (mediaId != null) base.clickable { onMediaClick(mediaId) } else base
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant)

                if (activity.mediaCoverUrl != null) {
                    AsyncImage(
                        model = activity.mediaCover.url() ?: activity.mediaCoverUrl,
                        contentDescription = activity.mediaTitle,
                        modifier = coverModifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = coverModifier, contentAlignment = Alignment.Center) {
                        Text(
                            text = activity.mediaTitle.take(2).ifBlank { "??" }.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    ActivityListStatusText(activity = activity)
                    activity.mediaType?.let { type ->
                        Spacer(Modifier.height(6.dp))
                        MediaTypeLabel(type)
                    }
                }
            }
        }

        else -> {
            val rawHtml = activity.text.orEmpty()
            if (rawHtml.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                AsyncRichTextRenderer(
                    html = rawHtml,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ActivityListStatusText(activity: UserActivity, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val styledText =
        remember(activity.status, activity.progress, activity.mediaTitle, primaryColor) {
            val statusText = (activity.status ?: "Updated")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val progressText = activity.progress.orEmpty()
            buildAnnotatedString {
                append("$statusText ")
                if (progressText.isNotEmpty()) {
                    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                        append(progressText)
                    }
                    append(" of ")
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(activity.mediaTitle)
                }
            }
        }
    Text(
        text = styledText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Anime/Manga tag for a list activity, mirroring the rendered AniList media link: a plain
 * accent-colored label in AniList's brand blue (anime) / orange (manga). See [AniListLinkCard].
 */
@Composable
private fun MediaTypeLabel(type: ActivityMediaType) {
    val (labelRes, color) = when (type) {
        ActivityMediaType.ANIME -> R.string.media_type_anime to Color(0xFF3DB4F2)
        ActivityMediaType.MANGA -> R.string.media_type_manga to Color(0xFFF2A33D)
    }
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ActivityCardFooter(
    activity: UserActivity,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    onCommentClick: () -> Unit = {},
    onLikeClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (activity.replyUserName != null && activity.repliedAt != null) {
                Surface(
                    onClick = {
                        val replyId = activity.lastReplyId
                        if (replyId != null) onLastReplyClick(activity.id, replyId)
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            start = 6.dp,
                            end = 12.dp,
                            top = 6.dp,
                            bottom = 6.dp
                        )
                    ) {
                        UserAvatar(
                            url = activity.replyUserAvatarUrl,
                            contentDescription = activity.replyUserName,
                            size = 20.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Last by ${activity.replyUserName} • ${
                                formatRelativeTimeSeconds(activity.repliedAt)
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActivityStatPill(
                icon = Icons.Outlined.ChatBubbleOutline,
                value = activity.replyCount,
                onClick = onCommentClick,
                contentDescription = stringResource(R.string.cd_comments),
                contentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )

            val isLiked = activity.isLiked
            ActivityStatPill(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                value = activity.likeCount,
                onClick = onLikeClick,
                contentDescription = if (isLiked) "Unlike" else "Like",
                contentColor = if (isLiked) Color(0xFFBE123C) else MaterialTheme.colorScheme.primary,
                containerColor = if (isLiked) Color(0xFFBE123C).copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.1f
                )
            )
        }
    }
}

private fun String.parseHexColor(): Color? =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrNull()

private fun formatRelativeTimeSeconds(timestampSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestampSeconds
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 604800}w ago"
        diff < 31536000 -> "${diff / 2592000}mo ago"
        else -> "${diff / 31536000}y ago"
    }
}