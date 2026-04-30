package com.anisync.android.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.GetNotificationsUseCase
import com.anisync.android.domain.NotificationFilter
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotifications: GetNotificationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var nextPage = 1
    private var loadJob: Job? = null

    init {
        load(reset = true, isInitial = true)
    }

    fun onAction(action: NotificationsAction) {
        when (action) {
            is NotificationsAction.SetFilter -> {
                if (action.filter == _uiState.value.filter) return
                _uiState.update { it.copy(filter = action.filter) }
                load(reset = true, isInitial = true)
            }
            NotificationsAction.Refresh -> load(reset = true, isInitial = false, refreshing = true)
            NotificationsAction.LoadNextPage -> {
                val s = _uiState.value
                if (!s.hasNextPage || s.isLoading || s.isPaginating || s.isRefreshing) return
                load(reset = false, isInitial = false)
            }
            NotificationsAction.Retry -> load(reset = true, isInitial = true)
        }
    }

    private fun load(reset: Boolean, isInitial: Boolean, refreshing: Boolean = false) {
        loadJob?.cancel()
        if (reset) nextPage = 1

        _uiState.update {
            it.copy(
                isLoading = isInitial,
                isRefreshing = refreshing,
                isPaginating = !isInitial && !refreshing,
                errorMessage = null
            )
        }

        val filter = _uiState.value.filter
        // Reset unread count only on first page load with the All filter (matches AniList web behavior).
        val resetUnread = reset && filter == NotificationFilter.ALL

        loadJob = viewModelScope.launch {
            val result = getNotifications.getPage(
                page = nextPage,
                typeFilter = filter.types,
                resetUnreadCount = resetUnread
            )
            when (result) {
                is Result.Success -> {
                    val page = result.data
                    _uiState.update { state ->
                        val merged = if (reset) page.items else state.items + page.items
                        state.copy(
                            items = merged,
                            entries = groupNotifications(merged),
                            isLoading = false,
                            isRefreshing = false,
                            isPaginating = false,
                            hasNextPage = page.hasNextPage,
                            errorMessage = null
                        )
                    }
                    if (page.hasNextPage) nextPage++
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
}
