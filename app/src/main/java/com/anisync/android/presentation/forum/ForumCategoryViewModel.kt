package com.anisync.android.presentation.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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

    private val _actions = MutableSharedFlow<ForumCategoryAction>()
    val actions: SharedFlow<ForumCategoryAction> = _actions.asSharedFlow()

    private var categoryId: Int = 0

    init {
        // Debounce search query changes and re-fetch
        _uiState
            .map { it.searchQuery }
            .distinctUntilChanged()
            .drop(1) // Skip initial empty value
            .debounce(400.milliseconds)
            .onEach { load(page = 1, replaceExisting = true) }
            .launchIn(viewModelScope)
    }

    fun initialize(categoryId: Int, categoryName: String) {
        if (this.categoryId == categoryId) return // Already loaded
        this.categoryId = categoryId
        _uiState.update { it.copy(categoryName = categoryName) }
        load(page = 1, replaceExisting = true)
    }

    fun onAction(action: ForumCategoryAction) {
        when (action) {
            is ForumCategoryAction.Load -> initialize(action.categoryId, action.categoryName)
            is ForumCategoryAction.Refresh -> {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                load(page = 1, replaceExisting = true)
            }
            is ForumCategoryAction.LoadMore -> {
                if (!_uiState.value.hasNextPage) return
                load(page = _uiState.value.currentPage + 1)
            }
            is ForumCategoryAction.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = action.query) }
            }
            is ForumCategoryAction.ShowSnackbar -> {
                viewModelScope.launch { _actions.emit(action) }
            }
            else -> viewModelScope.launch { _actions.emit(action) }
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
                        val threads = if (replaceExisting || page == 1) data.items
                        else current.threads + data.items
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            threads = threads,
                            hasNextPage = data.hasNextPage,
                            currentPage = data.currentPage,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, errorMessage = result.message)
                    }
                }
            }
        }
    }
}
