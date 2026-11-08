package com.anisync.android.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.RecapRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.RecapSettings
import com.anisync.android.worker.RecapWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.anisync.android.presentation.navigation.RecapStory
import com.anisync.android.presentation.recap.PreviewRecap
import com.anisync.android.presentation.recap.RecapPreviewStore
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class RecapBuilderViewModel @Inject constructor(
    private val recapRepository: RecapRepository,
    private val appSettings: AppSettings,
    private val previewStore: RecapPreviewStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _openStory = MutableStateFlow<RecapStory?>(null)

    /** The story route to open once, set when a preview is handed to its story. */
    val openStory: StateFlow<RecapStory?> = _openStory

    private val form = MutableStateFlow(
        RecapBuilderUiState(
            userId = DEFAULT_USER_ID,
            period = YearMonth.now().minusMonths(1).toString(),
            zone = ZoneId.systemDefault().id,
            previewAdult = appSettings.showAdultContent.value,
        )
    )

    val uiState: StateFlow<RecapBuilderUiState> = combine(
        form,
        recapRepository.observeRecaps(),
        appSettings.recapIncludeManga,
        appSettings.recapCountRepeats,
    ) { form, stored, manga, repeats ->
        form.copy(stored = stored, includeManga = manga, countRepeats = repeats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    fun onAction(action: RecapBuilderAction) {
        when (action) {
            is RecapBuilderAction.SetUserId -> form.update { it.copy(userId = action.value, inputError = false) }
            is RecapBuilderAction.SetPeriod -> form.update { it.copy(period = action.value, inputError = false) }
            is RecapBuilderAction.SetZone -> form.update { it.copy(zone = action.value, inputError = false) }
            is RecapBuilderAction.SetPreviewAdult -> form.update { it.copy(previewAdult = action.enabled) }
            RecapBuilderAction.Build -> build()
            RecapBuilderAction.OpenMine -> openMine()
            RecapBuilderAction.RunJob -> RecapWorker.runNow(context)
            RecapBuilderAction.ClearStored -> viewModelScope.launch {
                RecapWorker.cancelFirstBuild(context)
                recapRepository.clear()
            }
            is RecapBuilderAction.SetIncludeManga -> appSettings.setRecapIncludeManga(action.enabled)
            is RecapBuilderAction.SetCountRepeats -> appSettings.setRecapCountRepeats(action.enabled)
            RecapBuilderAction.OpenPreviewStory -> openPreviewStory()
            RecapBuilderAction.StoryOpened -> _openStory.value = null
        }
    }

    private fun build() {
        val current = form.value
        if (current.isBuilding) return
        val userId = current.userId.trim().toIntOrNull()?.takeIf { it > 0 }
        val period = RecapPeriod.fromKey(current.period.trim())
        val zone = runCatching { ZoneId.of(current.zone.trim()) }.getOrNull()
        if (userId == null || period == null || zone == null) {
            form.update { it.copy(inputError = true) }
            return
        }
        form.update { it.copy(isBuilding = true, buildError = null, preview = null, opened = null, openedNothing = false) }
        viewModelScope.launch {
            val settings = RecapSettings(
                includeManga = appSettings.recapIncludeManga.value,
                countRepeats = appSettings.recapCountRepeats.value,
                showAdult = current.previewAdult,
            )
            when (val result = recapRepository.previewRecap(userId, period, zone, settings)) {
                is Result.Success -> form.update { it.copy(isBuilding = false, preview = result.data) }
                is Result.Error -> form.update { it.copy(isBuilding = false, buildError = result.message) }
            }
        }
    }

    private fun openPreviewStory() {
        val preview = form.value.preview ?: return
        val recap = preview.recap ?: return
        previewStore.preview = PreviewRecap(
            recap = recap,
            previous = preview.previous,
            userName = preview.userName ?: form.value.userId,
            avatarUrl = preview.avatarUrl,
        )
        _openStory.value = RecapStory(periodKey = recap.period.key, preview = true)
    }

    /** The same period for the signed-in account, through the stored path a recap screen uses. */
    private fun openMine() {
        val current = form.value
        if (current.isBuilding) return
        val period = RecapPeriod.fromKey(current.period.trim()) ?: run {
            form.update { it.copy(inputError = true) }
            return
        }
        form.update { it.copy(isBuilding = true, buildError = null, preview = null, opened = null, openedNothing = false) }
        viewModelScope.launch {
            when (val result = recapRepository.getRecap(period)) {
                is Result.Success -> form.update {
                    it.copy(isBuilding = false, opened = result.data, openedNothing = result.data == null)
                }
                is Result.Error -> form.update { it.copy(isBuilding = false, buildError = result.message) }
            }
        }
    }

    private companion object {
        /** Goldiizz, whose September 2026 and 2025 the recap frames were drawn from. */
        const val DEFAULT_USER_ID = "287201"
    }
}
