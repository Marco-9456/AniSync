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
import java.time.YearMonth
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

    // Same, for the expanded-width month grid (a wider 6-week window).
    private var rawMonthEpisodes: List<AiringEpisode> = emptyList()

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
                    days = buildDays(state.weekStart, zoneId, rawEpisodes, following),
                    // Re-bucket the month grid too so its dots + selected-day list stay in sync.
                    month = state.month?.copy(
                        days = buildDayBuckets(state.month.gridStart, GRID_DAYS, zoneId, rawMonthEpisodes, following)
                    )
                )
            }

            CalendarAction.Refresh -> loadWeek(_uiState.value.weekStart, isRefresh = true)
            CalendarAction.Retry -> loadWeek(_uiState.value.weekStart)

            // --- month grid (expanded two-pane) ---
            CalendarAction.EnsureMonthLoaded ->
                if (_uiState.value.month == null) loadMonth(currentMonthAnchor(), preferredSelection = null)

            CalendarAction.PrevMonth ->
                _uiState.value.month?.let { loadMonth(it.monthAnchor.minusMonths(1), preferredSelection = null) }

            CalendarAction.NextMonth ->
                _uiState.value.month?.let { loadMonth(it.monthAnchor.plusMonths(1), preferredSelection = null) }

            CalendarAction.ThisMonth -> {
                val anchor = currentMonthAnchor()
                if (_uiState.value.month?.monthAnchor != anchor) {
                    loadMonth(anchor, preferredSelection = null)
                } else {
                    _uiState.update { it.copy(month = it.month?.copy(selectedDate = LocalDate.now(zoneId))) }
                }
            }

            is CalendarAction.SelectDay ->
                _uiState.update { it.copy(month = it.month?.copy(selectedDate = action.date)) }

            CalendarAction.RefreshMonth ->
                _uiState.value.month?.let { loadMonth(it.monthAnchor, it.selectedDate, isRefresh = true) }

            CalendarAction.RetryMonth ->
                _uiState.value.month?.let { loadMonth(it.monthAnchor, it.selectedDate) }
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

    /**
     * Load the 6-week grid anchored on [monthAnchor]'s month (the same [GRID_DAYS]-day window the
     * grid renders). [preferredSelection] keeps the user's chosen day across a refresh/retry; when
     * null the selection defaults to today (if today is in this month) or the 1st.
     */
    private fun loadMonth(
        monthAnchor: LocalDate,
        preferredSelection: LocalDate?,
        isRefresh: Boolean = false
    ) {
        val anchor = monthAnchor.withDayOfMonth(1)
        val gridStart = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val selected = preferredSelection ?: defaultSelectionFor(anchor)

        _uiState.update { state ->
            val keepDays = isRefresh && state.month?.monthAnchor == anchor
            state.copy(
                month = CalendarMonthState(
                    monthAnchor = anchor,
                    gridStart = gridStart,
                    days = if (keepDays) state.month!!.days else emptyList(),
                    selectedDate = selected,
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    error = null
                )
            )
        }

        viewModelScope.launch {
            val startSec = gridStart.atStartOfDay(zoneId).toEpochSecond()
            val endSec = gridStart.plusDays(GRID_DAYS.toLong()).atStartOfDay(zoneId).toEpochSecond()

            val result = calendarRepository.getWeekSchedule(startSec, endSec)
            // Drop the result if a newer month nav has since replaced the in-flight one.
            if (_uiState.value.month?.monthAnchor != anchor) return@launch

            when (result) {
                is Result.Success -> {
                    rawMonthEpisodes = result.data
                    _uiState.update { state ->
                        state.copy(
                            month = state.month?.copy(
                                days = buildDayBuckets(gridStart, GRID_DAYS, zoneId, rawMonthEpisodes, state.followingOnly),
                                isLoading = false,
                                isRefreshing = false,
                                error = null
                            )
                        )
                    }
                }

                is Result.Error -> {
                    rawMonthEpisodes = emptyList()
                    _uiState.update { state ->
                        state.copy(
                            month = state.month?.copy(
                                days = buildDayBuckets(gridStart, GRID_DAYS, zoneId, emptyList(), state.followingOnly),
                                isLoading = false,
                                isRefreshing = false,
                                error = result.message
                            )
                        )
                    }
                }
            }
        }
    }

    /** Default selected day for a month: today when today falls in it, otherwise the 1st. */
    private fun defaultSelectionFor(monthAnchor: LocalDate): LocalDate {
        val today = LocalDate.now(zoneId)
        return if (YearMonth.from(today) == YearMonth.from(monthAnchor)) today else monthAnchor
    }

    private fun currentWeekStart(): LocalDate =
        LocalDate.now(zoneId).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun currentMonthAnchor(): LocalDate = LocalDate.now(zoneId).withDayOfMonth(1)

    private companion object {
        const val GRID_DAYS = 42 // 6 weeks
    }
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
): List<CalendarDay> = buildDayBuckets(weekStart, 7, zoneId, episodes, followingOnly)

/**
 * Pure helper: bucket [episodes] into [count] consecutive days starting at [start], returning
 * exactly [count] ordered [CalendarDay]s (empty days included). Backs both the 7-day week and the
 * 42-day month grid. [followingOnly] keeps only on-list episodes; each day is sorted by airing time.
 */
internal fun buildDayBuckets(
    start: LocalDate,
    count: Int,
    zoneId: ZoneId,
    episodes: List<AiringEpisode>,
    followingOnly: Boolean
): List<CalendarDay> {
    val visible = if (followingOnly) episodes.filter { it.isOnList } else episodes
    val byDate = visible.groupBy {
        Instant.ofEpochSecond(it.airingAt).atZone(zoneId).toLocalDate()
    }
    return (0 until count).map { offset ->
        val date = start.plusDays(offset.toLong())
        CalendarDay(
            date = date,
            episodes = byDate[date].orEmpty().sortedBy { it.airingAt }
        )
    }
}
