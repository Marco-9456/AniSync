package com.anisync.android.presentation.discover.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.LocalCoverQuality
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.ListIndicator
import com.anisync.android.presentation.components.ListIndicatorStyle
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.LocalAdaptiveInfo
import com.anisync.android.presentation.util.LocalLibraryStatuses
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.presentation.util.toListIcon
import com.anisync.android.ui.theme.ExpressiveShapes
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.util.getTitle
import java.util.Locale

/**
 * The banner is lit right where the title sits, so the scrim has to be doing real work by the time
 * it reaches the bottom third rather than easing in over the whole card.
 */
private val SpotlightScrim: Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    0.38f to Color.Black.copy(alpha = 0.22f),
    0.68f to Color.Black.copy(alpha = 0.68f),
    1f to Color.Black.copy(alpha = 0.95f)
)

/** Wide enough to read as a banner, short enough to leave the rail beneath it above the fold. */
private const val SpotlightAspect = 364f / 176f

/**
 * Past compact widths the aspect ratio stops being the right rule: holding 364:176 on a 1245dp
 * tablet pane makes the card ~390dp tall, which is most of the viewport and pushes the ranked rail
 * it is supposed to introduce off the bottom. A fixed height lets the banner get wider and shorter
 * instead, which is closer to the shape AniList's banner art actually is.
 */
private val SpotlightHeightWide = 260.dp

/**
 * The single card at the head of Trending.
 *
 * It replaces a 420dp ten-page carousel that took roughly 58% of the first screen to show one
 * title. This costs 176dp, carries enough to decide on (score, format, length, genres) and adds a
 * list action, and the ranked rail underneath continues from #2 so the section still has depth.
 *
 * [LibraryEntry.bannerUrl] is null for a good share of AniList titles, so the cover is not a
 * fallback of last resort, it is the common case.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverSpotlight(
    item: LibraryEntry,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    titleLanguage: TitleLanguage,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val shape = ExpressiveShapes.mediaCover
    val title = remember(item, titleLanguage) { item.getTitle(titleLanguage) }
    val coverData = item.cover.url() ?: item.coverUrl
    val artData = item.bannerUrl ?: coverData
    val cacheKey = TransitionKeys.imageCacheKey(TransitionKeys.DISCOVER_SPOTLIGHT, item.mediaId) +
        "-" + LocalCoverQuality.current.name + TransitionKeys.coverVersion(artData)

    val formattedScore = remember(item.averageScore) {
        item.averageScore?.let { String.format(Locale.US, "%.1f", it / 10.0) }
    }
    val lengthLabel = remember(item) { item.lengthLabel() }
    val genreLabel = remember(item.genres) { item.genres.take(3).joinToString(" · ") }

    val rootModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        val spec = AppMotion.rememberSpatialSpec()
        with(sharedTransitionScope) {
            modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = TransitionKeys.cover(TransitionKeys.DISCOVER_SPOTLIGHT, item.mediaId)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spec },
                clipInOverlayDuringTransition = OverlayClip(shape)
            )
        }
    } else {
        modifier
    }

    val isCompact = LocalAdaptiveInfo.current.isCompact
    Box(
        modifier = rootModifier
            .fillMaxWidth()
            .then(
                if (isCompact) Modifier.aspectRatio(SpotlightAspect)
                else Modifier.height(SpotlightHeightWide)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title)
            )
            .semantics(mergeDescendants = true) {
                contentDescription = title
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artData)
                .crossfade(200)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .placeholderMemoryCacheKey(cacheKey)
                .memoryCacheKey(cacheKey)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(SpotlightScrim))

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.discover_spotlight_rank),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        val listStatus = LocalLibraryStatuses.current[item.mediaId]
        listStatus?.let { status ->
            ListIndicator(
                status = status,
                type = item.type,
                style = ListIndicatorStyle.Chip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.76f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (formattedScore != null) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = StarGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = formattedScore,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                val meta = listOfNotNull(item.format?.toLabel(), lengthLabel, item.seasonYear?.toString())
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.86f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (genreLabel.isNotEmpty()) {
                Text(
                    text = genreLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.76f)
                )
            }
        }

        // Already on a list means there is nothing to add; the status chip above says so and
        // the button would be claiming an action it cannot perform.
        if (listStatus == null) Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(
                    onClick = onAddClick,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.discover_add_to_list)
                ),
            contentAlignment = Alignment.Center
        ) {
            // The list's own glyph, not a generic plus: the button produces a Planning entry, and
            // this is the mark that entry then carries on every cover and in the rail.
            Icon(
                imageVector = LibraryStatus.PLANNING.toListIcon(),
                contentDescription = stringResource(R.string.discover_add_to_list),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Episodes for anime, chapters (and volumes when known) for manga. AniList leaves any of them null,
 * so nothing here is guaranteed to render.
 */
private fun LibraryEntry.lengthLabel(): String? {
    val parts = buildList {
        totalEpisodes?.let { add("$it ep") }
        totalChapters?.let { add("$it ch") }
        totalVolumes?.let { add("$it vol") }
    }
    return parts.take(2).joinToString("  ·  ").ifEmpty { null }
}
