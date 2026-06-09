package com.anisync.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.account.AccountManager
import com.anisync.android.domain.Result
import com.anisync.android.domain.SyncUserOptionsUseCase
import com.anisync.android.domain.UserOptionsPatch
import com.anisync.android.domain.UserOptionsRepository
import com.anisync.android.presentation.components.alert.ToastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AniListOptionsViewModel @Inject constructor(
    private val repository: UserOptionsRepository,
    private val syncUserOptions: SyncUserOptionsUseCase,
    private val accountManager: AccountManager,
    private val toastManager: ToastManager,
) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AniListOptionsUiState> = combine(
        repository.cachedOptions,
        repository.conflict,
        accountManager.activeAccount,
        _loading,
        _error,
    ) { options, conflict, account, loading, error ->
        AniListOptionsUiState(
            isLoading = loading,
            isSignedIn = account != null,
            error = error,
            options = options,
            conflict = conflict,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AniListOptionsUiState())

    init {
        refresh()
    }

    fun onAction(action: AniListOptionsAction) {
        when (action) {
            AniListOptionsAction.Refresh -> refresh()

            is AniListOptionsAction.SetAdultContent ->
                repository.applyEdit(UserOptionsPatch(displayAdultContent = action.enabled))

            is AniListOptionsAction.SetTitleLanguage ->
                repository.applyEdit(UserOptionsPatch(titleLanguage = action.language))

            is AniListOptionsAction.SetStaffNameLanguage ->
                repository.applyEdit(UserOptionsPatch(staffNameLanguage = action.language))

            is AniListOptionsAction.SetScoreFormat ->
                repository.applyEdit(UserOptionsPatch(scoreFormat = action.format))

            is AniListOptionsAction.SetAiringNotifications ->
                repository.applyEdit(UserOptionsPatch(airingNotifications = action.enabled))

            is AniListOptionsAction.SetRestrictMessagesToFollowing ->
                repository.applyEdit(UserOptionsPatch(restrictMessagesToFollowing = action.enabled))

            is AniListOptionsAction.SetActivityMergeTime ->
                repository.applyEdit(UserOptionsPatch(activityMergeTime = action.minutes))

            is AniListOptionsAction.SetListActivityDisabled -> {
                val current = repository.cachedOptions.value?.disabledListActivity.orEmpty()
                repository.applyEdit(
                    UserOptionsPatch(disabledListActivity = current + (action.status to action.disabled)),
                )
            }

            is AniListOptionsAction.SetProfileColor ->
                repository.applyEdit(UserOptionsPatch(profileColor = action.color))

            is AniListOptionsAction.ResolveConflict -> repository.resolveConflict(action.keepLocal)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            if (accountManager.activeAccount.value == null) {
                _loading.value = false
                return@launch
            }
            _loading.value = true
            when (val result = syncUserOptions()) {
                is Result.Error -> {
                    _error.value = result.message
                    toastManager.showResultError(result)
                }
                is Result.Success -> _error.value = null
            }
            _loading.value = false
        }
    }
}
