package com.anisync.android.presentation.forum

import androidx.compose.runtime.Stable
import com.anisync.android.domain.ForumThread

@Stable
data class ForumCategoryUiState(
    val categoryName: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val threads: List<ForumThread> = emptyList(),
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val savedThreadIds: Set<Int> = emptySet(),
    val sortLabel: String = "Recent"
)

sealed interface ForumCategoryAction {
    data class Load(val categoryId: Int, val categoryName: String) : ForumCategoryAction
    data object Refresh : ForumCategoryAction
    data object LoadMore : ForumCategoryAction
    data class OnSearchQueryChange(val query: String) : ForumCategoryAction
    data class OnThreadClick(val threadId: Int, val threadTitle: String) : ForumCategoryAction
    data class ToggleSaveThread(val thread: ForumThread) : ForumCategoryAction
    data class ToggleSubscribeThread(val thread: ForumThread) : ForumCategoryAction
    data class ChangeSort(val sort: String, val label: String) : ForumCategoryAction
    data class ShowSnackbar(val message: String) : ForumCategoryAction
}
