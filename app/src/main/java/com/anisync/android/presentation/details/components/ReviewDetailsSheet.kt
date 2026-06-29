package com.anisync.android.presentation.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.domain.MediaReview
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.ReviewAuthorBar
import com.anisync.android.presentation.components.ReviewVoteActions
import com.anisync.android.presentation.components.TranslateIconButton
import com.anisync.android.ui.theme.emphasis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailsSheet(
    review: MediaReview,
    onRateReview: (Int, com.anisync.android.type.ReviewRating) -> Unit = { _, _ -> },
    onUserClick: (String) -> Unit = {},
    onMediaClick: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
        ) {
            // Dynamic title: navigable (chevron) when opened from a context that can resolve the
            // media, otherwise a plain heading. Hidden entirely when the review carries no title.
            if (review.mediaTitle != null) {
                val navigable = onMediaClick != null && review.mediaId != null
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (navigable) Modifier.clickable { onMediaClick!!(review.mediaId!!) }
                            else Modifier
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = review.mediaTitle,
                        style = MaterialTheme.typography.headlineSmall.emphasis(),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (navigable) {
                        Spacer(Modifier.size(12.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            ReviewAuthorBar(review = review, onUserClick = onUserClick)

            Spacer(Modifier.height(20.dp))

            // Scrolling body fills the space between the pinned header and the vote actions.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = review.summary,
                        style = MaterialTheme.typography.titleMedium.emphasis(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    TranslateIconButton(
                        text = review.body,
                        iconSize = 20.dp,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (review.body != null) {
                    AsyncRichTextRenderer(
                        html = review.body,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_review_text),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            ReviewVoteActions(
                userRating = review.userRating,
                onRate = { onRateReview(review.id, it) },
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
