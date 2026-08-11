package com.anisync.android.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.LocalLibraryStatuses
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.util.getTitle
import java.util.Locale

/** Poster aspect from the design: a 171dp card carries a 243dp cover. */
private const val PosterAspect = 171f / 243f

private val CoverShape = RoundedCornerShape(18.dp)

/** Two title lines, the gap, and the meta row: the tallest the text block gets at default scale. */
private val TextBlockHeight = 70.dp

/**
 * The browsing card for Discover and the search grid: a clean cover with the title, type and score
 * underneath.
 *
 * The cover carries nothing but the artwork and, when the title is already tracked, the list
 * indicator fused into its bottom-right corner. Keeping the text off the art is what lets a long
 * title and the score stay readable, which is the whole reason this variant exists.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaPosterCard(
    item: LibraryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    transitionPrefix: String = TransitionKeys.DISCOVER,
    listStatus: LibraryStatus? = LocalLibraryStatuses.current[item.mediaId],
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val title = item.getTitle(titleLanguage)
    val spatialSpec = if (sharedTransitionScope != null) AppMotion.rememberSpatialSpec() else null

    val coverData = item.cover.url() ?: item.coverUrl
    val cacheKey = TransitionKeys.imageCacheKey(transitionPrefix, item.mediaId) +
            "-" + LocalCoverQuality.current.name + TransitionKeys.coverVersion(coverData)

    val coverModifier = if (
        sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null
    ) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = TransitionKeys.cover(transitionPrefix, item.mediaId)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                clipInOverlayDuringTransition = OverlayClip(CoverShape)
            )
        }
    } else {
        Modifier
    }

    val titleModifier = if (
        sharedTransitionScope != null && animatedVisibilityScope != null && spatialSpec != null
    ) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = TransitionKeys.title(transitionPrefix, item.mediaId)
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ -> spatialSpec },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
            )
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier.bouncyClickable(
            onClick = onClick,
            role = Role.Button,
            onClickLabel = stringResource(R.string.a11y_action_open_details, title)
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = coverModifier
                .fillMaxWidth()
                .aspectRatio(PosterAspect)
                .clip(CoverShape)
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
                modifier = Modifier.fillMaxWidth()
            )

            listStatus?.let { status ->
                ListIndicator(
                    status = status,
                    type = item.type,
                    style = ListIndicatorStyle.Corner,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }

        // The text block reserves the two-line height, not the title itself. A short title keeps
        // its meta row directly underneath with no hole in between, and every card in a row still
        // measures the same, so scrolling a LazyRow cannot resize it.
        Column(
            modifier = Modifier.heightIn(min = TextBlockHeight),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = titleModifier
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.format?.let { format ->
                    Text(
                        text = format.toLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                item.averageScore?.let { score ->
                    if (item.format != null) {
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = StarGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", score / 10.0),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
