package com.anisync.android.presentation.recap

import android.icu.text.MessageFormat
import android.text.format.DateFormat
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.anisync.android.R
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.formatHours
import com.anisync.android.domain.splitMinutes
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.lineBox
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Counts a sentence spells out rather than prints, "three of your 17 titles". */
private const val SPELLED_UP_TO = 20

@Composable
@ReadOnlyComposable
internal fun recapLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.ROOT

@Composable
@ReadOnlyComposable
internal fun count(value: Int): String = formatExactCount(value, recapLocale())

/**
 * A small count inside a sentence, spelled out in the app's language ("three", "thirteen"). Totals
 * that lead a page stay figures, so this is only for the counts a sentence tells in passing.
 */
@Composable
@ReadOnlyComposable
internal fun spelled(value: Int): String {
    val locale = recapLocale()
    if (value !in 0..SPELLED_UP_TO) return formatExactCount(value, locale)
    return spelledCounts.getOrPut(locale) { spellOut(locale) }[value]
}

/** Each language's counts spelled out, kept, since building the ICU formatter is the costly part. */
private val spelledCounts = ConcurrentHashMap<Locale, List<String>>()

/** ICU's MessageFormat reaches the spellout rules the SDK does not expose directly. */
private fun spellOut(locale: Locale): List<String> {
    val format = runCatching { MessageFormat("{0,spellout}", locale) }.getOrNull()
    return (0..SPELLED_UP_TO).map { value ->
        format?.let { runCatching { it.format(arrayOf<Any>(value)) }.getOrNull() } ?: formatExactCount(value, locale)
    }
}

internal fun String.capitalized(locale: Locale): String = replaceFirstChar { it.titlecase(locale) }

@Composable
@ReadOnlyComposable
internal fun episodesText(value: Int): String =
    pluralStringResourceOrDefault(R.plurals.share_recap_episode_count, value, count(value))

@Composable
@ReadOnlyComposable
internal fun titlesText(value: Int): String =
    pluralStringResourceOrDefault(R.plurals.share_stats_title_count, value, count(value))

/** "2 h 35 min", or "35 min" under an hour. */
@Composable
@ReadOnlyComposable
internal fun shortDuration(minutes: Int): String {
    val parts = splitMinutes(minutes)
    val hours = parts.days * 24 + parts.hours
    return if (hours > 0) {
        stringResource(R.string.share_recap_hours_minutes, count(hours), count(parts.minutes))
    } else {
        stringResource(R.string.share_recap_minutes, count(parts.minutes))
    }
}

/** "22 hours and 20 minutes", or "2 hours 35 minutes" without the and. */
@Composable
@ReadOnlyComposable
internal fun longDuration(minutes: Int, withAnd: Boolean): String {
    val parts = splitMinutes(minutes)
    val hours = parts.days * 24 + parts.hours
    val hourText = pluralStringResourceOrDefault(R.plurals.recap_hour_count, hours, count(hours))
    val minuteText = pluralStringResourceOrDefault(R.plurals.recap_minute_count, parts.minutes, count(parts.minutes))
    return when {
        hours == 0 -> minuteText
        parts.minutes == 0 -> hourText
        withAnd -> stringResource(R.string.notification_actors_two, hourText, minuteText)
        else -> stringResource(R.string.recap_duration_pair, hourText, minuteText)
    }
}

/** Hours to one decimal, "16.6 h". */
@Composable
@ReadOnlyComposable
internal fun decimalHours(minutes: Int): String = stringResource(R.string.stats_genre_hours, formatHours(minutes, recapLocale()))

/** Whole hours, "79 h". */
@Composable
@ReadOnlyComposable
internal fun wholeHours(minutes: Int): String = stringResource(R.string.stats_genre_hours, count(minutes / 60))

/** "A", "A and B", "A, B and C". */
@Composable
@ReadOnlyComposable
internal fun joinedNames(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names.first()
    else -> {
        var head = names.first()
        names.subList(1, names.lastIndex).forEach { head = stringResource(R.string.share_duration_list, head, it) }
        stringResource(R.string.notification_actors_two, head, names.last())
    }
}

@Composable
@ReadOnlyComposable
internal fun monthName(month: Month): String = month.getDisplayName(TextStyle.FULL_STANDALONE, recapLocale())

@Composable
@ReadOnlyComposable
internal fun weekdayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL_STANDALONE, recapLocale())

/** "20 September" in the app's language. */
@Composable
@ReadOnlyComposable
internal fun dayAndMonth(date: LocalDate): String {
    val locale = recapLocale()
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale))
}

/** "23 to 28 September", "September 23 to 28" where the month leads, or "30 March to 7 April". */
@Composable
@ReadOnlyComposable
internal fun dateRange(start: LocalDate, end: LocalDate): String {
    val locale = recapLocale()
    if (start.month != end.month || start.year != end.year) {
        return stringResource(R.string.recap_date_range, dayAndMonth(start), dayAndMonth(end))
    }
    val pattern = DateFormat.getBestDateTimePattern(locale, "dMMMM")
    val day = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "d"), locale)
    return if (pattern.indexOf('M') < pattern.indexOf('d')) {
        stringResource(R.string.recap_date_range, dayAndMonth(start), end.format(day))
    } else {
        stringResource(R.string.recap_date_range, start.format(day), dayAndMonth(end))
    }
}

@Composable
@ReadOnlyComposable
internal fun mostlyOn(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.MONDAY -> R.string.recap_mostly_monday
        DayOfWeek.TUESDAY -> R.string.recap_mostly_tuesday
        DayOfWeek.WEDNESDAY -> R.string.recap_mostly_wednesday
        DayOfWeek.THURSDAY -> R.string.recap_mostly_thursday
        DayOfWeek.FRIDAY -> R.string.recap_mostly_friday
        DayOfWeek.SATURDAY -> R.string.recap_mostly_saturday
        DayOfWeek.SUNDAY -> R.string.recap_mostly_sunday
    }
)

/**
 * Text in the line box the frames give it. A device grows a line to the font's extents, so a stack
 * of plain Text drifts lower with every line, 16dp by the end of WM1.
 */
@Composable
internal fun FrameText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(text = text, style = style, color = color, textAlign = textAlign, maxLines = maxLines, overflow = overflow, modifier = modifier.lineBox(style))
}
