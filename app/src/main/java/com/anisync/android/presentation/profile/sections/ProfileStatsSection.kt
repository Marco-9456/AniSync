package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.anisync.android.presentation.statistics.FormatsSection
import com.anisync.android.presentation.statistics.GenreCardModern
import com.anisync.android.presentation.statistics.HeroDashboard
import com.anisync.android.presentation.statistics.HorizontalStatsSection
import com.anisync.android.presentation.statistics.ReleaseYearsHistogramSection
import com.anisync.android.presentation.statistics.ScoreHistogramSection
import com.anisync.android.presentation.statistics.StudioCardModern
import com.anisync.android.presentation.util.formatDecimal

fun LazyListScope.profileStatsTab(
    uiState: ProfileUiState,
    onStatsTypeSelected: (ProfileStatsType) -> Unit,
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
        val stats = statsData.animeStats

        item(key = "anime_hero_dashboard") {
            HeroDashboard(
                count = stats.totalCount.toString(),
                countLabel = stringResource(R.string.statistics_total_anime),
                subStat1Value = formatDecimal(stats.daysWatched),
                subStat1Label = stringResource(R.string.statistics_days_watched),
                subStat1Icon = Icons.Default.Schedule,
                subStat2Value = formatDecimal(stats.meanScore),
                subStat2Label = stringResource(R.string.statistics_mean_score),
                subStat2Icon = Icons.Rounded.Star,
                episodes = stats.episodesWatched
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (stats.scoreDistribution.isNotEmpty()) {
            item(key = "anime_score_histogram") {
                ScoreHistogramSection(stats.scoreDistribution)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (stats.genreDistribution.isNotEmpty()) {
            item(key = "anime_top_genres") {
                HorizontalStatsSection(
                    title = stringResource(R.string.statistics_top_genres),
                    items = stats.genreDistribution,
                    key = { it.genre }
                ) {
                    GenreCardModern(it)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (stats.formatDistribution.isNotEmpty()) {
            item(key = "anime_formats") {
                FormatsSection(stats.formatDistribution)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (stats.releaseYearDistribution.isNotEmpty()) {
            item(key = "anime_years_histogram") {
                ReleaseYearsHistogramSection(stats.releaseYearDistribution)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (stats.studioDistribution.isNotEmpty()) {
            item(key = "anime_top_studios") {
                HorizontalStatsSection(
                    title = stringResource(R.string.statistics_top_studios),
                    items = stats.studioDistribution,
                    key = { it.studioName }
                ) {
                    StudioCardModern(it)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    } else {
        val stats = statsData.mangaStats

        if (stats == null) {
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
            return
        }

        item(key = "manga_dashboard") {
            HeroDashboard(
                count = stats.totalCount.toString(),
                countLabel = stringResource(R.string.statistics_total_manga),
                subStat1Value = stats.chaptersRead.toString(),
                subStat1Label = stringResource(R.string.statistics_chapters),
                subStat1Icon = Icons.AutoMirrored.Filled.MenuBook,
                subStat2Value = formatDecimal(stats.meanScore),
                subStat2Label = stringResource(R.string.statistics_mean_score),
                subStat2Icon = Icons.Rounded.Star,
                episodes = null
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item(key = "manga_placeholder") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.profile_manga_stats_coming_soon),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
