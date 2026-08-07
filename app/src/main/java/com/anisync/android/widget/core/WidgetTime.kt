package com.anisync.android.widget.core

import android.content.Context
import com.anisync.android.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Time formatting shared by the schedule-driven widgets.
 *
 * Everything here reads from `strings.xml` rather than building English by concatenation, which is
 * how the previous widgets did it and why several of their labels never reached Weblate.
 */
object WidgetTime {

    /** "In 2d 4h", "In 3h 12m", "In 8m", or the airing-now label once the timestamp has passed. */
    fun countdown(context: Context, airingAtSeconds: Long, nowSeconds: Long): String {
        val remaining = airingAtSeconds - nowSeconds
        if (remaining <= 0) return context.getString(R.string.widget_time_airing_now)

        val days = remaining / DAY
        val hours = (remaining % DAY) / HOUR
        val minutes = (remaining % HOUR) / MINUTE
        return when {
            days > 0 -> context.getString(R.string.widget_time_in_days, days, hours)
            hours > 0 -> context.getString(R.string.widget_time_in_hours, hours, minutes)
            else -> context.getString(R.string.widget_time_in_minutes, minutes)
        }
    }

    /** Wall-clock airing time in the device's locale and 12/24 hour setting. */
    fun clock(context: Context, airingAtSeconds: Long): String {
        val pattern = if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault())
            .format(Date(airingAtSeconds * 1000))
    }

    /** Midnight today, in epoch seconds, in the device's time zone. */
    fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis / 1000

    /** Midnight [dayOffset] days from today, in epoch seconds. */
    fun startOfDay(dayOffset: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, dayOffset)
    }.timeInMillis / 1000

    /** Short weekday name, "Mon", for the calendar day strip. */
    fun shortWeekday(dayOffset: Int): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
        return SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
    }

    /** Day of the month, for the calendar day strip. */
    fun dayOfMonth(dayOffset: Int): Int =
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            .get(Calendar.DAY_OF_MONTH)

    const val DAY = 24L * 60 * 60
    private const val HOUR = 60L * 60
    private const val MINUTE = 60L
}
