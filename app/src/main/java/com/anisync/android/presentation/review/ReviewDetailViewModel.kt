package com.anisync.android.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.ReviewDetailsQuery
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.Result
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.anisync.android.type.ReviewRating
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewDetailUiState(
    val isLoading: Boolean = true,
    val review: MediaReview? = null,
    val mediaId: Int? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    private val apolloClient: ApolloClient,
    private val detailsRepository: DetailsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewDetailUiState())
    val uiState: StateFlow<ReviewDetailUiState> = _uiState.asStateFlow()

    fun load(reviewId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = apolloClient
                    .query(ReviewDetailsQuery(id = Optional.present(reviewId)))
                    .fetchPolicy(FetchPolicy.CacheFirst)
                    .execute()

                if (response.hasErrors()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = response.errors?.first()?.message ?: "Failed to load review"
                        )
                    }
                    return@launch
                }

                val data = response.data?.Review
                if (data == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Review not found") }
                    return@launch
                }

                val review = MediaReview(
                    id = data.id,
                    summary = data.summary.orEmpty(),
                    body = data.body,
                    score = data.score ?: 0,
                    rating = data.rating ?: 0,
                    ratingAmount = data.ratingAmount ?: 0,
                    userRating = data.userRating?.name,
                    userName = data.user?.name.orEmpty(),
                    userAvatarUrl = data.user?.avatar?.large ?: data.user?.avatar?.medium,
                    createdAt = data.createdAt.toLong(),
                    mediaId = data.media?.id,
                    mediaTitle = data.media?.title?.userPreferred,
                    mediaCoverUrl = data.media?.coverImage?.large,
                    mediaBannerUrl = data.media?.bannerImage
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        review = review,
                        mediaId = data.media?.id
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load review")
                }
            }
        }
    }

    /**
     * Up/down-vote the review. Applies an optimistic count update immediately, then reconciles
     * the authoritative counts returned by the mutation.
     */
    fun rateReview(reviewId: Int, rating: ReviewRating) {
        val current = _uiState.value.review ?: return

        // Optimistic update mirroring the helpful-count maths used on the media-details sheet.
        val oldRating = current.userRating
        val newRatingStr = if (rating == ReviewRating.NO_VOTE) null else rating.name

        var updatedRating = current.rating
        var updatedAmount = current.ratingAmount
        when (oldRating) {
            "UP_VOTE" -> updatedRating -= 1
            "DOWN_VOTE" -> updatedRating += 1
        }
        if (oldRating != null && newRatingStr == null) updatedAmount -= 1
        if (oldRating == null && newRatingStr != null) updatedAmount += 1
        when (newRatingStr) {
            "UP_VOTE" -> updatedRating += 1
            "DOWN_VOTE" -> updatedRating -= 1
        }

        _uiState.update {
            it.copy(
                review = current.copy(
                    userRating = newRatingStr,
                    rating = updatedRating,
                    ratingAmount = updatedAmount
                )
            )
        }

        viewModelScope.launch {
            when (val result = detailsRepository.rateReview(reviewId, rating)) {
                is Result.Success -> {
                    val server = result.data
                    _uiState.update { state ->
                        val r = state.review ?: return@update state
                        state.copy(
                            review = r.copy(
                                rating = server.rating,
                                ratingAmount = server.ratingAmount,
                                userRating = server.userRating
                            )
                        )
                    }
                }
                is Result.Error -> {
                    // Roll back to the pre-vote state on failure.
                    _uiState.update { it.copy(review = current) }
                }
            }
        }
    }
}
