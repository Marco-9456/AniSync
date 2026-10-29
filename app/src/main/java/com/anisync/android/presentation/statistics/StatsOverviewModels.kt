package com.anisync.android.presentation.statistics

import com.anisync.android.domain.ActivityHistoryDay
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.stats.ActivityInsights
import com.anisync.android.presentation.profile.AnimeStatisticsUi
import com.anisync.android.presentation.profile.MangaStatisticsUi
import com.anisync.android.presentation.profile.StatusUiModel

/** Someone has to appear in at least two titles before the People teaser names them. */
private const val PEOPLE_MIN_TITLES = 2

/** The teasers' content for a list, decided before anything is drawn. */
fun buildAtAGlance(
    stats: AnimeStatisticsUi,
    insights: ActivityInsights?,
    history: List<ActivityHistoryDay>,
): AtAGlanceModel {
    val genres = stats.genreDistribution.sortedByDescending { it.count }
    val completed = stats.statusDistribution.countOf(LibraryStatus.COMPLETED)
    val voiceActor = stats.voiceActorDistribution.maxByOrNull { it.count }?.takeIf { it.count >= PEOPLE_MIN_TITLES }
    val studio = stats.studioDistribution.maxByOrNull { it.count }?.takeIf { it.count >= PEOPLE_MIN_TITLES }
    return AtAGlanceModel(
        activity = insights?.let { ActivityTeaser(it, history) },
        taste = genres.firstOrNull()?.let { top ->
            TasteTeaser(
                genre = top.genre,
                genreTitles = top.count,
                nextGenres = genres.drop(1).take(NEXT_GENRES).map { it.genre to it.count },
                scores = stats.scoreDistribution,
                summary = stats.scoreSummary,
                meanScore = stats.meanScore,
            )
        },
        format = stats.formatDistribution.takeIf { it.isNotEmpty() }?.let { formats ->
            FormatTeaser(
                shares = formats.sortedByDescending { it.count }.map { it.format to it.count },
                completedShare = if (completed > 0 && stats.totalCount > 0) {
                    Math.round(completed * 100.0 / stats.totalCount).toInt()
                } else {
                    null
                },
                onlyCountry = stats.countryDistribution.singleOrNull()?.countryCode?.takeIf { completed == 0 },
            )
        },
        eras = stats.eras?.let(::ErasTeaser),
        people = PeopleTeaser(
            personName = voiceActor?.name,
            personImage = voiceActor?.imageUrl,
            personRoles = voiceActor?.count ?: 0,
            studioName = studio?.studioName,
            studioTitles = studio?.count ?: 0,
        ),
    )
}

/** Manga has no voice actors or studios, so its People teaser names the staff member instead. */
fun buildAtAGlance(
    stats: MangaStatisticsUi,
    insights: ActivityInsights?,
    history: List<ActivityHistoryDay>,
): AtAGlanceModel {
    val genres = stats.genreDistribution.sortedByDescending { it.count }
    val completed = stats.statusDistribution.countOf(LibraryStatus.COMPLETED)
    val staff = stats.staffDistribution.maxByOrNull { it.count }?.takeIf { it.count >= PEOPLE_MIN_TITLES }
    return AtAGlanceModel(
        activity = insights?.let { ActivityTeaser(it, history) },
        taste = genres.firstOrNull()?.let { top ->
            TasteTeaser(
                genre = top.genre,
                genreTitles = top.count,
                nextGenres = genres.drop(1).take(NEXT_GENRES).map { it.genre to it.count },
                scores = stats.scoreDistribution,
                summary = stats.scoreSummary,
                meanScore = stats.meanScore,
            )
        },
        format = stats.formatDistribution.takeIf { it.isNotEmpty() }?.let { formats ->
            FormatTeaser(
                shares = formats.sortedByDescending { it.count }.map { it.format to it.count },
                completedShare = if (completed > 0 && stats.totalCount > 0) {
                    Math.round(completed * 100.0 / stats.totalCount).toInt()
                } else {
                    null
                },
                onlyCountry = stats.countryDistribution.singleOrNull()?.countryCode?.takeIf { completed == 0 },
            )
        },
        eras = stats.eras?.let(::ErasTeaser),
        people = PeopleTeaser(
            personName = staff?.name,
            personImage = staff?.imageUrl,
            personRoles = staff?.count ?: 0,
            studioName = null,
            studioTitles = 0,
        ),
    )
}

private const val NEXT_GENRES = 3

internal fun List<StatusUiModel>.countOf(status: LibraryStatus): Int =
    firstOrNull { it.status == status.name }?.count ?: 0

internal fun List<StatusUiModel>.heroLists(): List<HeroList> = mapNotNull { status ->
    LibraryStatus.entries.firstOrNull { it.name == status.status }?.let { HeroList(it, status.count) }
}
