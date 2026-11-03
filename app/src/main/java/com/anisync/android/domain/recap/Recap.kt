package com.anisync.android.domain.recap

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

/** What a recap needs to know about one title. */
data class RecapMedia(
    val id: Int,
    val type: RecapMediaType,
    /** The title as AniList prefers it for the account that asked. Franchise names come from it. */
    val title: String,
    val format: String?,
    val episodes: Int?,
    val chapters: Int?,
    /** Minutes per episode. */
    val duration: Int?,
    val genres: List<String>,
    val mainStudio: String?,
    val isAdult: Boolean,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    /** Related titles, for telling franchises apart. */
    val relationIds: List<Int> = emptyList(),
    val titleRomaji: String? = null,
    val titleEnglish: String? = null,
    val titleNative: String? = null,
    val coverUrl: String? = null,
    /** AniList's cover colour as a hex string such as "#e4a15d". */
    val color: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
) {
    val isFilm: Boolean get() = format == "MOVIE"
    val totalUnits: Int? get() = if (type == RecapMediaType.ANIME) episodes else chapters
}

/** Choices from the Recaps settings that change what gets counted. */
data class RecapSettings(
    val includeManga: Boolean = true,
    val countRepeats: Boolean = false,
    /** Adult titles always count, but are only named or shown with adult content or the recap switch on. */
    val showAdult: Boolean = false,
) {
    /** A short stamp of these choices, stored with a recap to say which ones it was counted with. */
    val key: String get() = "m${includeManga.bit}r${countRepeats.bit}a${showAdult.bit}"

    private val Boolean.bit: Int get() = if (this) 1 else 0
}

data class Recap(
    val period: RecapPeriod,
    val episodes: Int,
    val minutes: Int,
    val titles: Int,
    val finished: Int,
    val started: Int,
    val activeDays: Int,
    /** List updates logged in the period, counted or not, manga ones only when manga counts. */
    val updates: Int = 0,
    /** Every title watched, by time, adult ones left out unless they may be shown. */
    val ranked: List<RankedTitle>,
    /** Finished titles in the order they were finished, adult ones left out unless shown. */
    val finishedTitles: List<Int>,
    val genres: List<NamedMinutes>,
    val studios: List<NamedMinutes>,
    val episodesByDay: Map<LocalDate, Int>,
    val episodesByWeekday: Map<DayOfWeek, Int>,
    val streak: Streak,
    val biggestDay: BiggestDay,
    /** Episodes per month, only for a year. */
    val episodesByMonth: Map<Month, Int>?,
    /** Series with a new episode watched within two weeks of airing, only for a year built with airing times. */
    val keptUpWhileAiring: Int?,
    val manga: MangaRecap?,
    val franchise: Franchise?,
) {
    val busiestWeekday: DayOfWeek get() = episodesByWeekday.entries.maxWith(compareBy { it.value }).key
    val busiestMonth: Month? get() = episodesByMonth?.entries?.maxWithOrNull(compareBy({ it.value }, { -it.key.value }))?.key
    val quietestMonth: Month?
        get() = episodesByMonth?.entries?.filter { it.value > 0 }?.minWithOrNull(compareBy({ it.value }, { it.key.value }))?.key
}

data class RankedTitle(
    val mediaId: Int,
    val episodes: Int,
    val minutes: Int,
    val finishedInPeriod: Boolean,
    /** The episodes watched in the period, from the first to the last. */
    val watched: IntRange?,
)

data class NamedMinutes(val name: String, val minutes: Int)

data class Streak(val days: Int, val start: LocalDate, val end: LocalDate)

data class BiggestDay(
    val date: LocalDate,
    val episodes: Int,
    /** The title with the most episodes that day, and how many of them were its. */
    val leadingMediaId: Int?,
    val leadingEpisodes: Int,
    /** Every title that may be shown with that many episodes, lowest id first ("Animation! and Bananya"). */
    val leaders: List<Int> = listOfNotNull(leadingMediaId),
)

data class MangaRecap(
    val chapters: Int,
    val titles: Int,
    val topMediaId: Int?,
    val topChapters: Int,
)

/** Titles of one franchise watched in the period, named after the word their titles share. */
data class Franchise(val name: String?, val mediaIds: List<Int>)
