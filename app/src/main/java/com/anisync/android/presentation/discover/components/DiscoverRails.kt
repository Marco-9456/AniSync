package com.anisync.android.presentation.discover.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LocalCoverQuality
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.ListIndicator
import com.anisync.android.presentation.components.ListIndicatorStyle
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.LocalLibraryStatuses
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.ui.theme.ExpressiveShapes
import com.anisync.android.util.getTitle

/** Same cover proportion the shipped poster card uses, so every rail lines up. */
private const val PosterAspect = 171f / 243f

private val CoverToTitleGap = 8.dp
private val TitleLineHeight = 20.sp
private val TitleSlack = 6.dp

/** The dark foot a cover marker sits on. */
internal val MarkerScrim: Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.92f)
)

object DiscoverRailDefaults {
    /**
     * Pin a rail to the tallest a card can get, for the same reason
     * [com.anisync.android.presentation.components.MediaPosterCardDefaults.rowHeight] does: a
     * two-line title scrolling into view would otherwise resize the row under the finger.
     */
    @Composable
    fun height(cardWidth: Dp, titleLines: Int = 2): Dp = with(LocalDensity.current) {
        // The slack matters: two lines of text measure a shade over two line heights once
        // Compose has added its line-height distribution, and without it the title silently
        // collapses to one ellipsised line.
        cardWidth / PosterAspect + CoverToTitleGap +
            TitleLineHeight.toDp() * titleLines + TitleSlack
    }
}

/**
 * A horizontal rail of covers with an optional short marker burnt into the bottom-left of each.
 *
 * The marker is what separates one rail from the next: an air time on the timeline, a release
 * season on Not yet released, a format and length on New to AniList. Every one of them comes from
 * a field the query already selects, and a rail whose data has nothing to say passes null and
 * simply draws no marker.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaMarkerRail(
    items: List<LibraryEntry>,
    cardWidth: Dp,
    transitionPrefix: String,
    titleLanguage: TitleLanguage,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    marker: @Composable (LibraryEntry) -> String? = { null },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val placementSpec = AppMotion.rememberOffsetSpatialSpec()
    val fadeSpec = AppMotion.rememberEffectsSpec()

    LazyRow(
        modifier = modifier.height(DiscoverRailDefaults.height(cardWidth)),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items,
            key = { index, item -> "${transitionPrefix}_${item.mediaId}_$index" },
            contentType = { _, _ -> "marker_card" }
        ) { _, item ->
            MarkerCard(
                item = item,
                cardWidth = cardWidth,
                transitionPrefix = transitionPrefix,
                titleLanguage = titleLanguage,
                marker = marker(item),
                onClick = remember(item.mediaId) { { onItemClick(item.mediaId) } },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.animateItem(
                    fadeInSpec = fadeSpec,
                    fadeOutSpec = fadeSpec,
                    placementSpec = placementSpec
                )
            )
        }
    }
}

/**
 * Trending #2 and down, each cover carrying its position.
 *
 * The numerals are honest here and nowhere else on the screen: TRENDING_DESC is a ranking, while
 * POPULARITY_DESC rails and the ID_DESC catalogue rail are an ordering and a recency list. Putting
 * numbers on those would claim a league table the query never produced.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TrendingRankRail(
    items: List<LibraryEntry>,
    startRank: Int,
    titleLanguage: TitleLanguage,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val cardWidth = 112.dp
    val placementSpec = AppMotion.rememberOffsetSpatialSpec()
    val fadeSpec = AppMotion.rememberEffectsSpec()

    LazyRow(
        modifier = modifier.height(DiscoverRailDefaults.height(cardWidth)),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items,
            key = { index, item -> "rank_${item.mediaId}_$index" },
            contentType = { _, _ -> "rank_card" }
        ) { index, item ->
            MarkerCard(
                item = item,
                cardWidth = cardWidth,
                transitionPrefix = TransitionKeys.DISCOVER_TRENDING,
                titleLanguage = titleLanguage,
                marker = null,
                rank = startRank + index,
                onClick = remember(item.mediaId) { { onItemClick(item.mediaId) } },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.animateItem(
                    fadeInSpec = fadeSpec,
                    fadeOutSpec = fadeSpec,
                    placementSpec = placementSpec
                )
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MarkerCard(
    item: LibraryEntry,
    cardWidth: Dp,
    transitionPrefix: String,
    titleLanguage: TitleLanguage,
    marker: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val shape = ExpressiveShapes.mediaCover
    val title = remember(item, titleLanguage) { item.getTitle(titleLanguage) }
    val coverData = item.cover.url() ?: item.coverUrl
    val cacheKey = TransitionKeys.imageCacheKey(transitionPrefix, item.mediaId) +
        "-" + LocalCoverQuality.current.name + TransitionKeys.coverVersion(coverData)

    val coverModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        val spec = AppMotion.rememberSpatialSpec()
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = TransitionKeys.cover(transitionPrefix, item.mediaId)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spec },
                clipInOverlayDuringTransition = OverlayClip(shape)
            )
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .width(cardWidth)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title)
            ),
        verticalArrangement = Arrangement.spacedBy(CoverToTitleGap)
    ) {
        Box(
            modifier = coverModifier
                .fillMaxWidth()
                .aspectRatio(PosterAspect)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverData)
                    .crossfade(200)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .placeholderMemoryCacheKey(cacheKey)
                    .memoryCacheKey(cacheKey)
                    .build(),
                contentDescription = stringResource(R.string.a11y_media_poster, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (marker != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                        .background(MarkerScrim)
                )
                Text(
                    text = marker,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                )
            }

            if (rank != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LocalLibraryStatuses.current[item.mediaId]?.let { status ->
                ListIndicator(
                    status = status,
                    type = item.type,
                    style = ListIndicatorStyle.Corner,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            lineHeight = TitleLineHeight,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Stands in for a rail whose request is in flight.
 *
 * Only reachable after the first paint, which means a retry or a tab switch. Without it the
 * section disappeared for the length of the request and reappeared afterwards, so tapping Retry
 * looked like it had deleted the section.
 */
@Composable
fun SectionSkeletonRail(
    cardWidth: Dp,
    modifier: Modifier = Modifier,
    coverHeight: Dp? = null
) {
    val height = coverHeight ?: (cardWidth / PosterAspect)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DiscoverRailDefaults.height(cardWidth))
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(height)
                    .clip(ExpressiveShapes.mediaCover)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    }
}
