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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.formatExactCount
import com.anisync.android.domain.splitMinutes
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.AnimeStatisticsUi
import com.anisync.android.presentation.profile.MangaStatisticsUi
import com.anisync.android.presentation.profile.ProfileStatsType
import com.anisync.android.presentation.profile.ProfileUiState
import com.anisync.android.presentation.profile.StatisticsUiModel
import com.anisync.android.presentation.profile.StatsView
import com.anisync.android.presentation.profile.StatusUiModel
import com.anisync.android.presentation.statistics.ActivityHistorySection
import com.anisync.android.presentation.statistics.AtAGlance
import com.anisync.android.presentation.statistics.CountryDistributionRow
import com.anisync.android.presentation.statistics.EpisodeLengthDistributionSection
import com.anisync.android.presentation.statistics.StatsTiles
import com.anisync.android.presentation.statistics.buildAtAGlance
import com.anisync.android.presentation.statistics.countOf
import com.anisync.android.presentation.statistics.heroLists
import com.anisync.android.presentation.statistics.overviewTiles
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.type.MediaType
import com.anisync.android.presentation.statistics.FormatsSection
import com.anisync.android.presentation.statistics.GenreCardModern
import com.anisync.android.presentation.statistics.HeroDashboard
import com.anisync.android.presentation.statistics.HorizontalStatsSection
import com.anisync.android.presentation.statistics.ReadVolumeBreakdown
import com.anisync.android.presentation.statistics.ReleaseYearsHistogramSection
import com.anisync.android.presentation.statistics.ScoreHistogramSection
import com.anisync.android.presentation.statistics.StaffCardModern
import com.anisync.android.presentation.statistics.StandardDeviationCard
import com.anisync.android.presentation.statistics.StatusDistributionDonut
import com.anisync.android.presentation.statistics.StudioCardModern
import com.anisync.android.presentation.statistics.TagCloudSection
import com.anisync.android.presentation.statistics.TimeSpentBreakdown
import com.anisync.android.presentation.statistics.VoiceActorCardModern
import com.anisync.android.presentation.statistics.YearComparisonSection
import com.anisync.android.presentation.util.DashboardSection
import com.anisync.android.presentation.util.StatsDashboardGrid

