package com.anisync.android.presentation.review

import androidx.compose.runtime.Immutable
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.ReviewSortOption
import com.anisync.android.domain.SearchResult
import com.anisync.android.type.MediaType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class RecentReviewsUiState(
    // --- Result list ---
    val reviews: List<MediaReview> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null,
    val currentPage: Int = 1,

    // --- Filters (mirrors the forum advanced-search filters: sort / media / author,
    // plus the media-type toggle that predates them) ---
    val filter: ReviewMediaFilter = ReviewMediaFilter.ANIME,
    val sort: ReviewSortOption = ReviewSortOption.Default,
    val media: LibraryEntry? = null,
    val author: SearchResult.UserResult? = null,

    // --- Media picker substate (the Media filter sheet) ---
    val mediaPickerType: MediaType = MediaType.ANIME,
    val mediaPickerQuery: String = "",
    val mediaPickerResults: ImmutableList<LibraryEntry> = persistentListOf(),
    val isMediaPickerSearching: Boolean = false,

    // --- Author picker substate (the Author filter sheet) ---
    val authorPickerQuery: String = "",
    val authorPickerResults: ImmutableList<SearchResult.UserResult> = persistentListOf(),
    val isAuthorPickerSearching: Boolean = false,

    /** Shared error for the picker sheets. */
    val pickerError: String? = null
)

sealed interface RecentReviewsAction {
    data object LoadNextPage : RecentReviewsAction
    data object Retry : RecentReviewsAction

    data class SetFilter(val filter: ReviewMediaFilter) : RecentReviewsAction
    data class SetSort(val sort: ReviewSortOption) : RecentReviewsAction

    // Media filter
    data class OnMediaPickerQueryChange(val query: String) : RecentReviewsAction
    data class OnMediaPickerTypeChange(val type: MediaType) : RecentReviewsAction
    data class SelectMedia(val entry: LibraryEntry) : RecentReviewsAction
    data object ClearMedia : RecentReviewsAction

    // Author filter
    data class OnAuthorPickerQueryChange(val query: String) : RecentReviewsAction
    data class SelectAuthor(val user: SearchResult.UserResult) : RecentReviewsAction
    data object ClearAuthor : RecentReviewsAction
}
