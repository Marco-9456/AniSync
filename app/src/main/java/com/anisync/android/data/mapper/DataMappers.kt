package com.anisync.android.data.mapper

import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.FuzzyDateInput
import com.anisync.android.type.MediaListStatus
import com.apollographql.apollo.api.Optional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val SYSTEM_ZONE: ZoneId = ZoneId.systemDefault()

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
 * Maps fuzzy date components to epoch millis at default-zone 00:00 of the given date.
 */
fun mapFuzzyDateToLong(year: Int?, month: Int?, day: Int?): Long? {
    if (year == null) return null
    val safeMonth = month ?: 1
    val safeDay = day ?: 1
    return LocalDate.of(year, safeMonth, safeDay)
        .atStartOfDay(SYSTEM_ZONE)
        .toInstant()
        .toEpochMilli()
}

fun Long.toFuzzyDateInput(): FuzzyDateInput {
    val date = Instant.ofEpochMilli(this).atZone(SYSTEM_ZONE).toLocalDate()
    return FuzzyDateInput(
        year = Optional.present(date.year),
        month = Optional.present(date.monthValue),
        day = Optional.present(date.dayOfMonth)
    )
}

