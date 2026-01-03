package com.anisync.android.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Granular notification preferences.
 * Allows users to enable/disable specific notification types independently.
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Watching list - new episodes for shows you're watching
    private val _watchingEnabled = MutableStateFlow(prefs.getBoolean(KEY_WATCHING_ENABLED, true))
    val watchingEnabled: StateFlow<Boolean> = _watchingEnabled.asStateFlow()

    // Planning list - when Episode 1 airs for shows in your planning list
    private val _planningEnabled = MutableStateFlow(prefs.getBoolean(KEY_PLANNING_ENABLED, true))
    val planningEnabled: StateFlow<Boolean> = _planningEnabled.asStateFlow()

    // Upcoming alerts - proactive "airing soon" notifications
    private val _upcomingEnabled = MutableStateFlow(prefs.getBoolean(KEY_UPCOMING_ENABLED, true))
    val upcomingEnabled: StateFlow<Boolean> = _upcomingEnabled.asStateFlow()

    fun setWatchingEnabled(enabled: Boolean) {
        _watchingEnabled.value = enabled
        prefs.edit().putBoolean(KEY_WATCHING_ENABLED, enabled).apply()
    }

    fun setPlanningEnabled(enabled: Boolean) {
        _planningEnabled.value = enabled
        prefs.edit().putBoolean(KEY_PLANNING_ENABLED, enabled).apply()
    }

    fun setUpcomingEnabled(enabled: Boolean) {
        _upcomingEnabled.value = enabled
        prefs.edit().putBoolean(KEY_UPCOMING_ENABLED, enabled).apply()
    }

    /**
     * Reset all notification preferences to default (all enabled).
     */
    fun resetToDefaults() {
        setWatchingEnabled(true)
        setPlanningEnabled(true)
        setUpcomingEnabled(true)
    }

    companion object {
        private const val PREFS_NAME = "notification_preferences"
        private const val KEY_WATCHING_ENABLED = "watching_enabled"
        private const val KEY_PLANNING_ENABLED = "planning_enabled"
        private const val KEY_UPCOMING_ENABLED = "upcoming_enabled"
    }
}
