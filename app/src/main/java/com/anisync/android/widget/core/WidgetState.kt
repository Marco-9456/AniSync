package com.anisync.android.widget.core

import android.content.Context
import androidx.core.content.edit

/**
 * Per instance widget state in SharedPreferences, read synchronously.
 *
 * Keyed by appWidgetId, since two copies of a widget keep their own filter and selected day.
 *
 * Synchronous on purpose. The tap is already inside a broadcast receiver with execution time, and
 * the next thing it does is read this back to render. Anything async here brings back the window
 * where the write and the render disagree, which is the bug we set out to kill. The file is tiny and
 * commit() on the receiver thread costs well under a millisecond.
 */
object WidgetState {

    private const val FILE = "anisync_widget_state"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun key(appWidgetId: Int, name: String) = "${appWidgetId}_$name"

    fun getBoolean(context: Context, appWidgetId: Int, name: String, default: Boolean): Boolean =
        prefs(context).getBoolean(key(appWidgetId, name), default)

    fun getInt(context: Context, appWidgetId: Int, name: String, default: Int): Int =
        prefs(context).getInt(key(appWidgetId, name), default)

    fun getString(context: Context, appWidgetId: Int, name: String, default: String): String =
        prefs(context).getString(key(appWidgetId, name), default) ?: default

    /** Applies one encoded value from a tap intent. False if the payload is unusable. */
    fun apply(context: Context, appWidgetId: Int, name: String, value: String): Boolean {
        val editor = prefs(context).edit()
        when {
            value == "true" || value == "false" ->
                editor.putBoolean(key(appWidgetId, name), value.toBoolean())

            value.toIntOrNull() != null ->
                editor.putInt(key(appWidgetId, name), value.toInt())

            value.isNotEmpty() -> editor.putString(key(appWidgetId, name), value)

            else -> return false
        }
        return editor.commit()
    }

    /** Drops one instance. Called from onDeleted so a removed widget leaves nothing behind. */
    fun clear(context: Context, appWidgetId: Int) {
        val store = prefs(context)
        val prefix = "${appWidgetId}_"
        store.edit {
            store.all.keys.filter { it.startsWith(prefix) }.forEach { remove(it) }
        }
    }
}
