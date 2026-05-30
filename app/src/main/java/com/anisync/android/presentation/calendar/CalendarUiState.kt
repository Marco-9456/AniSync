package com.anisync.android.presentation.calendar

import androidx.compose.runtime.Immutable
import com.anisync.android.domain.AiringEpisode
import java.time.LocalDate

/**
 * State for the airing calendar screen. One [weekStart]-anchored ISO week (Monday-first)
 * is shown at a time; [days] always holds exactly 7 entries (empty days included) so the
 * day-tab row and pager are stable.
 */
@Immutable
data class CalendarUiState(
    val weekStart: LocalDate = LocalDate.now(),
    val days: List<CalendarDay> = emptyList(),
    val followingOnly: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

/** A single day of the displayed week and the episodes airing on it (sorted by time). */
@Immutable
data class CalendarDay(
    val date: LocalDate,
    val episodes: List<AiringEpisode>
)

sealed interface CalendarAction {
    data object PrevWeek : CalendarAction
    data object NextWeek : CalendarAction
    data object ThisWeek : CalendarAction
    data object ToggleFollowingOnly : CalendarAction
    data object Refresh : CalendarAction
    data object Retry : CalendarAction
}
