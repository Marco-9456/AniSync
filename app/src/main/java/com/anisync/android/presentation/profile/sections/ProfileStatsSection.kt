package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.CountryStat
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.PeopleList
import com.anisync.android.domain.PeopleSort
import com.anisync.android.domain.PersonStat
import com.anisync.android.domain.inPeopleOrder
import com.anisync.android.domain.formatOneDecimal
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.TagStat
import com.anisync.android.domain.stats.Eras
import com.anisync.android.domain.stats.ScoreSummary
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.splitMinutes
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.profile.AnimeStatisticsUi
import com.anisync.android.presentation.profile.GenreSort
import com.anisync.android.presentation.profile.LengthUiModel
import com.anisync.android.presentation.profile.MangaStatisticsUi
import com.anisync.android.presentation.profile.ProfileStatsType
import com.anisync.android.presentation.profile.ProfileUiState
import com.anisync.android.presentation.profile.ScoreUiModel
import com.anisync.android.presentation.profile.StatisticsUiModel
import com.anisync.android.presentation.profile.StatsView
import com.anisync.android.presentation.profile.StatusUiModel
import com.anisync.android.presentation.statistics.ActivityHistoryCard
import com.anisync.android.presentation.statistics.ActivityInsightTiles
import com.anisync.android.presentation.statistics.AtAGlance
import com.anisync.android.presentation.statistics.StatsSectionHeader
import com.anisync.android.presentation.statistics.StatsTiles
import com.anisync.android.presentation.statistics.statsBodySmall
import com.anisync.android.presentation.statistics.PeopleListCard
import com.anisync.android.presentation.statistics.PeopleSortOption
import com.anisync.android.presentation.statistics.PeopleSpotlight
import com.anisync.android.presentation.statistics.ShowMore
import com.anisync.android.presentation.statistics.chaptersLabel
import com.anisync.android.presentation.statistics.hoursLabel
import com.anisync.android.presentation.statistics.rolesLabel
import com.anisync.android.presentation.statistics.titlesLabel
import com.anisync.android.presentation.statistics.worksLabel
import com.anisync.android.presentation.statistics.DecadesCard
import com.anisync.android.presentation.statistics.ErasSpanHero
import com.anisync.android.presentation.statistics.ReleaseCapsules
import com.anisync.android.presentation.statistics.StartedNote
import com.anisync.android.presentation.statistics.YearsTimelineCard
import com.anisync.android.presentation.statistics.ListTile
import com.anisync.android.presentation.statistics.ListTilesCard
import com.anisync.android.presentation.statistics.FormatsCard
import com.anisync.android.presentation.statistics.LengthsCard
import com.anisync.android.presentation.statistics.CountriesCard
import com.anisync.android.presentation.statistics.TagBoard
import com.anisync.android.presentation.statistics.TasteScoresCard
import com.anisync.android.presentation.statistics.TasteSpreadCard
import com.anisync.android.presentation.statistics.WeekdayChart
import com.anisync.android.presentation.statistics.buildAtAGlance
import com.anisync.android.presentation.statistics.countOf
import com.anisync.android.presentation.statistics.heroLists
import com.anisync.android.presentation.statistics.overviewTiles
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.type.MediaType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import com.anisync.android.presentation.statistics.HeroDashboard
import com.anisync.android.presentation.statistics.GenreBoard
import com.anisync.android.presentation.statistics.MonthChart

