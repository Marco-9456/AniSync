package com.anisync.android.data.anisyncplus

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniSyncPlusSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    private val _aniWorldCalendarEnabled =
        MutableStateFlow(preferences.getBoolean(KEY_CALENDAR_ENABLED, true))
    val aniWorldCalendarEnabled: StateFlow<Boolean> = _aniWorldCalendarEnabled.asStateFlow()

    private val _rememberCalendarFilter =
        MutableStateFlow(preferences.getBoolean(KEY_REMEMBER_FILTER, false))
    val rememberCalendarFilter: StateFlow<Boolean> = _rememberCalendarFilter.asStateFlow()

    private val _calendarFollowingOnly =
        MutableStateFlow(preferences.getBoolean(KEY_FOLLOWING_ONLY, false))
    val calendarFollowingOnly: StateFlow<Boolean> = _calendarFollowingOnly.asStateFlow()

    fun setAniWorldCalendarEnabled(enabled: Boolean) {
        _aniWorldCalendarEnabled.value = enabled
        preferences.edit { putBoolean(KEY_CALENDAR_ENABLED, enabled) }
    }

    fun setRememberCalendarFilter(enabled: Boolean) {
        _rememberCalendarFilter.value = enabled
        preferences.edit { putBoolean(KEY_REMEMBER_FILTER, enabled) }
        if (!enabled) setCalendarFollowingOnly(false)
    }

    fun setCalendarFollowingOnly(enabled: Boolean) {
        _calendarFollowingOnly.value = enabled
        if (rememberCalendarFilter.value) {
            preferences.edit { putBoolean(KEY_FOLLOWING_ONLY, enabled) }
        } else {
            preferences.edit { remove(KEY_FOLLOWING_ONLY) }
        }
    }

    companion object {
        const val PREFERENCE_NAME = "anisync_plus_settings"
        private const val KEY_CALENDAR_ENABLED = "aniworld_calendar_enabled"
        private const val KEY_REMEMBER_FILTER = "remember_calendar_filter"
        private const val KEY_FOLLOWING_ONLY = "calendar_following_only"
    }
}
