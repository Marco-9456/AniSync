package com.anisync.android.widget.actions

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.anisync.android.widget.WeeklyCalendarWidget

/**
 * Action callback to toggle the "Expand Today" state in WeeklyCalendarWidget.
 * When expanded, shows all episodes for today instead of just the first 4.
 */
class ToggleExpandTodayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[ExpandTodayKey] ?: false
            prefs[ExpandTodayKey] = !current
        }
        WeeklyCalendarWidget().update(context, glanceId)
    }

    companion object {
        val ExpandTodayKey = booleanPreferencesKey("calendar_expand_today")
    }
}
