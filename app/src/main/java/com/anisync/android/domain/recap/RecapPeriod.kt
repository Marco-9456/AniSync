package com.anisync.android.domain.recap

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** A month or a year that a recap covers, in the device's time zone. */
sealed interface RecapPeriod {
    val key: String
    val firstDay: LocalDate
    val lastDay: LocalDate
    val previous: RecapPeriod

    data class Month(val month: YearMonth) : RecapPeriod {
        override val key: String get() = month.toString()
        override val firstDay: LocalDate get() = month.atDay(1)
        override val lastDay: LocalDate get() = month.atEndOfMonth()
        override val previous: RecapPeriod get() = Month(month.minusMonths(1))
    }

    data class Year(val year: Int) : RecapPeriod {
        override val key: String get() = year.toString()
        override val firstDay: LocalDate get() = LocalDate.of(year, 1, 1)
        override val lastDay: LocalDate get() = LocalDate.of(year, 12, 31)
        override val previous: RecapPeriod get() = Year(year - 1)

        val months: List<Month> get() = (1..12).map { Month(YearMonth.of(year, it)) }
    }

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(firstDay) && !date.isAfter(lastDay)

    /** The period's span in epoch seconds, start inclusive and end exclusive. */
    fun window(zone: ZoneId): ActivityWindow = ActivityWindow(
        from = firstDay.atStartOfDay(zone).toEpochSecond(),
        to = lastDay.plusDays(1).atStartOfDay(zone).toEpochSecond(),
    )

    companion object {
        /** Reads a [key] back: "2026-09" is a month and "2025" a year. */
        fun fromKey(key: String): RecapPeriod? = when {
            key.length == 4 -> key.toIntOrNull()?.let(::Year)
            else -> runCatching { Month(YearMonth.parse(key)) }.getOrNull()
        }
    }
}
