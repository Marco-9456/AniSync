package com.anisync.android.presentation.discover.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.MediaReview
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.ui.theme.emphasis

private val CardWidth = 300.dp
private val CardHeight = 248.dp

/**
 * Horizontally scrolling row of recent reviews, mirroring the media carousels on
 * Discover. Cards are fixed-size so the row stays tidy regardless of summary length.
 */
@Composable
fun RecentReviewsRow(
    reviews: List<MediaReview>,
    onReviewClick: (Int) -> Unit,
    modifier: Modifier = Modifier
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
            HorizontalReviewCard(
                review = review,
                onClick = { onReviewClick(review.id) }
            )
        }
    }
}

@Composable
private fun HorizontalReviewCard(
    review: MediaReview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scoreColor = remember(review.score) {
        when {
            review.score >= 75 -> Color(0xFF4CAF50)
            review.score >= 50 -> Color(0xFFFFC107)
            else -> Color(0xFFFF5722)
        }
    }
    val headerImageUrl = remember(review.mediaBannerUrl, review.mediaCoverUrl) {
        review.mediaBannerUrl ?: review.mediaCoverUrl
    }

    Card(
        modifier = modifier
            .width(CardWidth)
            .height(CardHeight),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (headerImageUrl != null) {
                AsyncImage(
                    model = headerImageUrl,
                    contentDescription = review.mediaTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (review.mediaTitle != null) {
                    Text(
                        text = review.mediaTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    UserAvatar(
                        url = review.userAvatarUrl,
                        contentDescription = null,
                        size = 24.dp
                    )
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.labelMedium.emphasis(),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .background(scoreColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${review.score}/100",
                            style = MaterialTheme.typography.labelSmall.emphasis(),
                            color = scoreColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = review.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${review.rating}/${review.ratingAmount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.found_helpful),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
