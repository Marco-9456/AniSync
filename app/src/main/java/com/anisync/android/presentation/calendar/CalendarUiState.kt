package com.anisync.android.presentation.calendar

import androidx.compose.runtime.Immutable
import com.anisync.android.domain.AiringEpisode
import java.time.LocalDate

/**
 * State for the airing calendar screen. One [weekStart]-anchored ISO week (Monday-first)
 * is shown at a time; [days] always holds exactly 7 entries (empty days included) so the
 * day-tab row and pager are stable.
 *
 * [month] is a parallel model used only by the expanded-width two-pane layout (a month grid
 * beside the selected day's airings). It stays null until that layout first requests it, so
 * the compact week pager never pays for the wider month fetch.
 */
@Immutable
data class CalendarUiState(
    val weekStart: LocalDate = LocalDate.now(),
    val days: List<CalendarDay> = emptyList(),
    val followingOnly: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val month: CalendarMonthState? = null
)

/** A single day of the displayed week and the episodes airing on it (sorted by time). */
@Immutable
data class CalendarDay(
    val date: LocalDate,
    val episodes: List<AiringEpisode>
)

/**
 * State for the expanded-width month grid. [days] is the 6-week (42-cell) Monday-first grid
 * starting at [gridStart] (the Monday on or before the 1st of [monthAnchor]'s month); cells
 * outside the anchored month are dimmed by the UI. [selectedDate] is the day whose airings the
 * detail pane shows (defaults to today when today is in view, else the 1st of the month).
 */
@Immutable
data class CalendarMonthState(
    val monthAnchor: LocalDate,
    val gridStart: LocalDate,
    val days: List<CalendarDay> = emptyList(),
    val selectedDate: LocalDate = monthAnchor,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    /** The grid cell for [selectedDate] (an empty day if the grid hasn't loaded it). */
    val selectedDay: CalendarDay
        get() = days.firstOrNull { it.date == selectedDate } ?: CalendarDay(selectedDate, emptyList())
}

sealed interface CalendarAction {
    data object PrevWeek : CalendarAction
    data object NextWeek : CalendarAction
    data object ThisWeek : CalendarAction
    data object ToggleFollowingOnly : CalendarAction
    data object Refresh : CalendarAction
    data object Retry : CalendarAction

    // Month grid (expanded-width two-pane) actions.
    /** Load the current month the first time the wide layout mounts (no-op if already loaded). */
    data object EnsureMonthLoaded : CalendarAction
    data object PrevMonth : CalendarAction
    data object NextMonth : CalendarAction
    data object ThisMonth : CalendarAction
    data class SelectDay(val date: LocalDate) : CalendarAction
    data object RefreshMonth : CalendarAction
    data object RetryMonth : CalendarAction
}
