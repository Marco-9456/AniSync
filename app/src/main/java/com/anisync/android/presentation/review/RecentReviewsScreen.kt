package com.anisync.android.presentation.review

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.presentation.components.CollapsingTopBarScaffold
import com.anisync.android.presentation.components.ReviewCard
import com.anisync.android.presentation.util.TransitionKeys

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RecentReviewsScreen(
    onBackClick: () -> Unit,
    onReviewClick: (Int) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: RecentReviewsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var openedFilter by remember { mutableStateOf<ReviewFilterId?>(null) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && uiState.hasNextPage && !uiState.isLoading) {
            viewModel.onAction(RecentReviewsAction.LoadNextPage)
        }
    }

    CollapsingTopBarScaffold(
        title = stringResource(R.string.recent_reviews_title),
        onBackClick = onBackClick,
        scrollableState = listState,
        scrolledContainerColor = MaterialTheme.colorScheme.background,
        belowBar = {
            ReviewSearchFilterChipBar(
                filter = uiState.filter,
                sort = uiState.sort,
                media = uiState.media,
                author = uiState.author,
                onTypeSelect = { viewModel.onAction(RecentReviewsAction.SetFilter(it)) },
                onChipTap = { openedFilter = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { topContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        AppCircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null && uiState.reviews.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.reviews.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.recent_reviews_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = topContentPadding + 8.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.reviews,
                            key = { _, review -> "recent_review_${review.id}" },
                            contentType = { _, _ -> "review" }
                        ) { _, review ->
                            ReviewCard(
                                review = review,
                                onClick = { onReviewClick(review.id) },
                                onUserClick = onUserClick,
                                modifier = Modifier.animateItem(),
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                transitionPrefix = TransitionKeys.RECENT_REVIEWS
                            )
                        }

                        if (uiState.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppCircularProgressIndicator(
                                        modifier = Modifier.width(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ReviewSearchFilterSheetHost(
        opened = openedFilter,
        sort = uiState.sort,
        media = uiState.media,
        author = uiState.author,
        mediaPickerType = uiState.mediaPickerType,
        mediaPickerQuery = uiState.mediaPickerQuery,
        mediaPickerResults = uiState.mediaPickerResults,
        isMediaPickerSearching = uiState.isMediaPickerSearching,
        authorPickerQuery = uiState.authorPickerQuery,
        authorPickerResults = uiState.authorPickerResults,
        isAuthorPickerSearching = uiState.isAuthorPickerSearching,
        pickerError = uiState.pickerError,
        onSortChange = { viewModel.onAction(RecentReviewsAction.SetSort(it)) },
        onMediaPickerTypeChange = { viewModel.onAction(RecentReviewsAction.OnMediaPickerTypeChange(it)) },
        onMediaPickerQueryChange = { viewModel.onAction(RecentReviewsAction.OnMediaPickerQueryChange(it)) },
        onSelectMedia = { viewModel.onAction(RecentReviewsAction.SelectMedia(it)) },
        onClearMedia = { viewModel.onAction(RecentReviewsAction.ClearMedia) },
        onAuthorPickerQueryChange = { viewModel.onAction(RecentReviewsAction.OnAuthorPickerQueryChange(it)) },
        onSelectAuthor = { viewModel.onAction(RecentReviewsAction.SelectAuthor(it)) },
        onClearAuthor = { viewModel.onAction(RecentReviewsAction.ClearAuthor) },
        onDismiss = { openedFilter = null }
    )
}
