package com.anisync.android.presentation.library.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatScore
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.ListIndicator
import com.anisync.android.presentation.components.ListIndicatorCorner
import com.anisync.android.presentation.components.ListIndicatorStyle
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatTimeUntilAiring
import com.anisync.android.presentation.util.bouncyCombinedClickable
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.ExpressiveShapes
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.ui.theme.listIndicatorColor
import com.anisync.android.util.getTitle
import com.anisync.android.presentation.util.toLabel

private const val PosterAspect = 171f / 243f
private val ActionSize = 48.dp

/**
 * The browsing card: a clean cover with the title and one line of state underneath.
 *
 * Text stays off the artwork for the same reason `MediaPosterCard` keeps it off — a long title and
 * a score both stay readable, and the cover keeps its own contrast. What this variant adds over
 * that one is the quick-progress button, because a grid of a list you are actively watching still
 * has to let you mark an episode without opening anything.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryPosterCard(
    entry: LibraryEntry,
    mediaType: MediaType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    showScore: Boolean = false,
    scoreFormat: ScoreFormat = ScoreFormat.POINT_10_DECIMAL,
    showListIndicator: Boolean = false,
    onIncrement: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val spatialSpec = AppMotion.rememberSpatialSpec()
    val haptic = rememberHapticFeedback()
    val title = entry.getTitle(titleLanguage)
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val aired = airedCount(entry, total)
    // An open-ended run has progress worth drawing too; only a finished or unstarted one
    // falls back to the browsing facts.
    val hasProgress = entry.progress > 0 && (total == null || entry.progress < total)

    val cardShape = ExpressiveShapes.mediaCover
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
            clipShape = cardShape
        )
    } else {
        Modifier.bouncyClickable(
            onClick = onClick,
            role = Role.Button,
            onClickLabel = stringResource(R.string.a11y_action_open_details, title),
            clipShape = cardShape
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
                clipInOverlayDuringTransition = OverlayClip(cardShape)
            )
        }
    } else {
        Modifier
    }

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .then(sharedModifier)
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, cardShape)
                } else {
                    Modifier
                }
            )
    ) {
        Column {
            PosterArt(
                entry = entry,
                title = title,
                total = total,
                aired = aired,
                showScore = showScore,
                scoreFormat = scoreFormat,
                showListIndicator = showListIndicator,
                onIncrement = onIncrement,
                onEdit = onEdit,
                selectionMode = selectionMode,
                selected = selected,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )

            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    // Two lines are reserved so every meta row in a grid row shares a baseline.
                    modifier = Modifier.height(40.dp)
                )
                Spacer(Modifier.height(10.dp))
                PosterMetaRow(
                    entry = entry,
                    mediaType = mediaType,
                    total = total,
                    aired = aired,
                    hasProgress = hasProgress
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PosterArt(
    entry: LibraryEntry,
    title: String,
    total: Int?,
    aired: Int?,
    showScore: Boolean,
    scoreFormat: ScoreFormat,
    showListIndicator: Boolean,
    onIncrement: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    selectionMode: Boolean,
    selected: Boolean,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    val spatialSpec = AppMotion.rememberSpatialSpec()
    val shape = ExpressiveShapes.mediaCover
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PosterAspect)
            .then(coverModifier)
            .clip(shape)
    ) {
        // 10dp inset either side, the 48dp action, and an 8dp gap the chip must never cross.
        val chipMaxWidth = (maxWidth - ActionSize - 28.dp).coerceAtLeast(0.dp)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(entry.cover.url() ?: entry.coverUrl)
                .crossfade(true)
                .placeholderMemoryCacheKey(cacheKey)
                .memoryCacheKey(cacheKey)
                .build(),
            contentDescription = stringResource(R.string.a11y_media_poster, title),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // The indicator takes the top-start corner here, because the bottom-end one is spent on
        // the action — the same swap `PosterCard` makes when a title occupies the bottom edge.
        if (showListIndicator) {
            ListIndicator(
                status = entry.status,
                type = entry.type,
                style = ListIndicatorStyle.Corner,
                corner = ListIndicatorCorner.TopStart,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        if (showScore && (entry.score ?: 0.0) > 0.0) {
            ScorePosterBadge(
                score = entry.score,
                format = scoreFormat,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            )
        }

        // The bottom-start corner is the only one the action and the indicator leave free, and a
        // grid with no airing date is a grid you have to open a card to read.
        if (!selectionMode) {
            PosterStateChip(
                entry = entry,
                aired = aired,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .widthIn(max = chipMaxWidth)
            )
        }

        when {
            selectionMode -> Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
            ) {
                PosterSelectionCheck(selected = selected)
            }

            onIncrement == null && onEdit != null -> Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
            ) {
                PosterEditButton(onEdit = onEdit)
            }

            onIncrement != null -> Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
            ) {
                PosterIncrementButton(
                    nextEpisode = entry.progress + 1,
                    ready = aired != null && entry.progress < aired &&
                        entry.status == LibraryStatus.CURRENT,
                    onIncrement = onIncrement,
                    onEdit = onEdit
                )
            }
        }
    }
}

/**
 * Progress when there is progress to report, otherwise the browsing facts. One line either way,
 * so a grid row never goes ragged.
 */
