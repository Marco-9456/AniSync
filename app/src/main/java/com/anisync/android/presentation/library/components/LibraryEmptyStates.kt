package com.anisync.android.presentation.library.components

import com.anisync.android.domain.url

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType
import com.anisync.android.util.getTitle

/**
 * Compact search result card for library items.
 * Used in the expanded search bar overlay to display matching results.
 */
@Composable
fun LibrarySearchResultCard(
    entry: LibraryEntry,
    mediaType: MediaType,
    onClick: () -> Unit,
    titleLanguage: TitleLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val title = remember(entry, titleLanguage) {
        entry.getTitle(titleLanguage)
    }

    val statusLabel = entry.status.toLabel(mediaType)
    val subtitle = remember(entry, mediaType, statusLabel) {
        val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
        "$statusLabel • ${entry.progress}/${total ?: "?"}"
    }

    val coverData = entry.cover.url() ?: entry.coverUrl
    val imageRequest = remember(coverData) {
        ImageRequest.Builder(context)
            .data(coverData)
            .crossfade(true)
            .build()
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                clipShape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.a11y_media_poster, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(50.dp)
                    .height(75.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Empty state display for library tabs.
 * Shows an appropriate icon and message based on the status and media type.
 */
@Composable
fun EmptyLibraryTabState(
    status: LibraryStatus?,
    type: MediaType,
    modifier: Modifier = Modifier
) {
    val icon = remember(status, type) {
        when (status) {
            LibraryStatus.CURRENT -> if (type == MediaType.ANIME) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook
            LibraryStatus.PLANNING -> Icons.Default.Check
            LibraryStatus.COMPLETED -> Icons.Default.Check
            else -> Icons.Default.Inbox
        }
    }

    val messageResId = remember(status, type) {
        when (status) {
            LibraryStatus.CURRENT -> if (type == MediaType.ANIME) {
                R.string.empty_watching
            } else {
                R.string.empty_reading
            }

            LibraryStatus.PLANNING -> R.string.empty_planning
            LibraryStatus.COMPLETED -> R.string.empty_completed
            else -> R.string.empty_default
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}