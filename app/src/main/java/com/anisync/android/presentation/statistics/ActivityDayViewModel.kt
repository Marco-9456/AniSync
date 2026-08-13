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
import java.time.LocalDate
import javax.inject.Inject

/** Half a day in seconds — the offset the heatmap rounds timestamps by. */
private const val HALF_DAY_SECONDS = 43_200L
private const val DAY_SECONDS = 86_400L

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

        // AniList stamps each bucket at midnight in its own zone, so its own timestamp is the only
        // exact day boundary available. Without one, fall back to the window the heatmap buckets
        // into: noon UTC the day before, to noon UTC on the day itself.
        val from = day.epochSeconds ?: (day.date.toEpochDay() * DAY_SECONDS - HALF_DAY_SECONDS)
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = profileRepository.getUserDayActivities(userId, from, from + DAY_SECONDS)) {
                is Result.Success -> _activities.value = result.data
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }
}
