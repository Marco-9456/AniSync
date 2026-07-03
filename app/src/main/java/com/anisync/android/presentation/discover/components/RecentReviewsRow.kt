package com.anisync.android.presentation.discover.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.MediaReview
import com.anisync.android.presentation.components.ReviewCard
import com.anisync.android.presentation.util.TransitionKeys

private val CardWidth = 310.dp
private val CardHeight = 280.dp

/**
 * Horizontally scrolling row of recent reviews, mirroring the media carousels on
 * Discover. Cards are fixed-size so the row stays tidy regardless of summary length.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RecentReviewsRow(
    reviews: List<MediaReview>,
    onReviewClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = reviews,
            key = { "recent_review_${it.id}" },
            contentType = { "recent_review_card" }
        ) { review ->
            ReviewCard(
                review = review,
                onClick = { onReviewClick(review.id) },
                // Carousel has no per-user nav; keep the whole card opening the review.
                onUserClick = { onReviewClick(review.id) },
                modifier = Modifier.width(CardWidth),
                fixedHeight = CardHeight,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                transitionPrefix = TransitionKeys.DISCOVER
            )
        }
    }
}
