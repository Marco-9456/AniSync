package com.anisync.android.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.formatEpisodesBehind
import com.anisync.android.presentation.util.formatTimeUntilAiring
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.type.MediaType

/**
 * Configuration for the LibraryMediaCard content display.
 */
data class LibraryCardConfig(
    val showProgressBar: Boolean = true,
    val showAdjusters: Boolean = true,
    val showAiringInfo: Boolean = true,
    val showBehindBadge: Boolean = true,
    val showMetadata: Boolean = false
)

/**
 * Default configuration for cards in watching/reading lists.
 */
val WatchingCardConfig = LibraryCardConfig(
    showProgressBar = true,
    showAdjusters = true,
    showAiringInfo = true,
    showBehindBadge = true
)

/**
 * Configuration for completed/other list cards (no controls).
 */
val CompletedCardConfig = LibraryCardConfig(
    showProgressBar = false,
    showAdjusters = false,
    showAiringInfo = false,
    showBehindBadge = false,
    showMetadata = true
)

/**
 * A reusable media card for library screens.
 * Configurable for different list contexts (Watching, Completed, etc).
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryMediaCard(
    entry: LibraryEntry,
    mediaType: MediaType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: LibraryCardConfig = WatchingCardConfig,
    onIncrement: (() -> Unit)? = null,
    onDecrement: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val progressPercent = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "Progress"
    )

    val cardModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            modifier
                .fillMaxWidth()
                .height(if (config.showAdjusters) 340.dp else 280.dp)
                .bouncyClickable(onClick = onClick)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "library_container_${entry.mediaId}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                )
        }
    } else {
        modifier
            .fillMaxWidth()
            .height(if (config.showAdjusters) 340.dp else 280.dp)
            .bouncyClickable(onClick = onClick)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = cardModifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Image section
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val cacheKey = "library_cover_${entry.mediaId}"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(entry.coverUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 200f
                            )
                        )
                )
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                )
            }

            // Content section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Badges and airing info
                if (config.showBehindBadge || config.showAiringInfo) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (config.showBehindBadge && entry.status == LibraryStatus.CURRENT) {
                            val nextAiring = entry.nextAiringEpisode
                            val latest = if (nextAiring != null) nextAiring - 1 else total

                            if (latest != null && entry.progress < latest) {
                                StatusBadge(
                                    formatEpisodesBehind(latest - entry.progress),
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.onError
                                )
                            } else {
                                StatusBadge(
                                    stringResource(R.string.badge_up_to_date),
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (config.showAiringInfo && entry.timeUntilAiring != null && entry.nextAiringEpisode != null) {
                            Text(
                                text = stringResource(
                                    R.string.airing_episode_in,
                                    entry.nextAiringEpisode,
                                    formatTimeUntilAiring(entry.timeUntilAiring)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Progress bar
                if (config.showProgressBar) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        )
                        Text(
                            text = stringResource(
                                R.string.progress_format,
                                entry.progress,
                                total?.toString() ?: stringResource(R.string.progress_unknown)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Adjusters - only render when enabled and callbacks provided
            if (config.showAdjusters && onIncrement != null && onDecrement != null) {
                // Haptic feedback is only needed when adjusters are shown
                val haptic = rememberHapticFeedback()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .bouncyClickable {
                                haptic.click()
                                onDecrement()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .bouncyClickable {
                                haptic.click()
                                onIncrement()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Configuration for previews without haptic feedback dependency.
 */
private val PreviewCardConfig = LibraryCardConfig(
    showProgressBar = true,
    showAdjusters = false, // Disable adjusters to avoid LocalAppSettings requirement
    showAiringInfo = false,
    showBehindBadge = false,
    showMetadata = false
)

@Preview(showBackground = true)
@Composable
private fun LibraryMediaCardWatchingPreview() {
    MaterialTheme {
        LibraryMediaCard(
            entry = LibraryEntry(
                id = 1,
                mediaId = 1,
                title = "Attack on Titan: Final Season Part 3",
                coverUrl = null,
                progress = 8,
                totalEpisodes = 12,
                totalChapters = null,
                totalVolumes = null,
                type = MediaType.ANIME,
                status = LibraryStatus.CURRENT,
                nextAiringEpisode = 10,
                timeUntilAiring = 172800
            ),
            mediaType = MediaType.ANIME,
            onClick = {},
            config = PreviewCardConfig,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryMediaCardCompletedPreview() {
    MaterialTheme {
        LibraryMediaCard(
            entry = LibraryEntry(
                id = 2,
                mediaId = 2,
                title = "Steins;Gate",
                coverUrl = null,
                progress = 24,
                totalEpisodes = 24,
                totalChapters = null,
                totalVolumes = null,
                type = MediaType.ANIME,
                status = LibraryStatus.COMPLETED
            ),
            mediaType = MediaType.ANIME,
            onClick = {},
            config = CompletedCardConfig,
            modifier = Modifier.padding(16.dp)
        )
    }
}