fun LazyListScope.profileStatsTab(
    uiState: ProfileUiState,
    onOpenView: (StatsView) -> Unit,
    onGenreSortSelected: (GenreSort) -> Unit,
    onPeopleSortSelected: (PeopleList, PeopleSort) -> Unit,
    onPeopleExpandToggled: (PeopleList) -> Unit,
    onVoiceActorClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    onMediaClick: (Int) -> Unit = {},
    onActivityClick: (Int) -> Unit = {},
    onOpenRecap: (String) -> Unit = {},
    onRetryStats: () -> Unit = {},
    statsColumns: Int = 1,
) {
    val ownProfile = uiState.viewerId != null && uiState.viewerId == uiState.profile?.id
    // Recaps come from the device, not from AniList's statistics, so they never wait on them.
    if (uiState.selectedStatsView == StatsView.RECAPS && ownProfile) {
        statsRecaps(uiState.recaps, statsColumns, onOpenRecap)
        return
    }
    if (uiState.selectedStatsView == StatsView.OVERVIEW && ownProfile) {
        recapShelf(uiState.recaps, statsColumns, onOpenRecap)
    }
    // A reload keeps the stats on screen. Swapping them for a spinner, or for the error of a failed
    // reload, sent the reader back to the top of the tab (#164).
    if (uiState.isStatsLoading && uiState.statsData == null) {
        item(key = "stats_loading") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                AppCircularProgressIndicator()
            }
        }
        return
    }

    if (uiState.statsErrorMessage != null && uiState.statsData == null) {
        item(key = "stats_error") {
            EmptyState(
                icon = Icons.Outlined.CloudOff,
                title = stringResource(R.string.forum_empty_failed_title),
                description = stringResource(if (uiState.statsRateLimited) R.string.profile_rate_limited_error else R.string.forum_empty_failed_desc),
                actionLabel = stringResource(R.string.retry),
                actionIcon = Icons.Default.Refresh,
                onAction = onRetryStats,
                emblemShape = RoundedCornerShape(22.dp),
                animationKey = uiState.statsErrorMessage,
            )
        }
        return
    }

    val statsData = uiState.statsData
    if (statsData == null) {
        item(key = "stats_empty") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.profile_placeholder_stats),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val view = uiState.selectedStatsView
    val isManga = uiState.selectedStatsType == ProfileStatsType.MANGA
    val isOwnProfile = uiState.viewerId != null && uiState.viewerId == uiState.profile?.id
    if (view == StatsView.OVERVIEW && (!isManga || statsData.mangaStats != null)) {
        statsOverview(uiState, statsData, isManga, statsColumns, onOpenView)
        return
    }
    if (view == StatsView.ACTIVITY) {
        statsActivity(statsData, uiState.profile?.id ?: 0, isOwnProfile, statsColumns, onMediaClick, onActivityClick)
        return
    }
    val taste = if (isManga) statsData.mangaStats?.let(::tasteModel) else tasteModel(statsData.animeStats)
    if (view == StatsView.TASTE && taste != null) {
        statsTaste(taste, uiState.genreSort, onGenreSortSelected, isOwnProfile, statsColumns)
        return
    }
    if (view == StatsView.ERAS) {
        val eras = if (isManga) statsData.mangaStats?.eras else statsData.animeStats.eras
        if (eras != null) {
            statsEras(eras, isManga, isOwnProfile, statsColumns)
            return
        }
    }
    if (view == StatsView.HOW_YOU_WATCH) {
        val joinedAt = uiState.profile?.createdAt
        val manga = statsData.mangaStats
        if (!isManga) {
            val anime = statsData.animeStats
            statsHowYouWatch(
                hero = { AnimeTimeHero(anime, joinedAt, isOwnProfile) },
                statuses = anime.statusDistribution,
                total = anime.totalCount,
                formats = anime.formatDistribution,
                lengths = anime.lengthDistribution,
                countries = anime.countryDistribution,
                isManga = false,
                isOwnProfile = isOwnProfile,
                columns = statsColumns
            )
            return
        } else if (manga != null) {
            statsHowYouWatch(
                hero = { MangaReadHero(manga, joinedAt, isOwnProfile) },
                statuses = manga.statusDistribution,
                total = manga.totalCount,
                formats = manga.formatDistribution,
                lengths = manga.lengthDistribution,
                countries = manga.countryDistribution,
                isManga = true,
                isOwnProfile = isOwnProfile,
                columns = statsColumns
            )
            return
        }
    }

    if (view == StatsView.PEOPLE) {
        val favourites = uiState.profile?.favoriteStaffOverview.orEmpty().map { it.id }.toSet()
        val manga = statsData.mangaStats
        val lists = if (!isManga) {
            val anime = statsData.animeStats
            listOf(
                PeopleList.ANIME_VOICE_ACTORS to anime.voiceActorDistribution.map {
                    PersonStat(it.id, it.name, it.imageUrl, it.count, it.meanScore, it.hoursWatched)
                },
                PeopleList.ANIME_STAFF to anime.staffDistribution.map {
                    PersonStat(it.id, it.name, it.imageUrl, it.count, it.meanScore, it.hoursWatched)
                },
                PeopleList.ANIME_STUDIOS to anime.studioDistribution.map {
                    PersonStat(it.id, it.studioName, null, it.count, it.meanScore, it.hoursWatched)
                },
            )
        } else {
            manga?.let {
                listOf(
                    PeopleList.MANGA_STAFF to it.staffDistribution.map { staff ->
                        PersonStat(staff.id, staff.name, staff.imageUrl, staff.count, staff.meanScore, staff.hoursWatched)
                    }
                )
            }
        }
        if (lists != null) {
            statsPeople(
                lists = lists.map { (list, initial) ->
                    PeopleListModel(
                        list = list,
                        initial = initial,
                        sort = uiState.peopleSorts[list] ?: PeopleSort.COUNT,
                        expanded = list in uiState.peopleExpanded,
                        fetched = uiState.peopleFetched[list]
                    )
                },
                favourites = favourites,
                isManga = isManga,
                isOwnProfile = isOwnProfile,
                columns = statsColumns,
                onSortSelected = onPeopleSortSelected,
                onToggleExpanded = onPeopleExpandToggled,
                onVoiceActorClick = onVoiceActorClick,
                onStaffClick = onStaffClick,
                onStudioClick = onStudioClick
            )
            return
        }
    }

    if (isManga && statsData.mangaStats == null) {
        item(key = "manga_no_stats") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.statistics_no_manga_stats),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * The Overview: the hero, three tiles and "At a glance". On a wide pane the teasers move to a
 * column of their own beside the hero.
 */
