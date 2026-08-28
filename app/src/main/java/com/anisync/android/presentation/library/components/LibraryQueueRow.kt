package com.anisync.android.presentation.library.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.url
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.bouncyCombinedClickable
import com.anisync.android.presentation.util.formatTimeUntilAiring
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.listIndicatorColor
import com.anisync.android.util.getTitle

/** Card height. Fixed so the right-hand action column lines up down the whole list. */
private val RowHeight = 112.dp
private val CoverWidth = 70.dp
private val ActionSize = 56.dp

/**
 * Above this many episodes a tick per episode is thinner than the gaps between them, so the bar
 * degrades to a continuous three-tier fill instead.
 */
private const val MaxTicks = 16

/**
 * A row of the library queue: cover, title, one line of state, an episode bar, and the single
 * action you came here to press.
 *
 * The card carries one action rather than the +/- pair the poster card used to: decrements are a
 * correction, increments are the daily job, and giving them equal weight cost half the action area
 * and a whole row of card height. Correcting a mistake goes through the edit sheet on long-press
 * of the button, which is also where every other per-entry field already lives.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryQueueRow(
    entry: LibraryEntry,
    mediaType: MediaType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    onIncrement: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val spatialSpec = AppMotion.rememberSpatialSpec()
    val effectsSpec = AppMotion.rememberSlowEffectsSpec()
    val haptic = rememberHapticFeedback()

    val title = entry.getTitle(titleLanguage)
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val aired = airedCount(entry, total)
    val animatedProgress by animateFloatAsState(
        targetValue = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f,
        animationSpec = effectsSpec,
        label = "QueueProgress"
    )

    val shape = RoundedCornerShape(16.dp)
    val clickModifier = if (onLongPress != null) {
        Modifier.bouncyCombinedClickable(
            onClick = onClick,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress()
            },
            role = Role.Button,
            onClickLabel = stringResource(R.string.a11y_action_open_details, title),
            onLongClickLabel = stringResource(R.string.a11y_action_select_entry),
            clipShape = shape
        )
    } else {
        Modifier.bouncyClickable(
            onClick = onClick,
            role = Role.Button,
            onClickLabel = stringResource(R.string.a11y_action_open_details, title),
            clipShape = shape
        )
    }

    val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = TransitionKeys.container(TransitionKeys.LIBRARY, entry.mediaId)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                clipInOverlayDuringTransition = OverlayClip(shape)
            )
        }
    } else {
        Modifier
    }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .then(clickModifier)
            .then(sharedModifier)
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            QueueCover(
                entry = entry,
                title = title,
                dimmed = selected,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(40.dp)
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QueueStateLine(
                        entry = entry,
                        total = total,
                        aired = aired,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.progress_format,
                            entry.progress,
                            total?.toString() ?: stringResource(R.string.progress_unknown)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(8.dp))
                EpisodeProgressBar(
                    progress = entry.progress,
                    aired = aired,
                    total = total,
                    fraction = animatedProgress,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier.size(ActionSize).align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                when {
                    selectionMode -> SelectionCheck(selected = selected)
                    onIncrement != null -> IncrementButton(
                        nextEpisode = entry.progress + 1,
                        onIncrement = onIncrement,
                        onEdit = onEdit
                    )
                    // A list with nothing to advance still gets its one-tap edit.
                    onEdit != null -> EditButton(onEdit = onEdit)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun QueueCover(
    entry: LibraryEntry,
    title: String,
    dimmed: Boolean,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    val spatialSpec = AppMotion.rememberSpatialSpec()
    // A list thumbnail this small takes the shape scale's list-item radius, not the 18dp poster
    // radius: 18 on a 70dp-wide cover eats a quarter of its width.
    val shape = RoundedCornerShape(12.dp)
    val cacheKey = TransitionKeys.imageCacheKey(TransitionKeys.LIBRARY, entry.mediaId) +
        "-" + com.anisync.android.domain.LocalCoverQuality.current.name +
        TransitionKeys.coverVersion(entry.cover.url() ?: entry.coverUrl)

    val coverModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = TransitionKeys.cover(TransitionKeys.LIBRARY, entry.mediaId)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                clipInOverlayDuringTransition = OverlayClip(shape)
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .width(CoverWidth)
            .aspectRatio(0.7f)
            .then(coverModifier)
            .clip(shape)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(entry.cover.url() ?: entry.coverUrl)
                .crossfade(true)
                .placeholderMemoryCacheKey(cacheKey)
                .memoryCacheKey(cacheKey)
                .build(),
            contentDescription = stringResource(R.string.a11y_media_poster, title),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (dimmed) 0.72f else 1f }
        )

        // Spot an annotated entry while scanning, without opening anything (#75).
        if (!entry.notes.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_note_stack_24px),
                    contentDescription = stringResource(R.string.a11y_has_notes),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(4.dp).size(14.dp)
                )
            }
        }
    }
}

/**
 * The one meta line: an episode you can watch now, or the wait until the next one.
 *
 * Being behind is reported in the tertiary tone rather than the error tone the old badge used. A
 * backlog is the normal state of a library, not a fault, and painting six cards red made the list
 * read as broken.
 */
