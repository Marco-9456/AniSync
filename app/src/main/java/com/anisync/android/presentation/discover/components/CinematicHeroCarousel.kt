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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.ui.theme.StarGold
import android.content.Context

/**
 * A cinematic Hero Carousel component rewritten from scratch according to
 * Material Design 3 guidelines for high-emphasis featured content.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CinematicHeroCarousel(
    items: List<LibraryEntry>,
    onItemClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val carouselState = rememberCarouselState { items.size }

    HorizontalCenteredHeroCarousel(
        state = carouselState,
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        itemSpacing = 16.dp,
        flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state = carouselState)
    ) { index ->
        val item = items[index]
        
        HeroCarouselItem(
            item = item,
            context = context,
            onClick = { onItemClick(item.mediaId) },
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HeroCarouselItem(
    item: LibraryEntry,
    context: Context,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val cacheKey = remember(item.mediaId) { "hero_cover_${item.mediaId}" }
            val imageRequest = remember(item.coverUrl, cacheKey) {
                ImageRequest.Builder(context)
                    .data(item.coverUrl)
                    .crossfade(true)
                    .placeholderMemoryCacheKey(cacheKey)
                    .memoryCacheKey(cacheKey)
                    .build()
            }

            with(sharedTransitionScope) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "hero_art_${item.mediaId}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }

            val brush = remember {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.9f)
                    ),
                    startY = 0.35f
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item.format?.let { format ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = format.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    item.averageScore?.let { score ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", score / 10.0),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                with(sharedTransitionScope) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "hero_title_${item.mediaId}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                }

                item.mediaStatus?.let { status ->
                    val statusLabel = remember(status) {
                        status.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() }
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
