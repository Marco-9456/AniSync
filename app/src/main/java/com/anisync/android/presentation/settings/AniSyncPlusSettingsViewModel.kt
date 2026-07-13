package com.anisync.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.anisyncplus.AniSyncPlusSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarRepository
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldRefreshCoordinator
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldRefreshResult
import de.mrxxxxx.anisyncplus.calendar.domain.AniWorldSyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AniSyncPlusSettingsUiState(
    val calendarEnabled: Boolean = true,
    val rememberFilter: Boolean = false,
    val sync: AniWorldSyncState = AniWorldSyncState(),
    val refreshing: Boolean = false
)

sealed interface AniSyncPlusSettingsAction {
    data class SetCalendarEnabled(val enabled: Boolean) : AniSyncPlusSettingsAction
    data class SetRememberFilter(val enabled: Boolean) : AniSyncPlusSettingsAction
    data object Refresh : AniSyncPlusSettingsAction
}

@HiltViewModel
class AniSyncPlusSettingsViewModel @Inject constructor(
    private val settings: AniSyncPlusSettings,
    repository: AniWorldCalendarRepository,
    private val refreshCoordinator: AniWorldRefreshCoordinator
) : ViewModel() {
    private val refreshing = MutableStateFlow(false)
    val uiState = combine(
        settings.aniWorldCalendarEnabled,
        settings.rememberCalendarFilter,
        repository.observeSyncState(),
        refreshing
    ) { enabled, remember, sync, isRefreshing ->
        AniSyncPlusSettingsUiState(enabled, remember, sync, isRefreshing)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AniSyncPlusSettingsUiState()
    )

    fun onAction(action: AniSyncPlusSettingsAction) {
        when (action) {
            is AniSyncPlusSettingsAction.SetCalendarEnabled ->
                settings.setAniWorldCalendarEnabled(action.enabled)
            is AniSyncPlusSettingsAction.SetRememberFilter ->
                settings.setRememberCalendarFilter(action.enabled)
            AniSyncPlusSettingsAction.Refresh -> {
                if (refreshing.value) return
                viewModelScope.launch {
                    refreshing.value = true
                    try {
                        refreshCoordinator.refresh()
                    } finally {
                        refreshing.value = false
                    }
                }
            }
        }
    }
}
