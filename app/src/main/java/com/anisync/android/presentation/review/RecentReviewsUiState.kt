package com.anisync.android.presentation.review

import androidx.compose.runtime.Stable
import com.anisync.android.domain.MediaReview

@Stable
data class RecentReviewsUiState(
    val filter: ReviewMediaFilter = ReviewMediaFilter.ANIME,
    val reviews: List<MediaReview> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null,
    val currentPage: Int = 1
)

sealed interface RecentReviewsAction {
    data object LoadNextPage : RecentReviewsAction
    data class SetFilter(val filter: ReviewMediaFilter) : RecentReviewsAction
    data object Retry : RecentReviewsAction
}
