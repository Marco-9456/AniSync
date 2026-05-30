package com.anisync.android.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.AiringEpisode
import com.anisync.android.domain.CalendarRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    appSettings: AppSettings
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage

    private val zoneId: ZoneId = ZoneId.systemDefault()

    // Unfiltered episodes for the displayed week — kept so the "following only" toggle
    // can re-bucket locally without a network round trip.
    private var rawEpisodes: List<AiringEpisode> = emptyList()

    private val _uiState = MutableStateFlow(CalendarUiState(weekStart = currentWeekStart()))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadWeek(_uiState.value.weekStart)
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            CalendarAction.PrevWeek -> changeWeek(_uiState.value.weekStart.minusWeeks(1))
            CalendarAction.NextWeek -> changeWeek(_uiState.value.weekStart.plusWeeks(1))
            CalendarAction.ThisWeek -> {
                val thisWeek = currentWeekStart()
                if (_uiState.value.weekStart != thisWeek) changeWeek(thisWeek)
            }

            CalendarAction.ToggleFollowingOnly -> _uiState.update { state ->
                val following = !state.followingOnly
                state.copy(
                    followingOnly = following,
                    days = buildDays(state.weekStart, zoneId, rawEpisodes, following)
                )
            }

            CalendarAction.Refresh -> loadWeek(_uiState.value.weekStart, isRefresh = true)
            CalendarAction.Retry -> loadWeek(_uiState.value.weekStart)
        }
    }

    private fun changeWeek(weekStart: LocalDate) {
        _uiState.update { it.copy(weekStart = weekStart, days = emptyList()) }
        loadWeek(weekStart)
    }

    private fun loadWeek(weekStart: LocalDate, isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, error = null) }

            val startSec = weekStart.atStartOfDay(zoneId).toEpochSecond()
            val endSec = weekStart.plusWeeks(1).atStartOfDay(zoneId).toEpochSecond()

            when (val result = calendarRepository.getWeekSchedule(startSec, endSec)) {
                is Result.Success -> {
                    rawEpisodes = result.data
                    _uiState.update { state ->
                        state.copy(
                            days = buildDays(weekStart, zoneId, rawEpisodes, state.followingOnly),
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                }

                is Result.Error -> {
                    rawEpisodes = emptyList()
                    _uiState.update {
                        it.copy(
                            days = buildDays(weekStart, zoneId, emptyList(), it.followingOnly),
                            isLoading = false,
                            isRefreshing = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    private fun currentWeekStart(): LocalDate =
        LocalDate.now(zoneId).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

/**
 * Pure helper: bucket [episodes] into the 7 days of the ISO week starting [weekStart].
 * Always returns exactly 7 [CalendarDay]s in order. Extracted for unit testing.
 */
internal fun buildDays(
    weekStart: LocalDate,
    zoneId: ZoneId,
    episodes: List<AiringEpisode>,
    followingOnly: Boolean
): List<CalendarDay> {
    val visible = if (followingOnly) episodes.filter { it.isOnList } else episodes
    val byDate = visible.groupBy {
        Instant.ofEpochSecond(it.airingAt).atZone(zoneId).toLocalDate()
    }
    return (0L until 7L).map { offset ->
        val date = weekStart.plusDays(offset)
        CalendarDay(
            date = date,
            episodes = byDate[date].orEmpty().sortedBy { it.airingAt }
        )
    }
}
