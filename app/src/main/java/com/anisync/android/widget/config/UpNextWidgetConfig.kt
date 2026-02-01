package com.anisync.android.widget.config

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

/**
 * Configuration keys for the UpNext widget.
 * These keys are stored in Glance's DataStore preferences per widget instance.
 */
object UpNextWidgetConfig {
    /**
     * Whether to show countdown timers on widget cards.
     * Default: true
     */
    val ShowCountdownKey = booleanPreferencesKey("show_countdown")
    
    /**
     * Maximum number of items to display in the expanded widget.
     * Range: 3-10, Default: 5
     */
    val MaxItemsKey = intPreferencesKey("max_items")
    
    /**
     * Whether to include "Planning" status entries in the widget.
     * Default: false (only show "Watching" entries)
     */
    val IncludePlanningKey = booleanPreferencesKey("include_planning")
    
    /**
     * Whether to show "Available Now" entries (episodes that have already aired).
     * Default: true
     */
    val ShowAvailableNowKey = booleanPreferencesKey("show_available_now")
    
    // Default values
    const val DEFAULT_SHOW_COUNTDOWN = true
    const val DEFAULT_MAX_ITEMS = 5
    const val DEFAULT_INCLUDE_PLANNING = false
    const val DEFAULT_SHOW_AVAILABLE_NOW = true
    
    const val MIN_ITEMS = 3
    const val MAX_ITEMS = 10
}
