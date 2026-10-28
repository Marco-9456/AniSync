package com.anisync.android.domain.recap

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId

/** AniList leaves some durations blank. An episode of unknown length counts as a TV episode. */
const val DEFAULT_EPISODE_MINUTES = 24

private val SERIES_FORMATS = setOf("TV", "TV_SHORT", "ONA")

/** Days after a title's last airing date that still count as keeping up with it. */
private const val AIRING_GRACE_DAYS = 7L

/**
 * Builds the recap for [period] from list activity, or null when no anime episode counts in it,
 * since a month without activity gets no recap.
 *
 * [records] may reach back before the period so completions can see what was logged earlier.
 * Episodes and minutes include adult titles, but unless [RecapSettings.showAdult] is on they are
 * never named, ranked or shown in genres and studios.
 */
fun buildRecap(
    period: RecapPeriod,
    records: List<ListActivityRecord>,
    media: Map<Int, RecapMedia>,
    settings: RecapSettings,
    zone: ZoneId,
): Recap? {
    fun dateOf(event: CountedEvent): LocalDate = Instant.ofEpochSecond(event.createdAt).atZone(zone).toLocalDate()
    fun visible(mediaId: Int): Boolean = settings.showAdult || media[mediaId]?.isAdult != true

    val events = countEvents(records) { media[it]?.totalUnits }
        .filter { dateOf(it) in period && (settings.countRepeats || !it.isRepeat) }
    val anime = events.filter { it.type == RecapMediaType.ANIME }
    val counted = anime.filter { it.units > 0 }
    if (counted.isEmpty()) return null

    val episodesByTitle = counted.groupingBy { it.mediaId }.fold(0) { sum, event -> sum + event.units }
    val minutesByTitle = episodesByTitle.mapValues { (id, episodes) ->
        episodes * (media[id]?.duration?.takeIf { it > 0 } ?: DEFAULT_EPISODE_MINUTES)
    }
    val finishedAt = anime.filter { it.isCompletion }
        .groupBy { it.mediaId }
        .mapValues { (_, completions) -> completions.minOf { it.createdAt } }
    val watched = counted.filter { it.range != null }
        .groupBy { it.mediaId }
        .mapValues { (_, list) -> list.minOf { it.range!!.first }..list.maxOf { it.range!!.last } }

    val ranked = episodesByTitle.keys
        .filter(::visible)
        .sortedWith(
            compareByDescending<Int> { minutesByTitle.getValue(it) }
                .thenBy { if (it in finishedAt) 0 else 1 }
                .thenBy { finishedAt[it] ?: Long.MAX_VALUE }
                .thenBy { it }
        )
        .map { id ->
            RankedTitle(
                mediaId = id,
                episodes = episodesByTitle.getValue(id),
                minutes = minutesByTitle.getValue(id),
                finishedInPeriod = id in finishedAt,
                watched = watched[id],
            )
        }

    val episodesByDay = counted.groupingBy(::dateOf).fold(0) { sum, event -> sum + event.units }.toSortedMap()
    val episodesByWeekday = DayOfWeek.entries.associateWith { day ->
        episodesByDay.entries.filter { it.key.dayOfWeek == day }.sumOf { it.value }
    }

    return Recap(
        period = period,
        episodes = episodesByTitle.values.sum(),
        minutes = minutesByTitle.values.sum(),
        titles = episodesByTitle.size,
        finished = finishedAt.size,
        started = counted.filter { !it.isRepeat && it.range?.first == 1 }.map { it.mediaId }.toSet().size,
        activeDays = episodesByDay.size,
        ranked = ranked,
        finishedTitles = finishedAt.entries.sortedBy { it.value }.map { it.key }.filter(::visible),
        genres = minutesBy(minutesByTitle, ::visible) { media[it]?.genres.orEmpty() },
        studios = minutesBy(minutesByTitle, ::visible) { listOfNotNull(media[it]?.mainStudio) },
        episodesByDay = episodesByDay,
        episodesByWeekday = episodesByWeekday,
        streak = longestStreak(episodesByDay.keys),
        biggestDay = biggestDay(counted, ::dateOf, ::visible),
        episodesByMonth = (period as? RecapPeriod.Year)?.let {
            Month.entries.associateWith { month ->
                episodesByDay.entries.filter { it.key.month == month }.sumOf { it.value }
            }
        },
        keptUpWhileAiring = (period as? RecapPeriod.Year)?.let { year ->
            keptUpWhileAiring(year, counted, media, ::dateOf)
        },
        manga = if (settings.includeManga) mangaRecap(events, ::visible) else null,
        franchise = largestFranchise(ranked.map { it.mediaId }, media),
    )
}

