package com.anisync.android.data.mapper

import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.FuzzyDateInput
import com.anisync.android.type.MediaListStatus
import com.apollographql.apollo.api.Optional
import java.util.Calendar


/**
 * Maps API [MediaListStatus] to Domain [LibraryStatus].
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

/**
 * Maps Domain [LibraryStatus] to API [MediaListStatus].
 */
fun LibraryStatus.toApiStatus(): MediaListStatus {
    return when (this) {
        LibraryStatus.CURRENT -> MediaListStatus.CURRENT
        LibraryStatus.PLANNING -> MediaListStatus.PLANNING
        LibraryStatus.COMPLETED -> MediaListStatus.COMPLETED
        LibraryStatus.DROPPED -> MediaListStatus.DROPPED
        LibraryStatus.PAUSED -> MediaListStatus.PAUSED
        LibraryStatus.REPEATING -> MediaListStatus.REPEATING
        LibraryStatus.UNKNOWN -> MediaListStatus.CURRENT // Default fallback
    }
}

/**
 * Helper to map fuzzy date components to a timestamp (milliseconds).
 */
fun mapFuzzyDateToLong(year: Int?, month: Int?, day: Int?): Long? {
    if (year == null) return null
    val c = Calendar.getInstance()
    c.clear()
    c.set(year, (month ?: 1) - 1, day ?: 1)
    return c.timeInMillis
}

/**
 * Maps a timestamp (Long) to API [FuzzyDateInput].
 */
fun Long.toFuzzyDateInput(): FuzzyDateInput {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@toFuzzyDateInput }
    return FuzzyDateInput(
        year = Optional.present(calendar.get(Calendar.YEAR)),
        month = Optional.present(calendar.get(Calendar.MONTH) + 1),
        day = Optional.present(calendar.get(Calendar.DAY_OF_MONTH))
    )
}

