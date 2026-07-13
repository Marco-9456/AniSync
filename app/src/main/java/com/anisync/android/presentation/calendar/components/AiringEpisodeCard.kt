package com.anisync.android.presentation.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * AniWorld-backed calendar card. The shared minute ticker is supplied by the parent; cards never
 * launch their own coroutine. Unmatched entries remain visible but are deliberately not clickable.
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
    val hasAired = episode.hasSourceTime && secondsUntil <= 0
    val timeText = episode.sourceLocalTimeMinutes?.let {
        (if (episode.isApproximate) "~ " else "") + "%02d:%02d".format(it / 60, it % 60)
    } ?: stringResource(R.string.calendar_time_missing)
    val releaseLabel = when (episode.releaseKind) {
        "FILM" -> stringResource(R.string.calendar_release_film)
        "SPECIAL" -> stringResource(R.string.calendar_release_special)
        "EPISODE" -> stringResource(R.string.calendar_episode_number, episode.episode)
        else -> stringResource(R.string.calendar_release_unknown)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (episode.isDetailsAvailable) {
                    Modifier.bouncyClickable(
                        onClick = onClick,
                        onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                        clipShape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .alpha(if (hasAired) 0.55f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (episode.coverImageUrl != null && episode.isDetailsAvailable) {
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
            } else {
                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ImageNotSupported,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                    text = releaseLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                episode.languageLabel?.let { language ->
                    Text(
                        text = language,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

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
                if (episode.hasSourceTime) {
                    Spacer(Modifier.height(2.dp))
                    val countdown = if (hasAired) {
                        stringResource(R.string.calendar_aired)
                    } else {
                        stringResource(
                            R.string.calendar_airs_in,
                            formatTimeUntilAiring(secondsUntil.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                        )
                    }
                    Text(
                        text = if (episode.isApproximate && !hasAired) {
                            stringResource(R.string.calendar_countdown_approximate, countdown)
                        } else {
                            countdown
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
}
