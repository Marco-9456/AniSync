package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.rounded.Star
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.SegmentedTabItem
import com.anisync.android.presentation.profile.ProfileStatsType
import com.anisync.android.presentation.profile.ProfileUiState
import com.anisync.android.presentation.statistics.ActivityHeatmapSection
import com.anisync.android.presentation.statistics.CountryDistributionRow
import com.anisync.android.presentation.statistics.EditorialStat
import com.anisync.android.presentation.statistics.EpisodeLengthDistributionSection
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
import com.anisync.android.presentation.util.formatDecimal

fun LazyListScope.profileStatsTab(
    uiState: ProfileUiState,
    onStatsTypeSelected: (ProfileStatsType) -> Unit,
    onVoiceActorClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    statsColumns: Int = 1,
    modifier: Modifier = Modifier
) {
    val selectedType = uiState.selectedStatsType
    val tabs = ProfileStatsType.entries
    val selectedIndex = tabs.indexOf(selectedType).coerceAtLeast(0)

    item(key = "stats_tabs") {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(tabs) { index, type ->
                val icon = when (type) {
                    ProfileStatsType.ANIME -> Icons.Default.Tv
                    ProfileStatsType.MANGA -> Icons.AutoMirrored.Filled.MenuBook
                }
                SegmentedTabItem(
                    index = index,
                    selectedIndex = selectedIndex,
                    selected = selectedType == type,
                    onClick = { onStatsTypeSelected(type) },
                    icon = icon,
                    label = stringResource(type.labelRes)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (uiState.isStatsLoading) {
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

    if (uiState.statsErrorMessage != null) {
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

    if (statsData.activityHistory.isNotEmpty()) {
        item(key = "activity_heatmap") {
            ActivityHeatmapSection(statsData.activityHistory)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (selectedType == ProfileStatsType.ANIME) {
        val (hero, rest) = animeStatSections(
            statsData.animeStats,
            onVoiceActorClick = onVoiceActorClick,
            onStaffClick = onStaffClick,
            onStudioClick = onStudioClick
        )
        statsDashboard(hero, rest, statsColumns)
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
            val (hero, rest) = mangaStatSections(mangaStats, onStaffClick = onStaffClick)
            statsDashboard(hero, rest, statsColumns)
        }
    }
}

/**
 * Emits the stats hero + its section cards. On compact ([columns] <= 1, i.e. a compact window) each
 * section is its own lazy item (full-bleed, lazily composed) exactly as before; otherwise the hero
 * spans full width and the sections go into a [StatsDashboardGrid] inside one item. [columns] only
 * gates compact-vs-flow — the grid itself measures its container and flows the cards across as many
 * columns as fit, so it adapts to the real width (notably the narrower two-pane right pane) rather
 * than the window class.
 */
private fun LazyListScope.statsDashboard(
    hero: DashboardSection,
    sections: List<DashboardSection>,
    columns: Int
) {
    if (columns <= 1) {
        item(key = hero.key) {
            hero.content()
            Spacer(Modifier.height(24.dp))
        }
        sections.forEach { section ->
            item(key = section.key) {
                section.content()
                Spacer(Modifier.height(24.dp))
            }
        }
    } else {
        item(key = "stats_dashboard") {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                hero.content()
                StatsDashboardGrid(sections = sections)
            }
        }
    }
}

private fun animeStatSections(
    stats: com.anisync.android.presentation.profile.AnimeStatisticsUi,
    onVoiceActorClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {}
): Pair<DashboardSection, List<DashboardSection>> {
    val hero = DashboardSection("anime_hero") {
        HeroDashboard(
            primaryValue = stats.totalCount.toString(),
            primaryUnit = "anime",
            primaryLabel = stringResource(R.string.statistics_total_anime),
            accentText = stringResource(R.string.statistics_days_of_your_life, formatDecimal(stats.daysWatched)),
            secondaryRow = listOf(
                EditorialStat(stats.episodesWatched.toString(), stringResource(R.string.statistics_episodes), Icons.Default.PlayArrow),
                EditorialStat(formatDecimal(stats.meanScore), stringResource(R.string.statistics_mean_score), Icons.Rounded.Star),
                EditorialStat(formatDecimal(stats.standardDeviation), stringResource(R.string.statistics_std_dev), Icons.Default.Equalizer)
            )
        )
    }

    val sections = buildList {
        if (stats.minutesWatched > 0) {
            add(DashboardSection("anime_time") { TimeSpentBreakdown(stats.minutesWatched) })
        }
        if (stats.statusDistribution.isNotEmpty()) {
            add(DashboardSection("anime_status") { StatusDistributionDonut(stats.statusDistribution) })
        }
        if (stats.scoreDistribution.isNotEmpty()) {
            add(DashboardSection("anime_scores") { ScoreHistogramSection(stats.scoreDistribution, stats.meanScore) })
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
        if (stats.formatDistribution.isNotEmpty()) {
            add(DashboardSection("anime_formats") { FormatsSection(stats.formatDistribution) })
        }
        if (stats.lengthDistribution.isNotEmpty()) {
            add(DashboardSection("anime_lengths") { EpisodeLengthDistributionSection(stats.lengthDistribution) })
        }
        if (stats.releaseYearDistribution.isNotEmpty()) {
            add(DashboardSection("anime_years") { ReleaseYearsHistogramSection(stats.releaseYearDistribution) })
        }
        if (stats.startYearDistribution.isNotEmpty()) {
            add(DashboardSection("anime_year_compare") {
                YearComparisonSection(
                    release = stats.releaseYearDistribution,
                    start = stats.startYearDistribution
                )
            })
        }
        if (stats.standardDeviation > 0.0) {
            add(DashboardSection("anime_stddev") { StandardDeviationCard(stats.standardDeviation, stats.meanScore) })
        }
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
        if (stats.countryDistribution.isNotEmpty()) {
            add(DashboardSection("anime_countries") { CountryDistributionRow(stats.countryDistribution) })
        }
    }
    return hero to sections
}

private fun mangaStatSections(
    stats: com.anisync.android.presentation.profile.MangaStatisticsUi,
    onStaffClick: (Int) -> Unit = {}
): Pair<DashboardSection, List<DashboardSection>> {
    val hero = DashboardSection("manga_hero") {
        HeroDashboard(
            primaryValue = stats.totalCount.toString(),
            primaryUnit = "manga",
            primaryLabel = stringResource(R.string.statistics_total_manga),
            secondaryRow = listOf(
                EditorialStat(stats.chaptersRead.toString(), stringResource(R.string.statistics_chapters), Icons.AutoMirrored.Filled.MenuBook),
                EditorialStat(formatDecimal(stats.meanScore), stringResource(R.string.statistics_mean_score), Icons.Rounded.Star),
                EditorialStat(formatDecimal(stats.standardDeviation), stringResource(R.string.statistics_std_dev), Icons.Default.Equalizer)
            )
        )
    }

    val sections = buildList {
        if (stats.chaptersRead > 0 || stats.volumesRead > 0) {
            add(DashboardSection("manga_read") { ReadVolumeBreakdown(stats.chaptersRead, stats.volumesRead) })
        }
        if (stats.statusDistribution.isNotEmpty()) {
            add(DashboardSection("manga_status") { StatusDistributionDonut(stats.statusDistribution, isManga = true) })
        }
        if (stats.scoreDistribution.isNotEmpty()) {
            add(DashboardSection("manga_scores") { ScoreHistogramSection(stats.scoreDistribution, stats.meanScore) })
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
        if (stats.releaseYearDistribution.isNotEmpty()) {
            add(DashboardSection("manga_years") { ReleaseYearsHistogramSection(stats.releaseYearDistribution) })
        }
        if (stats.startYearDistribution.isNotEmpty()) {
            add(DashboardSection("manga_year_compare") {
                YearComparisonSection(
                    release = stats.releaseYearDistribution,
                    start = stats.startYearDistribution
                )
            })
        }
        if (stats.standardDeviation > 0.0) {
            add(DashboardSection("manga_stddev") { StandardDeviationCard(stats.standardDeviation, stats.meanScore) })
        }
        if (stats.staffDistribution.isNotEmpty()) {
            add(DashboardSection("manga_staff") {
                HorizontalStatsSection(
                    title = stringResource(R.string.statistics_top_staff),
                    items = stats.staffDistribution,
                    key = { it.id }
                ) { StaffCardModern(it, onClick = { onStaffClick(it.id) }) }
            })
        }
        if (stats.countryDistribution.isNotEmpty()) {
            add(DashboardSection("manga_countries") { CountryDistributionRow(stats.countryDistribution) })
        }
    }
    return hero to sections
}
