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
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaFollowingEntry
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.util.toIcon
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType

private val StarColor = Color(0xFFFFC107)

private val FollowingCardWidth = 140.dp
private val FollowingCardHeight = 168.dp
private val FollowingAvatarSize = 56.dp

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

    Card(
        onClick = onClick,
        modifier = modifier
            .width(FollowingCardWidth)
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
                modifier = Modifier
                    .size(FollowingAvatarSize)
                    .clip(MaterialShapes.Clover8Leaf.toShape())
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialShapes.Clover8Leaf.toShape()
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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

            // Reserve bottom area so cards stay the same height regardless of score
            Spacer(modifier = Modifier.height(6.dp))
            if (entry.score != null) {
                ScoreChip(score = entry.score, format = entry.scoreFormat)
            }
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
    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier
            .clip(MaterialShapes.Clover8Leaf.toShape())
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialShapes.Clover8Leaf.toShape()
            )
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
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialShapes.Clover8Leaf.toShape())
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialShapes.Clover8Leaf.toShape()
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                if (entry.score != null) ScoreChip(score = entry.score, format = entry.scoreFormat)
                if (entry.progress != null && entry.progress > 0) {
                    Text(
                        text = stringResource(R.string.separator_bullet) + " ${entry.progress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
private fun ScoreChip(score: Double, format: ScoreFormat?) {
    val formatted = remember(score, format) {
        val numeric = if (score % 1.0 == 0.0) score.toInt().toString()
        else String.format("%.1f", score)
        when (format) {
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = StarColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = formatted,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
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
