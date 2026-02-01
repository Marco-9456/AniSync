package com.anisync.android.presentation.discover.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.FiniteAnimationSpec
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatAsTitle
import com.anisync.android.presentation.util.formatChaptersCount
import com.anisync.android.presentation.util.formatEpisodesCount
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.util.getTitle
import java.util.Locale

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
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    transitionPrefix: String = TransitionKeys.DISCOVER
) {
    val cardShape = remember(style) {
        when (style) {
            is CardStyle.Hero -> RoundedCornerShape(28.dp) // extraLarge
            is CardStyle.Standard, is CardStyle.Grid -> RoundedCornerShape(24.dp)
            is CardStyle.ListItem -> RoundedCornerShape(16.dp)
        }
    }

    val sizeModifier = remember(style) {
        when (style) {
            is CardStyle.Hero -> Modifier.height(style.height)
            is CardStyle.Standard -> Modifier.width(style.width).aspectRatio(0.6f)
            is CardStyle.Grid -> Modifier.fillMaxWidth().aspectRatio(style.aspectRatio)
            is CardStyle.ListItem -> Modifier.fillMaxWidth().height(120.dp)
        }
    }

    // Use memoized motion specs from AppMotion (only when shared transition is enabled)
    val spatialSpec = if (sharedTransitionScope != null) AppMotion.rememberSpatialSpec() else null
    val effectsSpec = if (sharedTransitionScope != null) AppMotion.rememberEffectsSpec() else null
    
    // Use TransitionKeys for consistent key generation
    val coverKey = TransitionKeys.cover(transitionPrefix, item.mediaId)
    val gradientKey = TransitionKeys.gradient(transitionPrefix, item.mediaId)
    val titleKey = TransitionKeys.title(transitionPrefix, item.mediaId)
    val cacheKey = TransitionKeys.imageCacheKey(transitionPrefix, item.mediaId)

    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val title = item.getTitle(titleLanguage)
    val baseModifier = modifier
        .then(sizeModifier)
        .clip(cardShape)
        .border(
            width = 1.dp,
            color = outlineVariant.copy(alpha = 0.5f),
            shape = cardShape
        )
        .bouncyClickable(
            onClick = onClick,
            role = Role.Button,
            onClickLabel = stringResource(R.string.a11y_action_open_details, title)
        )

    // Apply shared element transition if scopes are provided
    val cardModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null) {
        with(sharedTransitionScope) {
            baseModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = coverKey),
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
                titleLanguage = titleLanguage,
                titleKey = titleKey,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                spatialSpec = spatialSpec
            )
            else -> ImmersiveCardContent(
                item = item,
                style = style,
                transitionPrefix = transitionPrefix,
                titleLanguage = titleLanguage,
                cacheKey = cacheKey,
                gradientKey = gradientKey,
                titleKey = titleKey,
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
    titleLanguage: TitleLanguage,
    cacheKey: String,
    gradientKey: String,
    titleKey: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    spatialSpec: FiniteAnimationSpec<Rect>?,
    effectsSpec: FiniteAnimationSpec<Float>?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background Image
        val context = LocalContext.current
        val imageRequest = remember(item.coverUrl, cacheKey) {
            ImageRequest.Builder(context)
                .data(item.coverUrl)
                .crossfade(true)
                .placeholderMemoryCacheKey(cacheKey)
                .memoryCacheKey(cacheKey)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = item.getTitle(titleLanguage),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Gradient Overlay with shared bounds
        val gradientModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null && effectsSpec != null) {
            with(sharedTransitionScope) {
                Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = gradientKey),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ -> spatialSpec },
                        enter = fadeIn(effectsSpec),
                        exit = fadeOut(effectsSpec)
                    )
            }
        } else {
            Modifier.fillMaxSize()
        }

        val brush = remember {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.1f),
                    Color.Black.copy(alpha = 0.8f),
                    Color.Black
                )
            )
        }

        Box(
            modifier = gradientModifier.background(brush)
        )

        // 3. Top Badge (status for upcoming content)
        val statusBadge = remember(item.mediaStatus) { item.mediaStatus?.formatAsTitle() }
        if (statusBadge != null && style is CardStyle.Grid) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
        val contentPadding = remember(style) {
            if (style is CardStyle.Hero) 24.dp else 16.dp
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
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = formatLabel,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Title with shared bounds
            val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null) {
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

            Text(
                text = item.getTitle(titleLanguage),
                color = Color.White,
                style = when (style) {
                    is CardStyle.Hero -> MaterialTheme.typography.headlineSmall
                    else -> MaterialTheme.typography.titleMedium
                },
                fontWeight = if (style is CardStyle.Hero) FontWeight.Black else FontWeight.Bold,
                maxLines = if (style is CardStyle.Hero) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle / Metadata
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (style is CardStyle.Hero) {
                    val statusText = item.mediaStatus?.formatAsTitle()
                    val episodesText = item.totalEpisodes?.let { formatEpisodesCount(it) }
                    val chaptersText = item.totalChapters?.let { formatChaptersCount(it) }

                    val metadataText = remember(statusText, episodesText, chaptersText) {
                        val countsText = episodesText ?: chaptersText
                        listOfNotNull(statusText, countsText).joinToString(" • ")
                    }
                    if (metadataText.isNotEmpty()) {
                        Text(
                            text = metadataText,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    item.averageScore?.let { score ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", score / 10.0),
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

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListItemContent(
    item: LibraryEntry,
    transitionPrefix: String,
    titleLanguage: TitleLanguage,
    titleKey: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    spatialSpec: FiniteAnimationSpec<Rect>?
) {
    val title = item.getTitle(titleLanguage)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        val context = LocalContext.current
        val imageRequest = remember(item.coverUrl) {
            ImageRequest.Builder(context)
                .data(item.coverUrl)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.a11y_media_poster, title),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(width = 60.dp, height = 90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Format label
            item.format?.let { format ->
                val formatLabel = format.toLabel().uppercase()
                Text(
                    text = formatLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Title with shared bounds
            val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null) {
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

            Text(
                text = item.getTitle(titleLanguage),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier
            )

            // Status
            val statusText = remember(item.mediaStatus) { item.mediaStatus?.formatAsTitle() }
            if (statusText != null) {
                Text(
                    text = statusText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
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
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = String.format(Locale.US, "%.1f", score / 10.0),
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
    titleRomaji = "Frieren: Beyond Journey's End",
    titleEnglish = "Frieren: Beyond Journey's End",
    titleNative = "Sousou no Frieren",
    titleUserPreferred = "Frieren: Beyond Journey's End",
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
                            titleRomaji = "Blue Exorcist: Shimane Illuminati Saga",
                            titleEnglish = "Blue Exorcist: Shimane Illuminati Saga",
                            titleNative = "Ao no Exorcist",
                            titleUserPreferred = "Blue Exorcist: Shimane Illuminati Saga",
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
                            titleRomaji = "The Ronin",
                            titleEnglish = "The Ronin",
                            titleNative = "The Ronin",
                            titleUserPreferred = "The Ronin",
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
