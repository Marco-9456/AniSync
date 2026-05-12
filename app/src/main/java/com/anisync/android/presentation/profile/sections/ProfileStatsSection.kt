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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.profile.ProfileStatsType
import com.anisync.android.presentation.profile.ProfileUiState
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
import com.anisync.android.presentation.util.formatDecimal

fun LazyListScope.profileStatsTab(
    uiState: ProfileUiState,
    onStatsTypeSelected: (ProfileStatsType) -> Unit,
    onVoiceActorClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
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
                AnimatedTab(
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
                CircularProgressIndicator()
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

    if (selectedType == ProfileStatsType.ANIME) {
        animeStatsBranch(
            statsData.animeStats,
            onVoiceActorClick = onVoiceActorClick,
            onStaffClick = onStaffClick,
            onStudioClick = onStudioClick
        )
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
            mangaStatsBranch(mangaStats, onStaffClick = onStaffClick)
        }
    }
}

private fun LazyListScope.animeStatsBranch(
    stats: com.anisync.android.presentation.profile.AnimeStatisticsUi,
    onVoiceActorClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {}
) {
    item(key = "anime_hero") {
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
        Spacer(Modifier.height(24.dp))
    }

    if (stats.minutesWatched > 0) {
        item(key = "anime_time") {
            TimeSpentBreakdown(stats.minutesWatched)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.statusDistribution.isNotEmpty()) {
        item(key = "anime_status") {
            StatusDistributionDonut(stats.statusDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.scoreDistribution.isNotEmpty()) {
        item(key = "anime_scores") {
            ScoreHistogramSection(stats.scoreDistribution, stats.meanScore)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.genreDistribution.isNotEmpty()) {
        item(key = "anime_genres") {
            HorizontalStatsSection(
                title = stringResource(R.string.statistics_top_genres),
                items = stats.genreDistribution,
                key = { it.genre }
            ) { GenreCardModern(it) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.tagDistribution.isNotEmpty()) {
        item(key = "anime_tags") {
            TagCloudSection(stats.tagDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.formatDistribution.isNotEmpty()) {
        item(key = "anime_formats") {
            FormatsSection(stats.formatDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.lengthDistribution.isNotEmpty()) {
        item(key = "anime_lengths") {
            EpisodeLengthDistributionSection(stats.lengthDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.releaseYearDistribution.isNotEmpty()) {
        item(key = "anime_years") {
            ReleaseYearsHistogramSection(stats.releaseYearDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.startYearDistribution.isNotEmpty()) {
        item(key = "anime_year_compare") {
            YearComparisonSection(
                release = stats.releaseYearDistribution,
                start = stats.startYearDistribution
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.standardDeviation > 0.0) {
        item(key = "anime_stddev") {
            StandardDeviationCard(stats.standardDeviation, stats.meanScore)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.voiceActorDistribution.isNotEmpty()) {
        item(key = "anime_voice_actors") {
            HorizontalStatsSection(
                title = stringResource(R.string.statistics_top_voice_actors),
                items = stats.voiceActorDistribution,
                key = { it.id }
            ) { VoiceActorCardModern(it, onClick = { onVoiceActorClick(it.id) }) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.staffDistribution.isNotEmpty()) {
        item(key = "anime_staff") {
            HorizontalStatsSection(
                title = stringResource(R.string.statistics_top_staff),
                items = stats.staffDistribution,
                key = { it.id }
            ) { StaffCardModern(it, onClick = { onStaffClick(it.id) }) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.studioDistribution.isNotEmpty()) {
        item(key = "anime_studios") {
            HorizontalStatsSection(
                title = stringResource(R.string.statistics_top_studios),
                items = stats.studioDistribution,
                key = { it.id }
            ) { StudioCardModern(it, onClick = { onStudioClick(it.id) }) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.countryDistribution.isNotEmpty()) {
        item(key = "anime_countries") {
            CountryDistributionRow(stats.countryDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun LazyListScope.mangaStatsBranch(
    stats: com.anisync.android.presentation.profile.MangaStatisticsUi,
    onStaffClick: (Int) -> Unit = {}
) {
    item(key = "manga_hero") {
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
        Spacer(Modifier.height(24.dp))
    }

    if (stats.chaptersRead > 0 || stats.volumesRead > 0) {
        item(key = "manga_read") {
            ReadVolumeBreakdown(stats.chaptersRead, stats.volumesRead)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.statusDistribution.isNotEmpty()) {
        item(key = "manga_status") {
            StatusDistributionDonut(stats.statusDistribution, isManga = true)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.scoreDistribution.isNotEmpty()) {
        item(key = "manga_scores") {
            ScoreHistogramSection(stats.scoreDistribution, stats.meanScore)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.genreDistribution.isNotEmpty()) {
        item(key = "manga_genres") {
            HorizontalStatsSection(
                title = stringResource(R.string.statistics_top_genres),
                items = stats.genreDistribution,
                key = { it.genre }
            ) { GenreCardModern(it) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.tagDistribution.isNotEmpty()) {
        item(key = "manga_tags") {
            TagCloudSection(stats.tagDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.formatDistribution.isNotEmpty()) {
        item(key = "manga_formats") {
            FormatsSection(stats.formatDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.lengthDistribution.isNotEmpty()) {
        item(key = "manga_lengths") {
            EpisodeLengthDistributionSection(
                stats.lengthDistribution,
                title = stringResource(R.string.statistics_chapter_length_distribution)
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.releaseYearDistribution.isNotEmpty()) {
        item(key = "manga_years") {
            ReleaseYearsHistogramSection(stats.releaseYearDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.startYearDistribution.isNotEmpty()) {
        item(key = "manga_year_compare") {
            YearComparisonSection(
                release = stats.releaseYearDistribution,
                start = stats.startYearDistribution
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.standardDeviation > 0.0) {
        item(key = "manga_stddev") {
            StandardDeviationCard(stats.standardDeviation, stats.meanScore)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.staffDistribution.isNotEmpty()) {
        item(key = "manga_staff") {
            HorizontalStatsSection(
                title = stringResource(R.string.statistics_top_staff),
                items = stats.staffDistribution,
                key = { it.id }
            ) { StaffCardModern(it, onClick = { onStaffClick(it.id) }) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (stats.countryDistribution.isNotEmpty()) {
        item(key = "manga_countries") {
            CountryDistributionRow(stats.countryDistribution)
            Spacer(Modifier.height(24.dp))
        }
    }
}
