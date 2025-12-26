package com.anisync.android.presentation.discover.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.formatEpisodesCount
import com.anisync.android.presentation.util.formatChaptersCount
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.StarGold

/**
 * Sealed class defining the card style variants for the unified DiscoverMediaCard.
 */
sealed class CardStyle {
    /** Large cinematic hero card for carousels (380dp tall) */
    data class Hero(val height: Dp = 380.dp) : CardStyle()
    
    /** Standard vertical card for horizontal lists (150dp wide) */
    data class Standard(val width: Dp = 150.dp) : CardStyle()
    
    /** Grid card that fills available width with aspect ratio */
    data class Grid(val aspectRatio: Float = 0.7f) : CardStyle()
    
    /** Horizontal list item for search results */
    data object ListItem : CardStyle()
}

/**
 * A unified, customizable media card component for the Discover screen.
 * Consolidates Hero, Standard, Grid, and ListItem card variants into a single component.
 *
 * @param item The LibraryEntry to display
 * @param style The card style variant to render
 * @param onClick Click handler for the card
 * @param modifier Modifier for the card container
 * @param sharedTransitionScope Optional scope for shared element transitions
 * @param animatedVisibilityScope Optional scope for visibility animations
 * @param transitionPrefix Unique prefix for shared transition keys to avoid collisions
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiscoverMediaCard(
    item: LibraryEntry,
    style: CardStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    transitionPrefix: String = "discover"
) {
    val cardShape = when (style) {
        is CardStyle.Hero -> MaterialTheme.shapes.extraLarge
        is CardStyle.Standard, is CardStyle.Grid -> RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large))
        is CardStyle.ListItem -> RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    }

    val sizeModifier = when (style) {
        is CardStyle.Hero -> Modifier.height(style.height)
        is CardStyle.Standard -> Modifier.width(style.width).aspectRatio(0.6f)
        is CardStyle.Grid -> Modifier.fillMaxWidth().aspectRatio(style.aspectRatio)
        is CardStyle.ListItem -> Modifier.fillMaxWidth().height(120.dp)
    }

    val spatialSpec = if (sharedTransitionScope != null) {
        @Suppress("UNCHECKED_CAST")
        MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    } else null

    val effectsSpec = if (sharedTransitionScope != null) {
        @Suppress("UNCHECKED_CAST")
        MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    } else null

    val baseModifier = modifier
        .then(sizeModifier)
        .clip(cardShape)
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            shape = cardShape
        )
        .bouncyClickable(onClick = onClick)

    // Apply shared element transition if scopes are provided
    val cardModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null) {
        with(sharedTransitionScope) {
            baseModifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "${transitionPrefix}_media_cover_${item.mediaId}"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                clipInOverlayDuringTransition = OverlayClip(cardShape)
            )
        }
    } else {
        baseModifier
    }

    Surface(
        modifier = cardModifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = cardShape
    ) {
        when (style) {
            is CardStyle.ListItem -> ListItemContent(
                item = item,
                transitionPrefix = transitionPrefix,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                spatialSpec = spatialSpec
            )
            else -> ImmersiveCardContent(
                item = item,
                style = style,
                transitionPrefix = transitionPrefix,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                spatialSpec = spatialSpec,
                effectsSpec = effectsSpec
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImmersiveCardContent(
    item: LibraryEntry,
    style: CardStyle,
    transitionPrefix: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    spatialSpec: androidx.compose.animation.core.FiniteAnimationSpec<Rect>?,
    effectsSpec: androidx.compose.animation.core.FiniteAnimationSpec<Float>?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background Image
        val cacheKey = "${transitionPrefix}_cover_${item.mediaId}"
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.coverUrl)
                .crossfade(true)
                .placeholderMemoryCacheKey(cacheKey)
                .memoryCacheKey(cacheKey)
                .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
        )

        // 2. Gradient Overlay with shared bounds
        val gradientModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null && effectsSpec != null) {
            with(sharedTransitionScope) {
                Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "${transitionPrefix}_gradient_${item.mediaId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        enter = fadeIn(effectsSpec),
                        exit = fadeOut(effectsSpec)
                    )
            }
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = gradientModifier.background(
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

        // 3. Top Badge (status for upcoming content)
        val statusBadge = item.mediaStatus?.formatAsTitle()
        if (statusBadge != null && style is CardStyle.Grid) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(dimensionResource(R.dimen.spacing_normal))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small)))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small))
                    )
                    .padding(horizontal = dimensionResource(R.dimen.spacing_small), vertical = dimensionResource(R.dimen.spacing_tiny))
            ) {
                Text(
                    text = statusBadge,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Content Area
        val contentPadding = when (style) {
            is CardStyle.Hero -> dimensionResource(R.dimen.spacing_large)
            else -> dimensionResource(R.dimen.spacing_medium)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(contentPadding)
                .fillMaxWidth()
        ) {
            // Type Chip (Hero style only)
            if (style is CardStyle.Hero && item.format != null) {
                val formatLabel = item.format.toLabel()
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small))
                ) {
                    Text(
                        text = formatLabel,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.spacing_small),
                            vertical = dimensionResource(R.dimen.spacing_tiny)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            }

            // Title with shared bounds
            val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "${transitionPrefix}_media_title_${item.mediaId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                }
            } else {
                Modifier
            }

            Text(
                text = item.title,
                color = Color.White,
                style = when (style) {
                    is CardStyle.Hero -> MaterialTheme.typography.headlineSmall
                    else -> MaterialTheme.typography.titleMedium
                },
                fontWeight = when (style) {
                    is CardStyle.Hero -> FontWeight.Black
                    else -> FontWeight.Bold
                },
                maxLines = if (style is CardStyle.Hero) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))

            // Subtitle / Metadata
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (style) {
                    is CardStyle.Hero -> {
                        // Show status and episode/chapter count for hero
                        val statusText = item.mediaStatus?.formatAsTitle()
                        val countsText = when {
                            item.totalEpisodes != null -> formatEpisodesCount(item.totalEpisodes)
                            item.totalChapters != null -> formatChaptersCount(item.totalChapters)
                            else -> null
                        }
                        val metadataText = listOfNotNull(statusText, countsText).joinToString(" • ")
                        if (metadataText.isNotEmpty()) {
                            Text(
                                text = metadataText,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {
                        // Show rating for Standard/Grid
                        item.averageScore?.let { score ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StarGold,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_tiny))
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_tiny)))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", score / 10.0),
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard card layout: Image on top, content area below with title + type + rating pill.
 * This matches the original MediaCard design used in horizontal lists.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StandardCardContent(
    item: LibraryEntry,
    transitionPrefix: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    spatialSpec: androidx.compose.animation.core.FiniteAnimationSpec<Rect>?
) {
    Column {
        // Image Container
        val cacheKey = "${transitionPrefix}_cover_${item.mediaId}"
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.coverUrl)
                .crossfade(true)
                .placeholderMemoryCacheKey(cacheKey)
                .memoryCacheKey(cacheKey)
                .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f) // Standard poster ratio
        )

        // Content Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_normal))
        ) {
            // Title with shared bounds
            val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "${transitionPrefix}_media_title_${item.mediaId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                }
            } else {
                Modifier
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = titleModifier
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

            // Bottom Row: Type and Rating Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Type (e.g., "TV", "ANIME")
                Text(
                    text = item.type?.name ?: "TV",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Rating Pill with Star
                item.averageScore?.let { score ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = StarGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_tiny)))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", score / 10.0),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListItemContent(
    item: LibraryEntry,
    transitionPrefix: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    spatialSpec: androidx.compose.animation.core.FiniteAnimationSpec<Rect>?
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(dimensionResource(R.dimen.spacing_normal)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(width = 60.dp, height = 90.dp)
                .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Format label
            item.format?.let { format ->
                Text(
                    text = format.toLabel().uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_tiny))
                )
            }

            // Title with shared bounds
            val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "${transitionPrefix}_media_title_${item.mediaId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
                }
            } else {
                Modifier
            }

            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier
            )

            // Status
            val statusText = item.mediaStatus?.formatAsTitle()
            if (statusText != null) {
                Text(
                    text = statusText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_tiny))
                )
            }
        }

        // Rating
        item.averageScore?.let { score ->
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = StarGold,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_tiny))
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", score / 10.0),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- PREVIEWS ---

private val previewItem = LibraryEntry(
    id = 1,
    mediaId = 100,
    title = "Frieren: Beyond Journey's End",
    coverUrl = null,
    progress = 5,
    totalEpisodes = 28,
    totalChapters = null,
    totalVolumes = null,
    type = MediaType.ANIME,
    format = MediaFormat.TV,
    status = LibraryStatus.CURRENT,
    mediaStatus = "RELEASING",
    averageScore = 95
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Hero Card", showBackground = true, backgroundColor = 0xFF12140E)
@Composable
private fun HeroCardStylePreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                val animatedScope = this
                Box(modifier = Modifier.padding(16.dp)) {
                    DiscoverMediaCard(
                        item = previewItem,
                        style = CardStyle.Hero(),
                        onClick = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = animatedScope
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Standard Card", showBackground = true, backgroundColor = 0xFF12140E)
@Composable
private fun StandardCardStylePreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                val animatedScope = this
                Box(modifier = Modifier.padding(16.dp)) {
                    DiscoverMediaCard(
                        item = previewItem,
                        style = CardStyle.Standard(),
                        onClick = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = animatedScope
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Grid Card", showBackground = true, backgroundColor = 0xFF12140E)
@Composable
private fun GridCardStylePreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                val animatedScope = this
                Box(modifier = Modifier.padding(16.dp).width(200.dp)) {
                    DiscoverMediaCard(
                        item = previewItem.copy(
                            mediaStatus = "NOT_YET_RELEASED",
                            title = "Blue Exorcist: Shimane Illuminati Saga"
                        ),
                        style = CardStyle.Grid(),
                        onClick = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = animatedScope
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "List Item", showBackground = true, backgroundColor = 0xFF12140E)
@Composable
private fun ListItemStylePreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                val animatedScope = this
                Box(modifier = Modifier.padding(16.dp)) {
                    DiscoverMediaCard(
                        item = previewItem.copy(
                            format = MediaFormat.MOVIE,
                            title = "The Ronin",
                            mediaStatus = "FINISHED"
                        ),
                        style = CardStyle.ListItem,
                        onClick = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = animatedScope
                    )
                }
            }
        }
    }
}