private fun LazyListScope.statsOverview(
    uiState: ProfileUiState,
    statsData: StatisticsUiModel,
    isManga: Boolean,
    columns: Int,
    onOpenView: (StatsView) -> Unit,
) {
    val manga = statsData.mangaStats
    val glance = if (isManga && manga != null) {
        buildAtAGlance(manga, statsData.activityInsights, statsData.activityHistory)
    } else {
        buildAtAGlance(statsData.animeStats, statsData.activityInsights, statsData.activityHistory)
    }
    val isOwnProfile = uiState.viewerId != null && uiState.viewerId == uiState.profile?.id
    val heroAndTiles: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isManga && manga != null) {
                MangaHero(manga, isOwnProfile, uiState.uncountedManga)
            } else {
                AnimeHero(statsData.animeStats, isOwnProfile, uiState.uncountedAnime)
            }
            StatsTiles(
                if (isManga && manga != null) {
                    overviewTiles(
                        count = manga.chaptersRead,
                        countLabel = stringResource(R.string.statistics_chapters),
                        isManga = true,
                        meanScore = manga.meanScore,
                        standardDeviation = manga.standardDeviation,
                        summary = manga.scoreSummary,
                        topGenre = manga.genreDistribution.maxByOrNull { it.count }?.genre
                    )
                } else {
                    val anime = statsData.animeStats
                    overviewTiles(
                        count = anime.episodesWatched,
                        countLabel = stringResource(R.string.statistics_episodes),
                        isManga = false,
                        meanScore = anime.meanScore,
                        standardDeviation = anime.standardDeviation,
                        summary = anime.scoreSummary,
                        topGenre = anime.genreDistribution.maxByOrNull { it.count }?.genre
                    )
                }
            )
        }
    }
    if (columns <= 1) {
        item(key = "overview_hero") { Box(Modifier.padding(horizontal = 16.dp)) { heroAndTiles() } }
        item(key = "overview_glance_header") {
            StatsSectionHeader(title = stringResource(R.string.stats_at_a_glance))
        }
        item(key = "overview_glance") {
            AtAGlance(glance, isOwnProfile, onOpen = onOpenView, modifier = Modifier.padding(horizontal = 16.dp))
        }
    } else {
        item(key = "overview_wide") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.weight(1f)) { heroAndTiles() }
                AtAGlance(glance, isOwnProfile, onOpen = onOpenView, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** The Activity view: the history card, four insights, then activity by weekday and by month. */
private fun LazyListScope.statsActivity(
    statsData: StatisticsUiModel,
    userId: Int,
    isOwnProfile: Boolean,
    columns: Int,
    onMediaClick: (Int) -> Unit,
    onActivityClick: (Int) -> Unit,
) {
    val history = statsData.activityHistory
    val insights = statsData.activityInsights
    if (history.isEmpty() || insights == null) {
        item(key = "activity_empty") {
            EmptyState(
                icon = Icons.Default.CalendarMonth,
                title = stringResource(R.string.feed_empty_global_title),
                description = stringResource(R.string.stats_activity_empty_body)
            )
        }
        return
    }
    if (columns <= 1) {
        item(key = "activity_history") {
            ActivityHistoryCard(history, userId, onMediaClick, onActivityClick, Modifier.padding(horizontal = 16.dp))
        }
        item(key = "activity_insights") {
            ActivityInsightTiles(
                insights = insights,
                history = history,
                userId = userId,
                isOwnProfile = isOwnProfile,
                onMediaClick = onMediaClick,
                onActivityClick = onActivityClick,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )
        }
        item(key = "activity_weekday_header") { StatsSectionHeader(stringResource(R.string.stats_by_weekday)) }
        item(key = "activity_weekdays") { WeekdayChart(insights, Modifier.padding(horizontal = 16.dp)) }
        item(key = "activity_month_header") { StatsSectionHeader(stringResource(R.string.stats_by_month)) }
        item(key = "activity_months") { MonthChart(insights, Modifier.padding(horizontal = 16.dp)) }
    } else {
        // The heatmap needs the pane's full width to show every week without scrolling, so the
        // history card spans it and the insights take one row of four beneath.
        item(key = "activity_wide") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActivityHistoryCard(history, userId, onMediaClick, onActivityClick)
                ActivityInsightTiles(insights, history, userId, isOwnProfile, onMediaClick, onActivityClick, columns = 4)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        StatsSectionHeader(stringResource(R.string.stats_by_weekday), inset = 8.dp, top = 12.dp)
                        WeekdayChart(insights)
                    }
                    Column(Modifier.weight(1f)) {
                        StatsSectionHeader(stringResource(R.string.stats_by_month), inset = 8.dp, top = 12.dp)
                        MonthChart(insights)
                    }
                }
            }
        }
    }
}

