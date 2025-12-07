package com.anisync.android.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.GetNotificationsUseCase
import com.anisync.android.domain.Notification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data class Success(val notifications: List<Notification>) : NotificationUiState
    data class Error(val message: String) : NotificationUiState
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState

    init {
        fetchNotifications()
    }

    fun refresh() {
        fetchNotifications()
    }

    private fun fetchNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationUiState.Loading
            try {
                val notifications = getNotificationsUseCase()
                _uiState.value = NotificationUiState.Success(notifications)
            } catch (e: Exception) {
                _uiState.value = NotificationUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