@Composable
private fun QueueStateLine(
    entry: LibraryEntry,
    total: Int?,
    aired: Int?,
    modifier: Modifier = Modifier
) {
    val behind = if (aired != null && entry.progress < aired) aired - entry.progress else 0
    val ready = behind > 0 && entry.status == LibraryStatus.CURRENT
    val countdown = entry.dynamicTimeUntilAiring
    val nextEpisode = entry.nextAiringEpisode
    val tertiary = MaterialTheme.colorScheme.tertiary

    // One annotated string rather than several Texts: separate nodes let the row clip the last one
    // and leave an orphaned separator behind when a title runs long.
    val readyText = stringResource(R.string.library_episode_out_now, entry.progress + 1)
    val behindText = if (behind > 1) stringResource(R.string.library_episodes_behind_short, behind) else null
    val label: AnnotatedString? = when {
        ready -> buildAnnotatedString {
            append(readyText)
            if (behindText != null) {
                append(" · ")
                withStyle(SpanStyle(color = tertiary)) { append(behindText) }
            }
        }

        countdown != null && nextEpisode != null -> AnnotatedString(
            stringResource(
                R.string.airing_episode_in,
                nextEpisode,
                formatTimeUntilAiring(countdown)
            )
        )

        total != null && entry.progress >= total ->
            AnnotatedString(stringResource(R.string.library_all_caught_up))

        else -> null
    }
    if (label == null) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (ready) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(listIndicatorColor(ListIndicatorKind.WATCHING).content)
            )
        } else {
            Icon(
                imageVector = if (total != null && entry.progress >= total) {
                    Icons.Default.Check
                } else {
                    Icons.Default.Schedule
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (ready) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Watched, aired-but-unwatched and unaired in one glyph.
 *
 * A plain bar answers only "how far in"; the interesting question in a library is "how much is
 * waiting for me", which needs the middle tier. Short runs get a tick per episode because that is
 * also the count.
 */
@Composable
fun EpisodeProgressBar(
    progress: Int,
    aired: Int?,
    total: Int?,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val watched = MaterialTheme.colorScheme.primary
    val available = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val shape = RoundedCornerShape(3.dp)

    if (total != null && total in 1..MaxTicks) {
        Row(
            modifier = modifier.height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(total) { index ->
                val color = when {
                    index < progress -> watched
                    aired != null && index < aired -> available
                    else -> track
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(shape)
                        .background(color)
                )
            }
        }
    } else {
        val airedFraction = if (total != null && total > 0 && aired != null) {
            (aired.toFloat() / total).coerceIn(0f, 1f)
        } else {
            fraction
        }
        Box(modifier = modifier.height(6.dp).clip(shape).background(track)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(airedFraction)
                    .fillMaxSize()
                    .background(available)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxSize()
                    .background(watched)
            )
        }
    }
}

/**
 * The single primary action, as a squircle so it reads as a control against the card's own radius.
 * Long-press opens the edit sheet, which is where a decrement or any other correction belongs.
 */
@Composable
private fun IncrementButton(
    nextEpisode: Int,
    onIncrement: () -> Unit,
    onEdit: (() -> Unit)?
) {
    val haptic = rememberHapticFeedback()
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .size(ActionSize)
            .bouncyCombinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onIncrement()
                },
                onLongClick = onEdit?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_mark_episode, nextEpisode),
                onLongClickLabel = stringResource(R.string.a11y_action_edit_entry),
                clipShape = shape
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EditButton(onEdit: () -> Unit) {
    val haptic = rememberHapticFeedback()
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .size(ActionSize)
            .bouncyClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEdit()
                },
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_edit_entry),
                clipShape = shape
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** The action slot becomes the checkbox during selection, so nothing shifts on the way in. */
@Composable
private fun SelectionCheck(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * How many episodes exist to watch right now.
 *
 * The next airing episode number minus one is the last one out; without a schedule the whole run
 * is assumed available, which is right for anything finished.
 */
fun airedCount(entry: LibraryEntry, total: Int?): Int? {
    val next = entry.nextAiringEpisode
    return if (next != null) (next - 1).coerceAtLeast(0) else total
}
