package com.anisync.android.domain.recap

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId

/** AniList leaves some durations blank. An episode of unknown length counts as a TV episode. */
const val DEFAULT_EPISODE_MINUTES = 24

private val SERIES_FORMATS = setOf("TV", "TV_SHORT", "ONA")

/** How long after an episode airs that watching it still counts as keeping up. */
private const val KEPT_UP_SECONDS = 14L * 24 * 60 * 60

/** Streams sometimes go out a little before the broadcast time AniList lists. */
private const val EARLY_RELEASE_SECONDS = 24L * 60 * 60

/** One update can hold at most a day of watching. More than that is a list catching up. */
const val CATCH_UP_MINUTES = 24 * 60

/** The reading time a chapter is taken to need when telling a catch-up from reading. */
const val MINUTES_PER_CHAPTER = 5

/**
 * Builds the recap for [period] from list activity, or null when no anime episode counts in it,
 * since a month without activity gets no recap.
 *
 * [records] may reach back before the period so completions can see what was logged earlier.
 * Episodes and minutes include adult titles, but unless [RecapSettings.showAdult] is on they are
 * never named, ranked or shown in genres and studios.
 *
 * An update that would take more than a day to watch or read is a list catching up (a progress
 * set from 0 to 1175 in one go), so it adds nothing. It still moves the title's progress, so the
 * episodes after it count as usual.
 *
 * [airing] holds AniList's airing times by title and episode, in epoch seconds. A year counts the
 * shows kept up with only when it is given, a running year built without it leaves that out.
 */
fun buildRecap(
    period: RecapPeriod,
    records: List<ListActivityRecord>,
    media: Map<Int, RecapMedia>,
    settings: RecapSettings,
    zone: ZoneId,
    airing: Map<Int, Map<Int, Long>>? = null,
): Recap? {
    fun dateOf(event: CountedEvent): LocalDate = Instant.ofEpochSecond(event.createdAt).atZone(zone).toLocalDate()
    fun visible(mediaId: Int): Boolean = settings.showAdult || media[mediaId]?.isAdult != true

    val inPeriod = countEvents(records) { media[it]?.totalUnits }.filter { dateOf(it) in period }
    val events = inPeriod
        .filter { settings.countRepeats || !it.isRepeat }
        .filterNot { it.isCatchUp(media[it.mediaId]) }
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
        updates = inPeriod.count { it.type == RecapMediaType.ANIME || settings.includeManga },
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
        keptUpWhileAiring = if (period is RecapPeriod.Year) {
            airing?.let { keptUpWhileAiring(counted, media, it, period.window(zone)) }
        } else {
            null
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
    val byTitle = events.groupingBy { it.mediaId }.fold(0) { sum, event -> sum + event.units }
    val most = byTitle.values.max()
    val tied = byTitle.filterValues { it == most }.keys.sorted()
    return BiggestDay(
        date = date,
        episodes = events.sumOf { it.units },
        leadingEpisodes = most,
        leaders = tied.filter(visible),
    )
}

/**
 * Series with at least one episode that aired in [year] and was watched within two weeks of
 * airing. Rewatches, films, OVAs and specials never count.
 */
private fun keptUpWhileAiring(
    counted: List<CountedEvent>,
    media: Map<Int, RecapMedia>,
    airing: Map<Int, Map<Int, Long>>,
    year: ActivityWindow,
): Int = counted.filter { !it.isRepeat && media[it.mediaId]?.format in SERIES_FORMATS }
    .groupBy { it.mediaId }
    .count { (id, events) ->
        val aired = airing[id] ?: return@count false
        events.any { event ->
            event.range?.any { episode ->
                val airedAt = aired[episode] ?: return@any false
                airedAt >= year.from && airedAt < year.to &&
                    event.createdAt - airedAt in -EARLY_RELEASE_SECONDS..KEPT_UP_SECONDS
            } == true
        }
    }

/** True for an update holding more than a day of watching or reading. */
private fun CountedEvent.isCatchUp(title: RecapMedia?): Boolean {
    val minutesEach = if (type == RecapMediaType.MANGA) {
        MINUTES_PER_CHAPTER
    } else {
        title?.duration?.takeIf { it > 0 } ?: DEFAULT_EPISODE_MINUTES
    }
    return units.toLong() * minutesEach > CATCH_UP_MINUTES
}

/** The series a year's kept up count needs airing times for: every one watched in it. */
fun keptUpCandidates(
    year: RecapPeriod.Year,
    zone: ZoneId,
    records: List<ListActivityRecord>,
    media: Map<Int, RecapMedia>,
): Set<Int> {
    val window = year.window(zone)
    return records.asSequence()
        .filter { it.type == RecapMediaType.ANIME && it.createdAt >= window.from && it.createdAt < window.to }
        .filter { isCountable(it.status) && media[it.mediaId]?.format in SERIES_FORMATS }
        .mapTo(LinkedHashSet()) { it.mediaId }
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
