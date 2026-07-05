package com.anisync.android.presentation.notifications.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.anisync.android.presentation.components.UserAvatar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.domain.ActivityKind
import com.anisync.android.domain.ActivityLikeNotification
import com.anisync.android.domain.ActivityMentionNotification
import com.anisync.android.domain.ActivityMessageNotification
import com.anisync.android.domain.ActivityReplyLikeNotification
import com.anisync.android.domain.ActivityReplyNotification
import com.anisync.android.domain.ActivityReplySubscribedNotification
import com.anisync.android.domain.ActivitySnapshot
import com.anisync.android.domain.AiringNotification
import com.anisync.android.domain.FollowingNotification
import com.anisync.android.domain.MediaDataChangeNotification
import com.anisync.android.domain.MediaDeletionNotification
import com.anisync.android.domain.MediaMergeNotification
import com.anisync.android.domain.RelatedMediaAdditionNotification
import com.anisync.android.domain.ThreadCommentLikeNotification
import com.anisync.android.domain.ThreadCommentMentionNotification
import com.anisync.android.domain.ThreadCommentReplyNotification
import com.anisync.android.domain.ThreadCommentSubscribedNotification
import com.anisync.android.domain.ThreadLikeNotification
import com.anisync.android.domain.UnknownNotification
import com.anisync.android.domain.User
import com.anisync.android.domain.indefiniteNoun
import com.anisync.android.domain.noun
import com.anisync.android.presentation.notifications.NotificationEntry
import com.anisync.android.presentation.notifications.NotificationTarget
import com.anisync.android.presentation.util.selectedPaneItem
import org.jsoup.Jsoup