/** What the Taste view draws, the same for anime and manga. */
private class TasteModel(
    val scores: List<ScoreUiModel>,
    val summary: ScoreSummary,
    val meanScore: Double,
    val standardDeviation: Double,
    val totalCount: Int,
    val genres: List<GenreStat>,
    val tags: List<TagStat>,
    val isManga: Boolean,
)

private fun tasteModel(stats: AnimeStatisticsUi) = TasteModel(
    scores = stats.scoreDistribution,
    summary = stats.scoreSummary,
    meanScore = stats.meanScore,
    standardDeviation = stats.standardDeviation,
    totalCount = stats.totalCount,
    genres = stats.genreDistribution,
    tags = stats.tagDistribution,
    isManga = false,
)

private fun tasteModel(stats: MangaStatisticsUi) = TasteModel(
    scores = stats.scoreDistribution,
    summary = stats.scoreSummary,
    meanScore = stats.meanScore,
    standardDeviation = stats.standardDeviation,
    totalCount = stats.totalCount,
    genres = stats.genreDistribution,
    tags = stats.tagDistribution,
    isManga = true,
)

/** The Taste view: scores and how far they spread, genres, then tags. */
private fun LazyListScope.statsTaste(
    taste: TasteModel,
    genreSort: GenreSort,
    onGenreSortSelected: (GenreSort) -> Unit,
    isOwnProfile: Boolean,
    columns: Int,
) {
    val scores: @Composable (Modifier) -> Unit = { modifier ->
        if (taste.summary.scored > 0) {
            TasteScoresCard(taste.scores, taste.summary, taste.meanScore, taste.totalCount, modifier)
        } else {
            Text(
                text = stringResource(if (isOwnProfile) R.string.stats_teaser_score_prompt else R.string.stats_teaser_score_prompt_other),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.padding(horizontal = 8.dp)
            )
        }
    }
    val spread: @Composable (Modifier) -> Unit = { modifier ->
        if (taste.summary.hasSpread && taste.standardDeviation > 0.0) {
            TasteSpreadCard(taste.standardDeviation, taste.scores, taste.meanScore, isOwnProfile, modifier)
        }
    }
    val genres: @Composable (Modifier) -> Unit = { modifier ->
        GenreBoard(taste.genres, genreSort, onGenreSortSelected, taste.isManga, modifier)
    }
    val note: @Composable () -> Unit = {
        Text(
            text = stringResource(R.string.stats_adult_note),
            style = statsBodySmall(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)
        )
    }
    if (columns <= 1) {
        item(key = "taste_scores") { scores(Modifier.padding(horizontal = 16.dp)) }
        item(key = "taste_spread") { spread(Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)) }
        if (taste.genres.isNotEmpty()) {
            item(key = "taste_genres_header") { StatsSectionHeader(stringResource(R.string.label_genres)) }
            item(key = "taste_genres") { genres(Modifier.padding(horizontal = 16.dp)) }
        }
        if (taste.tags.isNotEmpty()) {
            item(key = "taste_tags_header") { StatsSectionHeader(stringResource(R.string.label_tags)) }
            item(key = "taste_tags") { TagBoard(taste.tags, Modifier.padding(horizontal = 16.dp)) }
        }
        item(key = "taste_adult_note") { note() }
    } else {
        item(key = "taste_wide") {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        scores(Modifier)
                        spread(Modifier)
                        if (taste.tags.isNotEmpty()) {
                            StatsSectionHeader(stringResource(R.string.label_tags), inset = 8.dp, top = 12.dp)
                            TagBoard(taste.tags)
                        }
                    }
                    if (taste.genres.isNotEmpty()) {
                        Column(Modifier.weight(1f)) {
                            genres(Modifier)
                        }
                    }
                }
                note()
            }
        }
    }
}

