package com.anisync.android.presentation.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaFollowingEntry
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.components.MetaChip
import com.anisync.android.presentation.components.NoteBottomSheet
import com.anisync.android.presentation.components.NotePreview
import com.anisync.android.presentation.components.ScoreChip
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.util.toColor
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType

/** Progress label for a following entry: "Ep N" for anime, "Ch N" for manga. */
private fun progressChipLabel(progress: Int, mediaType: MediaType?): String =
    if (mediaType == MediaType.MANGA) "Ch $progress" else "Ep $progress"

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

/**
 * A followed user's entry for a media: avatar beside the user's name, status/score/progress chips
 * and a one-line note preview that opens the full note in a sheet when it overflows (#78). Used both
 * stacked in the detail page's Following strip and listed in the "show all" sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FollowingRow(
    entry: MediaFollowingEntry,
    mediaType: MediaType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNoteSheet by remember(entry.userId) { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedAvatar(url = entry.userAvatarUrl, size = 48.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.userName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Single line so every row is the same height (the strip stacks them at a fixed size).
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                maxLines = 1
            ) {
                MetaChip(label = entry.status.toLabel(mediaType), accent = entry.status.toColor())
                ScoreChip(score = entry.score, format = entry.scoreFormat)
                entry.progress?.takeIf { it > 0 }?.let {
                    MetaChip(label = progressChipLabel(it, mediaType))
                }
            }
            if (!entry.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                NotePreview(
                    note = entry.notes,
                    onOpen = { showNoteSheet = true }
                )
            }
        }
    }

    if (showNoteSheet && !entry.notes.isNullOrBlank()) {
        NoteBottomSheet(
            note = entry.notes,
            heading = entry.userName,
            onDismiss = { showNoteSheet = false }
        )
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
                    progress = 1100,
                    notes = "Read it twice."
                ),
                mediaType = MediaType.ANIME,
                onClick = {}
            )
        }
    }
}
