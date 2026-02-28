package com.anisync.android.presentation.forum

import androidx.compose.runtime.Immutable
import com.anisync.android.domain.ForumCategory
import com.anisync.android.domain.ForumThread
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Feed modes matching AniList's forum hub tabs.
 */
enum class ForumFeed(val label: String) {
    OVERVIEW("Overview"),
    RECENT("Recent"),
    NEW("New"),
    SUBSCRIBED("Subscribed"),
    SAVED("Saved")
}

@Immutable
data class ForumUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val threads: ImmutableList<ForumThread> = persistentListOf(),
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val selectedFeed: ForumFeed = ForumFeed.OVERVIEW,
    val selectedCategoryId: Int? = null,
    val searchQuery: String = "",
    val savedThreadIds: ImmutableSet<Int> = persistentSetOf(),
    val errorMessage: String? = null
)

sealed interface ForumAction {
    data object Refresh : ForumAction
    data object LoadMore : ForumAction
    data class OnFeedChange(val feed: ForumFeed) : ForumAction
    data class OnCategoryChange(val categoryId: Int?) : ForumAction
    data class OnSearchQueryChange(val query: String) : ForumAction
    data class ToggleSaveThread(val thread: ForumThread) : ForumAction
    data class ToggleSubscribeThread(val thread: ForumThread) : ForumAction
    data class OnThreadClick(val threadId: Int, val threadTitle: String) : ForumAction
    data object OnCreateThreadClick : ForumAction
    data class OnCategoryClick(val category: ForumCategory) : ForumAction
    data class ShowSnackbar(val message: String) : ForumAction
}