package com.anisync.android.presentation.components.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatScore
import com.anisync.android.presentation.components.ListIndicator
import com.anisync.android.presentation.components.ListIndicatorStyle
import com.anisync.android.presentation.library.components.EditButton
import com.anisync.android.presentation.library.components.EpisodeProgressBar
import com.anisync.android.presentation.library.components.IncrementButton
import com.anisync.android.presentation.library.components.QueueCover
import com.anisync.android.presentation.library.components.QueueStateLine
import com.anisync.android.presentation.library.components.StateLineWithCount
import com.anisync.android.presentation.library.components.airedCount
import com.anisync.android.presentation.library.components.progressLabel
import com.anisync.android.presentation.util.LocalLibraryStatuses
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.communityScoreLabel
import com.anisync.android.presentation.util.formatAndYear
import com.anisync.android.presentation.util.formatTimeUntilAiring
import com.anisync.android.presentation.util.lengthFact
import com.anisync.android.presentation.util.releaseStatusLabel
import com.anisync.android.presentation.util.selectedPaneItem
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.util.getTitle

private val RowHeight = 120.dp
private val CompactRowHeight = 76.dp
private val RowShape = RoundedCornerShape(16.dp)

/**
 * A title in Library search: the queue row's cover and action on a row tall enough for two lines
 * of title. An entry in progress keeps the queue row's state line and episode bar, anything else
 * shows its facts, with the viewer's own score when the cards are set to show it.
 */
@Composable
fun LibraryResultRow(
    entry: LibraryEntry,
    mediaType: MediaType,
    titleLanguage: TitleLanguage,
    showScore: Boolean,
    scoreFormat: ScoreFormat,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = entry.getTitle(titleLanguage)
    val total = if (mediaType == MediaType.MANGA) entry.totalChapters else entry.totalEpisodes
    val hasProgress = entry.progress > 0 && (total == null || entry.progress < total)

    ResultRowFrame(title = title, onClick = onClick, modifier = modifier, cover = {
        QueueCover(
            entry = entry,
            title = title,
            dimmed = false,
            priority = entry.priorityLevel,
            sharedTransitionScope = null,
            animatedVisibilityScope = null
        )
    }, trailing = {
        if (entry.status == LibraryStatus.CURRENT || entry.status == LibraryStatus.REPEATING) {
            IncrementButton(nextEpisode = entry.progress + 1, onIncrement = onIncrement, onEdit = onEdit)
        } else {
            EditButton(onEdit = onEdit)
        }
    }) {
        if (hasProgress) {
            val aired = airedCount(entry, total)
            Column {
                StateLineWithCount(
                    stateLine = { QueueStateLine(entry = entry, total = total, aired = aired) },
                    count = progressLabel(entry.progress, aired, total),
                    compactCount = progressLabel(entry.progress, aired, total, compact = true),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                EpisodeProgressBar(
                    progress = entry.progress,
                    aired = aired,
                    total = total,
                    fraction = if ((total ?: 0) > 0) entry.progress.toFloat() / total!! else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            val score = entry.score?.takeIf { showScore && it > 0.0 }
            FactsBand(
                facts = entry.factsLine(mediaType),
                score = score?.let { formatScore(it, scoreFormat) },
                scoreStar = scoreFormat != ScoreFormat.POINT_5 && scoreFormat != ScoreFormat.POINT_3,
                entry = entry
            )
        }
    }
}

/**
 * A title in Discover search. It ends in the list it is already on, or in Add to list, which puts
 * it on Planning without leaving the search.
 */
@Composable
fun DiscoverResultRow(
    entry: LibraryEntry,
    titleLanguage: TitleLanguage,
    onClick: () -> Unit,
    onAddToList: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val title = entry.getTitle(titleLanguage)
    val listStatus = LocalLibraryStatuses.current[entry.mediaId]

    ResultRowFrame(title = title, onClick = onClick, selected = selected, modifier = modifier, cover = {
        QueueCover(
            entry = entry,
            title = title,
            dimmed = false,
            priority = null,
            sharedTransitionScope = null,
            animatedVisibilityScope = null
        )
    }, trailing = {
        if (listStatus != null) {
            ListIndicator(status = listStatus, type = entry.type, style = ListIndicatorStyle.Chip)
        } else {
            AddToListButton(onClick = onAddToList, size = 56.dp, corner = 18.dp)
        }
    }) {
        FactsBand(
            facts = entry.factsLine(),
            score = entry.communityScoreLabel(),
            scoreStar = true,
            entry = entry
        )
    }
}

/**
 * A character or staff member: the portrait in the cover slot, the native name or occupation where
 * a title's facts go, and favourites where its score goes.
 */
@Composable
fun PersonResultRow(
    name: String,
    detail: String?,
    imageUrl: String?,
    favourites: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    ResultRowFrame(title = name, onClick = onClick, selected = selected, modifier = modifier, cover = {
        PersonPortrait(
            imageUrl = imageUrl,
            contentDescription = name,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(70.dp).height(100.dp)
        )
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            detail?.let { FactsText(it) }
            favourites?.let { FavouritesLine(it) }
        }
    }
}

/** A user, who has nothing to show but an avatar and a name. */
@Composable
fun UserResultRow(
    name: String,
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompactRowFrame(title = name, onClick = onClick, modifier = modifier, thumb = {
        com.anisync.android.presentation.components.UserAvatar(
            contentDescription = name,
            size = 56.dp,
            url = avatarUrl
        )
    })
}

/** A studio: no artwork exists for one, so a tile stands in for it. */
@Composable
fun StudioResultRow(
    name: String,
    favourites: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    CompactRowFrame(
        title = name,
        onClick = onClick,
        selected = selected,
        modifier = modifier,
        thumb = { StudioTile(size = 56.dp) },
        detail = favourites?.let { { FavouritesLine(it) } }
    )
}

/**
 * Cover, content and action in the queue row's proportions: the cover sits 10dp from every edge,
 * the title takes two real lines at the top and [band] is pinned to the bottom of the column.
 */
@Composable
private fun ResultRowFrame(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    cover: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    band: @Composable () -> Unit
) {
    Card(
        shape = RowShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .selectedPaneItem(selected, RowShape)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                clipShape = RowShape
            )
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            cover()
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(top = 2.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                band()
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.align(Alignment.CenterVertically)) { trailing() }
            }
        }
    }
}

