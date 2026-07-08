package com.anisync.android.presentation.review

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Share
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.presentation.components.AsyncRichTextRenderer
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.ReviewAuthorBar
import com.anisync.android.presentation.components.ReviewScorePill
import com.anisync.android.presentation.components.ReviewVoteActions
import com.anisync.android.presentation.components.TranslateIconButton
import com.anisync.android.presentation.share.ReviewShareCard
import com.anisync.android.presentation.share.ShareImageSheet
import com.anisync.android.presentation.util.AppMotion
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ReviewDetailScreen(
    reviewId: Int,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onMediaClick: (Int) -> Unit,
    viewModel: ReviewDetailViewModel = hiltViewModel(),
    sourceScreen: String = "link",
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showShareImage by remember { mutableStateOf(false) }

    LaunchedEffect(reviewId) { viewModel.load(reviewId) }

    val scrollState = rememberScrollState()

    CollapsingTopBarScaffold(
        title = stringResource(R.string.label_review),
        onBackClick = onBackClick,
        scrollableState = scrollState,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        actions = {
            TranslateIconButton(
                text = uiState.review?.body,
                tint = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = {
                    if (uiState.review != null) showShareImage = true
                    else ShareUtils.shareText(context, "https://anilist.co/review/$reviewId")
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.cd_share),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { topContentPadding ->
        when {
            uiState.isLoading && uiState.review == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topContentPadding),
                    contentAlignment = Alignment.Center
                ) { AppCircularProgressIndicator() }
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

                if (showShareImage) {
                    ShareImageSheet(
                        onDismiss = { showShareImage = false },
                        link = "https://anilist.co/review/$reviewId"
                    ) {
                        ReviewShareCard(review = review)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(top = topContentPadding)
                ) {
                    // Inset hero banner with the score pill floating over it. When launched
                    // from a review card (Discover / Recent Reviews), the card's banner
                    // morphs into this box via shared bounds keyed on the source screen.
                    val bannerUrl = review.mediaBannerUrl ?: review.mediaCoverUrl
                    val bannerShape = RoundedCornerShape(24.dp)
                    val bannerSharedModifier =
                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            val spatialSpec = AppMotion.rememberSpatialSpec()
                            with(sharedTransitionScope) {
                                Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(
                                        key = TransitionKeys.reviewBanner(sourceScreen, reviewId)
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ -> spatialSpec },
                                    clipInOverlayDuringTransition = OverlayClip(bannerShape)
                                )
                            }
                        } else {
                            Modifier
                        }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .height(200.dp)
                            .then(bannerSharedModifier)
                            .clip(bannerShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (bannerUrl != null) {
                            AsyncImage(
                                model = bannerUrl,
                                contentDescription = review.mediaTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.5f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.45f)
                                    )
                                )
                        )
                        ReviewScorePill(
                            score = review.score,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(8.dp))

                        // Clickable media title → media details (chevron only when navigable).
                        if (review.mediaTitle != null) {
                            val navigable = mediaId != null
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(
                                        if (navigable) Modifier.clickable { onMediaClick(mediaId!!) }
                                        else Modifier
                                    )
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = review.mediaTitle,
                                    style = MaterialTheme.typography.headlineSmall.emphasis(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
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

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = review.summary,
                            style = MaterialTheme.typography.titleMedium.emphasis(),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(16.dp))

                        if (review.body != null) {
                            AsyncRichTextRenderer(
                                html = review.body,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }

                        Spacer(Modifier.height(28.dp))

                        ReviewVoteActions(
                            userRating = review.userRating,
                            onRate = { viewModel.rateReview(review.id, it) }
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