/** The Eras view: the span, decades, then release and start years. */
private fun LazyListScope.statsEras(eras: Eras, isManga: Boolean, isOwnProfile: Boolean, columns: Int) {
    val released: @Composable (Modifier) -> Unit = { modifier ->
        YearsTimelineCard(
            title = stringResource(
                when {
                    !isOwnProfile -> R.string.stats_eras_released_other
                    isManga -> R.string.stats_eras_released_manga
                    else -> R.string.stats_eras_released_anime
                }
            ),
            years = eras.releaseYears,
            modifier = modifier
        ) { ReleaseCapsules(eras) }
    }
    val started: @Composable (Modifier) -> Unit = { modifier ->
        YearsTimelineCard(
            title = stringResource(
                when {
                    !isOwnProfile -> R.string.stats_eras_started_other
                    isManga -> R.string.stats_eras_started_manga
                    else -> R.string.stats_eras_started_anime
                }
            ),
            years = eras.startYears,
            modifier = modifier
        ) { StartedNote(eras, isManga, isOwnProfile) }
    }
    if (columns <= 1) {
        item(key = "eras_span") { ErasSpanHero(eras, isManga, isOwnProfile, Modifier.padding(horizontal = 16.dp)) }
        item(key = "eras_decades_header") { StatsSectionHeader(stringResource(R.string.stats_header_by_decade)) }
        item(key = "eras_decades") { DecadesCard(eras, isManga, isOwnProfile, Modifier.padding(horizontal = 16.dp)) }
        item(key = "eras_released_header") { StatsSectionHeader(stringResource(R.string.stats_header_released)) }
        item(key = "eras_released") { released(Modifier.padding(horizontal = 16.dp)) }
        if (eras.startYears.isNotEmpty()) {
            item(key = "eras_started_header") { StatsSectionHeader(stringResource(R.string.stats_header_started)) }
            item(key = "eras_started") { started(Modifier.padding(horizontal = 16.dp)) }
        }
    } else {
        item(key = "eras_wide") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    ErasSpanHero(eras, isManga, isOwnProfile)
                    StatsSectionHeader(stringResource(R.string.stats_header_by_decade), inset = 8.dp)
                    DecadesCard(eras, isManga, isOwnProfile)
                }
                Column(Modifier.weight(1f)) {
                    StatsSectionHeader(stringResource(R.string.stats_header_released), inset = 8.dp, top = 0.dp)
                    released(Modifier)
                    if (eras.startYears.isNotEmpty()) {
                        StatsSectionHeader(stringResource(R.string.stats_header_started), inset = 8.dp)
                        started(Modifier)
                    }
                }
            }
        }
    }
}

/** One People list as the view draws it: its first rows from the stats, its order and what was fetched. */
private class PeopleListModel(
    val list: PeopleList,
    val initial: List<PersonStat>,
    val sort: PeopleSort,
    val expanded: Boolean,
    val fetched: Map<PeopleSort, List<PersonStat>>?,
) {
    /** Ten people or twelve studios before Show more. */
    val cap: Int = if (list == PeopleList.ANIME_STUDIOS) STUDIO_ROWS else PEOPLE_ROWS

    /** The fetched list for this order, or the first rows put in order until it lands. */
    val ordered: List<PersonStat> = (fetched?.get(sort) ?: initial).inPeopleOrder(sort)

    val shown: List<PersonStat> = if (expanded) ordered else ordered.take(cap)

    /** More may wait on AniList while only the first rows are here and they filled their cap. */
    val showMore: ShowMore? = when {
        expanded -> ShowMore.FEWER
        ordered.size > cap -> ShowMore.MORE
        fetched?.get(sort) == null && initial.size >= cap -> ShowMore.MORE
        else -> null
    }
}

