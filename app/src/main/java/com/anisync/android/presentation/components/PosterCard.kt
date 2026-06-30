package com.anisync.android.presentation.components

import com.anisync.android.domain.url

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.util.getTitle

/**
 * A shared media card component that displays a poster image with a gradient overlay and title.
 * Used in Grids (SectionGridScreen) and horizontal lists (ProfileScreen).
 *
 * @param title Media title to display
 * @param coverUrl URL for the poster image
 * @param mediaId Unique ID for shared element transitions
 * @param onClick Click handler
 * @param sharedTransitionScope Scope for shared element animations
 * @param animatedVisibilityScope Scope for visibility animations
 * @param modifier Composable modifier
 * @param transitionPrefix Unique prefix for shared transition keys (e.g., "discover", "library") to avoid collisions
 * @param aspectRatio Aspect ratio of the card (default 0.7f for standard posters)
 * @param shape Shape of the card corners
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PosterCard(
    item: com.anisync.android.domain.LibraryEntry,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    transitionPrefix: String = TransitionKeys.POSTER,
    aspectRatio: Float = 0.7f,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    // Use memoized motion specs from AppMotion
    val spatialSpec = AppMotion.rememberSpatialSpec()
    val effectsSpec = AppMotion.rememberEffectsSpec()
    val title = item.getTitle(titleLanguage)
    
    // Use TransitionKeys for consistent key generation
    val coverKey = TransitionKeys.cover(transitionPrefix, item.mediaId)
    val gradientKey = TransitionKeys.gradient(transitionPrefix, item.mediaId)
    val titleKey = TransitionKeys.title(transitionPrefix, item.mediaId)
    val cacheKey = (TransitionKeys.imageCacheKey(transitionPrefix, item.mediaId) + "-" + com.anisync.android.domain.LocalCoverQuality.current.name + TransitionKeys.coverVersion(item.cover.url() ?: item.coverUrl))

    with(sharedTransitionScope) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
                .fillMaxWidth()
                .bouncyClickable(
                    onClick = onClick,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.a11y_action_open_details, title)
                )
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = coverKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(shape)
                )
        ) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.cover.url() ?: item.coverUrl)
                        .crossfade(true)
                        .placeholderMemoryCacheKey(cacheKey)
                        .memoryCacheKey(cacheKey)
                        .build(),
                    contentDescription = stringResource(R.string.a11y_media_poster, title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                )

                // Gradient overlay at bottom for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = gradientKey),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            enter = fadeIn(effectsSpec),
                            exit = fadeOut(effectsSpec)
                        )
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                // Title at bottom
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = titleKey),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun PosterCardPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                val item = com.anisync.android.domain.LibraryEntry(
                    id = 1,
                    mediaId = 1,
                    titleRomaji = "Attack on Titan",
                    titleEnglish = "Attack on Titan",
                    titleNative = "Shingeki no Kyojin",
                    titleUserPreferred = "Attack on Titan",
                    coverUrl = null,
                    progress = 0,
                    totalEpisodes = 25,
                    totalChapters = null,
                    totalVolumes = null,
                    type = com.anisync.android.type.MediaType.ANIME,
                    format = com.anisync.android.type.MediaFormat.TV,
                    status = com.anisync.android.domain.LibraryStatus.CURRENT
                )
                PosterCard(
                    item = item,
                    onClick = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    }
}
