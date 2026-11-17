package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.anisync.android.presentation.components.MediaPosterCard
import com.anisync.android.presentation.components.MediaPosterCardDefaults
import com.anisync.android.presentation.components.NoteBottomSheet
import com.anisync.android.presentation.components.NotePreview
import com.anisync.android.presentation.util.LocalLibraryStatuses
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatScore
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.profile.lists.ProfileListSort
import com.anisync.android.presentation.util.bouncyCombinedClickable
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import com.anisync.android.util.getTitle
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.anisync.android.ui.theme.ExpressiveShapes

/**
 * A title on someone's list: what it is, then their line (status, progress, score, when), then the
 * viewer's own entry when they have it. The status is left out when the chip already names it.
 */
@Composable
fun ProfileListRow(
    entry: LibraryEntry,
    mediaType: MediaType,
    ownerAvatar: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    showStatus: Boolean = true,
    yours: LibraryEntry? = null,
    yourAvatar: String? = null,
    yourScoreFormat: ScoreFormat = ScoreFormat.POINT_100,
    dateSort: ProfileListSort = ProfileListSort.UPDATED,
    onLongClick: (() -> Unit)? = null,
    /** Leads the facts line, which then leaves the length out to make room. */
    factsLead: String? = null,
    showDate: Boolean = true
) {
    val title = entry.getTitle(titleLanguage)
    val shape = RoundedCornerShape(16.dp)
    var noteOpen by remember(entry.id) { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .bouncyCombinedClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                onLongClick = onLongClick,
                onLongClickLabel = onLongClick?.let { stringResource(R.string.a11y_action_more_actions) },
                pressedScale = 0.98f
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = entry.cover.url() ?: entry.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 56.dp, height = 80.dp)
                    .clip(ExpressiveShapes.mediaCover)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val facts = mediaFacts(entry, mediaType, factsLead)
                if (facts.isNotEmpty()) {
                    Text(
                        text = facts,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                PersonLine(
                    avatar = ownerAvatar,
                    text = theirLine(entry, mediaType, showStatus, dateSort.takeIf { showDate }),
                    color = MaterialTheme.colorScheme.onSurface
                )
                entry.notes?.takeIf { it.isNotBlank() }?.let { note ->
                    NotePreview(note = note, onOpen = { noteOpen = true }, modifier = Modifier.padding(start = 2.dp))
                }
                if (yours != null) {
                    PersonLine(
                        avatar = yourAvatar,
                        text = yourLine(yours, entry, mediaType, yourScoreFormat),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    if (noteOpen) {
        entry.notes?.let { NoteBottomSheet(note = it, heading = title, onDismiss = { noteOpen = false }) }
    }
}

@Composable
private fun PersonLine(avatar: String?, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserAvatar(contentDescription = null, size = 18.dp, url = avatar)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun mediaFacts(entry: LibraryEntry, mediaType: MediaType, lead: String?): String {
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    return listOfNotNull(
        lead,
        entry.format?.toLabel(),
        entry.releaseYear?.toString(),
        total?.takeIf { it > 0 && lead == null }?.let {
            pluralStringResourceOrDefault(if (mediaType == MediaType.MANGA) R.plurals.profile_row_chapters else R.plurals.profile_row_episodes, it, it)
        }
    ).joinToString(" · ")
}

@Composable
private fun theirLine(entry: LibraryEntry, mediaType: MediaType, showStatus: Boolean, dateSort: ProfileListSort?): String {
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val parts = buildList {
        if (showStatus) add(entry.status.toLabel(mediaType))
        if (entry.status == LibraryStatus.CURRENT || entry.status == LibraryStatus.PAUSED ||
            entry.status == LibraryStatus.REPEATING || entry.status == LibraryStatus.DROPPED
        ) {
            if (entry.progress > 0) add(progressText(entry.progress, total, mediaType))
        }
        entry.score?.takeIf { it > 0.0 }?.let { add(scoreOnScale(it, entry.scoreFormat ?: ScoreFormat.POINT_100)) }
        if (entry.rewatches > 0) {
            add(pluralStringResourceOrDefault(if (mediaType == MediaType.MANGA) R.plurals.profile_row_reread else R.plurals.profile_row_rewatched, entry.rewatches, entry.rewatches))
        }
        when (dateSort) {
            ProfileListSort.UPDATED -> entry.updatedAt?.let { add(stringResource(R.string.profile_row_updated, shortDate(it, fuzzy = false))) }
            ProfileListSort.STARTED -> entry.startedAt?.let { add(stringResource(R.string.profile_row_started, shortDate(it, fuzzy = true))) }
            ProfileListSort.ADDED -> entry.createdAt?.let { add(stringResource(R.string.profile_row_added, shortDate(it, fuzzy = false))) }
            null -> Unit
            else -> entry.completedAt?.let { add(stringResource(R.string.profile_row_finished, shortDate(it, fuzzy = true))) }
        }
    }
    return parts.joinToString(" · ")
}

@Composable
private fun yourLine(yours: LibraryEntry, theirs: LibraryEntry, mediaType: MediaType, format: ScoreFormat): String {
    val total = if (mediaType == MediaType.MANGA) theirs.totalChapters else theirs.totalEpisodes
    val parts = buildList {
        add(stringResource(R.string.profile_list_you))
        add(yours.status.toLabel(mediaType))
        val underway = yours.status == LibraryStatus.CURRENT || yours.status == LibraryStatus.REPEATING
        when {
            yours.status != LibraryStatus.COMPLETED && yours.status != LibraryStatus.PLANNING && yours.progress > 0 ->
                add(progressText(yours.progress, total, mediaType))
            underway -> add(stringResource(R.string.profile_row_not_started))
        }
        val score = yours.score?.takeIf { it > 0.0 }
        when {
            score != null -> add(scoreOnScale(score, format))
            yours.status == LibraryStatus.COMPLETED -> add(stringResource(R.string.profile_row_no_score))
        }
    }
    return parts.joinToString(" · ")
}

@Composable
private fun progressText(progress: Int, total: Int?, mediaType: MediaType): String {
    val manga = mediaType == MediaType.MANGA
    return if ((total ?: 0) > 0) {
        stringResource(if (manga) R.string.profile_chapter_of else R.string.profile_episode_of, progress, total!!)
    } else {
        stringResource(if (manga) R.string.profile_chapter else R.string.profile_episode, progress)
    }
}

/** List dates (started, finished) are stored as midnight UTC, so they are read back in UTC. */
private fun shortDate(millis: Long, fuzzy: Boolean): String {
    val zone = if (fuzzy) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
    val year = Instant.ofEpochMilli(millis).atZone(zone.toZoneId()).year
    val format = if (year == Instant.now().atZone(zone.toZoneId()).year) {
        SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd"), Locale.getDefault())
    } else {
        DateFormat.getDateInstance(DateFormat.MEDIUM)
    }
    format.timeZone = zone
    return format.format(Date(millis))
}

/** The app's poster card, with the owner's score where the community average usually sits. */
@Composable
fun ProfileListGridCell(
    entry: LibraryEntry,
    titleLanguage: TitleLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showYourStatus: Boolean = true
) {
    val facts = listOfNotNull(entry.format?.toLabel(), entry.releaseYear?.toString()).joinToString(" · ")
    val score = entry.score?.takeIf { it > 0.0 }?.let { scoreOnScale(it, entry.scoreFormat ?: ScoreFormat.POINT_100) }
    MediaPosterCard(
        item = entry,
        onClick = onClick,
        modifier = modifier,
        titleLanguage = titleLanguage,
        transitionPrefix = TransitionKeys.PROFILE,
        listStatus = if (showYourStatus) LocalLibraryStatuses.current[entry.mediaId] else null,
        meta = {
            if (facts.isNotEmpty()) MediaPosterCardDefaults.MetaText(facts)
            if (score != null) {
                if (facts.isNotEmpty()) MediaPosterCardDefaults.MetaText("·")
                MediaPosterCardDefaults.MetaText(score, emphasized = true)
            }
        }
    )
}

/** Numeric scores carry their scale, "8 / 10", stars and faces read on their own. */
internal fun scoreOnScale(score: Double, format: ScoreFormat): String = when (format) {
    ScoreFormat.POINT_10, ScoreFormat.POINT_10_DECIMAL -> formatScore(score, format) + " / 10"
    ScoreFormat.POINT_100 -> formatScore(score, format) + " / 100"
    else -> formatScore(score, format)
}
