package com.anisync.android.domain.recap

import java.time.Month

/** The pages a recap's story can tell, months and years each in their own order. */
enum class RecapPage {
    OPENER,
    TIME,
    MOST_WATCHED,
    TOP_FIVE,
    MOOD,
    FINISHED,
    RHYTHM,
    MONTH_CARD,
    YEAR_OPENER,
    YEAR_NUMBERS,
    YEAR_TOP_FIVE,
    YEAR_TASTE,
    YEAR_HABITS,
    YEAR_CARD,
}

/**
 * The pages [recap] has something to show on. A page about titles goes when every title is one
 * that may not be shown, the finished page goes when nothing was finished, and the card closes
 * every story.
 */
fun recapPages(recap: Recap): List<RecapPage> = if (recap.period is RecapPeriod.Year) {
    buildList {
        add(RecapPage.YEAR_OPENER)
        add(RecapPage.YEAR_NUMBERS)
        if (recap.ranked.isNotEmpty()) add(RecapPage.YEAR_TOP_FIVE)
        if (recap.genres.isNotEmpty()) add(RecapPage.YEAR_TASTE)
        add(RecapPage.YEAR_HABITS)
        add(RecapPage.YEAR_CARD)
    }
} else {
    buildList {
        add(RecapPage.OPENER)
        add(RecapPage.TIME)
        if (recap.ranked.isNotEmpty()) add(RecapPage.MOST_WATCHED)
        if (recap.ranked.size >= 2) add(RecapPage.TOP_FIVE)
        if (recap.genres.isNotEmpty()) add(RecapPage.MOOD)
        if (recap.finished > 0) add(RecapPage.FINISHED)
        add(RecapPage.RHYTHM)
        add(RecapPage.MONTH_CARD)
    }
}

/** Where a page takes the seed of its colours from. */
sealed interface PageSeed {
    /** The cover of the title at [rank] in the period's ranking, the top title at 0. */
    data class Title(val rank: Int) : PageSeed

    data object TopGenre : PageSeed

    /** The app's own palette. */
    data object App : PageSeed
}

/** The seeds the frames give each page, so consecutive pages rarely wear the same colours. */
val RecapPage.seed: PageSeed
    get() = when (this) {
        RecapPage.OPENER, RecapPage.TIME, RecapPage.MOST_WATCHED, RecapPage.MONTH_CARD,
        RecapPage.YEAR_OPENER, RecapPage.YEAR_TOP_FIVE, RecapPage.YEAR_CARD -> PageSeed.Title(0)
        RecapPage.FINISHED -> PageSeed.Title(1)
        RecapPage.MOOD, RecapPage.YEAR_HABITS -> PageSeed.Title(2)
        RecapPage.YEAR_NUMBERS -> PageSeed.Title(3)
        RecapPage.TOP_FIVE, RecapPage.YEAR_TASTE -> PageSeed.TopGenre
        RecapPage.RHYTHM -> PageSeed.App
    }

/** Pages drawn on their scheme's primary container rather than its background. */
val RecapPage.onContainer: Boolean get() = this == RecapPage.TIME || this == RecapPage.YEAR_NUMBERS

/** The last page holds the card and never moves on by itself. */
val RecapPage.isCard: Boolean get() = this == RecapPage.MONTH_CARD || this == RecapPage.YEAR_CARD

/** What the top five's footnote says. */
sealed interface RankNote {
    /** The top two took the same time. [secondFinished] only matters when [firstFinished] is true. */
    data class Tie(val first: Int, val second: Int, val firstFinished: Boolean, val secondFinished: Boolean) : RankNote

    /** A pile of short episodes outside the top five, which counting by episodes would have put first. */
    data class Shorts(val mediaId: Int, val episodes: Int, val minutesEach: Int, val topIsFilm: Boolean) : RankNote

    data object ByTime : RankNote
}

/** An episode this short or shorter is a short. */
const val SHORT_EPISODE_MINUTES = 10

fun rankNote(recap: Recap, media: Map<Int, RecapMedia>): RankNote {
    val ranked = recap.ranked
    if (ranked.size >= 2 && ranked[0].minutes == ranked[1].minutes) {
        return RankNote.Tie(ranked[0].mediaId, ranked[1].mediaId, ranked[0].finishedInPeriod, ranked[1].finishedInPeriod)
    }
    val top = ranked.take(5)
    val mostInTop = top.maxOfOrNull { it.episodes } ?: return RankNote.ByTime
    val shorts = ranked.drop(5)
        .filter { title -> media[title.mediaId]?.duration?.let { it in 1..SHORT_EPISODE_MINUTES } == true }
        .filter { it.episodes > mostInTop }
        .maxByOrNull { it.episodes }
        ?: return RankNote.ByTime
    return RankNote.Shorts(
        mediaId = shorts.mediaId,
        episodes = shorts.episodes,
        minutesEach = media.getValue(shorts.mediaId).duration!!,
        topIsFilm = media[top.first().mediaId]?.isFilm == true,
    )
}

/** True when a title of this length counts as a short. */
fun RecapMedia.isShort(): Boolean = duration?.let { it in 1..SHORT_EPISODE_MINUTES } == true

/** The busiest month of a year, and whether it left the others far behind. */
data class BusiestMonth(val month: Month, val episodes: Int, val ranAway: Boolean)

fun busiestMonth(recap: Recap): BusiestMonth? {
    val months = recap.episodesByMonth ?: return null
    val busiest = recap.busiestMonth ?: return null
    val episodes = months.getValue(busiest)
    if (episodes <= 0) return null
    val next = months.filterKeys { it != busiest }.values.maxOrNull() ?: 0
    return BusiestMonth(busiest, episodes, ranAway = episodes * 2 >= next * 3 && episodes > next)
}

/** The finished titles a story names past the covers it shows. */
data class MoreFinished(val count: Int, val first: Int, val last: Int)

fun moreFinished(recap: Recap, shown: Int): MoreFinished? {
    val rest = recap.finishedTitles.drop(shown)
    if (rest.isEmpty()) return null
    return MoreFinished(rest.size, rest.first(), rest.last())
}
