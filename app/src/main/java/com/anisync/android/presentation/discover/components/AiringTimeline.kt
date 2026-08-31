package com.anisync.android.presentation.discover.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.AiringEpisode
import com.anisync.android.domain.LocalCoverQuality
import com.anisync.android.presentation.components.ListIndicator
import com.anisync.android.presentation.components.ListIndicatorStyle
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.ui.theme.ExpressiveShapes
import com.anisync.android.util.TitleUtils
import java.util.Date

private val CardWidth = 132.dp
private val ItemGap = 12.dp
private val AxisHeight = 12.dp
private val NowMarkerWidth = 48.dp
private const val PosterAspect = 171f / 243f

/**
 * Today's schedule drawn as the day itself.
 *
 * `airingSchedules` comes back sorted by TIME, so the rail is already in the order a timeline
 * wants. A spine runs the length of it, filled in behind the episodes that have gone out and left
 * as track ahead of the ones that have not, with a NOW marker breaking the line at the boundary.
 * Position along the spine is reading order, not drawn to scale: four shows all landing at 23:00
 * would otherwise sit on top of each other.
 *
 * There is no manga counterpart. AniList publishes no per-chapter release time, so the Manga tab
 * shows Releasing now in this slot rather than a timeline invented from data that does not exist.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AiringTimeline(
    episodes: List<AiringEpisode>,
    titleLanguage: TitleLanguage,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    nowEpochSec: Long = System.currentTimeMillis() / 1000,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val rowHeight = with(LocalDensity.current) {
        // Trailing 6dp is slack for the two-line title, which otherwise measures a hair over
        // two line heights and gets clamped to one.
        AxisHeight + 6.dp + 16.dp + 8.dp + (CardWidth / PosterAspect) + 8.dp +
            20.sp.toDp() * 2 + 6.dp
    }
    // Index of the first episode still to come; the marker slots in just before it.
    val nowIndex = remember(episodes, nowEpochSec) {
        episodes.indexOfFirst { it.airingAt > nowEpochSec }.let { if (it < 0) episodes.size else it }
    }
    val placementSpec = AppMotion.rememberOffsetSpatialSpec()

    // Open on the present, not on midnight. Item 0 is the lead cap and each episode before the
    // marker is one item, so the last aired episode sits at index nowIndex and starting there
    // leaves it at the left edge with the marker and everything still to come after it.
    val listState = rememberLazyListState()
    LaunchedEffect(episodes, nowIndex) {
        if (episodes.isNotEmpty()) listState.scrollToItem(nowIndex.coerceAtMost(episodes.size))
    }

    LazyRow(
        state = listState,
        modifier = modifier.height(rowHeight),
        horizontalArrangement = Arrangement.Start
    ) {
        // The spine has to reach the screen edge, so the rail carries its own end caps rather
        // than a contentPadding the line could not paint into.
        item(key = "lead", contentType = "axis_cap") {
            AxisCap(width = 18.dp, elapsed = nowIndex > 0, height = rowHeight)
        }
        episodes.forEachIndexed { index, episode ->
            if (index == nowIndex) {
                item(key = "now", contentType = "axis_now") {
                    NowMarker(height = rowHeight)
                }
            }
            item(key = "airing_${episode.id}", contentType = "airing_card") {
                AiringCard(
                    episode = episode,
                    aired = episode.airingAt <= nowEpochSec,
                    titleLanguage = titleLanguage,
                    onClick = { onItemClick(episode.mediaId) },
                    modifier = Modifier.animateItem(placementSpec = placementSpec),
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
        if (nowIndex >= episodes.size) {
            item(key = "now", contentType = "axis_now") { NowMarker(height = rowHeight) }
        }
        item(key = "tail", contentType = "axis_cap") {
            AxisCap(width = 18.dp, elapsed = nowIndex >= episodes.size, height = rowHeight)
        }
    }
}

/** A bare stretch of spine, used to run the line out to both screen edges. */
@Composable
private fun AxisCap(width: Dp, elapsed: Boolean, height: Dp) {
    Box(modifier = Modifier.width(width).height(height)) {
        Spine(elapsed = elapsed)
    }
}

@Composable
private fun BoxScope.Spine(elapsed: Boolean) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(top = AxisHeight / 2 - 1.dp)
            .height(2.dp)
            .background(
                if (elapsed) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
    )
}

@Composable
private fun NowMarker(height: Dp) {
    Box(modifier = Modifier.width(NowMarkerWidth).height(height)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)
                .height(AxisHeight + 6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.discover_airing_now),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AiringCard(
    episode: AiringEpisode,
    aired: Boolean,
    titleLanguage: TitleLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val shape = ExpressiveShapes.mediaCover
    val title = remember(episode, titleLanguage) {
        TitleUtils.getTitle(
            titleLanguage,
            episode.titleRomaji,
            episode.titleEnglish,
            episode.titleNative,
            episode.titleUserPreferred
        )
    }
    val timeText = remember(context, episode.airingAt) {
        DateFormat.getTimeFormat(context).format(Date(episode.airingAt * 1000))
    }
    val cacheKey = TransitionKeys.imageCacheKey(TransitionKeys.DISCOVER_AIRING, episode.mediaId) +
        "-" + LocalCoverQuality.current.name + TransitionKeys.coverVersion(episode.coverImageUrl)

    val coverModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        val spec = AppMotion.rememberSpatialSpec()
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = TransitionKeys.cover(TransitionKeys.DISCOVER_AIRING, episode.mediaId)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spec },
                clipInOverlayDuringTransition = OverlayClip(shape)
            )
        }
    } else {
        Modifier
    }

    // The gap lives inside the item so consecutive spine segments meet; a LazyRow arrangement gap
    // would leave a hole in the line.
    Box(modifier = modifier.width(CardWidth + ItemGap)) {
        Spine(elapsed = aired)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ItemGap / 2)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(AxisHeight)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = AxisHeight / 2 - 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (aired) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (aired) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PosterAspect)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .bouncyClickable(
                        onClick = onClick,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                        clipShape = shape
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(episode.coverImageUrl)
                        .crossfade(200)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = stringResource(R.string.a11y_media_poster, title),
                    contentScale = ContentScale.Crop,
                    modifier = coverModifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                        .background(MarkerScrim)
                )
                Text(
                    text = stringResource(R.string.discover_episode_short, episode.episode),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
                episode.listStatus?.let { status ->
                    ListIndicator(
                        status = status,
                        type = com.anisync.android.type.MediaType.ANIME,
                        style = ListIndicatorStyle.Corner,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
