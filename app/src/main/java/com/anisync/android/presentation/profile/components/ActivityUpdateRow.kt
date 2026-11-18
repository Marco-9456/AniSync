package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.UserActivity
import com.anisync.android.domain.recap.parseProgress
import com.anisync.android.domain.url
import java.util.Date

@Composable
internal fun ActivityUpdateRow(
    activity: UserActivity,
    shape: Shape,
    onClick: () -> Unit,
    onMediaClick: (Int) -> Unit,
    onLikeClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    extraTop: Dp = 0.dp,
    extraBottom: Dp = 0.dp
) {
    val context = LocalContext.current
    val time = remember(activity.timestamp) {
        android.text.format.DateFormat.getTimeFormat(context).format(Date(activity.timestamp))
    }
    val progress = updateAction(activity.status, activity.progress)
    Surface(
        onClick = onClick,
        shape = shape,
        color = color,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp + extraTop, bottom = 8.dp + extraBottom)
        ) {
            AsyncImage(
                model = activity.mediaCover?.url() ?: activity.mediaCoverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClickLabel = activity.mediaTitle) {
                        activity.mediaId?.let(onMediaClick)
                    }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (progress.isNotBlank()) {
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = activity.mediaTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 6.dp)) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = onLikeClick != null,
                            role = Role.Button,
                            onClickLabel = stringResource(if (activity.isLiked) R.string.cd_unlike else R.string.cd_like)
                        ) { onLikeClick?.invoke() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (activity.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (activity.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activity.likeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

internal fun updateRowShape(top: Boolean, bottom: Boolean): Shape = RoundedCornerShape(
    topStart = if (top) 24.dp else 0.dp,
    topEnd = if (top) 24.dp else 0.dp,
    bottomStart = if (bottom) 24.dp else 0.dp,
    bottomEnd = if (bottom) 24.dp else 0.dp
)

/**
 * What the update did, "Read chapters 1 to 30" rather than AniList's "read chapter 1 - 30". Verbs
 * without a count, such as completed or dropped, keep AniList's wording.
 */
@Composable
private fun updateAction(status: String?, progress: String?): String {
    val verb = status?.trim()?.lowercase().orEmpty()
    val range = parseProgress(progress)
    val phrase = when {
        verb.startsWith("rewatched episode") -> R.string.profile_update_rewatched_one to R.string.profile_update_rewatched_range
        verb.startsWith("watched episode") -> R.string.profile_update_watched_one to R.string.profile_update_watched_range
        verb.startsWith("reread chapter") -> R.string.profile_update_reread_one to R.string.profile_update_reread_range
        verb.startsWith("read chapter") -> R.string.profile_update_read_one to R.string.profile_update_read_range
        else -> null
    }
    if (phrase != null && range != null) {
        return if (range.first == range.last) {
            stringResource(phrase.first, range.first)
        } else {
            stringResource(phrase.second, range.first, range.last)
        }
    }
    val capitalised = verb.replaceFirstChar { it.titlecase() }
    return listOfNotNull(capitalised.takeIf { it.isNotBlank() }, progress?.takeIf { it.isNotBlank() }).joinToString(" ")
}
