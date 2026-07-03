package com.anisync.android.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.domain.MediaReview
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys

/**
 * Single review card shared by the Discover "Recent Reviews" carousel, the profile
 * reviews tab, the dedicated reviews screen and the media-details reviews list.
 *
 * @param showBanner Show the media banner with the score pill overlaid. Media details
 *   passes `false` (the review is already for the open media); the score then moves
 *   into the footer next to the likes pill.
 * @param fixedHeight When set (carousel), the card is exactly this tall and the footer
 *   is pinned to the bottom; when null (lists), the card wraps its content.
 * @param summaryMaxLines Defaults to 2 for fixed-height carousel cards, 3 for lists.
 * @param sharedTransitionScope With [animatedVisibilityScope] and [transitionPrefix], the
 *   banner becomes a shared element morphing into the review-detail hero banner.
 * @param transitionPrefix Source-screen prefix for [TransitionKeys.reviewBanner]; must match
 *   the `sourceScreen` passed on the `ReviewDetail` route.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ReviewCard(
    review: MediaReview,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    showBanner: Boolean = true,
    fixedHeight: Dp? = null,
    summaryMaxLines: Int = if (fixedHeight != null) 2 else 3,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    transitionPrefix: String? = null,
) {
    val scoreColor = remember(review.score) {
        when {
            review.score >= 75 -> Color(0xFF4CAF50) // Green
            review.score >= 50 -> Color(0xFFFFC107) // Amber
            else -> Color(0xFFFF5722) // Red-orange
        }
    }
    val headerImageUrl = remember(review.mediaBannerUrl, review.mediaCoverUrl) {
        review.mediaBannerUrl ?: review.mediaCoverUrl
    }

    Card(
        modifier = modifier.then(
            if (fixedHeight != null) Modifier.height(fixedHeight) else Modifier.fillMaxWidth()
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fixedHeight != null) Modifier.fillMaxHeight() else Modifier)
        ) {
            if (showBanner) {
                // All four corners, not just the bottom pair: at rest the Card's own 16dp
                // shape rounds the top anyway, but mid-flight the shared banner renders in
                // the overlay OUTSIDE the Card's clip — bottom-only rounding left square
                // top corners visible during the return transition.
                val bannerShape = RoundedCornerShape(16.dp)
                val bannerModifier =
                    if (sharedTransitionScope != null && animatedVisibilityScope != null && transitionPrefix != null) {
                        val spatialSpec = AppMotion.rememberSpatialSpec()
                        with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(
                                    key = TransitionKeys.reviewBanner(transitionPrefix, review.id)
                                ),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ -> spatialSpec },
                                clipInOverlayDuringTransition = OverlayClip(bannerShape)
                            )
                        }
                    } else {
                        Modifier
                    }

                // clip BEFORE background: painting first left the square surfaceVariant
                // corners visible behind the rounded image at the banner's bottom edge.
                Box(
                    modifier = bannerModifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(bannerShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (headerImageUrl != null) {
                        AsyncImage(
                            model = headerImageUrl,
                            contentDescription = review.mediaTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    ScorePill(
                        score = review.score,
                        scoreColor = scoreColor,
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fixedHeight != null) Modifier.weight(1f) else Modifier)
                    .padding(20.dp)
            ) {
                if (showBanner && review.mediaTitle != null) {
                    Text(
                        text = review.mediaTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = review.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = summaryMaxLines,
                    lineHeight = 18.sp,
                    fontSize = 13.sp
                )

                if (fixedHeight != null) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUserClick(review.userName) }
                            .padding(vertical = 2.dp)
                    ) {
                        UserAvatar(
                            url = review.userAvatarUrl,
                            contentDescription = null,
                            size = 32.dp
                        )
                        Text(
                            text = review.userName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // No banner means the score has nowhere to sit, so surface it here.
                        if (!showBanner) {
                            ScorePill(score = review.score, scoreColor = scoreColor)
                        }
                        ReviewVoteCountPill(
                            rating = review.rating,
                            ratingAmount = review.ratingAmount
                        )
                    }
                }
            }
        }
    }
}

/** Score chip: a star + the numeric score on a solid score-coloured pill. */
@Composable
private fun ScorePill(
    score: Int,
    scoreColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(scoreColor, RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 11.sp
        )
    }
}
