package com.anisync.android.presentation.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.MediaReview
import com.anisync.android.ui.theme.LocalExpressiveTypography

/**
 * Shareable pull-quote card for a review: media banner with a floating score badge, the
 * review's one-line summary set as a lead quote, and the author byline. Uses [MediaReview.summary]
 * (plain text) rather than the HTML body so the exported image stays a clean quote.
 */
@Composable
fun ReviewShareCard(
    review: MediaReview,
    modifier: Modifier = Modifier,
) {
    val expressive = LocalExpressiveTypography.current

    ShareCardScaffold(modifier = modifier, handle = review.userName) {
        ShareCardBannerBox(
            bannerUrl = review.mediaBannerUrl ?: review.mediaCoverUrl,
            height = 140.dp
        ) {
            if (review.mediaTitle != null) {
                Text(
                    text = review.mediaTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_review_score, review.score),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "“",
                style = expressive.statNumericMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            Text(
                text = review.summary,
                style = expressive.editorialLead,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_review_byline).uppercase(),
                    style = expressive.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = review.userName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