/** The People view: the spotlight, then voice actors, staff and studios, each with its own order. */
private fun LazyListScope.statsPeople(
    lists: List<PeopleListModel>,
    favourites: Set<Int>,
    isManga: Boolean,
    isOwnProfile: Boolean,
    columns: Int,
    onSortSelected: (PeopleList, PeopleSort) -> Unit,
    onToggleExpanded: (PeopleList) -> Unit,
    onVoiceActorClick: (Int) -> Unit,
    onStaffClick: (Int) -> Unit,
    onStudioClick: (Int) -> Unit,
) {
    val voiceActors = lists.firstOrNull { it.list == PeopleList.ANIME_VOICE_ACTORS }?.initial.orEmpty()
    val staff = lists.firstOrNull { it.list == PeopleList.ANIME_STAFF || it.list == PeopleList.MANGA_STAFF }?.initial.orEmpty()
    val spotlightVoice = voiceActors.maxByOrNull { it.count }
    val spotlightPerson = spotlightVoice ?: staff.maxByOrNull { it.count }
    val visible = lists.filter { it.initial.isNotEmpty() }
    if (spotlightPerson == null && visible.isEmpty()) {
        item(key = "people_empty") {
            Text(
                text = stringResource(if (isOwnProfile) R.string.stats_teaser_people_locked else R.string.stats_teaser_people_locked_other),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        return
    }
    val spotlight: @Composable (Modifier) -> Unit = { modifier ->
        if (spotlightPerson != null) {
            val locale = LocalConfiguration.current.locales[0]
            val isVoice = spotlightVoice != null
            val count = if (isVoice) rolesLabel(spotlightPerson.count) else worksLabel(spotlightPerson.count)
            val amount = spotlightPerson.amount.roundToInt()
            val time = if (isManga) {
                pluralStringResourceOrDefault(R.plurals.stats_chapters_short, amount, formatExactCount(amount, locale))
            } else {
                pluralStringResourceOrDefault(R.plurals.stats_hours_total, amount, formatExactCount(amount, locale))
            }
            PeopleSpotlight(
                person = spotlightPerson,
                eyebrow = stringResource(
                    when {
                        isVoice && isOwnProfile -> R.string.stats_spotlight_voice_own
                        isVoice -> R.string.stats_spotlight_voice_other
                        isOwnProfile -> R.string.stats_spotlight_staff_own
                        else -> R.string.stats_spotlight_staff_other
                    }
                ),
                detail = stringResource(
                    when {
                        isManga && isOwnProfile -> R.string.stats_spotlight_detail_manga_own
                        isManga -> R.string.stats_spotlight_detail_manga_other
                        isOwnProfile -> R.string.stats_spotlight_detail_anime_own
                        else -> R.string.stats_spotlight_detail_anime_other
                    },
                    count,
                    time
                ),
                isFavourite = spotlightPerson.id in favourites,
                isOwnProfile = isOwnProfile,
                onClick = { if (isVoice) onVoiceActorClick(spotlightPerson.id) else onStaffClick(spotlightPerson.id) },
                modifier = modifier
            )
        }
    }
    val card: @Composable (PeopleListModel, Dp, Dp, Modifier) -> Unit = { model, inset, top, modifier ->
        val isVoice = model.list == PeopleList.ANIME_VOICE_ACTORS
        StatsSectionHeader(
            title = stringResource(
                when (model.list) {
                    PeopleList.ANIME_VOICE_ACTORS -> R.string.person_tab_voice_actors
                    PeopleList.ANIME_STUDIOS -> R.string.search_header_studios
                    PeopleList.ANIME_STAFF, PeopleList.MANGA_STAFF -> R.string.search_header_staff
                }
            ),
            inset = inset,
            top = top
        )
        PeopleListCard(
            rows = model.shown,
            sorts = listOf(
                PeopleSortOption(
                    PeopleSort.COUNT,
                    stringResource(if (isVoice) R.string.stats_people_sort_roles else R.string.details_label_titles),
                    if (isVoice) Icons.Default.Person else Icons.Default.Tv
                ),
                PeopleSortOption(
                    PeopleSort.PROGRESS,
                    stringResource(if (isManga) R.string.search_sort_chapters else R.string.stats_genre_sort_hours),
                    if (isManga) Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Schedule
                ),
                PeopleSortOption(PeopleSort.SCORE, stringResource(R.string.sort_score), Icons.Default.Star),
            ),
            selectedSort = model.sort,
            onSortChange = { onSortSelected(model.list, it) },
            countLabel = { count ->
                when (model.list) {
                    PeopleList.ANIME_VOICE_ACTORS -> rolesLabel(count)
                    PeopleList.ANIME_STAFF, PeopleList.MANGA_STAFF -> worksLabel(count)
                    PeopleList.ANIME_STUDIOS -> titlesLabel(count)
                }
            },
            amountLabel = { if (isManga) chaptersLabel(it) else hoursLabel(it) },
            onClick = when (model.list) {
                PeopleList.ANIME_VOICE_ACTORS -> onVoiceActorClick
                PeopleList.ANIME_STUDIOS -> onStudioClick
                PeopleList.ANIME_STAFF, PeopleList.MANGA_STAFF -> onStaffClick
            },
            showMore = model.showMore,
            onToggleMore = { onToggleExpanded(model.list) },
            modifier = modifier,
            monogram = model.list == PeopleList.ANIME_STUDIOS
        )
    }
    if (columns <= 1) {
        item(key = "people_spotlight") { spotlight(Modifier.padding(horizontal = 16.dp)) }
        visible.forEach { model ->
            item(key = "people_" + model.list.name) {
                Column { card(model, 24.dp, 24.dp, Modifier.padding(horizontal = 16.dp)) }
            }
        }
    } else {
        item(key = "people_wide") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    spotlight(Modifier)
                    visible.firstOrNull()?.let { card(it, 8.dp, 24.dp, Modifier) }
                }
                Column(Modifier.weight(1f)) {
                    visible.drop(1).forEachIndexed { index, model -> card(model, 8.dp, if (index == 0) 0.dp else 24.dp, Modifier) }
                }
            }
        }
    }
}

private const val STUDIO_ROWS = 12
private const val PEOPLE_ROWS = 10

