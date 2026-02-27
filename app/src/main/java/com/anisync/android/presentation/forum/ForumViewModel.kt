package com.anisync.android.presentation.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
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
                _uiState.update { it.copy(selectedFeed = action.feed, threads = emptyList(), isLoading = true) }
                load(page = 1, replaceExisting = true)
            }
            is ForumAction.ShowSnackbar -> {
                viewModelScope.launch { _actions.emit(action) }
            }
            // Navigation actions are forwarded to the UI layer
            else -> viewModelScope.launch { _actions.emit(action) }
        }
    }

    private fun load(page: Int, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            if (page == 1) _uiState.update { it.copy(isLoading = true) }

            val feed = _uiState.value.selectedFeed
            val sortParam = when (feed) {
                ForumFeed.OVERVIEW -> "IS_STICKY,REPLIED_AT_DESC"
                ForumFeed.RECENT -> "REPLIED_AT_DESC"
                ForumFeed.NEW -> "CREATED_AT_DESC"
            }

            when (val result = forumRepository.getRecentThreads(page, sortParam)) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        val updatedThreads = if (replaceExisting || page == 1) {
                            data.items
                        } else {
                            current.threads + data.items
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
