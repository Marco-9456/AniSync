package com.anisync.android.presentation.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val forumRepository: ForumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumUiState())
    val uiState: StateFlow<ForumUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<ForumAction>()
    val actions: SharedFlow<ForumAction> = _actions.asSharedFlow()

    init {
        loadSavedIds()
        load(page = 1)
    }

    fun onAction(action: ForumAction) {
        when (action) {
            is ForumAction.Refresh -> {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                load(page = 1, replaceExisting = true)
            }

            is ForumAction.LoadMore -> {
                if (!_uiState.value.hasNextPage || _uiState.value.isLoading) return
                load(page = _uiState.value.currentPage + 1)
            }

            is ForumAction.OnFeedChange -> {
                if (_uiState.value.selectedFeed == action.feed) return
                _uiState.update {
                    it.copy(
                        selectedFeed = action.feed,
                        threads = persistentListOf(),
                        isLoading = true
                    )
                }
                load(page = 1, replaceExisting = true)
            }

            is ForumAction.OnCategoryChange -> {
                if (_uiState.value.selectedCategoryId == action.categoryId) return
                _uiState.update {
                    it.copy(
                        selectedCategoryId = action.categoryId,
                        threads = persistentListOf(),
                        isLoading = true
                    )
                }
                load(page = 1, replaceExisting = true)
            }

            is ForumAction.OnSearchQueryChange -> {
                _uiState.update {
                    it.copy(
                        searchQuery = action.query,
                        threads = persistentListOf(),
                        isLoading = true
                    )
                }
                load(page = 1, replaceExisting = true)
            }

            is ForumAction.ToggleSaveThread -> {
                toggleSave(action.thread)
            }

            is ForumAction.ToggleSubscribeThread -> {
                toggleSubscribe(action.thread)
            }

            is ForumAction.ShowSnackbar -> {
                viewModelScope.launch { _actions.emit(action) }
            }
            // Navigation actions are forwarded to the UI layer
            else -> viewModelScope.launch { _actions.emit(action) }
        }
    }

    private fun loadSavedIds() {
        viewModelScope.launch {
            val saved = forumRepository.getSavedThreads()
            _uiState.update { it.copy(savedThreadIds = saved.map { t -> t.id }.toPersistentSet()) }
        }
    }

    private fun toggleSave(thread: com.anisync.android.domain.ForumThread) {
        viewModelScope.launch {
            val isSaved = forumRepository.isThreadSaved(thread.id)
            if (isSaved) {
                forumRepository.unsaveThread(thread.id)
                _uiState.update { it.copy(savedThreadIds = (it.savedThreadIds - thread.id).toPersistentSet()) }
            } else {
                forumRepository.saveThread(thread)
                _uiState.update { it.copy(savedThreadIds = (it.savedThreadIds + thread.id).toPersistentSet()) }
            }
            // If we're on the Saved feed, reload
            if (_uiState.value.selectedFeed == ForumFeed.SAVED) {
                load(page = 1, replaceExisting = true)
            }
        }
    }

    private fun toggleSubscribe(thread: ForumThread) {
        val wasSubscribed = thread.isSubscribed

        // Optimistic update — toggle isSubscribed on the thread in the list
        _uiState.update { state ->
            state.copy(
                threads = state.threads.map {
                    if (it.id == thread.id) it.copy(isSubscribed = !wasSubscribed) else it
                }.toPersistentList()
            )
        }

        viewModelScope.launch {
            val result = forumRepository.toggleThreadSubscription(thread.id, !wasSubscribed)
            if (result is Result.Error) {
                // Revert
                _uiState.update { state ->
                    state.copy(
                        threads = state.threads.map {
                            if (it.id == thread.id) it.copy(isSubscribed = wasSubscribed) else it
                        }.toPersistentList()
                    )
                }
                _actions.emit(ForumAction.ShowSnackbar(result.message))
            } else if (_uiState.value.selectedFeed == ForumFeed.SUBSCRIBED) {
                // Reload if viewing Subscribed feed
                load(page = 1, replaceExisting = true)
            }
        }
    }

    private fun load(page: Int, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            if (page == 1) _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value
            val feed = state.selectedFeed
            val categoryId = state.selectedCategoryId
            val search = state.searchQuery.takeIf { it.isNotBlank() }

            // SAVED feed is local-only — no API call
            if (feed == ForumFeed.SAVED) {
                val savedThreads = forumRepository.getSavedThreads()
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        threads = savedThreads.toPersistentList(),
                        hasNextPage = false,
                        currentPage = 1,
                        errorMessage = null
                    )
                }
                return@launch
            }

            // SUBSCRIBED feed uses API with subscribed = true
            if (feed == ForumFeed.SUBSCRIBED) {
                when (val result = forumRepository.getSubscribedThreads(page)) {
                    is Result.Success -> {
                        val data = result.data
                        _uiState.update { current ->
                            val updatedThreads = if (replaceExisting || page == 1) {
                                data.items.toPersistentList()
                            } else {
                                (current.threads + data.items).toPersistentList()
                            }
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                threads = updatedThreads,
                                hasNextPage = data.hasNextPage,
                                currentPage = data.currentPage,
                                errorMessage = null
                            )
                        }
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
                return@launch
            }

            // If a category or search is active, use getThreadsByCategory
            if (categoryId != null || search != null) {
                when (val result = forumRepository.getThreadsByCategory(categoryId, search, page)) {
                    is Result.Success -> {
                        val data = result.data
                        _uiState.update { current ->
                            val updatedThreads = if (replaceExisting || page == 1) {
                                data.items.toPersistentList()
                            } else {
                                (current.threads + data.items).toPersistentList()
                            }
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                threads = updatedThreads,
                                hasNextPage = data.hasNextPage,
                                currentPage = data.currentPage,
                                errorMessage = null
                            )
                        }
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
                return@launch
            }

            // Default: use sort-based feed (Overview/Recent/New)
            val sortParam = when (feed) {
                ForumFeed.OVERVIEW -> "IS_STICKY,REPLIED_AT_DESC"
                ForumFeed.RECENT -> "REPLIED_AT_DESC"
                ForumFeed.NEW -> "CREATED_AT_DESC"
                else -> "REPLIED_AT_DESC"
            }

            when (val result = forumRepository.getRecentThreads(page, sortParam)) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        val updatedThreads = if (replaceExisting || page == 1) {
                            data.items.toPersistentList()
                        } else {
                            (current.threads + data.items).toPersistentList()
                        }
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            threads = updatedThreads,
                            hasNextPage = data.hasNextPage,
                            currentPage = data.currentPage,
                            errorMessage = null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}