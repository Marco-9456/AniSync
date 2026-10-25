package com.anisync.android.domain

import com.anisync.android.type.MediaSeason
import java.util.Calendar

/** AniList files a title under the calendar quarter it mostly airs in, so December is Fall. */
fun seasonOfMonth(calendarMonth: Int): MediaSeason = when (calendarMonth) {
    Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH -> MediaSeason.WINTER
    Calendar.APRIL, Calendar.MAY, Calendar.JUNE -> MediaSeason.SPRING
    Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER -> MediaSeason.SUMMER
    else -> MediaSeason.FALL
}
