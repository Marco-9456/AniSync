package com.anisync.android.presentation.forum.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.ui.theme.LocalAvatarShape
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
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value.formatCount(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

@Composable
fun AuthorRow(
    name: String,
    avatarUrl: String?,
    timestampSeconds: Long,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 24.dp,
    onUserClick: ((String) -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.then(
            if (onUserClick != null) Modifier.clickable { onUserClick(name) } else Modifier
        )
    ) {
        UserAvatar(
            url = avatarUrl,
            contentDescription = "Avatar of $name",
            size = avatarSize
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.separator_bullet),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = timestampSeconds.toRelativeTime(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun Int.formatCount(): String = when {
    this >= 1_000_000 -> {
        val m = this / 1_000_000.0
        if (m % 1.0 == 0.0) "${m.toInt()}M" else "%.1fM".format(m)
    }
    this >= 1000 -> {
        val k = this / 1000.0
        if (k % 1.0 == 0.0) "${k.toInt()}k" else "%.1fk".format(k)
    }
    else -> toString()
}

private fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - this
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 604800}w ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(this * 1000))
    }
}
