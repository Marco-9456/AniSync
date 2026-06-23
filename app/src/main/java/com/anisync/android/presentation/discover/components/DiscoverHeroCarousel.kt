package com.anisync.android.presentation.discover.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LocalCoverQuality
import com.anisync.android.domain.url
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.util.getTitle

private val HeroScrimBrush: Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    0.45f to Color.Black.copy(alpha = 0.0f),
    1f to Color.Black.copy(alpha = 0.85f)
)

private const val MAX_HERO_ITEMS = 10

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun DiscoverHeroCarousel(
    items: List<LibraryEntry>,
    onItemClick: (Int) -> Unit,
    titleLanguage: TitleLanguage,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val coverQuality = LocalCoverQuality.current

    val carouselState = rememberCarouselState(initialItem = 0) { items.size }
    val itemCount = items.size

    val focusedIndex by remember(carouselState, itemCount) {
        derivedStateOf {
            if (itemCount <= 0) 0 else carouselState.currentItem.coerceIn(0, itemCount - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Trending carousel" }
    ) {
        HorizontalCenteredHeroCarousel(
            state = carouselState,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state = carouselState),
            // Cap each hero's width so a wide window shows SEVERAL heroes filling the row (M3) rather
            // than stretching one item into a cropped strip. On a phone only one fits, as before.
            maxItemWidth = 400.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) { index ->
            val item = items[index]
            val coverData = item.cover.url() ?: item.coverUrl
            val cacheKey = remember(item.mediaId, coverQuality) {
                TransitionKeys.imageCacheKey(
                    TransitionKeys.DISCOVER,
                    item.mediaId
                ) + "-" + coverQuality.name
            }
            val imageRequest = remember(coverData, cacheKey) {
                ImageRequest.Builder(context)
                    .data(coverData)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .placeholderMemoryCacheKey(cacheKey)
                    .memoryCacheKey(cacheKey)
                    .build()
            }
            HeroCarouselItem(
                item = item,
                index = index,
                total = itemCount,
                titleLanguage = titleLanguage,
                onClick = { onItemClick(item.mediaId) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                imageRequest = imageRequest,
                modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        CarouselPageIndicator(
            count = itemCount.coerceAtMost(MAX_HERO_ITEMS),
            focusedIndex = focusedIndex.coerceAtMost(MAX_HERO_ITEMS - 1),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clearAndSetSemantics {}
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HeroCarouselItem(
    item: LibraryEntry,
    index: Int,
    total: Int,
    titleLanguage: TitleLanguage,
    onClick: () -> Unit,
    imageRequest: ImageRequest,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val itemShape = MaterialTheme.shapes.extraLarge
    val spatialSpec = AppMotion.rememberSpatialSpec()

    val coverKey =
        remember(item.mediaId) { TransitionKeys.cover(TransitionKeys.DISCOVER, item.mediaId) }
    val title = remember(item, titleLanguage) { item.getTitle(titleLanguage) }
    val statusLabel = remember(item.mediaStatus) {
        item.mediaStatus?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
    }
    val formattedScore = remember(item.averageScore) {
        item.averageScore?.let { String.format(java.util.Locale.US, "%.1f", it / 10.0) }
    }
    val a11yLabel = remember(index, total, title, item.format, formattedScore) {
        buildString {
            append("Trending item ")
            append(index + 1)
            append(" of ")
            append(total)
            append(": ")
            append(title)
            item.format?.let { append(", ").append(it.name) }
            formattedScore?.let { append(", rated ").append(it) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = "View details"
            )
            .semantics(mergeDescendants = true) { contentDescription = a11yLabel }
    ) {
        with(sharedTransitionScope) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = coverKey),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        clipInOverlayDuringTransition = OverlayClip(itemShape)
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HeroScrimBrush)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item.format?.let { format ->
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = format.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                formattedScore?.let { scoreText ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = scoreText,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.4).sp
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            statusLabel?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CarouselPageIndicator(
    count: Int,
    focusedIndex: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 1) {
        Spacer(modifier = modifier.height(8.dp))
        return
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fastSpec = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.Dp>()
        repeat(count) { i ->
            val focused = i == focusedIndex
            val size by animateDpAsState(
                targetValue = if (focused) 8.dp else 6.dp,
                animationSpec = fastSpec,
                label = "indicatorDotSize"
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        if (focused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
            )
        }
    }
}
