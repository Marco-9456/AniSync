package com.anisync.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.TitleLanguage
import com.anisync.android.data.account.AccountManager
import com.anisync.android.data.toLocalTitleLanguage
import com.anisync.android.domain.AniListTitleLanguage
import com.anisync.android.domain.AniListUserOptions
import com.anisync.android.domain.Result
import com.anisync.android.domain.SyncUserOptionsUseCase
import com.anisync.android.domain.UserOptionsPatch
import com.anisync.android.domain.UserOptionsRepository
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
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
    private val appSettings: AppSettings,
    private val accountManager: AccountManager,
    private val toastManager: ToastManager,
) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    private val _saving = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private data class ContentState(
        val options: AniListUserOptions?,
        val adultOverride: Boolean,
        val showAdult: Boolean,
        val titleOverride: Boolean,
        val titleLanguage: TitleLanguage,
    )

    val uiState: StateFlow<AniListOptionsUiState> = combine(
        combine(
            repository.cachedOptions,
            appSettings.adultContentOverrideEnabled,
            appSettings.showAdultContent,
            appSettings.titleLanguageOverrideEnabled,
            appSettings.titleLanguage,
        ) { options, adultOverride, showAdult, titleOverride, titleLanguage ->
            ContentState(options, adultOverride, showAdult, titleOverride, titleLanguage)
        },
        appSettings.staffNameLanguage,
        accountManager.activeAccount,
        _loading,
        combine(_saving, _error) { saving, error -> saving to error },
    ) { content, staffLanguage, account, loading, savingError ->
        AniListOptionsUiState(
            isLoading = loading,
            isSaving = savingError.first,
            isSignedIn = account != null,
            error = savingError.second,
            options = content.options,
            adultOverrideEnabled = content.adultOverride,
            localShowAdultContent = content.showAdult,
            titleLanguageOverrideEnabled = content.titleOverride,
            localTitleLanguage = content.titleLanguage,
            localStaffNameLanguage = staffLanguage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AniListOptionsUiState())

    init {
        refresh()
    }

    fun onAction(action: AniListOptionsAction) {
        when (action) {
            AniListOptionsAction.Refresh -> refresh()

            is AniListOptionsAction.SetAdultContent ->
                if (appSettings.adultContentOverrideEnabled.value) {
                    appSettings.setShowAdultContent(action.enabled)
                } else {
                    push(UserOptionsPatch(displayAdultContent = action.enabled))
                }

            is AniListOptionsAction.SetAdultOverrideEnabled -> {
                appSettings.setAdultContentOverrideEnabled(action.enabled)
                // Handing control back to AniList: snap the local value to the account value now.
                if (!action.enabled) {
                    repository.cachedOptions.value?.let { appSettings.setShowAdultContent(it.displayAdultContent) }
                }
            }

            is AniListOptionsAction.SetTitleLanguageAccount ->
                push(UserOptionsPatch(titleLanguage = action.language))

            is AniListOptionsAction.SetTitleLanguageOverrideEnabled -> {
                appSettings.setTitleLanguageOverrideEnabled(action.enabled)
                if (!action.enabled) {
                    repository.cachedOptions.value?.titleLanguage?.let {
                        appSettings.setTitleLanguage(it.toLocalTitleLanguage())
                    }
                }
            }

            is AniListOptionsAction.SetLocalTitleLanguage ->
                appSettings.setTitleLanguage(action.language)

            is AniListOptionsAction.SetStaffNameLanguage ->
                push(UserOptionsPatch(staffNameLanguage = action.language))

            is AniListOptionsAction.SetScoreFormat ->
                push(UserOptionsPatch(scoreFormat = action.format))

            is AniListOptionsAction.SetAiringNotifications ->
                push(UserOptionsPatch(airingNotifications = action.enabled))

            is AniListOptionsAction.SetRestrictMessagesToFollowing ->
                push(UserOptionsPatch(restrictMessagesToFollowing = action.enabled))

            is AniListOptionsAction.SetActivityMergeTime ->
                push(UserOptionsPatch(activityMergeTime = action.minutes))

            is AniListOptionsAction.SetListActivityDisabled -> {
                val current = repository.cachedOptions.value?.disabledListActivity.orEmpty()
                push(UserOptionsPatch(disabledListActivity = current + (action.status to action.disabled)))
            }

            is AniListOptionsAction.SetProfileColor ->
                push(UserOptionsPatch(profileColor = action.color))
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            if (accountManager.activeAccount.value == null) {
                _loading.value = false
                return@launch
            }
            _loading.value = true
            _error.value = null
            when (val result = syncUserOptions()) {
                is Result.Error -> _error.value = result.message
                is Result.Success -> Unit
            }
            _loading.value = false
        }
    }

    /** Pushes a partial edit to AniList. The repository updates the cache + mirror on success. */
    private fun push(patch: UserOptionsPatch) {
        viewModelScope.launch {
            _saving.value = true
            when (val result = repository.updateOptions(patch)) {
                is Result.Success -> toastManager.showToast(ToastType.SUCCESS, message = "Saved to AniList")
                is Result.Error -> toastManager.showResultError(result)
            }
            _saving.value = false
        }
    }
}
