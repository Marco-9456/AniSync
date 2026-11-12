package com.anisync.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.SupportPromptStore
import com.anisync.android.domain.SupportersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val supportersRepository: SupportersRepository,
    private val supportPrompt: SupportPromptStore,
) : ViewModel() {

    val uiState: StateFlow<SupportUiState> = combine(
        supportersRepository.observeSupporters(),
        supportPrompt.remindersEnabled,
    ) { supporters, reminders ->
        SupportUiState(supporters = supporters, remindersEnabled = reminders)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SupportUiState(remindersEnabled = supportPrompt.remindersEnabled.value),
    )

    init {
        viewModelScope.launch { supportersRepository.refreshSupporters() }
    }

    fun onAction(action: SupportAction) {
        when (action) {
            SupportAction.TipOpened -> supportPrompt.onTipOpened()
            is SupportAction.SetRemindersEnabled -> supportPrompt.setRemindersEnabled(action.enabled)
        }
    }
}
