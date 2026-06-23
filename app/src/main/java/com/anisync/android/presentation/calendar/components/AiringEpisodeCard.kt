package com.anisync.android.presentation.calendar.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.AiringEpisode
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.ScoreBadge
import com.anisync.android.presentation.components.StatusBadge
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatTimeUntilAiring
import com.anisync.android.util.TitleUtils
import java.util.Date

/**
 * A single airing entry in the calendar: cover thumbnail, title, episode number, the
 * local airing time, a live countdown ("in 2h" / "Aired"), the average score, and a
 * "Watching"/"Planning" chip when the media is on the viewer's list.
 *
 * @param nowEpochSec current time in Unix seconds, hoisted so one ticker drives every
 *   card's countdown without each card holding its own clock.
 */
@Composable
fun AiringEpisodeCard(
    episode: AiringEpisode,
    titleLanguage: TitleLanguage,
    nowEpochSec: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    val context = LocalContext.current
    val title = TitleUtils.getTitle(
        titleLanguage,
        episode.titleRomaji,
        episode.titleEnglish,
        episode.titleNative,
        episode.titleUserPreferred
    )
    val secondsUntil = episode.airingAt - nowEpochSec
    val hasAired = secondsUntil <= 0

    val timeText = remember(context, episode.airingAt) {
        DateFormat.getTimeFormat(context).format(Date(episode.airingAt * 1000))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(
                onClick = onClick,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                clipShape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .alpha(if (hasAired) 0.55f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(episode.coverImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(58.dp)
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.calendar_episode_number, episode.episode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val statusLabelRes = when (episode.listStatus) {
                    LibraryStatus.CURRENT, LibraryStatus.REPEATING -> R.string.calendar_chip_watching
                    LibraryStatus.PLANNING -> R.string.calendar_chip_planning
                    else -> null
                }
                if (episode.averageScore != null || statusLabelRes != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusLabelRes?.let {
                            StatusBadge(
                                text = stringResource(it),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        episode.averageScore?.takeIf { it > 0 }?.let { ScoreBadge(score = it) }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (hasAired) {
                        stringResource(R.string.calendar_aired)
                    } else {
                        stringResource(R.string.calendar_airs_in, formatTimeUntilAiring(secondsUntil.toInt()))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasAired) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}
