package com.anisync.android.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.RecapRepository
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.worker.RecapWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecapsSettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val recapRepository: RecapRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<RecapsSettingsUiState> = combine(
        appSettings.recapMonthly,
        appSettings.recapYearly,
        appSettings.recapIncludeManga,
        appSettings.recapCountRepeats,
        appSettings.recapShowAdult,
    ) { monthly, yearly, manga, repeats, adult ->
        RecapsSettingsUiState(monthly = monthly, yearly = yearly, includeManga = manga, countRepeats = repeats, showAdult = adult)
    }.combine(
        combine(appSettings.showAdultContent, appSettings.recapThemeSong, appSettings.recapSongsOnMobileData, ::Triple)
    ) { state, (adultContent, song, mobileData) ->
        state.copy(adultContent = adultContent, themeSong = song, songsOnMobileData = mobileData)
    }.combine(
        combine(appSettings.recapDelivery, appSettings.notificationsEnabled, appSettings.recapTime, ::Triple)
    ) { state, (delivery, allowed, time) ->
        state.copy(delivery = delivery, notificationsAllowed = allowed, time = time)
    }.combine(
        combine(appSettings.hideScoresOnCards, appSettings.defaultCardSize, ::Pair)
    ) { state, (hideScores, size) ->
        state.copy(hideScoresOnCards = hideScores, defaultCardSize = size)
    }.combine(
        combine(recapRepository.observeRecaps(), recapRepository.observeStoredBytes(), ::Pair)
    ) { state, (recaps, bytes) ->
        state.copy(storedMonths = recaps.count { it.isFinal && it.period is RecapPeriod.Month }, storedBytes = bytes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), current())

    fun onAction(action: RecapsSettingsAction) {
        when (action) {
            is RecapsSettingsAction.SetMonthly -> {
                appSettings.setRecapMonthly(action.enabled)
                reschedule()
            }
            is RecapsSettingsAction.SetYearly -> {
                appSettings.setRecapYearly(action.enabled)
                reschedule()
            }
            is RecapsSettingsAction.SetTime -> {
                appSettings.setRecapTime(action.time)
                reschedule()
            }
            is RecapsSettingsAction.SetIncludeManga -> appSettings.setRecapIncludeManga(action.enabled)
            is RecapsSettingsAction.SetCountRepeats -> appSettings.setRecapCountRepeats(action.enabled)
            is RecapsSettingsAction.SetShowAdult -> appSettings.setRecapShowAdult(action.enabled)
            is RecapsSettingsAction.SetThemeSong -> appSettings.setRecapThemeSong(action.enabled)
            is RecapsSettingsAction.SetSongsOnMobileData -> appSettings.setRecapSongsOnMobileData(action.enabled)
            is RecapsSettingsAction.SetDelivery -> appSettings.setRecapDelivery(action.delivery)
            is RecapsSettingsAction.SetHideScoresOnCards -> appSettings.setHideScoresOnCards(action.hidden)
            is RecapsSettingsAction.SetDefaultCardSize -> appSettings.setDefaultCardSize(action.size)
            RecapsSettingsAction.ClearStored -> viewModelScope.launch {
                RecapWorker.cancelFirstBuild(context)
                recapRepository.clear()
            }
        }
    }

    /** The queued run was measured from the old time, and the job reads the switches when it runs. */
    private fun reschedule() {
        RecapWorker.schedule(context, appSettings.recapTime.value, ExistingWorkPolicy.REPLACE)
    }

    private fun current() = RecapsSettingsUiState(
        monthly = appSettings.recapMonthly.value,
        yearly = appSettings.recapYearly.value,
        includeManga = appSettings.recapIncludeManga.value,
        countRepeats = appSettings.recapCountRepeats.value,
        showAdult = appSettings.recapShowAdult.value,
        adultContent = appSettings.showAdultContent.value,
        themeSong = appSettings.recapThemeSong.value,
        songsOnMobileData = appSettings.recapSongsOnMobileData.value,
        delivery = appSettings.recapDelivery.value,
        notificationsAllowed = appSettings.notificationsEnabled.value,
        time = appSettings.recapTime.value,
        hideScoresOnCards = appSettings.hideScoresOnCards.value,
        defaultCardSize = appSettings.defaultCardSize.value,
    )
}
