package com.anisync.android.presentation.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.UserActivity
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import kotlinx.collections.immutable.ImmutableList

/**
 * One card for the run of list updates a person posts while catching up.
 *
 * The global feed is mostly these, and one card each meant the same avatar, name and timestamp
 * three times over for three episodes of the same evening. The author is stated once, then each
 * update is a row that opens its own activity.
 */
@Composable
fun GroupedListActivityCard(
    activities: ImmutableList<UserActivity>,
    onActivityClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onUserClick: (String) -> Unit = {},
    onMediaClick: (Int) -> Unit = {}
) {
    val author = activities.first()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserAvatar(
                    url = author.userAvatarUrl,
                    contentDescription = author.userName,
                    size = 32.dp,
                    modifier = Modifier.clickable { author.userName?.let(onUserClick) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = author.userName.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { author.userName?.let(onUserClick) }
                    )
                    Text(
                        text = stringResource(
                            R.string.feed_group_summary,
                            pluralStringResourceOrDefault(
                                R.plurals.feed_group_updates,
                                activities.size,
                                activities.size
                            ),
                            formatRelativeTimeSeconds(
                                LocalResources.current,
                                author.timestamp / 1000L
                            )
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            activities.forEach { activity ->
                GroupedUpdateRow(
                    activity = activity,
                    onClick = { onActivityClick(activity.id) },
                    onMediaClick = onMediaClick
                )
            }
        }
    }
}

@Composable
private fun GroupedUpdateRow(
    activity: UserActivity,
    onClick: () -> Unit,
    onMediaClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GroupedCover(activity = activity, onMediaClick = onMediaClick)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.mediaTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = groupedProgressText(activity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (activity.isLiked) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = activity.likeCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupedCover(activity: UserActivity, onMediaClick: (Int) -> Unit) {
    val coverModifier = Modifier
        .width(40.dp)
        .aspectRatio(0.71f)
        .clip(RoundedCornerShape(8.dp))
        .let { base ->
            val mediaId = activity.mediaId
            if (mediaId != null) base.clickable { onMediaClick(mediaId) } else base
        }
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (activity.mediaCoverUrl != null) {
        AsyncImage(
            model = activity.mediaCover.url() ?: activity.mediaCoverUrl,
            contentDescription = activity.mediaTitle,
            modifier = coverModifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = coverModifier, contentAlignment = Alignment.Center) {
            Text(
                text = activity.mediaTitle.take(2).ifBlank { "??" }.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "Watched episode 5", with the number in the accent progress wears everywhere else. */
@Composable
private fun groupedProgressText(activity: UserActivity): AnnotatedString {
    val accent = MaterialTheme.colorScheme.primary
    return remember(activity.status, activity.progress, accent) {
        val status = (activity.status ?: "Updated")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        buildAnnotatedString {
            append(status)
            activity.progress?.takeIf { it.isNotBlank() }?.let { progress ->
                append(" ")
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
                    append(progress)
                }
            }
        }
    }
}
