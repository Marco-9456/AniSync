package com.anisync.android.presentation.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentReviewsViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialFilter =
        ReviewMediaFilter.fromRouteArg(savedStateHandle.get<String>("mediaType"))

    private val _uiState = MutableStateFlow(RecentReviewsUiState(filter = initialFilter))
    val uiState: StateFlow<RecentReviewsUiState> = _uiState.asStateFlow()

    // Reviews can shift between pages as new ones are published; dedupe by id so a
    // review fetched on page 1 doesn't reappear when page 2 loads.
    private var knownIds: Set<Int> = emptySet()

    init {
        loadInitial()
    }

    fun onAction(action: RecentReviewsAction) {
        when (action) {
            is RecentReviewsAction.LoadNextPage -> loadNextPage()
            is RecentReviewsAction.SetFilter -> setFilter(action.filter)
            is RecentReviewsAction.Retry -> loadInitial()
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            knownIds = emptySet()

            when (
                val result = discoverRepository.getRecentReviews(
                    mediaType = _uiState.value.filter.toMediaType(),
                    page = 1
                )
            ) {
                is Result.Success -> {
                    knownIds = result.data.reviews.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            reviews = result.data.reviews,
                            hasNextPage = result.data.hasNextPage,
                            currentPage = 1,
                            isLoading = false
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun loadNextPage() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasNextPage || current.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            when (
                val result = discoverRepository.getRecentReviews(
                    mediaType = current.filter.toMediaType(),
                    page = current.currentPage + 1
                )
            ) {
                is Result.Success -> {
                    val newReviews = result.data.reviews.filter { it.id !in knownIds }
                    knownIds = knownIds + newReviews.map { it.id }
                    _uiState.update {
                        it.copy(
                            reviews = it.reviews + newReviews,
                            hasNextPage = result.data.hasNextPage,
                            currentPage = current.currentPage + 1,
                            isLoadingMore = false
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    private fun setFilter(filter: ReviewMediaFilter) {
        if (_uiState.value.filter == filter) return
        _uiState.update {
            it.copy(
                filter = filter,
                reviews = emptyList(),
                currentPage = 1,
                hasNextPage = true
            )
        }
        loadInitial()
    }
}