/** How you watch (How you read for manga): time, the lists, formats, lengths and countries. */
private fun LazyListScope.statsHowYouWatch(
    hero: @Composable () -> Unit,
    statuses: List<StatusUiModel>,
    total: Int,
    formats: List<FormatStat>,
    lengths: List<LengthUiModel>,
    countries: List<CountryStat>,
    isManga: Boolean,
    isOwnProfile: Boolean,
    columns: Int,
) {
    val mediaType = if (isManga) MediaType.MANGA else MediaType.ANIME
    val tiles = statuses.heroLists().map { ListTile(it.status, it.count) }
    val lists: @Composable (Modifier) -> Unit = { modifier ->
        ListTilesCard(tiles, statuses.countOf(LibraryStatus.COMPLETED), total, mediaType, isOwnProfile, modifier)
    }
    val listsTitle = if (isOwnProfile) R.string.library_lists_title else R.string.stats_header_their_lists
    val lengthsTitle = if (isManga) R.string.stats_header_chapter_length else R.string.stats_header_episode_length
    if (columns <= 1) {
        item(key = "watch_hero") { Box(Modifier.padding(horizontal = 16.dp)) { hero() } }
        if (tiles.isNotEmpty()) {
            item(key = "watch_lists_header") { StatsSectionHeader(stringResource(listsTitle)) }
            item(key = "watch_lists") { lists(Modifier.padding(horizontal = 16.dp)) }
        }
        if (formats.isNotEmpty()) {
            item(key = "watch_formats_header") { StatsSectionHeader(stringResource(R.string.stats_header_formats)) }
            item(key = "watch_formats") { FormatsCard(formats, isManga, isOwnProfile, Modifier.padding(horizontal = 16.dp)) }
        }
        if (lengths.any { it.label.isNotEmpty() }) {
            item(key = "watch_lengths_header") { StatsSectionHeader(stringResource(lengthsTitle)) }
            item(key = "watch_lengths") { LengthsCard(lengths, isManga, Modifier.padding(horizontal = 16.dp)) }
        }
        if (countries.isNotEmpty()) {
            item(key = "watch_countries_header") { StatsSectionHeader(stringResource(R.string.stats_header_countries)) }
            item(key = "watch_countries") { CountriesCard(countries, Modifier.padding(horizontal = 16.dp)) }
        }
    } else {
        item(key = "watch_wide") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    hero()
                    if (tiles.isNotEmpty()) {
                        StatsSectionHeader(stringResource(listsTitle), inset = 8.dp)
                        lists(Modifier)
                    }
                    if (countries.isNotEmpty()) {
                        StatsSectionHeader(stringResource(R.string.stats_header_countries), inset = 8.dp)
                        CountriesCard(countries)
                    }
                }
                Column(Modifier.weight(1f)) {
                    if (formats.isNotEmpty()) {
                        StatsSectionHeader(stringResource(R.string.stats_header_formats), inset = 8.dp, top = 0.dp)
                        FormatsCard(formats, isManga, isOwnProfile)
                    }
                    if (lengths.any { it.label.isNotEmpty() }) {
                        StatsSectionHeader(stringResource(lengthsTitle), inset = 8.dp)
                        LengthsCard(lengths, isManga)
                    }
                }
            }
        }
    }
}

/** The anime time hero: days on the wide axis, what is left over, and the daily average. */
@Composable
private fun AnimeTimeHero(stats: AnimeStatisticsUi, joinedAt: Long?, isOwnProfile: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val parts = splitMinutes(stats.minutesWatched)
    val hours = pluralStringResourceOrDefault(R.plurals.stats_hours, parts.hours, parts.hours)
    val minutes = pluralStringResourceOrDefault(R.plurals.stats_minutes, parts.minutes, parts.minutes)
    val (value, unit, lead) = when {
        parts.days > 0 -> Triple(
            parts.days,
            pluralStringResourceOrDefault(R.plurals.stats_unit_days, parts.days),
            when {
                parts.hours > 0 && parts.minutes > 0 -> stringResource(R.string.stats_time_on_top_two, hours, minutes)
                parts.hours > 0 -> stringResource(R.string.stats_time_on_top, hours)
                parts.minutes > 0 -> stringResource(R.string.stats_time_on_top, minutes)
                else -> null
            }
        )
        parts.hours > 0 -> Triple(
            parts.hours,
            pluralStringResourceOrDefault(R.plurals.stats_unit_hours, parts.hours),
            if (parts.minutes > 0) stringResource(R.string.stats_time_on_top, minutes) else null
        )
        else -> Triple(parts.minutes, pluralStringResourceOrDefault(R.plurals.stats_unit_minutes, parts.minutes), null)
    }
    val detail = joinedAt?.takeIf { stats.minutesWatched > 0 }?.let { millis ->
        val joined = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val days = ChronoUnit.DAYS.between(joined, LocalDate.now()).coerceAtLeast(1)
        val perDay = (stats.minutesWatched / days).toInt()
        val perDayText = when {
            perDay >= 60 && perDay % 60 > 0 -> stringResource(R.string.stats_per_day_hm, perDay / 60, perDay % 60)
            perDay >= 60 -> stringResource(R.string.stats_per_day_h, perDay / 60)
            else -> stringResource(R.string.details_duration_minutes, perDay.coerceAtLeast(1))
        }
        val totalHours = stats.minutesWatched / 60
        stringResource(
            if (isOwnProfile) R.string.stats_time_detail_own else R.string.stats_time_detail_other,
            pluralStringResourceOrDefault(R.plurals.stats_hours_total, totalHours, formatExactCount(totalHours, locale)),
            pluralStringResourceOrDefault(R.plurals.stats_episodes_total, stats.episodesWatched, formatExactCount(stats.episodesWatched, locale)),
            perDayText,
            monthYear(joined, locale)
        )
    }
    HeroDashboard(
        eyebrow = stringResource(R.string.stats_time_eyebrow),
        value = formatExactCount(value, locale),
        unit = unit,
        lead = lead,
        lists = emptyList(),
        mediaType = MediaType.ANIME,
        note = null,
        staleNote = null,
        detail = detail
    )
}

