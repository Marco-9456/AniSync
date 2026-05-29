package com.anisync.android.presentation.review

import com.anisync.android.domain.url

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.ErrorState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailScreen(
    reviewId: Int,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onMediaClick: (Int) -> Unit,
    viewModel: ReviewDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(reviewId) { viewModel.load(reviewId) }

    val scrollState = rememberScrollState()

    CollapsingTopBarScaffold(
        title = stringResource(R.string.label_review),
        onBackClick = onBackClick,
        scrollableState = scrollState,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { topContentPadding ->
        when {
            uiState.isLoading && uiState.review == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topContentPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            uiState.errorMessage != null && uiState.review == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topContentPadding)
                ) {
                    ErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.load(reviewId) }
                    )
                }
            }

            uiState.review != null -> {
                val review = uiState.review!!
                val mediaId = uiState.mediaId
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = topContentPadding + 16.dp,
                            bottom = 16.dp
                        )
                ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UserAvatar(
                                url = review.userAvatarUrl,
                                contentDescription = null,
                                size = 48.dp,
                                modifier = Modifier.clickable { onUserClick(review.userName) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = review.userName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onUserClick(review.userName) }
                                )
                                val dateStr = remember(review.createdAt) {
                                    if (review.createdAt > 0) DateFormat.getDateInstance().format(Date(review.createdAt * 1000L)) else ""
                                }
                                if (dateStr.isNotEmpty()) {
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            val scoreColor = when {
                                review.score >= 75 -> Color(0xFF4CAF50)
                                review.score >= 50 -> Color(0xFFFFC107)
                                else -> Color(0xFFFF5722)
                            }
                            Box(
                                modifier = Modifier
                                    .background(scoreColor.copy(alpha = 0.15f), MaterialTheme.shapes.small)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${review.score}/100",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = scoreColor
                                )
                            }
                        }

                        if (review.mediaTitle != null && mediaId != null) {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onMediaClick(mediaId) }
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(12.dp)
                            ) {
                                AsyncImage(
                                    model = review.mediaCover.url() ?: review.mediaCoverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(64.dp)
                                        .clip(MaterialTheme.shapes.small)
                                )
                                Text(
                                    text = review.mediaTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = review.summary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(16.dp))
                        if (review.body != null) {
                            AsyncRichTextRenderer(
                                html = review.body,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

