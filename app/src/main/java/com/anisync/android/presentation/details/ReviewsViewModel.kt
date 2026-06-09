package com.anisync.android.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val detailsRepository: DetailsRepository
) : ViewModel() {

    private val _reviews = MutableStateFlow<List<MediaReview>>(emptyList())
    val reviews: StateFlow<List<MediaReview>> = _reviews.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentPage = 1
    private var hasNextPage = true
    private var currentMediaId: Int? = null

    fun loadInitial(mediaId: Int) {
        if (currentMediaId == mediaId) return // Already loaded or loading
        currentMediaId = mediaId
        currentPage = 1
        hasNextPage = true
        _reviews.value = emptyList()
        fetchNextPage()
    }

    fun fetchNextPage() {
        if (_isLoading.value || !hasNextPage) return
        val mediaId = currentMediaId ?: return
        // Set synchronously so a fast scroll firing fetchNextPage() again before the
        // coroutine starts can't re-fetch the same page (double currentPage++ skips a page).
        _isLoading.value = true

        viewModelScope.launch {
            when (val result = detailsRepository.getMediaReviews(mediaId, currentPage)) {
                is Result.Success -> {
                    val (newReviews, hasNext) = result.data
                    // Reviews can shift between pages as votes/new reviews land; dedupe by id so a
                    // review fetched on an earlier page doesn't reappear and crash LazyColumn with a
                    // duplicate key.
                    _reviews.value = (_reviews.value + newReviews).distinctBy { it.id }
                    hasNextPage = hasNext
                    if (hasNext) {
                        currentPage++
                    }
                }
                is Result.Error -> {
                    // Handle error if needed
                }
            }
            _isLoading.value = false
        }
    }

    fun rateReview(reviewId: Int, rating: com.anisync.android.type.ReviewRating) {
        viewModelScope.launch {
            when (val result = detailsRepository.rateReview(reviewId, rating)) {
                is Result.Success -> {
                    // Optimistic update
                    val currentList = _reviews.value
                    val index = currentList.indexOfFirst { it.id == reviewId }
                    if (index != -1) {
                        val review = currentList[index]
                        val isUpVoted = review.userRating == "UP_VOTE"
                        val isDownVoted = review.userRating == "DOWN_VOTE"
                        val newRatingStr = rating.rawValue
                        
                        // We do a naive optimistic update that works for simple cases
                        val updatedList = currentList.toMutableList()
                        updatedList[index] = review.copy(
                            userRating = if (rating == com.anisync.android.type.ReviewRating.NO_VOTE) null else newRatingStr,
                            // Simplistic rating math for optimistic effect
                            ratingAmount = if (rating != com.anisync.android.type.ReviewRating.NO_VOTE && review.userRating == null) review.ratingAmount + 1 else review.ratingAmount,
                            rating = if (rating == com.anisync.android.type.ReviewRating.UP_VOTE && !isUpVoted) review.rating + 1 else review.rating
                        )
                        _reviews.value = updatedList
                    }
                }
                is Result.Error -> {
                    // handle error
                }
            }
        }
    }
}
