package com.anisync.android.widget.core

import android.content.Context
import com.anisync.android.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Time formatting for the schedule widgets.
 *
 * Everything comes out of strings.xml instead of being glued together in English. The old widgets
 * did the latter, which is why a few of their labels never made it to Weblate.
 */
object WidgetTime {

    /** "In 2d 4h", "In 3h 12m", "In 8m", or the airing now label once the time has passed. */
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

    /**
     * Same countdown but one unit only, "2d", "5h", "8m", for lines sharing a row with other text.
     *
     * Rounds down, so "1d" means anything from 24 to 47 hours. None of this is a deadline and the
     * exact time is on the card in the app.
     */
    fun compactCountdown(context: Context, airingAtSeconds: Long, nowSeconds: Long): String {
        val remaining = airingAtSeconds - nowSeconds
        if (remaining <= 0) return context.getString(R.string.widget_time_airing_now)

        val days = remaining / DAY
        val hours = remaining / HOUR
        return when {
            days > 0 -> context.getString(R.string.widget_time_short_days, days)
            hours > 0 -> context.getString(R.string.widget_time_short_hours, hours)
            else -> context.getString(R.string.widget_time_short_minutes, remaining / MINUTE)
        }
    }

    /** Short weekday name for a timestamp, "Sat". */
    fun weekdayOf(airingAtSeconds: Long): String =
        SimpleDateFormat("EEE", Locale.getDefault()).format(Date(airingAtSeconds * 1000))

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

    /**
     * Airing time, always 24 hour.
     *
     * The calendar card has a fixed 48dp time column with no room for an AM/PM suffix. The airing
     * today row has the space, so it uses [clock] and follows the device setting.
     */
    fun clock24(airingAtSeconds: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(airingAtSeconds * 1000))

    /**
     * One letter weekday, "M", for the day strip.
     *
     * Falls back to the first letter of the short name where the narrow form is not defined, which
     * is what TextStyle.NARROW does anyway.
     */
    fun narrowWeekday(dayOffset: Int): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
        return calendar.getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.SHORT,
            Locale.getDefault()
        )?.take(1)?.uppercase(Locale.getDefault())
            ?: shortWeekday(dayOffset).take(1).uppercase(Locale.getDefault())
    }

    /** Short weekday name, "Mon", for the day strip content description. */
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
