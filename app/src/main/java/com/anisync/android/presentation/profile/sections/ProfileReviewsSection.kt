package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.ReviewCard
import com.anisync.android.presentation.profile.ProfileUiState

fun LazyListScope.profileReviewsTab(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier
) {
    if (uiState.isReviewsLoading && uiState.reviews.isEmpty()) {
        item(key = "reviews_loading") {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (uiState.reviewsErrorMessage != null && uiState.reviews.isEmpty()) {
        item(key = "reviews_error") {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.reviewsErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    if (uiState.reviews.isEmpty()) {
        item(key = "reviews_empty") {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.profile_placeholder_reviews),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    items(
        items = uiState.reviews,
        key = { "user_review_${it.id}" }
    ) { review ->
        ReviewCard(
            review = review,
            onClick = { /* TODO: Open review details */ },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .animateItem()
        )
    }
}
