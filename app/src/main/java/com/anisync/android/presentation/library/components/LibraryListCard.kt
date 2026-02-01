package com.anisync.android.presentation.library.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
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
import com.anisync.android.presentation.components.StatusBadge
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatEpisodesBehind
import com.anisync.android.presentation.util.formatTimeUntilAiring
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType
import com.anisync.android.util.getTitle

/**
 * List view card for library entries.
 * Displays media cover, title, progress, and increment/decrement buttons.
 * Used when the library is in list view mode (non-grid).
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryListCard(
    entry: LibraryEntry,
    mediaType: MediaType,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    modifier: Modifier = Modifier
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val haptic = rememberHapticFeedback()
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val progressPercent = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f
    val title = entry.getTitle(titleLanguage)

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "Progress"
    )

    with(sharedTransitionScope) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(110.dp)
                .bouncyClickable(
                    onClick = onClick,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                    clipShape = RoundedCornerShape(16.dp)
                )
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "library_container_${entry.mediaId}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                )
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val cacheKey = "library_cover_${entry.mediaId}"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(entry.coverUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = stringResource(R.string.a11y_media_poster, title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "library_media_title_${entry.mediaId}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> spatialSpec },
                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AiringStatusRow(entry = entry, mediaType = mediaType)
                    }
                    ProgressRow(
                        progress = animatedProgress,
                        current = entry.progress,
                        total = total
                    )
                }

                ProgressButtons(
                    onIncrement = { haptic.click(); onIncrement() },
                    onDecrement = { haptic.click(); onDecrement() },
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AiringStatusRow(
    entry: LibraryEntry,
    mediaType: MediaType,
    modifier: Modifier = Modifier
) {
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        val nextAiring = entry.nextAiringEpisode
        val latest = if (nextAiring != null) nextAiring - 1 else total
        
        if (entry.status == LibraryStatus.CURRENT) {
            if (latest != null && entry.progress < latest) {
                StatusBadge(
                    formatEpisodesBehind(latest - entry.progress),
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer
                )
            } else {
                StatusBadge(
                    stringResource(R.string.badge_up_to_date),
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        if (entry.timeUntilAiring != null && entry.nextAiringEpisode != null) {
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
}

@Composable
private fun ProgressRow(
    progress: Float,
    current: Int,
    total: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.progress_format,
                current,
                total?.toString() ?: stringResource(R.string.progress_unknown)
            ),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ProgressButtons(
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .bouncyClickable(
                    onClick = onIncrement,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.a11y_action_increment_progress),
                    clipShape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.a11y_action_increment_progress),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .bouncyClickable(
                    onClick = onDecrement,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.a11y_action_decrement_progress),
                    clipShape = RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = stringResource(R.string.a11y_action_decrement_progress),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
