package com.anisync.android.presentation.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaFollowingEntry
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.util.toIcon
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import androidx.compose.ui.unit.Dp
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.ui.theme.LocalAvatarShape

private val StarColor = Color(0xFFFFC107)

private val FollowingCardWidth = 140.dp
private val FollowingCardWidthMax = 220.dp
private val FollowingCardHeight = 168.dp
private val FollowingAvatarSize = 56.dp

// Fixed chrome inside the card, used to derive how much width the variable-length
// stats/status pills actually need (see rememberFollowingCardWidth).
private val CardHorizontalPadding = 24.dp // Column padding 12dp * 2
private val PillHorizontalPadding = 16.dp // pill padding 8dp * 2
private val PillIconAndGap = 14.dp        // 12dp icon + 2dp/4dp gap
private val StatsSeparatorWidth = 15.dp   // 6dp + 3dp dot + 6dp between score and Ep

/**
 * Per-card width that fits the card's own content. Measures the formatted score
 * ("8.5/10"), episode ("Ep 213") and status label, adds the fixed pill/card chrome,
 * and returns the larger of the default width and what's needed — so only the cards
 * whose content would otherwise truncate grow, the rest stay compact. Clamped to
 * [FollowingCardWidth, FollowingCardWidthMax].
 */
@Composable
private fun rememberFollowingCardWidth(entry: MediaFollowingEntry, mediaType: MediaType?): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Measure with the exact weights the pills render at — SemiBold/Medium glyphs are
    // wider than Normal, so measuring at the base weight would undercount and clip.
    val statsStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
    val statusStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)

    val scoreText = entry.score?.let { formatScore(it, entry.scoreFormat) }
    val epText = entry.progress?.takeIf { it > 0 }?.let { "Ep $it" }
    val statusLabel = entry.status.toLabel(mediaType)

    return remember(scoreText, epText, statusLabel, statsStyle, statusStyle, density) {
        fun textWidth(text: String, style: TextStyle): Dp =
            with(density) { measurer.measure(text, style).size.width.toDp() }

        val statsWidth = PillHorizontalPadding +
            (if (scoreText != null) PillIconAndGap + textWidth(scoreText, statsStyle) else 0.dp) +
            (if (scoreText != null && epText != null) StatsSeparatorWidth else 0.dp) +
            (if (epText != null) textWidth(epText, statsStyle) else 0.dp)

        val statusWidth = PillHorizontalPadding + PillIconAndGap + textWidth(statusLabel, statusStyle)

        // +2dp guards against px->dp rounding so the weighted text never lands a hair short.
        (CardHorizontalPadding + maxOf(statsWidth, statusWidth) + 2.dp)
            .coerceIn(FollowingCardWidth, FollowingCardWidthMax)
    }
}

private fun formatScore(score: Double, format: ScoreFormat?): String {
    val numeric = if (score % 1.0 == 0.0) score.toInt().toString() else String.format("%.1f", score)
    return when (format) {
        ScoreFormat.POINT_100 -> "${score.toInt()}/100"
        ScoreFormat.POINT_10 -> "${score.toInt()}/10"
        ScoreFormat.POINT_10_DECIMAL -> "$numeric/10"
        ScoreFormat.POINT_5 -> "${score.toInt()}/5"
        ScoreFormat.POINT_3 -> when {
            score >= 3.0 -> ":)"
            score >= 2.0 -> ":|"
            score >= 1.0 -> ":("
            else -> numeric
        }
        null -> numeric
    }
}

@Composable
fun FollowingItem(
    entry: MediaFollowingEntry,
    mediaType: MediaType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = entry.status.toColor()
    val statusLabel = entry.status.toLabel(mediaType)
    val statusIcon = entry.status.toIcon(mediaType)
    val cardWidth = rememberFollowingCardWidth(entry, mediaType)

    Card(
        onClick = onClick,
        modifier = modifier
            .width(cardWidth)
            .height(FollowingCardHeight),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedAvatar(
                url = entry.userAvatarUrl,
                size = FollowingAvatarSize
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.userName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            StatusPill(
                label = statusLabel,
                icon = statusIcon,
                color = statusColor
            )

            // Push stats to the bottom to maintain consistent card height
            Spacer(modifier = Modifier.weight(1f))
            StatsPill(
                score = entry.score,
                format = entry.scoreFormat,
                progress = entry.progress
            )
        }
    }
}

/**
 * Avatar request that disables hardware bitmaps so animated GIF/WebP avatars
 * (AniList serves these on `avatar.large`) animate reliably; hardware bitmaps
 * with a clipped CircleShape can otherwise freeze on the first frame.
 */
@Composable
private fun AnimatedAvatar(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .crossfade(true)
            .build()
    }
    UserAvatar(
        contentDescription = null,
        size = size,
        model = request,
        modifier = modifier
    )
}

@Composable
fun FollowingRow(
    entry: MediaFollowingEntry,
    mediaType: MediaType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = entry.status.toColor()
    val statusLabel = entry.status.toLabel(mediaType)
    val statusIcon = entry.status.toIcon(mediaType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedAvatar(
            url = entry.userAvatarUrl,
            size = 48.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = entry.userName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(label = statusLabel, icon = statusIcon, color = statusColor)
                StatsPill(score = entry.score, format = entry.scoreFormat, progress = entry.progress)
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun StatsPill(
    score: Double?,
    format: ScoreFormat?,
    progress: Int?,
    modifier: Modifier = Modifier
) {
    if (score == null && (progress == null || progress <= 0)) return

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (score != null) {
            val formatted = remember(score, format) { formatScore(score, format) }
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = StarColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = formatted,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Add a dot separator if both score and progress exist
        if (score != null && progress != null && progress > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Spacer(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (progress != null && progress > 0) {
            Text(
                text = "Ep $progress",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

private fun LibraryStatus.toColor(): Color = when (this) {
    LibraryStatus.CURRENT -> Color(0xFF4CAF50)
    LibraryStatus.COMPLETED -> Color(0xFF2196F3)
    LibraryStatus.PLANNING -> Color(0xFF9C27B0)
    LibraryStatus.PAUSED -> Color(0xFFFF9800)
    LibraryStatus.DROPPED -> Color(0xFFF44336)
    LibraryStatus.REPEATING -> Color(0xFF009688)
    LibraryStatus.UNKNOWN -> Color(0xFF9E9E9E)
}

@Preview(showBackground = true)
@Composable
private fun FollowingItemPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            FollowingItem(
                entry = MediaFollowingEntry(
                    userId = 1,
                    userName = "AnimeFan99",
                    userAvatarUrl = null,
                    status = LibraryStatus.CURRENT,
                    score = 9.5,
                    scoreFormat = ScoreFormat.POINT_10_DECIMAL,
                    progress = 1045
                ),
                mediaType = MediaType.ANIME,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FollowingRowPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            FollowingRow(
                entry = MediaFollowingEntry(
                    userId = 2,
                    userName = "MangaReader22",
                    userAvatarUrl = null,
                    status = LibraryStatus.COMPLETED,
                    score = 10.0,
                    scoreFormat = ScoreFormat.POINT_10,
                    progress = 1100
                ),
                mediaType = MediaType.ANIME,
                onClick = {}
            )
        }
    }
}