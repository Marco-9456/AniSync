package com.anisync.android.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.ui.theme.StatManga
import com.anisync.android.ui.theme.StatScore
import com.anisync.android.ui.theme.StatusCompleted
import java.util.Locale

/**
 * Displays a 2x2 grid of profile statistics cards plus an anime status bar.
 */
@Composable
fun ProfileStatsRow(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row: Days Watched & Anime Completed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Memoize formatted days watched
            val daysWatchedFormatted = remember(profile.daysWatched) {
                String.format(Locale.US, "%.1f", profile.daysWatched)
            }
            
            StatCard(
                icon = Icons.Default.AccessTime,
                value = daysWatchedFormatted,
                label = stringResource(R.string.stat_days_watched),
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                iconColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            StatCard(
                icon = Icons.Default.Movie,
                value = profile.animeCount.toString(),
                label = stringResource(R.string.stat_anime_completed),
                iconBackgroundColor = StatusCompleted.copy(alpha = 0.15f),
                iconColor = StatusCompleted,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Second row: Manga Read & Mean Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                value = profile.mangaCount.toString(),
                label = stringResource(R.string.stat_manga_read),
                iconBackgroundColor = StatManga.copy(alpha = 0.15f),
                iconColor = StatManga,
                modifier = Modifier.weight(1f)
            )
            
            // Memoize formatted mean score
            val meanScoreFormatted = remember(profile.meanScore) {
                String.format(Locale.US, "%.1f", profile.meanScore)
            }
            
            StatCard(
                icon = Icons.Default.Star,
                value = meanScoreFormatted,
                label = stringResource(R.string.stat_mean_score),
                iconBackgroundColor = StatScore.copy(alpha = 0.15f),
                iconColor = StatScore,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Anime Status Bar
        AnimeStatusBar(profile = profile)
    }
}

/**
 * A single statistics card displaying an icon, value, and label.
 */
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconBackgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Icon in colored circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBackgroundColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Value
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
