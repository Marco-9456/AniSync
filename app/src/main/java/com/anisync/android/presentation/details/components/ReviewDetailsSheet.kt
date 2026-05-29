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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.R
import com.anisync.android.domain.MediaReview
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailsSheet(
    review: MediaReview,
    onRateReview: (Int, com.anisync.android.type.ReviewRating) -> Unit = { _, _ -> },
    onUserClick: (String) -> Unit = {},
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 24.dp)
        ) {
            // Header: User info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserAvatar(
                    url = review.userAvatarUrl,
                    contentDescription = null,
                    size = 48.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.titleMedium.emphasis(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { onUserClick(review.userName) }
                    )

                    val dateStr = remember(review.createdAt) {
                        if (review.createdAt > 0) {
                            DateFormat.getDateInstance().format(Date(review.createdAt * 1000L))
                        } else ""
                    }

                    if (dateStr.isNotEmpty()) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Score Badge
                val scoreColor = remember(review.score) {
                    when {
                        review.score >= 75 -> Color(0xFF4CAF50)
                        review.score >= 50 -> Color(0xFFFFC107)
                        else -> Color(0xFFFF5722)
                    }
                }
                Box(
                    modifier = Modifier
                        .background(
                            scoreColor.copy(alpha = 0.15f),
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${review.score}/100",
                        style = MaterialTheme.typography.labelLarge.emphasis(),
                        color = scoreColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = review.summary,
                    style = MaterialTheme.typography.titleMedium.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(32.dp))

                // Footer stats and Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${review.rating}/${review.ratingAmount} ${stringResource(R.string.found_helpful)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isUpVoted = review.userRating == "UP_VOTE"
                        val isDownVoted = review.userRating == "DOWN_VOTE"

                        IconButton(
                            onClick = { 
                                val newRating = if (isUpVoted) com.anisync.android.type.ReviewRating.NO_VOTE else com.anisync.android.type.ReviewRating.UP_VOTE
                                onRateReview(review.id, newRating) 
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ThumbUp,
                                contentDescription = stringResource(R.string.cd_like_review),
                                tint = if (isUpVoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { 
                                val newRating = if (isDownVoted) com.anisync.android.type.ReviewRating.NO_VOTE else com.anisync.android.type.ReviewRating.DOWN_VOTE
                                onRateReview(review.id, newRating) 
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ThumbDown,
                                contentDescription = stringResource(R.string.cd_dislike_review),
                                tint = if (isDownVoted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
