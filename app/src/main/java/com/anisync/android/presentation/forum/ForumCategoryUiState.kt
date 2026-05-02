package com.anisync.android.presentation.forum

import androidx.compose.runtime.Immutable
import com.anisync.android.domain.ForumThread
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class ForumCategoryUiState(
    val categoryName: String = "",
    val isLoading: Boolean = true,
    val isPaginating: Boolean = false,
    val isRefreshing: Boolean = false,
    val threads: ImmutableList<ForumThread> = persistentListOf(),
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val savedThreadIds: ImmutableSet<Int> = persistentSetOf(),
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
}