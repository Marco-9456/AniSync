package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.formatDecimal

data class EditorialStat(
    val value: String,
    val label: String,
    val icon: ImageVector? = null
)

enum class RatingBadgeSize { Default, Small }

@Composable
fun RatingBadge(meanScore: Float, size: RatingBadgeSize = RatingBadgeSize.Default) {
    val hasRating = meanScore > 0.0f
    val (iconSize, textStyle, verticalPadding) = when (size) {
        RatingBadgeSize.Default -> Triple(12.dp, MaterialTheme.typography.labelMedium, 4.dp)
        RatingBadgeSize.Small -> Triple(10.dp, MaterialTheme.typography.labelSmall, 2.dp)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = verticalPadding)
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = if (hasRating) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (hasRating) formatDecimal(meanScore) else stringResource(R.string.not_available),
            style = textStyle,
            color = if (hasRating) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

fun formatDisplayName(format: String): String = when (format) {
    "TV" -> "TV Series"
    "TV_SHORT" -> "Short"
    "MOVIE" -> "Movie"
    "SPECIAL" -> "Special"
    "OVA" -> "OVA"
    "ONA" -> "ONA"
    "MUSIC" -> "Music"
    "MANGA" -> "Manga"
    "NOVEL" -> "Novel"
    "ONE_SHOT" -> "One Shot"
    else -> format.replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}

fun getFormatIcon(format: String): ImageVector = when (format) {
    "TV", "TV_SHORT" -> Icons.Default.Tv
    "MOVIE" -> Icons.Default.Movie
    "SPECIAL", "OVA", "ONA" -> Icons.Default.Videocam
    "MUSIC" -> Icons.Default.MusicNote
    "MANGA", "NOVEL", "ONE_SHOT" -> Icons.Default.Book
    else -> Icons.Default.PlayArrow
}

// region Previews

@Preview(showBackground = true, name = "RatingBadge — default × rated/unrated")
@Composable
private fun RatingBadgeDefaultPreview() {
    StatPreviewSurface(isDark = false) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RatingBadge(meanScore = 8.4f, size = RatingBadgeSize.Default)
            RatingBadge(meanScore = 0f, size = RatingBadgeSize.Default)
        }
    }
}

@Preview(showBackground = true, name = "RatingBadge — small × rated/unrated")
@Composable
private fun RatingBadgeSmallPreview() {
    StatPreviewSurface(isDark = false) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RatingBadge(meanScore = 7.1f, size = RatingBadgeSize.Small)
            RatingBadge(meanScore = 0f, size = RatingBadgeSize.Small)
        }
    }
}

@Preview(showBackground = true, name = "RatingBadge — dark")
@Composable
private fun RatingBadgeDarkPreview() {
    StatPreviewSurface(isDark = true) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RatingBadge(meanScore = 9.2f)
            RatingBadge(meanScore = 0f)
            RatingBadge(meanScore = 6.0f, size = RatingBadgeSize.Small)
        }
    }
}

@Preview(showBackground = true, name = "formatDisplayName + icon — every format")
@Composable
private fun FormatHelpersPreview() {
    val samples = listOf("TV", "TV_SHORT", "MOVIE", "SPECIAL", "OVA", "ONA", "MUSIC", "MANGA", "NOVEL", "ONE_SHOT", "WEIRD_UNKNOWN_TYPE")
    StatPreviewSurface(isDark = false) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            samples.forEach { fmt ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(getFormatIcon(fmt), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(formatDisplayName(fmt), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// endregion
