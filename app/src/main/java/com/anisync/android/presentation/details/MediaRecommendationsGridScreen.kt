package com.anisync.android.presentation.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.anisync.android.presentation.util.posterGridColumns
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.details.components.RecommendationItem
import com.anisync.android.presentation.util.AppMotion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MediaRecommendationsGridScreen(
    mediaId: Int,
    mediaTitle: String,
    onBackClick: () -> Unit,
    onRecommendationClick: (Int) -> Unit,
    viewModel: MediaDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyGridState()

    var shouldKeepTopBarOverlayForReturn by rememberSaveable { mutableStateOf(false) }
    var hasObservedGridReEnter by rememberSaveable { mutableStateOf(false) }

    val navigateToRecommendation: (Int) -> Unit = remember(onRecommendationClick) {
        { recId ->
            shouldKeepTopBarOverlayForReturn = true
            hasObservedGridReEnter = false
            onRecommendationClick(recId)
        }
    }

    val isGridEnteringFromBackStack by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.PreEnter &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isGridTargetingVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.targetState == EnterExitState.Visible
        }
    }
    val isGridFullyVisible by remember(animatedVisibilityScope) {
        derivedStateOf {
            animatedVisibilityScope?.transition?.currentState == EnterExitState.Visible &&
                animatedVisibilityScope.transition.targetState == EnterExitState.Visible
        }
    }
    val isSharedTransitionRunning by remember(sharedTransitionScope) {
        derivedStateOf { sharedTransitionScope?.isTransitionActive == true }
    }
    val shouldRenderTopBarInOverlay by remember(
        sharedTransitionScope,
        animatedVisibilityScope
    ) {
        derivedStateOf {
            sharedTransitionScope != null &&
                animatedVisibilityScope != null &&
                shouldKeepTopBarOverlayForReturn &&
                isGridTargetingVisible &&
                (
                    isGridEnteringFromBackStack ||
                        (hasObservedGridReEnter && isSharedTransitionRunning)
                    )
        }
    }
    val topBarOverlayAlpha = if (animatedVisibilityScope != null) {
        val alpha by animatedVisibilityScope.transition.animateFloat(label = "MediaRecommendationsGridTopBarOverlayAlpha") { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }
        alpha
    } else {
        1f
    }

    androidx.compose.runtime.LaunchedEffect(shouldKeepTopBarOverlayForReturn, isGridEnteringFromBackStack) {
        if (shouldKeepTopBarOverlayForReturn && isGridEnteringFromBackStack) {
            hasObservedGridReEnter = true
        }
    }

    androidx.compose.runtime.LaunchedEffect(
        shouldKeepTopBarOverlayForReturn,
        hasObservedGridReEnter,
        isGridFullyVisible,
        isSharedTransitionRunning
    ) {
        if (
            shouldKeepTopBarOverlayForReturn &&
            hasObservedGridReEnter &&
            isGridFullyVisible &&
            !isSharedTransitionRunning
        ) {
            shouldKeepTopBarOverlayForReturn = false
            hasObservedGridReEnter = false
        }
    }

    val topBarOverlayModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .renderInSharedTransitionScopeOverlay(
                    zIndexInOverlay = 1f,
                    renderInOverlay = { shouldRenderTopBarInOverlay }
                )
                .graphicsLayer {
                    alpha = if (shouldRenderTopBarInOverlay) topBarOverlayAlpha else 1f
                }
        }
    } else {
        Modifier
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.section_recommendations),
        onBackClick = onBackClick,
        scrollableState = listState,
        topBarModifier = topBarOverlayModifier,
        scrolledContainerColor = MaterialTheme.colorScheme.background
    ) { topContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is DetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                is DetailsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is DetailsUiState.Success -> {
                    val recommendations = remember(state.details.recommendations) {
                        state.details.recommendations.distinctBy { it.id }
                    }
                    if (recommendations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topContentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_no_recommendations),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val placementSpec = AppMotion.rememberOffsetSpatialSpec()
                        val fadeSpec = AppMotion.rememberEffectsSpec()

                        LazyVerticalGrid(
                            state = listState,
                            columns = posterGridColumns(baseMinSize = 110.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = topContentPadding + 16.dp,
                                end = 16.dp,
                                bottom = 96.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(recommendations, key = { "rec_${it.id}" }) { recommendation ->
                                RecommendationItem(
                                    recommendation = recommendation,
                                    fillCell = true,
                                    onClick = { navigateToRecommendation(recommendation.id) },
                                    onRate = { isUpvote ->
                                        val rating = when {
                                            isUpvote && recommendation.userRating == "RATE_UP" ->
                                                com.anisync.android.type.RecommendationRating.NO_RATING
                                            isUpvote ->
                                                com.anisync.android.type.RecommendationRating.RATE_UP
                                            !isUpvote && recommendation.userRating == "RATE_DOWN" ->
                                                com.anisync.android.type.RecommendationRating.NO_RATING
                                            else ->
                                                com.anisync.android.type.RecommendationRating.RATE_DOWN
                                        }
                                        viewModel.rateRecommendation(recommendation.id, rating)
                                    },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = fadeSpec,
                                        fadeOutSpec = fadeSpec,
                                        placementSpec = placementSpec
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
