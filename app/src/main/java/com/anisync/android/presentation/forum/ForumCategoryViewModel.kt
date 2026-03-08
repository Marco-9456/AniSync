package com.anisync.android.presentation.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class ForumCategoryViewModel @Inject constructor(
    private val forumRepository: ForumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumCategoryUiState())
    val uiState: StateFlow<ForumCategoryUiState> = _uiState.asStateFlow()

    private val _actions = Channel<ForumCategoryAction>(Channel.BUFFERED)
    val actions: Flow<ForumCategoryAction> = _actions.receiveAsFlow()

    private var categoryId: Int = 0
    private var currentSort: String? = null

    init {
        _uiState
            .map { it.searchQuery }
            .distinctUntilChanged()
            .drop(1)
            .debounce(400.milliseconds)
            .onEach { load(page = 1, replaceExisting = true) }
            .launchIn(viewModelScope)
    }

    fun initialize(categoryId: Int, categoryName: String) {
        if (this.categoryId == categoryId) return // Already loaded
        this.categoryId = categoryId
        _uiState.update { it.copy(categoryName = categoryName) }
        loadSavedIds()
        load(page = 1, replaceExisting = true)
    }

    fun onAction(action: ForumCategoryAction) {
        when (action) {
            is ForumCategoryAction.Load -> initialize(action.categoryId, action.categoryName)
            is ForumCategoryAction.Refresh -> {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                loadSavedIds()
                load(page = 1, replaceExisting = true)
            }

            is ForumCategoryAction.LoadMore -> {
                if (!_uiState.value.hasNextPage || _uiState.value.isLoading || _uiState.value.isPaginating) return
                _uiState.update { it.copy(isPaginating = true) }
                load(page = _uiState.value.currentPage + 1)
            }

            is ForumCategoryAction.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = action.query) }
            }

            is ForumCategoryAction.ToggleSaveThread -> toggleSave(action.thread)
            is ForumCategoryAction.ToggleSubscribeThread -> toggleSubscribe(action.thread)
            is ForumCategoryAction.ChangeSort -> {
                currentSort = action.sort
                _uiState.update { it.copy(sortLabel = action.label, isLoading = true) }
                load(page = 1, replaceExisting = true)
            }

            is ForumCategoryAction.ShowSnackbar -> {
                viewModelScope.launch { _actions.send(action) }
            }

            else -> viewModelScope.launch { _actions.send(action) }
        }
    }

    private fun load(page: Int, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            if (page == 1 && !_uiState.value.isRefreshing) {
                _uiState.update { it.copy(isLoading = true) }
            }
            val query = _uiState.value.searchQuery.takeIf { it.isNotBlank() }

            when (val result = forumRepository.getThreadsByCategory(categoryId, query, page)) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        val threads =
                            if (replaceExisting || page == 1) data.items.toPersistentList()
                            else (current.threads + data.items).toPersistentList()
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            threads = threads,
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
                            isPaginating = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadSavedIds() {
        viewModelScope.launch {
            val saved = forumRepository.getSavedThreads()
            _uiState.update { it.copy(savedThreadIds = saved.map { t -> t.id }.toPersistentSet()) }
        }
    }

    private fun toggleSave(thread: ForumThread) {
        viewModelScope.launch {
            val isSaved = thread.id in _uiState.value.savedThreadIds
            if (isSaved) {
                forumRepository.unsaveThread(thread.id)
                _uiState.update { it.copy(savedThreadIds = (it.savedThreadIds - thread.id).toPersistentSet()) }
                _actions.send(ForumCategoryAction.ShowSnackbar("Thread unsaved"))
            } else {
                forumRepository.saveThread(thread)
                _uiState.update { it.copy(savedThreadIds = (it.savedThreadIds + thread.id).toPersistentSet()) }
                _actions.send(ForumCategoryAction.ShowSnackbar("Thread saved"))
            }
        }
    }

    private fun toggleSubscribe(thread: ForumThread) {
        viewModelScope.launch {
            val newState = !thread.isSubscribed
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    threads = state.threads.map { t ->
                        if (t.id == thread.id) t.copy(isSubscribed = newState) else t
                    }.toPersistentList()
                )
            }
            when (forumRepository.toggleThreadSubscription(thread.id, newState)) {
                is Result.Success -> {
                    _actions.send(
                        ForumCategoryAction.ShowSnackbar(
                            if (newState) "Subscribed to thread" else "Unsubscribed from thread"
                        )
                    )
                }

                is Result.Error -> {
                    // Revert optimistic update
                    _uiState.update { state ->
                        state.copy(
                            threads = state.threads.map { t ->
                                if (t.id == thread.id) t.copy(isSubscribed = !newState) else t
                            }.toPersistentList()
                        )
                    }
                    _actions.send(ForumCategoryAction.ShowSnackbar("Failed to update subscription"))
                }
            }
        }
    }
}