package com.anisync.android.data.mapper

import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.FuzzyDateInput
import com.anisync.android.type.MediaListStatus
import com.apollographql.apollo.api.Optional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private val SYSTEM_ZONE: ZoneId = ZoneId.systemDefault()

/*
 * A FuzzyDate carries a pure calendar date (year/month/day) — no time, no zone.
 * We anchor it to UTC midnight so it round-trips cleanly with Material3's
 * DatePicker, whose selectedDateMillis is always UTC-based. Interpreting these
 * millis in the system zone (the previous behaviour) shifted the date by the UTC
 * offset — picking the 3rd stored the 2nd for users behind UTC. See issue #85.
 */

fun MediaListStatus.toDomainStatus(): LibraryStatus {
    return when (this) {
        MediaListStatus.CURRENT -> LibraryStatus.CURRENT
        MediaListStatus.PLANNING -> LibraryStatus.PLANNING
        MediaListStatus.COMPLETED -> LibraryStatus.COMPLETED
        MediaListStatus.DROPPED -> LibraryStatus.DROPPED
        MediaListStatus.PAUSED -> LibraryStatus.PAUSED
        MediaListStatus.REPEATING -> LibraryStatus.REPEATING
        MediaListStatus.UNKNOWN__ -> LibraryStatus.UNKNOWN
    }
}

fun LibraryStatus.toApiStatus(): MediaListStatus {
    return when (this) {
        LibraryStatus.CURRENT -> MediaListStatus.CURRENT
        LibraryStatus.PLANNING -> MediaListStatus.PLANNING
        LibraryStatus.COMPLETED -> MediaListStatus.COMPLETED
        LibraryStatus.DROPPED -> MediaListStatus.DROPPED
        LibraryStatus.PAUSED -> MediaListStatus.PAUSED
        LibraryStatus.REPEATING -> MediaListStatus.REPEATING
        LibraryStatus.UNKNOWN -> MediaListStatus.CURRENT
    }
}

/**
 * Maps fuzzy date components to epoch millis at UTC 00:00 of the given date.
 */
fun mapFuzzyDateToLong(year: Int?, month: Int?, day: Int?): Long? {
    if (year == null) return null
    val safeMonth = month ?: 1
    val safeDay = day ?: 1
    return LocalDate.of(year, safeMonth, safeDay)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
}

fun Long.toFuzzyDateInput(): FuzzyDateInput {
    val date = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    return FuzzyDateInput(
        year = Optional.present(date.year),
        month = Optional.present(date.monthValue),
        day = Optional.present(date.dayOfMonth)
    )
}

/**
 * The user's *local* today, expressed as UTC-midnight millis so it matches the
 * fuzzy-date/DatePicker convention used everywhere else. Use this for auto-filled
 * start/completed dates instead of [System.currentTimeMillis], whose time-of-day
 * component would otherwise land on the wrong calendar day near midnight.
 */
fun todayUtcMillis(): Long =
    LocalDate.now(SYSTEM_ZONE).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

