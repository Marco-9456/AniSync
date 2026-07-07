package com.anisync.android.presentation.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.profile.ProfileStatsType
import com.anisync.android.presentation.profile.StatisticsUiModel
import com.anisync.android.presentation.util.formatCompactNumber
import com.anisync.android.presentation.util.formatDecimal
import com.anisync.android.ui.theme.LocalExpressiveTypography

/**
 * Shareable card summarising a user's anime **or** manga statistics. The header rides the user's
 * own AniList banner (falling back to an accent when they have none), with their avatar and the
 * headline total below, then three secondary stat tiles and a top-genres row. Reads its numbers
 * straight off [StatisticsUiModel] so it always matches the Stats tab.
 */
@Composable
fun ProfileStatsShareCard(
    profile: UserProfile,
    stats: StatisticsUiModel,
    type: ProfileStatsType,
    modifier: Modifier = Modifier,
) {
    if (LocalShareCardConfig.current.template == ShareCardTemplate.HERO) {
        RecapShareCard(profile = profile, stats = stats, type = type, modifier = modifier)
        return
    }
    val expressive = LocalExpressiveTypography.current
    val isAnime = type == ProfileStatsType.ANIME

    ShareCardScaffold(modifier = modifier, handle = profile.name) {
        ShareCardBannerBox(bannerUrl = profile.bannerUrl, height = 108.dp, scrimAlpha = 0.78f) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // UserAvatar honours the user's chosen avatar shape + frame (LocalAvatarShape).
                UserAvatar(
                    contentDescription = null,
                    size = 52.dp,
                    url = profile.avatarUrl,
                    borderColor = Color.White.copy(alpha = 0.9f),
                    borderWidth = 2.dp
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            if (isAnime) R.string.share_stats_eyebrow_anime
                            else R.string.share_stats_eyebrow_manga
                        ).uppercase(),
                        style = expressive.statLabel,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            val total = if (isAnime) stats.animeStats.totalCount else (stats.mangaStats?.totalCount ?: 0)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatCompactNumber(total),
                    style = expressive.statNumericLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(
                        if (isAnime) R.string.share_stats_unit_anime
                        else R.string.share_stats_unit_manga
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isAnime) {
                    val a = stats.animeStats
                    ShareStatTile(formatDecimal(a.daysWatched), stringResource(R.string.share_stats_days), Modifier.weight(1f))
                    ShareStatTile(formatCompactNumber(a.episodesWatched), stringResource(R.string.share_stats_episodes), Modifier.weight(1f))
                    ShareStatTile(formatDecimal(a.meanScore), stringResource(R.string.share_stats_mean), Modifier.weight(1f))
                } else {
                    val m = stats.mangaStats
                    ShareStatTile(formatCompactNumber(m?.chaptersRead ?: 0), stringResource(R.string.share_stats_chapters), Modifier.weight(1f))
                    ShareStatTile(formatCompactNumber(m?.volumesRead ?: 0), stringResource(R.string.share_stats_volumes), Modifier.weight(1f))
                    ShareStatTile(formatDecimal(m?.meanScore ?: 0.0), stringResource(R.string.share_stats_mean), Modifier.weight(1f))
                }
            }

            val genres = (if (isAnime) stats.animeStats.genreDistribution
            else stats.mangaStats?.genreDistribution.orEmpty()).take(3)
            if (genres.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.share_stats_top_genres).uppercase(),
                    style = expressive.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    genres.forEach { g -> ShareChip(g.genre) }
                }
            }
        }
    }
}

/**
 * The "Recap" stats template: a Wrapped-style card for the **selected library** (anime or manga,
 * matching the Stats tab toggle) — headline title count, a 2×2 tile grid, the library's top genres,
 * and a spotlight line (most-watched studio for anime, most-read author for manga).
 */
@Composable
private fun RecapShareCard(
    profile: UserProfile,
    stats: StatisticsUiModel,
    type: ProfileStatsType,
    modifier: Modifier = Modifier,
) {
    val expressive = LocalExpressiveTypography.current
    val isAnime = type == ProfileStatsType.ANIME
    val anime = stats.animeStats
    val manga = stats.mangaStats

    val totalTitles = if (isAnime) anime.totalCount else (manga?.totalCount ?: 0)
    val topGenres = (if (isAnime) anime.genreDistribution else manga?.genreDistribution.orEmpty()).take(3)
    val completed = (if (isAnime) anime.statusDistribution else manga?.statusDistribution.orEmpty())
        .firstOrNull { it.status.equals("COMPLETED", ignoreCase = true) }?.count
    // Spotlight: the studio you watched most vs the author you read most.
    val spotlightLabel = stringResource(
        if (isAnime) R.string.share_recap_top_studio else R.string.share_recap_top_staff
    )
    val spotlightName = if (isAnime) {
        anime.studioDistribution.firstOrNull()?.studioName
    } else {
        manga?.staffDistribution?.firstOrNull()?.name
    }

    ShareCardScaffold(modifier = modifier, handle = profile.name) {
        ShareCardBannerBox(bannerUrl = profile.bannerUrl, height = 108.dp, scrimAlpha = 0.78f) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    contentDescription = null,
                    size = 52.dp,
                    url = profile.avatarUrl,
                    borderColor = Color.White.copy(alpha = 0.9f),
                    borderWidth = 2.dp
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.share_recap_eyebrow).uppercase(),
                        style = expressive.statLabel,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatCompactNumber(totalTitles),
                    style = expressive.statNumericLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(
                        if (isAnime) R.string.share_stats_unit_anime else R.string.share_stats_unit_manga
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isAnime) {
                    ShareStatTile(formatDecimal(anime.daysWatched), stringResource(R.string.share_stats_days), Modifier.weight(1f))
                    ShareStatTile(formatCompactNumber(anime.episodesWatched), stringResource(R.string.share_stats_episodes), Modifier.weight(1f))
                } else {
                    ShareStatTile(formatCompactNumber(manga?.chaptersRead ?: 0), stringResource(R.string.share_stats_chapters), Modifier.weight(1f))
                    ShareStatTile(formatCompactNumber(manga?.volumesRead ?: 0), stringResource(R.string.share_stats_volumes), Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShareStatTile(
                    formatCompactNumber(completed ?: 0),
                    stringResource(R.string.share_recap_completed),
                    Modifier.weight(1f)
                )
                ShareStatTile(
                    formatDecimal(if (isAnime) anime.meanScore else (manga?.meanScore ?: 0.0)),
                    stringResource(R.string.share_stats_mean),
                    Modifier.weight(1f)
                )
            }

            if (topGenres.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.share_stats_top_genres).uppercase(),
                    style = expressive.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    topGenres.forEach { g -> ShareChip(g.genre) }
                }
            }

            if (spotlightName != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = spotlightLabel.uppercase(),
                    style = expressive.statLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = spotlightName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
