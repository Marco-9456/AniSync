package com.anisync.android.presentation.library.components

import com.anisync.android.domain.url

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.components.CompletedCardConfig
import com.anisync.android.presentation.components.ScoreChip
import com.anisync.android.presentation.components.LibraryCardConfig
import com.anisync.android.presentation.components.WatchingCardConfig
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.bouncyCombinedClickable
import com.anisync.android.presentation.util.formatEpisodesBehind
import com.anisync.android.presentation.util.formatTimeUntilAiring
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType
import com.anisync.android.util.getTitle

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryListCard(
    entry: LibraryEntry,
    mediaType: MediaType,
    nowEpochSec: Long = 0L,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: LibraryCardConfig = WatchingCardConfig,
    showScore: Boolean = false,
    scoreFormat: ScoreFormat = ScoreFormat.POINT_10_DECIMAL,
    onIncrement: (() -> Unit)? = null,
    onDecrement: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val spatialSpec = AppMotion.rememberSpatialSpec()
    val effectsSpec = AppMotion.rememberSlowEffectsSpec()

    val containerKey = TransitionKeys.container(TransitionKeys.LIBRARY, entry.mediaId)
    val coverKey = TransitionKeys.cover(TransitionKeys.LIBRARY, entry.mediaId)
    val titleKey = TransitionKeys.title(TransitionKeys.LIBRARY, entry.mediaId)
    val cacheKey = (TransitionKeys.imageCacheKey(TransitionKeys.LIBRARY, entry.mediaId) + "-" + com.anisync.android.domain.LocalCoverQuality.current.name + TransitionKeys.coverVersion(entry.cover.url() ?: entry.coverUrl))

    val total: Int? = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val progressPercent = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f
    val title = entry.getTitle(titleLanguage)

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = effectsSpec,
        label = "Progress"
    )

    val baseModifier = modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .then(
            if (config.showAdjusters && onEdit != null) {
                Modifier.bouncyCombinedClickable(
                    onClick = onClick,
                    onLongClick = onEdit,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                    onLongClickLabel = stringResource(R.string.a11y_action_edit_entry),
                    clipShape = RoundedCornerShape(16.dp)
                )
            } else {
                Modifier.bouncyClickable(
                    onClick = onClick,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                    clipShape = RoundedCornerShape(16.dp)
                )
            }
        )

    val cardModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            baseModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = containerKey),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
            )
        }
    } else {
        baseModifier
    }

    val coverShape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    val coverSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = coverKey),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                clipInOverlayDuringTransition = OverlayClip(coverShape)
            )
        }
    } else {
        Modifier
    }
    val titleSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = titleKey),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
            )
        }
    } else {
        Modifier
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = cardModifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
        ) {
            // Left Side: Image
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(0.7f) // Dynamically sets Row height and prevents image cropping
                    .then(coverSharedModifier)
                    .clip(coverShape)
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
                    modifier = Modifier.fillMaxSize()
                )

                // "Has notes" indicator — lets you spot annotated entries while scanning the list
                // without opening anything (#75). Read the note itself on the detail page or in the
                // Notes journal.
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
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    }
                }
            }

            // Right Side: Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    modifier = titleSharedModifier
                )

                // Badges and Airing Info
                val showScoreChip = showScore && (entry.score ?: 0.0) > 0.0
                if (showScoreChip || config.showBehindBadge || config.showAiringInfo) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (showScoreChip) {
                            ScoreChip(score = entry.score, format = scoreFormat)
                        }

                        if (config.showBehindBadge && entry.status == LibraryStatus.CURRENT) {
                            val latest = entry.latestReleasedEpisode

                            if (latest != null && entry.progress < latest) {
                                StatusBadge(
                                    formatEpisodesBehind(latest - entry.progress),
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.onError
                                )
                            }
                        }

                        val remainingSeconds = entry.nextAiringEpisodeTime
                            ?.minus(nowEpochSec)
                            ?.coerceAtLeast(0L)
                            ?.coerceAtMost(Int.MAX_VALUE.toLong())
                            ?.toInt()
                        if (config.showAiringInfo && remainingSeconds != null && entry.nextAiringEpisode != null) {
                            val airingText = stringResource(
                                R.string.airing_episode_in,
                                entry.nextAiringEpisode,
                                formatTimeUntilAiring(remainingSeconds)
                            )
                            Text(
                                text = if (entry.nextAiringIsApproximate) {
                                    stringResource(R.string.calendar_countdown_approximate, airingText)
                                } else {
                                    airingText
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Metadata section (for completed lists)
                if (config.showMetadata) {
                    Text(
                        text = stringResource(
                            R.string.progress_format,
                            entry.progress,
                            total?.toString() ?: stringResource(R.string.progress_unknown)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Progress Bar Area
                if (config.showProgressBar) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.progress_format,
                                    entry.progress,
                                    total?.toString()
                                        ?: stringResource(R.string.progress_unknown)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            // Right Side: Adjusters / Edit Button vertically stacked
            if (config.showAdjusters && onIncrement != null && onDecrement != null) {
                val haptic = rememberHapticFeedback()
                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .bouncyClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onIncrement()
                                },
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.a11y_action_increment_progress),
                                clipShape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .bouncyClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDecrement()
                                },
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.a11y_action_decrement_progress),
                                clipShape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else if (!config.showAdjusters && onEdit != null) {
                val haptic = rememberHapticFeedback()
                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .bouncyClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEdit()
                                },
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.a11y_action_edit_entry),
                                clipShape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// PRIVATE COMPONENTS & MOCKS FOR PREVIEWS
// --------------------------------------------------------------------------------

@Composable
private fun StatusBadge(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

private fun mockEntry(
    id: Int = 1,
    title: String = "Frieren: Beyond Journey's End",
    progress: Int = 12,
    total: Int? = 28,
    status: LibraryStatus = LibraryStatus.CURRENT,
    nextAiring: Int? = 13,
    timeUntil: Int? = 86400
): LibraryEntry {
    return LibraryEntry(
        id = id,
        mediaId = id,
        titleRomaji = title,
        titleEnglish = title,
        titleNative = title,
        titleUserPreferred = title,
        coverUrl = "",
        progress = progress,
        totalEpisodes = total,
        totalChapters = null,
        totalVolumes = null,
        type = MediaType.ANIME,
        status = status,
        nextAiringEpisode = nextAiring,
        timeUntilAiring = timeUntil
    )
}

@Composable
private fun PreviewMediaCardTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings(context) }

    MaterialTheme {
        CompositionLocalProvider(LocalAppSettings provides appSettings) {
            content()
        }
    }
}

@Preview(name = "List - Anime Watching (Behind)", showBackground = true)
@Composable
private fun PreviewLibraryListCardWatchingBehind() {
    PreviewMediaCardTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LibraryListCard(
                entry = mockEntry(progress = 10, total = 24, nextAiring = 12),
                mediaType = MediaType.ANIME,
                onClick = {},
                config = WatchingCardConfig,
                onIncrement = {},
                onDecrement = {}
            )
        }
    }
}

@Preview(name = "List - Manga Completed", showBackground = true)
@Composable
private fun PreviewLibraryListCardCompleted() {
    val completedEntry = mockEntry(
        title = "Berserk",
        progress = 364,
        total = 364,
        status = LibraryStatus.COMPLETED,
        nextAiring = null,
        timeUntil = null
    ).copy(totalChapters = 364, totalEpisodes = null, type = MediaType.MANGA)

    PreviewMediaCardTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LibraryListCard(
                entry = completedEntry,
                mediaType = MediaType.MANGA,
                onClick = {},
                config = CompletedCardConfig,
                onEdit = {} // Added to preview to show the Edit button correctly
            )
        }
    }
}