@Composable
private fun PosterMetaRow(
    entry: LibraryEntry,
    mediaType: MediaType,
    total: Int?,
    aired: Int?,
    hasProgress: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (hasProgress) {
            EpisodeProgressBar(
                progress = entry.progress,
                aired = aired,
                total = total,
                fraction = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f,
                modifier = Modifier.weight(1f),
                allowTicks = false
            )
            Text(
                text = progressLabel(entry.progress, aired, total, compact = true),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        } else {
            entry.format?.let { format ->
                Text(
                    text = format.toLabel(),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "·",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // The score is the cover badge's job. Printing it here as well drew the rating twice
            // on any title long enough to have no progress bar.
            run {
                Text(
                    // A run with no length still reports where you are in it, rather than a bare
                    // question mark.
                    text = if (total != null) {
                        stringResource(
                            if (mediaType == MediaType.MANGA) {
                                R.string.library_chapter_count
                            } else {
                                R.string.library_episode_count
                            },
                            total
                        )
                    } else {
                        stringResource(
                            R.string.progress_format,
                            entry.progress,
                            stringResource(R.string.progress_unknown)
                        )
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PosterIncrementButton(
    nextEpisode: Int,
    ready: Boolean,
    onIncrement: () -> Unit,
    onEdit: (() -> Unit)?
) {
    val haptic = rememberHapticFeedback()
    val shape = RoundedCornerShape(16.dp)
    Box {
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
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        // Which of these can I advance right now, without reading a word.
        if (ready) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 0.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(listIndicatorColor(ListIndicatorKind.WATCHING).content)
            )
        }
    }
}

@Composable
private fun PosterEditButton(onEdit: () -> Unit) {
    val haptic = rememberHapticFeedback()
    val shape = RoundedCornerShape(16.dp)
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

@Composable
private fun PosterSelectionCheck(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(ActionSize)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Black.copy(alpha = 0.45f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                Color.White.copy(alpha = 0.85f)
            },
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Score overlaid on the cover's top corner. Own dark scrim and white text because it sits on
 * artwork rather than a surface. The leading star is dropped for the formats that already read as
 * stars or smileys.
 */
@Composable
private fun ScorePosterBadge(
    score: Double?,
    format: ScoreFormat,
    modifier: Modifier = Modifier
) {
    val showStar = format != ScoreFormat.POINT_5 && format != ScoreFormat.POINT_3
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (showStar) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = StarGold,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = formatScore(score, format),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/**
 * When the next episode lands, or how much is already waiting.
 *
 * Deliberately not "EP n out now": that phrasing reads as a release announcement even on a show
 * that finished years ago. The count of what is waiting says the same thing without implying the
 * series is still going out.
 */
@Composable
private fun PosterStateChip(
    entry: LibraryEntry,
    aired: Int?,
    modifier: Modifier = Modifier
) {
    val behind = if (aired != null && entry.progress < aired) aired - entry.progress else 0
    val countdown = entry.dynamicTimeUntilAiring
    val nextEpisode = entry.nextAiringEpisode
    val label = when {
        behind > 0 -> stringResource(R.string.library_episodes_behind_short, behind)
        // Just the time. "Ep 1176 in 1d" does not fit beside the action on a poster this narrow,
        // and truncating it cuts off the countdown — the one part worth reading. The clock icon
        // carries the meaning; the episode number is on the row layout and the detail page.
        countdown != null && nextEpisode != null -> formatTimeUntilAiring(countdown)

        else -> null
    } ?: return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (behind > 0) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(listIndicatorColor(ListIndicatorKind.WATCHING).content)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