@Composable
private fun CompactRowFrame(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    thumb: @Composable () -> Unit,
    detail: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RowShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(CompactRowHeight)
            .selectedPaneItem(selected, RowShape)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                clipShape = RowShape
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            thumb()
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                detail?.invoke()
            }
        }
    }
}

/**
 * Format, year and length on the first line, then the score and where the title is in its run.
 * A title that airs on a schedule says when the next episode lands instead of "Releasing", the way
 * the queue row does.
 */
@Composable
private fun FactsBand(
    facts: String?,
    score: String?,
    scoreStar: Boolean,
    entry: LibraryEntry
) {
    val countdown = entry.dynamicTimeUntilAiring
    val nextEpisode = entry.nextAiringEpisode
    val state = if (countdown != null && nextEpisode != null) {
        stringResource(R.string.airing_episode_in, nextEpisode, formatTimeUntilAiring(countdown))
    } else {
        entry.releaseStatusLabel()
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        facts?.let { FactsText(it) }
        if (score != null || state != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (score != null) {
                    IconValue(
                        icon = Icons.Default.Star.takeIf { scoreStar },
                        tint = StarGold,
                        value = score
                    )
                }
                if (score != null && state != null) Separator()
                if (state != null) {
                    StateLabel(
                        text = state,
                        icon = Icons.Default.Schedule.takeIf { countdown != null && nextEpisode != null }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryEntry.factsLine(mediaType: MediaType? = type): String? =
    listOfNotNull(formatAndYear(), lengthFact(mediaType)).joinToString(" · ").ifEmpty { null }

@Composable
private fun FactsText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** A 14dp glyph and a bold value: the score with its star, favourites with their heart. */
@Composable
private fun IconValue(icon: ImageVector?, tint: Color, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun FavouritesLine(count: Int) {
    IconValue(
        icon = Icons.Default.Favorite,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        value = formatCount(count)
    )
}

@Composable
private fun RowScope.StateLabel(text: String, icon: ImageVector?) {
    Row(
        modifier = Modifier.weight(1f, fill = false),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Separator() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
