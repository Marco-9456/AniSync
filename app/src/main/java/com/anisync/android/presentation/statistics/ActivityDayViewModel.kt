package com.anisync.android.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.UserActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * AniList counts each day of the activity history from midnight in London (its buckets sit at 23:00
 * UTC in summer and 00:00 UTC in winter), so a day there runs 23 or 25 hours when the clocks change.
 */
private val ANILIST_DAY_ZONE: ZoneId = ZoneId.of("Europe/London")

/**
 * The seconds AniList counted into [day]'s bar, from its own bucket timestamp when there is one, as
 * `start` until the next London midnight.
 */
internal fun aniListDayWindow(day: ActivityDay): Pair<Long, Long> {
    val start = day.epochSeconds ?: day.date.atStartOfDay(ANILIST_DAY_ZONE).toEpochSecond()
    val date = Instant.ofEpochSecond(start).atZone(ANILIST_DAY_ZONE).toLocalDate()
    return start to date.plusDays(1).atStartOfDay(ANILIST_DAY_ZONE).toEpochSecond()
}

/**
 * Loads the activities behind one day of the Activity History week breakdown.
 *
 * Sheet-scoped rather than part of `ProfileViewModel`, matching the app's other on-demand sheets,
 * so the profile screen carries no state for a list that only exists while the sheet is open.
 */
@HiltViewModel
class ActivityDayViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _activities = MutableStateFlow<List<UserActivity>>(emptyList())
    val activities: StateFlow<List<UserActivity>> = _activities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loaded: Pair<Int, LocalDate>? = null

    internal fun load(userId: Int, day: ActivityDay) {
        if (loaded == userId to day.date) return
        loaded = userId to day.date
        _activities.value = emptyList()
        _errorMessage.value = null

        val (from, to) = aniListDayWindow(day)
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = profileRepository.getUserDayActivities(userId, from, to)) {
                is Result.Success -> _activities.value = result.data
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }
}
