package com.anisync.android.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

class ToggleCalendarFilterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[CalendarFilterKey] ?: false
            prefs[CalendarFilterKey] = !current
        }
        WeeklyCalendarWidget().update(context, glanceId)
    }

    companion object {
        val CalendarFilterKey = booleanPreferencesKey("calendar_filter_my_list")
    }
}
