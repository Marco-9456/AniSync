package com.anisync.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.SponsorsRepository
import com.anisync.android.domain.Sponsor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SponsorsViewModel @Inject constructor(
    private val repo: SponsorsRepository
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Ready(
            val sponsors: List<Sponsor>,
            val updatedAt: String,
            val isRefreshing: Boolean = false
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repo.loadBundled() }
                .onSuccess { _state.value = UiState.Ready(it.sponsors, it.updatedAt) }
                .onFailure {
                    _state.value = UiState.Error(it.message ?: "Failed to load sponsors")
                }
        }
    }

    fun refresh() {
        val current = _state.value as? UiState.Ready ?: return
        if (current.isRefreshing) return
        _state.value = current.copy(isRefreshing = true)
        viewModelScope.launch {
            repo.refreshFromRemote()
                .onSuccess {
                    _state.value = UiState.Ready(it.sponsors, it.updatedAt)
                }
                .onFailure {
                    _state.value = current.copy(isRefreshing = false)
                }
        }
    }
}
