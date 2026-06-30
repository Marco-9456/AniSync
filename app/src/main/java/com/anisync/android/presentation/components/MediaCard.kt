package com.anisync.android.presentation.components

import com.anisync.android.domain.url

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.util.getTitle

/**
 * A specialized Media Card for the Discover screen.
 * Displays a poster image with content area showing title, media type, and rating.
 *
 * @param item The library entry data to display
 * @param onClick Click handler for card tap
 * @param modifier Composable modifier
 * @param sharedTransitionScope Scope for shared element animations
 * @param animatedVisibilityScope Scope for visibility animations
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaCard(
    item: LibraryEntry,
    onClick: () -> Unit,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // Use memoized motion specs from AppMotion
    val spatialSpec = AppMotion.rememberSpatialSpec()
    val title = item.getTitle(titleLanguage)
    
    // Use TransitionKeys for consistent key generation
    val coverKey = TransitionKeys.cover(TransitionKeys.DISCOVER, item.mediaId)
    val titleKey = TransitionKeys.title(TransitionKeys.DISCOVER, item.mediaId)
    val cacheKey = (TransitionKeys.imageCacheKey(TransitionKeys.DISCOVER, item.mediaId) + "-" + com.anisync.android.domain.LocalCoverQuality.current.name + TransitionKeys.coverVersion(item.cover.url() ?: item.coverUrl))

    with(sharedTransitionScope) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer, // Matches the white/light background of the design
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
                .width(150.dp)
                .bouncyClickable(
                    onClick = onClick,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.a11y_action_open_details, title)
                )
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = coverKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> spatialSpec },
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(12.dp))
                )
        ) {
            Column {
                // Image Container
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
                        .aspectRatio(0.7f) // Standard poster ratio
                )

                // Content Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Title (Bold, Black)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = titleKey),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> spatialSpec },
                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Row: Type and Rating Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Type (e.g., "TV") - Secondary Color
                        Text(
                            text = item.type?.name ?: "TV",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Rating Pill (e.g., "8.8" with Star) - Only show if score is available
                        item.averageScore?.let { score ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest, // Light grey/purple tint
                                shape = RoundedCornerShape(8.dp)
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
                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Convert from 0-100 scale to 0-10 scale for display
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
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun MediaCardPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                MediaCard(
                    item = LibraryEntry(
                        id = 1,
                        mediaId = 100,
                        titleRomaji = "Frieren: Beyond Journey's End",
                        titleEnglish = "Frieren: Beyond Journey's End",
                        titleNative = "Sousou no Frieren",
                        titleUserPreferred = "Frieren: Beyond Journey's End",
                        coverUrl = null,
                        type = MediaType.ANIME,
                        averageScore = 95,
                        mediaStatus = "FINISHED",
                        status = com.anisync.android.domain.LibraryStatus.CURRENT,
                        progress = 5,
                        totalEpisodes = 28,
                        totalChapters = null,
                        totalVolumes = null
                    ),
                    onClick = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
        }
    }
}
