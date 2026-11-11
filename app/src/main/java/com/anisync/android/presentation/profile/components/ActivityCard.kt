package com.anisync.android.presentation.profile.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ActivityMediaType
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserActivity
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.DonatorBadge
import com.anisync.android.presentation.components.DonatorBadgePlacement
import com.anisync.android.presentation.components.ReadMoreToggle
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.util.selectedPaneItem

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
    maxBodyLines: Int? = null,
    /** False on a profile, where every card in the list belongs to the same person. */
    showAuthor: Boolean = true
) {
    if (activity.type == ActivityType.MEDIA_LIST) {
        ActivityListCard(
            activity = activity,
            onClick = onClick,
            modifier = modifier,
            showAuthor = showAuthor,
            selected = selected,
            onUserClick = onUserClick,
            onMediaClick = onMediaClick,
            onLastReplyClick = onLastReplyClick,
            onLikeClick = onLikeClick,
            onSubscribeClick = onSubscribeClick,
            onDeleteClick = onDeleteClick
        )
        return
    }

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
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick
            )

            val isTextual = activity.type == ActivityType.TEXT || activity.type == ActivityType.MESSAGE
            when {
                // Overview teaser: a fixed line cap that opens the full activity on tap (non-interactive).
                maxBodyLines != null -> {
                    ClampedActivityBody(maxLines = maxBodyLines, fadeColor = containerColor) {
                        ActivityCardBody(activity = activity)
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
                        ActivityCardBody(activity = activity)
                    }
                }
                else -> ActivityCardBody(activity = activity)
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
        // Bodies emit stacked siblings (a leading Spacer + the media/rich-text block), so they must
        // arrange vertically. Placing them straight in the Box would overlap the Spacer onto the
        // block and swallow the header↔body gap — the list-activity teaser then sat tighter here
        // than the same card in the Activity feed. A Column restores the intended spacing.
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Collapsed height for a status/message body in the full feed. ~10 lines of body text, or a peek of
 * an embedded image — enough to judge the post without letting one long post dominate the scroll.
 * Bodies taller than this collapse behind a "Read more" toggle; shorter ones render in full.
 */
private val ACTIVITY_BODY_COLLAPSED_MAX = 240.dp

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
            // See ClampedActivityBody: bodies are stacked siblings, so a Column keeps the leading
            // Spacer from overlapping the block and preserves the header↔body gap.
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
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
    onEditClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Sender Avatar Only (Simplified for all cards, including messages)
        UserAvatar(
            url = activity.userAvatarUrl,
            contentDescription = activity.userName,
            size = 36.dp,
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

                DonatorBadge(
                    tier = activity.userDonatorTier,
                    badgeText = activity.userDonatorBadge,
                    placement = DonatorBadgePlacement.NextToName
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
                        contentDescription = stringResource(
                            if (activity.isPrivate) R.string.activity_detail_private else R.string.locked
                        ),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = formatRelativeTimeSeconds(LocalResources.current, activity.timestamp / 1000L),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Subscribe stays on the card because it is a state you read at a glance; share, the
        // AniList link and the owner's own edit and delete live in the overflow menu.
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onSubscribeClick != null) {
                IconButton(onClick = onSubscribeClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (activity.isSubscribed) Icons.Filled.Notifications
                        else Icons.Outlined.NotificationsNone,
                        contentDescription = stringResource(
                            if (activity.isSubscribed) R.string.cd_unsubscribe else R.string.cd_subscribe
                        ),
                        tint = if (activity.isSubscribed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            ActivityOverflowButton(
                activity = activity,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick
            )
        }
    }
}

/** Status and message bodies only — a list update is drawn by [ActivityListCard] instead. */
@Composable
private fun ActivityCardBody(activity: UserActivity) {
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

/**
 * Anime/Manga tag for a list activity, mirroring the rendered AniList media link: a plain
 * accent-colored label in AniList's brand blue (anime) / orange (manga). See [AniListLinkCard].
 */
@Composable
internal fun MediaTypeLabel(type: ActivityMediaType) {
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

/**
 * One row under a hairline: who replied last on the left, replies and likes on the right.
 *
 * The two used to be separate clusters with the counts tucked into the corner. Sharing a row makes
 * the divider mean something — everything above it is the post, everything below is what happened
 * to it — and puts the two things you tap within a thumb's reach of each other.
 */
@Composable
private fun ActivityCardFooter(
    activity: UserActivity,
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    onCommentClick: () -> Unit = {},
    onLikeClick: (() -> Unit)? = null
) {
    HorizontalDivider(
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActivityLastReply(
            activity = activity,
            onLastReplyClick = onLastReplyClick,
            modifier = Modifier.weight(1f),
            avatarSize = 20.dp
        )
        ActivityCounts(
            activity = activity,
            onCommentClick = onCommentClick,
            onLikeClick = onLikeClick
        )
    }
}