@Composable
fun NotificationGroupCard(
    entry: NotificationEntry,
    onMediaClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    onActivityClick: (Int) -> Unit,
    onThreadClick: (threadId: Int, commentId: Int?) -> Unit,
    modifier: Modifier = Modifier,
    // The target open in the two-pane detail (or null). When this card resolves to that target it
    // shows the Material 3 selection ring (two-pane only).
    selectedTarget: NotificationTarget? = null
) {
    val payload = entry.toPayload()
    // Moderation notes (data change / merge / deletion reasons) can be long; the card expands in
    // place instead of navigating, and the cover thumb keeps the media-details click.
    var expanded by rememberSaveable(entry.key) { mutableStateOf(false) }
    Card(
        onClick = {
            if (payload.expandableNote) {
                expanded = !expanded
            } else {
                payload.handleClick(onMediaClick, onUserClick, onActivityClick, onThreadClick)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .selectedPaneItem(payload.isSelectedBy(selectedTarget), MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (payload.leadingMediaCover != null) {
            // Media layout: cover on the left sits beside the headline + subtitle,
            // timestamp pinned to the top-right corner. Used for airing and other
            // media-centred notifications.
            MediaCardBody(
                entry = entry,
                payload = payload,
                expanded = expanded,
                onMediaClick = onMediaClick
            )
        } else {
            // Social layout: vertical stack — avatars/icon header, headline, subtitle.
            SocialCardBody(
                entry = entry,
                payload = payload,
                expanded = expanded,
                onMediaClick = onMediaClick,
                onUserClick = onUserClick
            )
        }
    }
}

/** Explicit expand/collapse affordance under an expandable note — the whole card toggles it. */
@Composable
private fun ExpandAffordance(expanded: Boolean) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (expanded) "Show less" else "Show more",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MediaCardBody(
    entry: NotificationEntry,
    payload: GroupPayload,
    expanded: Boolean,
    onMediaClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        MediaCoverThumb(
            url = payload.leadingMediaCover!!,
            onClick = { payload.mediaId?.let(onMediaClick) }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = payload.headline,
                style = MaterialTheme.typography.titleSmall.emphasis(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            SubtitleSlots(payload = payload, expanded = expanded)
            if (payload.expandableNote) {
                ExpandAffordance(expanded)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatRelative(entry.representative.createdAt.toLong()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SocialCardBody(
    entry: NotificationEntry,
    payload: GroupPayload,
    expanded: Boolean,
    onMediaClick: (Int) -> Unit,
    onUserClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NotificationLeading(
                payload = payload,
                onMediaClick = onMediaClick,
                onUserClick = onUserClick,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatRelative(entry.representative.createdAt.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = payload.headline,
            style = MaterialTheme.typography.titleSmall.emphasis(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        SubtitleSlots(payload = payload, expanded = expanded)
        if (payload.expandableNote) {
            ExpandAffordance(expanded)
        }
    }
}

/**
 * Plain context line + plaintext body preview. Both slots are optional;
 * the plain line typically anchors the user (thread title, list status),
 * and the body slot strips AniList HTML (spoilers, embedded media,
 * AniList link cards, videos) down to a short text excerpt so the card
 * stays compact and predictable.
 */
@Composable
private fun SubtitleSlots(payload: GroupPayload, expanded: Boolean = false) {
    val plain = payload.subtitlePlain
    val bodyExcerpt = payload.subtitleHtml
        ?.takeIf { it.isNotBlank() }
        ?.let { remember(it) { htmlToPlainExcerpt(it) } }
        ?.takeIf { it.isNotBlank() }
    if (plain == null && bodyExcerpt == null) return
    val maxLines = if (expanded) Int.MAX_VALUE else 2

    if (plain != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = plain,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (bodyExcerpt != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = bodyExcerpt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Strips AniList rich-text HTML to a plain text excerpt. Drops embedded
 * images, videos and AniList link cards — those would overflow the card
 * and add visual noise. Whitespace is collapsed so a short multi-paragraph
 * comment reads as a single tidy line. Spoiler spans are removed entirely:
 * `.text()` would otherwise print their hidden content in the preview.
 */
private fun htmlToPlainExcerpt(html: String): String {
    val document = Jsoup.parse(html)
    document.select("span.markdown_spoiler").remove()
    return document.text().trim()
}

@Composable
private fun NotificationLeading(
    payload: GroupPayload,
    onMediaClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    containerColor: Color
) {
    val avatarSize = 44.dp
    when {
        payload.leadingMediaCover != null -> {
            MediaCoverThumb(
                url = payload.leadingMediaCover,
                onClick = { payload.mediaId?.let(onMediaClick) }
            )
        }
        payload.actors.isNotEmpty() -> {
            StackedAvatars(
                actors = payload.actors,
                onUserClick = onUserClick,
                size = avatarSize,
                overlapColor = containerColor
            )
        }
        payload.fallbackIcon != null -> {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = payload.fallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StackedAvatars(
    actors: List<User>,
    onUserClick: (String) -> Unit,
    size: Dp,
    overlapColor: Color,
    maxVisible: Int = 3
) {
    val visible = actors.take(maxVisible)
    val overlapAmount = size * 0.4f
    Box(contentAlignment = Alignment.CenterStart) {
        visible.asReversed().forEachIndexed { reversedIdx, actor ->
            val actualIdx = visible.size - 1 - reversedIdx
            val startOffset = (size - overlapAmount) * actualIdx
            UserAvatar(
                url = actor.avatarUrl,
                contentDescription = actor.name,
                size = size,
                modifier = Modifier
                    .padding(start = startOffset)
                    .clickable { actor.name.let(onUserClick) },
                borderWidth = 2.dp,
                borderColor = overlapColor
            )
        }
    }
}

@Composable
private fun MediaCoverThumb(url: String, onClick: (() -> Unit)?) {
    val base = Modifier
        .size(width = 44.dp, height = 60.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    val mod = if (onClick != null) base.clickable(onClick = onClick) else base
    Box(modifier = mod) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ---------- Domain → display payload ----------

private data class GroupPayload(
    val headline: String,
    /** Plain context line (e.g. thread title, "Episode 5", list status text). */
    val subtitlePlain: String? = null,
    /** Rendered AniList HTML body (comment / status / message). */
    val subtitleHtml: String? = null,
    val actors: List<User> = emptyList(),
    val fallbackIcon: ImageVector? = null,
    val leadingMediaCover: String? = null,
    val activityId: Int? = null,
    val mediaId: Int? = null,
    val threadId: Int? = null,
    val threadCommentId: Int? = null,
    val userName: String? = null,
    /** Card click expands the note in place instead of navigating (moderation reasons). */
    val expandableNote: Boolean = false
) {
    fun handleClick(
        onMediaClick: (Int) -> Unit,
        onUserClick: (String) -> Unit,
        onActivityClick: (Int) -> Unit,
        onThreadClick: (Int, Int?) -> Unit
    ) {
        when {
            activityId != null -> onActivityClick(activityId)
            threadId != null -> onThreadClick(threadId, threadCommentId)
            mediaId != null -> onMediaClick(mediaId)
            userName != null -> onUserClick(userName)
        }
    }

    /** True when this card's click target equals [target] — mirrors [handleClick]'s priority so the
     *  open notification's card shows the selected state in the two-pane list-detail layout. */
    fun isSelectedBy(target: NotificationTarget?): Boolean = when {
        target == null -> false
        activityId != null -> target is NotificationTarget.Activity && target.activityId == activityId
        threadId != null ->
            target is NotificationTarget.Thread &&
                target.threadId == threadId && target.commentId == threadCommentId
        mediaId != null -> target is NotificationTarget.Media && target.mediaId == mediaId
        userName != null -> target is NotificationTarget.Profile && target.username == userName
        else -> false
    }
}

private fun NotificationEntry.toPayload(): GroupPayload {
    val rep = representative
    return when (rep) {
        is AiringNotification -> GroupPayload(
            headline = rep.media?.title ?: "Anime",
            subtitlePlain = "Episode ${rep.episode}",
            leadingMediaCover = rep.media?.coverUrl,
            fallbackIcon = Icons.Default.PlayCircleOutline,
            mediaId = rep.media?.id
        )
        is FollowingNotification -> GroupPayload(
            headline = "${rep.user?.name ?: "Someone"} started following you",
            actors = listOfNotNull(rep.user),
            fallbackIcon = Icons.Default.Person,
            userName = rep.user?.name
        )
        is ActivityLikeNotification -> activityLikePayload(
            actors = actors,
            verb = "liked ${activityNoun(rep.activity, possessive = "your")}",
            activity = rep.activity,
            activityId = rep.activityId
        )
        is ActivityReplyLikeNotification -> activityLikePayload(
            actors = actors,
            verb = "liked your reply",
            activity = rep.activity,
            activityId = rep.activityId
        )
        is ActivityReplyNotification -> activitySingleActorPayload(
            user = rep.user,
            verb = "replied to ${activityNoun(rep.activity, possessive = "your")}",
            activity = rep.activity,
            activityId = rep.activityId
        )
        is ActivityReplySubscribedNotification -> activitySingleActorPayload(
            user = rep.user,
            verb = "replied to a post you're subscribed to",
            activity = rep.activity,
            activityId = rep.activityId
        )
        is ActivityMentionNotification -> activitySingleActorPayload(
            user = rep.user,
            verb = "mentioned you in ${activityNoun(rep.activity, possessive = "a")}",
            activity = rep.activity,
            activityId = rep.activityId
        )
        is ActivityMessageNotification -> {
            val name = rep.user?.name ?: "Someone"
            GroupPayload(
                headline = "$name sent you a message",
                subtitleHtml = rep.messagePreview?.takeIf { it.isNotBlank() },
                actors = listOfNotNull(rep.user),
                fallbackIcon = Icons.Default.Person,
                activityId = rep.activityId,
                userName = name
            )
        }
        is ThreadLikeNotification -> GroupPayload(
            headline = combineActors(actors, verb = "liked your thread"),
            subtitlePlain = rep.threadTitle.takeIf { it.isNotBlank() },
            actors = actors,
            fallbackIcon = Icons.Default.Person,
            threadId = rep.threadId
        )
        is ThreadCommentLikeNotification -> GroupPayload(
            headline = combineActors(actors, verb = "liked your comment"),
            subtitlePlain = rep.threadTitle.takeIf { it.isNotBlank() },
            subtitleHtml = rep.commentPreview?.takeIf { it.isNotBlank() },
            actors = actors,
            fallbackIcon = Icons.Default.Person,
            threadId = rep.threadId,
            threadCommentId = rep.commentId
        )
        is ThreadCommentReplyNotification -> threadCommentPayload(
            user = rep.user,
            verb = "replied to your comment",
            threadTitle = rep.threadTitle,
            threadId = rep.threadId,
            commentId = rep.commentId,
            html = rep.commentPreview
        )
        is ThreadCommentSubscribedNotification -> threadCommentPayload(
            user = rep.user,
            verb = "commented in a thread you're subscribed to",
            threadTitle = rep.threadTitle,
            threadId = rep.threadId,
            commentId = rep.commentId,
            html = rep.commentPreview
        )
        is ThreadCommentMentionNotification -> threadCommentPayload(
            user = rep.user,
            verb = "mentioned you in a comment",
            threadTitle = rep.threadTitle,
            threadId = rep.threadId,
            commentId = rep.commentId,
            html = rep.commentPreview
        )
        is RelatedMediaAdditionNotification -> GroupPayload(
            headline = rep.media?.title ?: "A new title",
            subtitlePlain = rep.context.trim().ifEmpty { "was added to the site" },
            leadingMediaCover = rep.media?.coverUrl,
            fallbackIcon = Icons.Default.PlayCircleOutline,
            mediaId = rep.mediaId
        )
        is MediaDataChangeNotification -> GroupPayload(
            headline = rep.media?.title ?: "A title on your list",
            subtitlePlain = rep.context.trim().ifBlank { "had data updated" },
            // Moderation note rides the body slot so the card can expand to show all of it.
            subtitleHtml = rep.reason.takeIf { it.isNotBlank() },
            leadingMediaCover = rep.media?.coverUrl,
            fallbackIcon = Icons.Default.Edit,
            mediaId = rep.mediaId,
            expandableNote = rep.reason.isNotBlank()
        )
        is MediaMergeNotification -> {
            val merged = rep.deletedMediaTitles.joinToString(", ").ifBlank { "Entries" }
            val target = rep.media?.title ?: "another entry"
            GroupPayload(
                headline = "$merged merged into $target",
                subtitleHtml = rep.reason.takeIf { it.isNotBlank() },
                leadingMediaCover = rep.media?.coverUrl,
                fallbackIcon = Icons.AutoMirrored.Filled.MergeType,
                mediaId = rep.mediaId,
                expandableNote = rep.reason.isNotBlank()
            )
        }
        is MediaDeletionNotification -> GroupPayload(
            headline = "${rep.deletedMediaTitle} was removed from the site",
            subtitleHtml = rep.reason.takeIf { it.isNotBlank() },
            fallbackIcon = Icons.Default.Delete,
            expandableNote = rep.reason.isNotBlank()
        )
        is UnknownNotification -> GroupPayload(headline = "New notification")
    }
}

private fun activityLikePayload(
    actors: List<User>,
    verb: String,
    activity: ActivitySnapshot?,
    activityId: Int?
): GroupPayload {
    val (plain, html) = activitySubtitle(activity)
    return GroupPayload(
        headline = combineActors(actors, verb = verb),
        subtitlePlain = plain,
        subtitleHtml = html,
        actors = actors,
        fallbackIcon = Icons.Default.Person,
        activityId = activityId
    )
}

private fun activitySingleActorPayload(
    user: User?,
    verb: String,
    activity: ActivitySnapshot?,
    activityId: Int?
): GroupPayload {
    val name = user?.name ?: "Someone"
    val (plain, html) = activitySubtitle(activity)
    return GroupPayload(
        headline = "$name $verb",
        subtitlePlain = plain,
        subtitleHtml = html,
        actors = listOfNotNull(user),
        fallbackIcon = Icons.Default.Person,
        activityId = activityId,
        userName = name
    )
}

private fun threadCommentPayload(
    user: User?,
    verb: String,
    threadTitle: String,
    threadId: Int,
    commentId: Int?,
    html: String?
): GroupPayload {
    val name = user?.name ?: "Someone"
    return GroupPayload(
        headline = "$name $verb",
        subtitlePlain = threadTitle.takeIf { it.isNotBlank() },
        subtitleHtml = html?.takeIf { it.isNotBlank() },
        actors = listOfNotNull(user),
        fallbackIcon = Icons.Default.Person,
        threadId = threadId,
        threadCommentId = commentId
    )
}

private fun combineActors(actors: List<User>, verb: String): String {
    return when (actors.size) {
        0 -> "Someone $verb"
        1 -> "${actors[0].name} $verb"
        2 -> "${actors[0].name} and ${actors[1].name} $verb"
        else -> {
            val others = actors.size - 2
            val tail = if (others == 1) "1 other" else "$others others"
            "${actors[0].name}, ${actors[1].name} and $tail $verb"
        }
    }
}

/**
 * Picks a precise noun phrase for the activity referenced by the notification.
 * AniList serves a generic "activity" string for all three subtypes, but the
 * activity union itself tells us whether the post is a text status, a list
 * update, or a DM — surface that distinction directly in the headline.
 * "a" as the possessive picks the right indefinite article ("an anime list update").
 */
private fun activityNoun(activity: ActivitySnapshot?, possessive: String): String =
    if (possessive == "a") activity?.kind.indefiniteNoun() else "$possessive ${activity?.kind.noun()}"

/**
 * Splits an activity snapshot into the two subtitle slots — list activities
 * become a single plain sentence ("Watched episode 5 of Frieren"), while text
 * and message bodies flow through the rich-text slot so AniList markup renders
 * properly.
 */
private fun activitySubtitle(activity: ActivitySnapshot?): Pair<String?, String?> {
    if (activity == null) return null to null
    return when (activity.kind) {
        ActivityKind.TEXT -> null to activity.text?.takeIf { it.isNotBlank() }
        ActivityKind.MESSAGE -> null to activity.message?.takeIf { it.isNotBlank() }
        ActivityKind.ANIME_LIST, ActivityKind.MANGA_LIST -> formatListActivity(activity) to null
        ActivityKind.UNKNOWN -> null to null
    }
}

/**
 * AniList serves the verb (e.g. "plans to watch", "watched episode") in
 * [ActivitySnapshot.listStatus] and the numeric progress separately. Statuses
 * that carry their own counter ("watched episode", "read chapter") read
 * naturally as `"$status $progress of $title"`; statuses without a counter
 * ("Plans to watch", "Completed") read as `"$status $title"` and tacking
 * "of" between them sounds wrong.
 */
private fun formatListActivity(activity: ActivitySnapshot): String? {
    val rawStatus = activity.listStatus?.trim().orEmpty()
    val progress = activity.listProgress?.trim().orEmpty()
    val title = activity.listMedia?.title.orEmpty()
    if (rawStatus.isEmpty() && title.isEmpty()) return null
    val status = rawStatus.replaceFirstChar { it.uppercase() }
    return when {
        progress.isNotEmpty() && title.isNotEmpty() -> "$status $progress of $title"
        progress.isNotEmpty() -> "$status $progress".trim()
        title.isNotEmpty() && status.isNotEmpty() -> "$status $title"
        title.isNotEmpty() -> title
        else -> status
    }
}

private fun formatRelative(timestampSeconds: Long): String {
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
