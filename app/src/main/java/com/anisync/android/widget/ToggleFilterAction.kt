package com.anisync.android.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

class ToggleFilterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[FilterKey] ?: false
            prefs[FilterKey] = !current
        }
        AiringTodayWidget().update(context, glanceId)
    }

    companion object {
        val FilterKey = booleanPreferencesKey("filter_my_list")
    }
}