/** The manga hero: chapters on the wide axis, volumes under them, and the daily average. */
@Composable
private fun MangaReadHero(stats: MangaStatisticsUi, joinedAt: Long?, isOwnProfile: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val detail = joinedAt?.takeIf { stats.chaptersRead > 0 }?.let { millis ->
        val joined = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val days = ChronoUnit.DAYS.between(joined, LocalDate.now()).coerceAtLeast(1)
        stringResource(
            if (isOwnProfile) R.string.stats_read_detail_own else R.string.stats_read_detail_other,
            formatOneDecimal(stats.chaptersRead.toDouble() / days, locale),
            monthYear(joined, locale)
        )
    }
    HeroDashboard(
        eyebrow = stringResource(R.string.stats_read_eyebrow),
        value = formatExactCount(stats.chaptersRead, locale),
        unit = pluralStringResourceOrDefault(R.plurals.stats_unit_chapters, stats.chaptersRead),
        lead = stats.volumesRead.takeIf { it > 0 }?.let {
            pluralStringResourceOrDefault(R.plurals.stats_hero_volumes, it, formatExactCount(it, locale))
        },
        lists = emptyList(),
        mediaType = MediaType.MANGA,
        note = null,
        staleNote = null,
        detail = detail
    )
}

/** "June 2019", in the locale's own order and month form. */
private fun monthYear(date: LocalDate, locale: java.util.Locale): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMyyyy")
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}

@Composable
private fun AnimeHero(stats: AnimeStatisticsUi, isOwnProfile: Boolean, uncounted: Int) {
    val locale = LocalConfiguration.current.locales[0]
    val parts = splitMinutes(stats.minutesWatched)
    val days = pluralStringResourceOrDefault(R.plurals.statistics_activity_days, parts.days, parts.days)
    val hours = pluralStringResourceOrDefault(R.plurals.stats_hours, parts.hours, parts.hours)
    val minutes = pluralStringResourceOrDefault(R.plurals.stats_minutes, parts.minutes, parts.minutes)
    val lead = when {
        stats.minutesWatched <= 0 -> null
        parts.days > 0 && parts.hours > 0 -> stringResource(R.string.stats_hero_watch_time, days, hours)
        parts.days > 0 -> stringResource(R.string.stats_hero_watch_time_single, days)
        parts.hours > 0 && parts.minutes > 0 -> stringResource(R.string.stats_hero_watch_time, hours, minutes)
        parts.hours > 0 -> stringResource(R.string.stats_hero_watch_time_single, hours)
        else -> stringResource(R.string.stats_hero_watch_time_single, minutes)
    }
    HeroDashboard(
        eyebrow = stringResource(R.string.stats_hero_eyebrow_anime),
        value = formatExactCount(stats.totalCount, locale),
        unit = pluralStringResourceOrDefault(R.plurals.stats_unit_anime, stats.totalCount),
        lead = lead,
        lists = stats.statusDistribution.heroLists(),
        mediaType = MediaType.ANIME,
        note = listsNote(stats.statusDistribution, stats.totalCount, isOwnProfile),
        staleNote = uncountedNote(uncounted)
    )
}

@Composable
private fun MangaHero(stats: MangaStatisticsUi, isOwnProfile: Boolean, uncounted: Int) {
    val locale = LocalConfiguration.current.locales[0]
    HeroDashboard(
        eyebrow = stringResource(R.string.stats_hero_eyebrow_manga),
        value = formatExactCount(stats.totalCount, locale),
        unit = pluralStringResourceOrDefault(R.plurals.stats_unit_manga, stats.totalCount),
        lead = stats.chaptersRead.takeIf { it > 0 }?.let {
            pluralStringResourceOrDefault(R.plurals.stats_hero_chapters_read, it, formatExactCount(it, locale))
        },
        lists = stats.statusDistribution.heroLists(),
        mediaType = MediaType.MANGA,
        note = listsNote(stats.statusDistribution, stats.totalCount, isOwnProfile),
        staleNote = uncountedNote(uncounted)
    )
}

/** Says how many of the viewer's own changes AniList has not counted yet, when there are any. */
@Composable
private fun uncountedNote(uncounted: Int): String? =
    if (uncounted > 0) pluralStringResourceOrDefault(R.plurals.stats_hero_stale, uncounted, uncounted) else null

/** Explains the gap between AniList's total, which leaves Planning out, and what is on the lists. */
@Composable
private fun listsNote(statuses: List<StatusUiModel>, total: Int, isOwnProfile: Boolean): String? {
    if (statuses.countOf(LibraryStatus.PLANNING) <= 0) return null
    val locale = LocalConfiguration.current.locales[0]
    val onLists = formatExactCount(statuses.sumOf { it.count }, locale)
    return stringResource(
        if (isOwnProfile) R.string.stats_hero_lists_note_own else R.string.stats_hero_lists_note_other,
        onLists,
        formatExactCount(total, locale)
    )
}
