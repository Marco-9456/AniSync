package com.anisync.android.presentation.discover.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.formatChaptersCount
import com.anisync.android.presentation.util.formatEpisodesCount
import com.anisync.android.presentation.util.toLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CinematicHeroCarousel(
    items: List<LibraryEntry>,
    onItemClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val carouselState = rememberCarouselState { items.size }

    HorizontalCenteredHeroCarousel(
        state = carouselState,
        modifier = Modifier
            .height(380.dp)
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 48.dp),
        itemSpacing = 24.dp,
        flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state = carouselState)
    ) { itemIndex ->
        val item = items[itemIndex]
        HeroCard(
            item = item,
            onClick = { onItemClick(item.mediaId) },
            modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroCard(
    item: LibraryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    with(sharedTransitionScope) {
        Card(
            modifier = modifier
                .height(380.dp)
                .bouncyClickable(onClick = onClick)
                .sharedElement(
                    sharedContentState = rememberSharedContentState(key = "discover_media_cover_${item.mediaId}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(MaterialTheme.shapes.extraLarge)
                ),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val cacheKey = "discover_cover_${item.mediaId}"
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.coverUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "discover_gradient_${item.mediaId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            enter = fadeIn(effectsSpec),
                            exit = fadeOut(effectsSpec)
                        )
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black
                                )
                            )
                        )
                )
                Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.format?.toLabel() ?: stringResource(R.string.media_type_anime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "discover_media_title_${item.mediaId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                )

                val statusText = item.mediaStatus.formatAsTitle()
                val countsText = if (item.totalEpisodes != null) formatEpisodesCount(item.totalEpisodes) else if (item.totalChapters != null) formatChaptersCount(item.totalChapters) else null

                if (statusText != null || countsText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = listOfNotNull(statusText, countsText).joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            }
        }
    }
}