fun LazyListScope.profileStatsTab(
    uiState: ProfileUiState,
    onOpenView: (StatsView) -> Unit,
    onVoiceActorClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    onMediaClick: (Int) -> Unit = {},
    onActivityClick: (Int) -> Unit = {},
    statsColumns: Int = 1,
) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.statsErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
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
    if (view == StatsView.OVERVIEW && (!isManga || statsData.mangaStats != null)) {
        statsOverview(uiState, statsData, isManga, statsColumns, onOpenView)
        return
    }
    if (view == StatsView.ACTIVITY && statsData.activityHistory.isNotEmpty()) {
        item(key = "activity_heatmap") {
            ActivityHistorySection(
                statsData.activityHistory,
                userId = uiState.profile?.id ?: 0,
                onMediaClick = onMediaClick,
                onActivityClick = onActivityClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (uiState.selectedStatsType == ProfileStatsType.ANIME) {
        val sections = animeStatSections(
            statsData.animeStats,
            view,
            onVoiceActorClick = onVoiceActorClick,
            onStaffClick = onStaffClick,
            onStudioClick = onStudioClick
        )
        statsDashboard(sections, statsColumns)
    } else {
        val mangaStats = statsData.mangaStats
        if (mangaStats == null) {
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
        } else {
            statsDashboard(mangaStatSections(mangaStats, view, onStaffClick = onStaffClick), statsColumns)
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
            if (isManga && manga != null) MangaHero(manga, isOwnProfile) else AnimeHero(statsData.animeStats, isOwnProfile)
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
            SectionHeader(title = stringResource(R.string.stats_at_a_glance), modifier = Modifier.padding(top = 24.dp))
        }
        item(key = "overview_glance") {
            AtAGlance(glance, onOpen = onOpenView, modifier = Modifier.padding(horizontal = 16.dp))
        }
    } else {
        item(key = "overview_wide") {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.weight(1f)) { heroAndTiles() }
                AtAGlance(glance, onOpen = onOpenView, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AnimeHero(stats: AnimeStatisticsUi, isOwnProfile: Boolean) {
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
        staleNote = null
    )
}

@Composable
private fun MangaHero(stats: MangaStatisticsUi, isOwnProfile: Boolean) {
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
        staleNote = null
    )
}

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

/**
 * Emits a view's section cards. On compact ([columns] <= 1, i.e. a compact window) each section is
 * its own lazy item (full-bleed, lazily composed); otherwise the first section spans full width and
 * the rest go into a [StatsDashboardGrid] inside one item. [columns] only gates compact-vs-flow, the
 * grid itself measures its container and flows the cards across as many columns as fit.
 */
private fun LazyListScope.statsDashboard(
    sections: List<DashboardSection>,
    columns: Int
) {
    if (sections.isEmpty()) return
    if (columns <= 1) {
        sections.forEach { section ->
            item(key = section.key) {
                section.content()
                Spacer(Modifier.height(24.dp))
            }
        }
    } else {
        item(key = "stats_dashboard") {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                sections.first().content()
                StatsDashboardGrid(sections = sections.drop(1))
            }
        }
    }
}

private fun animeStatSections(
    stats: AnimeStatisticsUi,
    view: StatsView,
    onVoiceActorClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {}
): List<DashboardSection> = buildList {
    when (view) {
        StatsView.TASTE -> {
            if (stats.scoreDistribution.isNotEmpty()) {
                add(DashboardSection("anime_scores") { ScoreHistogramSection(stats.scoreDistribution, stats.meanScore) })
            }
            if (stats.standardDeviation > 0.0) {
                add(DashboardSection("anime_stddev") { StandardDeviationCard(stats.standardDeviation, stats.meanScore) })
            }
            if (stats.genreDistribution.isNotEmpty()) {
                add(DashboardSection("anime_genres") {
                    HorizontalStatsSection(
                        title = stringResource(R.string.statistics_top_genres),
                        items = stats.genreDistribution,
                        key = { it.genre }
                    ) { GenreCardModern(it) }
                })
            }
            if (stats.tagDistribution.isNotEmpty()) {
                add(DashboardSection("anime_tags") { TagCloudSection(stats.tagDistribution) })
            }
        }
        StatsView.HOW_YOU_WATCH -> {
            if (stats.minutesWatched > 0) {
                add(DashboardSection("anime_time") { TimeSpentBreakdown(stats.minutesWatched) })
            }
            if (stats.statusDistribution.isNotEmpty()) {
                add(DashboardSection("anime_status") { StatusDistributionDonut(stats.statusDistribution) })
            }
            if (stats.formatDistribution.isNotEmpty()) {
                add(DashboardSection("anime_formats") { FormatsSection(stats.formatDistribution) })
            }
            if (stats.lengthDistribution.isNotEmpty()) {
                add(DashboardSection("anime_lengths") { EpisodeLengthDistributionSection(stats.lengthDistribution) })
            }
            if (stats.countryDistribution.isNotEmpty()) {
                add(DashboardSection("anime_countries") { CountryDistributionRow(stats.countryDistribution) })
            }
        }
        StatsView.ERAS -> {
            if (stats.releaseYearDistribution.isNotEmpty()) {
                add(DashboardSection("anime_years") { ReleaseYearsHistogramSection(stats.releaseYearDistribution) })
            }
            if (stats.startYearDistribution.isNotEmpty()) {
                add(DashboardSection("anime_year_compare") {
                    YearComparisonSection(release = stats.releaseYearDistribution, start = stats.startYearDistribution)
                })
            }
        }
        StatsView.PEOPLE -> {
            if (stats.voiceActorDistribution.isNotEmpty()) {
                add(DashboardSection("anime_voice_actors") {
                    HorizontalStatsSection(
                        title = stringResource(R.string.statistics_top_voice_actors),
                        items = stats.voiceActorDistribution,
                        key = { it.id }
                    ) { VoiceActorCardModern(it, onClick = { onVoiceActorClick(it.id) }) }
                })
            }
            if (stats.staffDistribution.isNotEmpty()) {
                add(DashboardSection("anime_staff") {
                    HorizontalStatsSection(
                        title = stringResource(R.string.statistics_top_staff),
                        items = stats.staffDistribution,
                        key = { it.id }
                    ) { StaffCardModern(it, onClick = { onStaffClick(it.id) }) }
                })
            }
            if (stats.studioDistribution.isNotEmpty()) {
                add(DashboardSection("anime_studios") {
                    HorizontalStatsSection(
                        title = stringResource(R.string.statistics_top_studios),
                        items = stats.studioDistribution,
                        key = { it.id }
                    ) { StudioCardModern(it, onClick = { onStudioClick(it.id) }) }
                })
            }
        }
        StatsView.OVERVIEW, StatsView.ACTIVITY, StatsView.RECAPS -> Unit
    }
}

private fun mangaStatSections(
    stats: MangaStatisticsUi,
    view: StatsView,
    onStaffClick: (Int) -> Unit = {}
): List<DashboardSection> = buildList {
    when (view) {
        StatsView.TASTE -> {
            if (stats.scoreDistribution.isNotEmpty()) {
                add(DashboardSection("manga_scores") { ScoreHistogramSection(stats.scoreDistribution, stats.meanScore) })
            }
            if (stats.standardDeviation > 0.0) {
                add(DashboardSection("manga_stddev") { StandardDeviationCard(stats.standardDeviation, stats.meanScore) })
            }
            if (stats.genreDistribution.isNotEmpty()) {
                add(DashboardSection("manga_genres") {
                    HorizontalStatsSection(
                        title = stringResource(R.string.statistics_top_genres),
                        items = stats.genreDistribution,
                        key = { it.genre }
                    ) { GenreCardModern(it) }
                })
            }
            if (stats.tagDistribution.isNotEmpty()) {
                add(DashboardSection("manga_tags") { TagCloudSection(stats.tagDistribution) })
            }
        }
        StatsView.HOW_YOU_WATCH -> {
            if (stats.chaptersRead > 0 || stats.volumesRead > 0) {
                add(DashboardSection("manga_read") { ReadVolumeBreakdown(stats.chaptersRead, stats.volumesRead) })
            }
            if (stats.statusDistribution.isNotEmpty()) {
                add(DashboardSection("manga_status") { StatusDistributionDonut(stats.statusDistribution, isManga = true) })
            }
            if (stats.formatDistribution.isNotEmpty()) {
                add(DashboardSection("manga_formats") { FormatsSection(stats.formatDistribution) })
            }
            if (stats.lengthDistribution.isNotEmpty()) {
                add(DashboardSection("manga_lengths") {
                    EpisodeLengthDistributionSection(
                        stats.lengthDistribution,
                        title = stringResource(R.string.statistics_chapter_length_distribution)
                    )
                })
            }
            if (stats.countryDistribution.isNotEmpty()) {
                add(DashboardSection("manga_countries") { CountryDistributionRow(stats.countryDistribution) })
            }
        }
        StatsView.ERAS -> {
            if (stats.releaseYearDistribution.isNotEmpty()) {
                add(DashboardSection("manga_years") { ReleaseYearsHistogramSection(stats.releaseYearDistribution) })
            }
            if (stats.startYearDistribution.isNotEmpty()) {
                add(DashboardSection("manga_year_compare") {
                    YearComparisonSection(release = stats.releaseYearDistribution, start = stats.startYearDistribution)
                })
            }
        }
        StatsView.PEOPLE -> if (stats.staffDistribution.isNotEmpty()) {
            add(DashboardSection("manga_staff") {
                HorizontalStatsSection(
                    title = stringResource(R.string.statistics_top_staff),
                    items = stats.staffDistribution,
                    key = { it.id }
                ) { StaffCardModern(it, onClick = { onStaffClick(it.id) }) }
            })
        }
        StatsView.OVERVIEW, StatsView.ACTIVITY, StatsView.RECAPS -> Unit
    }
}
