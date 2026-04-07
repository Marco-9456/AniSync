package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.statistics.GenreCardModern
import com.anisync.android.presentation.statistics.HeroDashboard
import com.anisync.android.presentation.util.formatDecimal

fun LazyListScope.profileStatsTab(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    if (profile.animeCount == 0 && profile.mangaCount == 0) {
        item {
            com.anisync.android.presentation.profile.components.PlaceholderTabContent(
                message = stringResource(R.string.profile_placeholder_stats),
                modifier = modifier
            )
        }
        return
    }

    item(key = "stats_header") {
        SectionHeader(
            title = stringResource(R.string.statistics_title),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
        )
    }

    item(key = "stats_hero_dashboard") {
        HeroDashboard(
            count = profile.animeCount.toString(),
            countLabel = stringResource(R.string.statistics_total_anime),
            subStat1Value = formatDecimal(profile.daysWatched),
            subStat1Label = stringResource(R.string.statistics_days_watched),
            subStat1Icon = Icons.Default.Tv,
            subStat2Value = formatDecimal(profile.meanScore),
            subStat2Label = stringResource(R.string.statistics_mean_score),
            subStat2Icon = Icons.Default.Star,
            episodes = profile.chaptersRead.takeIf { it > 0 },
            modifier = modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (profile.topGenres.isNotEmpty()) {
        item(key = "stats_genres_header") {
            SectionHeader(
                title = "Top Genres",
                level = HeaderLevel.Section,
                padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }

        item(key = "stats_genres_list") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(profile.topGenres, key = { it.genre }) { genre ->
                    GenreCardModern(genre = genre)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
