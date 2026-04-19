package com.anisync.android.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.ReviewDetailsQuery
import com.anisync.android.domain.MediaReview
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
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
    private val apolloClient: ApolloClient
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
                    userRating = null,
                    userName = data.user?.name.orEmpty(),
                    userAvatarUrl = data.user?.avatar?.large ?: data.user?.avatar?.medium,
                    createdAt = data.createdAt.toLong(),
                    mediaTitle = data.media?.title?.userPreferred,
                    mediaCoverUrl = data.media?.coverImage?.large
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
}