private fun minutesBy(
    minutesByTitle: Map<Int, Int>,
    visible: (Int) -> Boolean,
    names: (Int) -> List<String>,
): List<NamedMinutes> {
    val totals = HashMap<String, Int>()
    minutesByTitle.filterKeys(visible).forEach { (id, minutes) ->
        names(id).distinct().forEach { totals[it] = (totals[it] ?: 0) + minutes }
    }
    return totals.map { NamedMinutes(it.key, it.value) }
        .sortedWith(compareByDescending<NamedMinutes> { it.minutes }.thenBy { it.name })
}

/** The longest run of consecutive active days. Equal runs show the most recent. */
internal fun longestStreak(days: Collection<LocalDate>): Streak {
    val sorted = days.sorted()
    var best = Streak(1, sorted.first(), sorted.first())
    var start = sorted.first()
    var length = 1
    for (index in 1 until sorted.size) {
        if (sorted[index - 1].plusDays(1) == sorted[index]) {
            length++
        } else {
            length = 1
            start = sorted[index]
        }
        if (length >= best.days) best = Streak(length, start, sorted[index])
    }
    return best
}

private fun biggestDay(
    counted: List<CountedEvent>,
    dateOf: (CountedEvent) -> LocalDate,
    visible: (Int) -> Boolean,
): BiggestDay {
    val byDay = counted.groupBy(dateOf)
    val (date, events) = byDay.entries.maxWith(compareBy({ it.value.sumOf { event -> event.units } }, { it.key }))
    val leading = events.groupingBy { it.mediaId }.fold(0) { sum, event -> sum + event.units }
        .entries.maxWith(compareBy({ it.value }, { -it.key }))
    return BiggestDay(
        date = date,
        episodes = events.sumOf { it.units },
        leadingMediaId = leading.key.takeIf(visible),
        leadingEpisodes = leading.value,
    )
}

private fun keptUpWhileAiring(
    year: RecapPeriod.Year,
    counted: List<CountedEvent>,
    media: Map<Int, RecapMedia>,
    dateOf: (CountedEvent) -> LocalDate,
): Int = counted.filter { !it.isRepeat }
    .groupBy { it.mediaId }
    .count { (id, events) ->
        val title = media[id] ?: return@count false
        val start = title.startDate ?: return@count false
        if (start.year != year.year || title.format !in SERIES_FORMATS) return@count false
        val last = (title.endDate ?: year.lastDay).plusDays(AIRING_GRACE_DAYS)
        events.any { val date = dateOf(it); !date.isBefore(start) && !date.isAfter(last) }
    }

private fun mangaRecap(events: List<CountedEvent>, visible: (Int) -> Boolean): MangaRecap? {
    val chapters = events.filter { it.type == RecapMediaType.MANGA && it.units > 0 }
    if (chapters.isEmpty()) return null
    val byTitle = chapters.groupingBy { it.mediaId }.fold(0) { sum, event -> sum + event.units }
    val top = byTitle.entries.filter { visible(it.key) }.maxWithOrNull(compareBy({ it.value }, { -it.key }))
    return MangaRecap(
        chapters = byTitle.values.sum(),
        titles = byTitle.size,
        topMediaId = top?.key,
        topChapters = top?.value ?: 0,
    )
}
