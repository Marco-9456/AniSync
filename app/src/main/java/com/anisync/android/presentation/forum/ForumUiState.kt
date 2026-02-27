package com.anisync.android.presentation.forum

import androidx.compose.runtime.Stable
import com.anisync.android.domain.ForumCategory
import com.anisync.android.domain.ForumThread

/**
 * Feed modes matching AniList's forum hub tabs.
 */
enum class ForumFeed(val label: String) {
    OVERVIEW("Overview"),
    RECENT("Recent"),
    NEW("New")
}

@Stable
data class ForumUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val threads: List<ForumThread> = emptyList(),
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val selectedFeed: ForumFeed = ForumFeed.OVERVIEW,
    val errorMessage: String? = null
)

sealed interface ForumAction {
    data object Refresh : ForumAction
    data object LoadMore : ForumAction
    data class OnFeedChange(val feed: ForumFeed) : ForumAction
    data class OnThreadClick(val threadId: Int, val threadTitle: String) : ForumAction
    data object OnCreateThreadClick : ForumAction
    data class OnCategoryClick(val category: ForumCategory) : ForumAction
    data class ShowSnackbar(val message: String) : ForumAction
}
