package com.anisync.android.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.anisyncplus.AniSyncPlusSettings
import com.anisync.android.domain.AiringEpisode
import com.anisync.android.domain.CalendarRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldCalendarRepository
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldRefreshCoordinator
import de.mrxxxxx.anisyncplus.calendar.api.AniWorldRefreshResult
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
    private val aniWorldRepository: AniWorldCalendarRepository,
    private val refreshCoordinator: AniWorldRefreshCoordinator,
    private val aniSyncPlusSettings: AniSyncPlusSettings,
    appSettings: AppSettings
) : ViewModel() {
    val titleLanguage = appSettings.titleLanguage
    private val zoneId: ZoneId = ZoneId.of("Europe/Berlin")
    private var rawEpisodes: List<AiringEpisode> = emptyList()
    private var rawMonthEpisodes: List<AiringEpisode> = emptyList()

    private val initialFollowingOnly = if (aniSyncPlusSettings.rememberCalendarFilter.value) {
        aniSyncPlusSettings.calendarFollowingOnly.value
    } else {
        false
    }
    private val _uiState = MutableStateFlow(
        CalendarUiState(
            weekStart = currentWeekStart(),
            followingOnly = initialFollowingOnly
        )
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            aniWorldRepository.observeSnapshot().collect { snapshot ->
                _uiState.update {
                    it.copy(
                        availableRangeStart = snapshot?.rangeStart,
                        availableRangeEnd = snapshot?.rangeEnd,
                        lastSuccessfulRefresh = snapshot?.fetchedAt
                    )
                }
                if (snapshot != null && !overlaps(
                        _uiState.value.weekStart,
                        _uiState.value.weekStart.plusDays(6),
                        snapshot.rangeStart,
                        snapshot.rangeEnd
                    )
                ) {
                    changeWeek(snapshot.rangeStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                } else {
                    loadWeek(_uiState.value.weekStart)
                }
            }
        }
        viewModelScope.launch {
            aniWorldRepository.observeSyncState().collect { sync ->
                _uiState.update {
                    it.copy(
                        lastSyncError = sync.lastErrorMessage,
                        isSyncing = false
                    )
                }
            }
        }
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            CalendarAction.PrevWeek -> {
                val target = _uiState.value.weekStart.minusWeeks(1)
                if (weekOverlapsAvailableRange(target)) changeWeek(target)
            }
            CalendarAction.NextWeek -> {
                val target = _uiState.value.weekStart.plusWeeks(1)
                if (weekOverlapsAvailableRange(target)) changeWeek(target)
            }
            CalendarAction.ThisWeek -> {
                val target = currentWeekStart()
                if (weekOverlapsAvailableRange(target) && target != _uiState.value.weekStart) changeWeek(target)
            }
            CalendarAction.ToggleFollowingOnly -> _uiState.update { state ->
                val following = !state.followingOnly
                aniSyncPlusSettings.setCalendarFollowingOnly(following)
                state.copy(
                    followingOnly = following,
                    days = buildDays(state.weekStart, zoneId, rawEpisodes, following),
                    month = state.month?.copy(
                        days = buildDayBuckets(state.month.gridStart, GRID_DAYS, zoneId, rawMonthEpisodes, following)
                    )
                )
            }
            CalendarAction.Refresh -> loadWeek(_uiState.value.weekStart, isRefresh = true)
            CalendarAction.Retry -> loadWeek(_uiState.value.weekStart, isRefresh = true)
            CalendarAction.EnsureMonthLoaded ->
                if (_uiState.value.month == null) {
                    val anchor = _uiState.value.availableRangeStart?.withDayOfMonth(1) ?: currentMonthAnchor()
                    loadMonth(anchor, preferredSelection = null)
                }
            CalendarAction.PrevMonth -> _uiState.value.month?.let {
                val target = it.monthAnchor.minusMonths(1)
                if (monthOverlapsAvailableRange(target)) loadMonth(target, preferredSelection = null)
            }
            CalendarAction.NextMonth -> _uiState.value.month?.let {
                val target = it.monthAnchor.plusMonths(1)
                if (monthOverlapsAvailableRange(target)) loadMonth(target, preferredSelection = null)
            }
            CalendarAction.ThisMonth -> {
                val anchor = currentMonthAnchor()
                if (monthOverlapsAvailableRange(anchor)) {
                    if (_uiState.value.month?.monthAnchor != anchor) loadMonth(anchor, preferredSelection = null)
                    else _uiState.update { it.copy(month = it.month?.copy(selectedDate = LocalDate.now(zoneId))) }
                }
            }
            is CalendarAction.SelectDay -> {
                if (dateInsideAvailableRange(action.date)) {
                    _uiState.update { it.copy(month = it.month?.copy(selectedDate = action.date)) }
                }
            }
            CalendarAction.RefreshMonth ->
                _uiState.value.month?.let { loadMonth(it.monthAnchor, it.selectedDate, isRefresh = true) }
            CalendarAction.RetryMonth ->
                _uiState.value.month?.let { loadMonth(it.monthAnchor, it.selectedDate, isRefresh = true) }
        }
    }

    private fun changeWeek(weekStart: LocalDate) {
        _uiState.update { it.copy(weekStart = weekStart, days = emptyList()) }
        loadWeek(weekStart)
    }

    private fun loadWeek(weekStart: LocalDate, isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh && it.days.isEmpty(),
                    isRefreshing = isRefresh,
                    isSyncing = isRefresh,
                    error = null
                )
            }
            val refreshError = if (isRefresh) refreshError() else null
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
                            isSyncing = false,
                            error = refreshError
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isSyncing = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadMonth(
        monthAnchor: LocalDate,
        preferredSelection: LocalDate?,
        isRefresh: Boolean = false
    ) {
        val anchor = monthAnchor.withDayOfMonth(1)
        val gridStart = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val selected = preferredSelection?.takeIf(::dateInsideAvailableRange)
            ?: defaultSelectionFor(anchor)
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
                ),
                isSyncing = isRefresh
            )
        }
        viewModelScope.launch {
            val refreshError = if (isRefresh) refreshError() else null
            val startSec = gridStart.atStartOfDay(zoneId).toEpochSecond()
            val endSec = gridStart.plusDays(GRID_DAYS.toLong()).atStartOfDay(zoneId).toEpochSecond()
            val result = calendarRepository.getWeekSchedule(startSec, endSec)
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
                                error = refreshError
                            ),
                            isSyncing = false
                        )
                    }
                }
                is Result.Error -> _uiState.update { state ->
                    state.copy(
                        month = state.month?.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.message
                        ),
                        isSyncing = false
                    )
                }
            }
        }
    }

    private suspend fun refreshError(): String? = when (val result = refreshCoordinator.refresh()) {
        is AniWorldRefreshResult.Success -> null
        is AniWorldRefreshResult.Failure -> result.message
    }

    private fun defaultSelectionFor(monthAnchor: LocalDate): LocalDate {
        val today = LocalDate.now(zoneId)
        val preferred = if (YearMonth.from(today) == YearMonth.from(monthAnchor)) today else monthAnchor
        if (dateInsideAvailableRange(preferred)) return preferred
        return _uiState.value.availableRangeStart?.coerceAtLeast(monthAnchor) ?: preferred
    }

    private fun weekOverlapsAvailableRange(weekStart: LocalDate): Boolean {
        val state = _uiState.value
        val start = state.availableRangeStart ?: return false
        val end = state.availableRangeEnd ?: return false
        return overlaps(weekStart, weekStart.plusDays(6), start, end)
    }

    private fun monthOverlapsAvailableRange(monthAnchor: LocalDate): Boolean {
        val state = _uiState.value
        val start = state.availableRangeStart ?: return false
        val end = state.availableRangeEnd ?: return false
        val month = YearMonth.from(monthAnchor)
        return overlaps(month.atDay(1), month.atEndOfMonth(), start, end)
    }

    private fun dateInsideAvailableRange(date: LocalDate): Boolean {
        val start = _uiState.value.availableRangeStart ?: return false
        val end = _uiState.value.availableRangeEnd ?: return false
        return date in start..end
    }

    private fun overlaps(
        leftStart: LocalDate,
        leftEnd: LocalDate,
        rightStart: LocalDate,
        rightEnd: LocalDate
    ): Boolean = leftStart <= rightEnd && rightStart <= leftEnd

    private fun currentWeekStart(): LocalDate =
        LocalDate.now(zoneId).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun currentMonthAnchor(): LocalDate = LocalDate.now(zoneId).withDayOfMonth(1)

    private companion object {
        const val GRID_DAYS = 42
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
        it.sourceDateEpochDay?.let(LocalDate::ofEpochDay)
            ?: Instant.ofEpochSecond(it.airingAt).atZone(zoneId).toLocalDate()
    }
    return (0 until count).map { offset ->
        val date = start.plusDays(offset.toLong())
        CalendarDay(
            date = date,
            episodes = byDate[date].orEmpty().sortedWith(
                compareBy<AiringEpisode> {
                    it.sourceLocalTimeMinutes
                        ?: it.airingAt.takeIf { epoch -> epoch > 0L }
                            ?.let { epoch -> Instant.ofEpochSecond(epoch).atZone(zoneId).toLocalTime() }
                            ?.let { time -> time.hour * 60 + time.minute }
                        ?: Int.MAX_VALUE
                }.thenBy { it.id }
            )
        )
    }
}
