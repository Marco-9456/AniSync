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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    showProgressBar = true,
    showAdjusters = false,
    showAiringInfo = false,
    showBehindBadge = false,
    showMetadata = false
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
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: LibraryCardConfig = WatchingCardConfig,
    onIncrement: (() -> Unit)? = null,
    onDecrement: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    // Use memoized motion specs from AppMotion
    val spatialSpec = AppMotion.rememberSpatialSpec()
    val effectsSpec = AppMotion.rememberSlowEffectsSpec()

    // Use TransitionKeys for consistent key generation
    val containerKey = TransitionKeys.container(TransitionKeys.LIBRARY, entry.mediaId)
    val coverKey = TransitionKeys.cover(TransitionKeys.LIBRARY, entry.mediaId)
    val titleKey = TransitionKeys.title(TransitionKeys.LIBRARY, entry.mediaId)
    val cacheKey = TransitionKeys.imageCacheKey(TransitionKeys.LIBRARY, entry.mediaId)

    val total: Int? = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val progressPercent = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f
    val title = entry.getTitle(titleLanguage)

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = effectsSpec,
        label = "Progress"
    )

    val cardModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            modifier
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
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = containerKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                )
        }
    } else {
        modifier
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
    }

    val coverShape = RoundedCornerShape(16.dp)
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
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f) // Ensure image is never cropped differently across lists
                    .then(coverSharedModifier)
                    .clip(coverShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(entry.coverUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = stringResource(R.string.a11y_media_poster, title),
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .then(titleSharedModifier)
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(20.dp) // Reserve fixed height matching badge/text
                    ) {
                        if (config.showBehindBadge && entry.status == LibraryStatus.CURRENT) {
                            val nextAiring = entry.nextAiringEpisode
                            val latest: Int? = if (nextAiring != null) nextAiring - 1 else total

                            if (latest != null && entry.progress < latest) {
                                StatusBadge(
                                    formatEpisodesBehind(latest - entry.progress),
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.onError
                                )
                            }
                        }

                        if (config.showAiringInfo && entry.dynamicTimeUntilAiring != null && entry.nextAiringEpisode != null) {
                            Text(
                                text = stringResource(
                                    R.string.airing_episode_in,
                                    entry.nextAiringEpisode ?: 0, // safe fallback for preview
                                    formatTimeUntilAiring(entry.dynamicTimeUntilAiring ?: 0)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                // Edit button for non-adjuster cards
                if (!config.showAdjusters && onEdit != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val haptic = rememberHapticFeedback()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp), // Adjust padding as needed
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f) // Fill width like adjusters
                                .height(48.dp) // Minimum touch target
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
                                    contentDescription = null, // Label is on Surface
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
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
                            .height(48.dp) // Minimum touch target
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
                                contentDescription = stringResource(R.string.a11y_action_decrement_progress),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp) // Minimum touch target
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.a11y_action_increment_progress),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// PREVIEWS
// --------------------------------------------------------------------------------

/**
 * Mock data generator for Previews.
 * Adjust the constructor arguments to match your actual LibraryEntry definition.
 */
private fun mockEntry(
    id: Int = 1,
    title: String = "Frieren: Beyond Journey's End",
    progress: Int = 12,
    total: Int? = 28,
    status: LibraryStatus = LibraryStatus.CURRENT,
    nextAiring: Int? = 13,
    timeUntil: Int? = 86400 // 1 day
): LibraryEntry {
    // We construct a mock object here.
    // NOTE: This assumes LibraryEntry is a data class.
    // You may need to update this to match your actual Domain model constructor.
    return LibraryEntry(
        id = id,
        mediaId = id,
        titleRomaji = title,
        titleEnglish = title,
        titleNative = title,
        titleUserPreferred = title,
        coverUrl = "", // Empty for preview
        progress = progress,
        totalEpisodes = total, // Assuming anime for default
        totalChapters = null,
        totalVolumes = null,
        type = MediaType.ANIME,
        status = status,
        nextAiringEpisode = nextAiring,
        timeUntilAiring = timeUntil,
        // Add other required fields with dummy values if your model has them
    )
}

// Simplified mock StatusBadge for preview context if it's not available in this file scope
@Composable
private fun StatusBadge(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PreviewMediaCardTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appSettings = androidx.compose.runtime.remember { AppSettings(context) }

    MaterialTheme {
        CompositionLocalProvider(LocalAppSettings provides appSettings) {
            content()
        }
    }
}

@Preview(name = "Anime - Watching (Behind)", showBackground = true)
@Composable
private fun PreviewLibraryCardWatchingBehind() {
    PreviewMediaCardTheme {
        Box(modifier = Modifier
            .padding(16.dp)
            .width(200.dp)) {
            LibraryMediaCard(
                entry = mockEntry(progress = 10, total = 24, nextAiring = 12),
                mediaType = MediaType.ANIME,
                titleLanguage = TitleLanguage.ROMAJI,
                onClick = {},
                config = WatchingCardConfig,
                onIncrement = {},
                onDecrement = {}
            )
        }
    }
}

@Preview(name = "Anime - Up To Date", showBackground = true)
@Composable
private fun PreviewLibraryCardUpToDate() {
    PreviewMediaCardTheme {
        Box(modifier = Modifier
            .padding(16.dp)
            .width(200.dp)) {
            LibraryMediaCard(
                entry = mockEntry(progress = 11, total = 24, nextAiring = 12),
                mediaType = MediaType.ANIME,
                titleLanguage = TitleLanguage.ROMAJI,
                onClick = {},
                config = WatchingCardConfig,
                onIncrement = {},
                onDecrement = {}
            )
        }
    }
}

@Preview(name = "Manga - Completed", showBackground = true)
@Composable
private fun PreviewLibraryCardCompleted() {
    val completedEntry = mockEntry(
        title = "Berserk",
        progress = 364,
        total = 364,
        status = LibraryStatus.COMPLETED,
        nextAiring = null,
        timeUntil = null
    ).copy(totalChapters = 364, totalEpisodes = null, type = MediaType.MANGA)

    PreviewMediaCardTheme {
        Box(modifier = Modifier
            .padding(16.dp)
            .width(200.dp)) {
            LibraryMediaCard(
                entry = completedEntry,
                mediaType = MediaType.MANGA,
                titleLanguage = TitleLanguage.ROMAJI,
                onClick = {},
                config = CompletedCardConfig
            )
        }
    }
}
