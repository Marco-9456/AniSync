package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.ui.theme.LocalAvatarShape
import com.anisync.android.ui.theme.emphasis

/**
 * Uppercase icon+text marker row rendered at the very top of content cards.
 * Renders nothing when both flags are false.
 */
@Composable
fun CardLabelStrip(
    isPinned: Boolean,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isPinned && !isLocked) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPinned) {
            CardLabel(
                icon = Icons.Filled.PushPin,
                text = stringResource(R.string.label_pinned),
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (isLocked) {
            CardLabel(
                icon = Icons.Outlined.Lock,
                text = stringResource(R.string.label_locked),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CardLabel(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Chained two-avatar header: leading avatar, trailing avatar overlapping by a bit,
 * then "nameA → nameB" row with a subtitle beneath.
 * Falls back to a single avatar when the second is null.
 */
@Composable
fun ChainedAuthorRow(
    leadingAvatarUrl: String?,
    leadingName: String,
    trailingAvatarUrl: String?,
    trailingName: String?,
    subtitle: String,
    modifier: Modifier = Modifier,
    avatarSize: androidx.compose.ui.unit.Dp = 28.dp,
    onLeadingClick: ((String) -> Unit)? = null,
    onTrailingClick: ((String) -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        AvatarChain(
            leadingAvatarUrl = leadingAvatarUrl,
            leadingName = leadingName,
            trailingAvatarUrl = trailingAvatarUrl,
            trailingName = trailingName,
            avatarSize = avatarSize,
            onLeadingClick = onLeadingClick,
            onTrailingClick = onTrailingClick
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = leadingName,
                    style = MaterialTheme.typography.bodyLarge.emphasis(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).then(
                        if (onLeadingClick != null) Modifier.clickable { onLeadingClick(leadingName) } else Modifier
                    )
                )
                if (!trailingName.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp).size(14.dp)
                    )
                    Text(
                        text = trailingName,
                        style = MaterialTheme.typography.bodyLarge.emphasis(),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).then(
                            if (onTrailingClick != null) Modifier.clickable { onTrailingClick(trailingName) } else Modifier
                        )
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AvatarChain(
    leadingAvatarUrl: String?,
    leadingName: String,
    trailingAvatarUrl: String?,
    trailingName: String?,
    avatarSize: androidx.compose.ui.unit.Dp,
    onLeadingClick: ((String) -> Unit)?,
    onTrailingClick: ((String) -> Unit)?
) {
    if (trailingName == null) {
        AvatarCircle(
            avatarUrl = leadingAvatarUrl,
            contentDescription = leadingName,
            size = avatarSize,
            onClick = onLeadingClick?.let { { it(leadingName) } }
        )
        return
    }
    val overlap = avatarSize * 0.35f
    Box(
        modifier = Modifier
            .width(avatarSize * 2 - overlap + 4.dp)
            .height(avatarSize + 4.dp)
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            AvatarCircle(
                avatarUrl = leadingAvatarUrl,
                contentDescription = leadingName,
                size = avatarSize,
                onClick = onLeadingClick?.let { { it(leadingName) } }
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = avatarSize - overlap)
        ) {
            AvatarCircle(
                avatarUrl = trailingAvatarUrl,
                contentDescription = trailingName,
                size = avatarSize,
                ringColor = MaterialTheme.colorScheme.surfaceContainerLow,
                onClick = onTrailingClick?.let { { it(trailingName) } }
            )
        }
    }
}

@Composable
private fun AvatarCircle(
    avatarUrl: String?,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    ringColor: Color? = null,
    onClick: (() -> Unit)?
) {
    val base = Modifier
        .size(size)
        .clip(LocalAvatarShape.current)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    val ringed = if (ringColor != null) {
        Modifier
            .size(size + 4.dp)
            .clip(LocalAvatarShape.current)
            .background(ringColor)
            .padding(2.dp)
            .clip(LocalAvatarShape.current)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    } else base
    Box(
        modifier = ringed.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(LocalAvatarShape.current)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

fun formatRelativeTimeSeconds(timestampSeconds: Long): String {
    if (timestampSeconds <= 0L) return ""
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestampSeconds
    return when {
        diff < 0 -> "just now"
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 604800}w ago"
        diff < 31536000 -> "${diff / 2592000}mo ago"
        else -> "${diff / 31536000}y ago"
    }
}
