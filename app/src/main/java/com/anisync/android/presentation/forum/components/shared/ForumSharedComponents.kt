package com.anisync.android.presentation.forum.components.shared

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatBadge(
    icon: ImageVector,
    value: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = value.formatCount(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AuthorRow(
    name: String,
    avatarUrl: String?,
    timestampSeconds: Long,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 20.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar of $name",
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "•",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = timestampSeconds.toRelativeTime(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

private fun Int.formatCount(): String = when {
    this >= 1000 -> "${this / 1000}k"
    else -> toString()
}

private fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - this
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 2592000 -> "${diff / 86400}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(this * 1000))
    }
}
