package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.UserActivity
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.components.ActivityUpdateRow
import com.anisync.android.presentation.profile.components.progressText
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatAndYear
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.ExpressiveShapes
import com.anisync.android.util.getTitle
import java.time.ZoneId

private val CoverWidth = 112.dp
private val CoverHeight = 160.dp
private val PortraitWidth = 88.dp
private val PortraitHeight = 124.dp
private val CoverShape = ExpressiveShapes.mediaCover

@Composable
internal fun OverviewHeader(title: String, onAction: (() -> Unit)?, subtitle: String? = null) {
    SectionHeader(
        title = title,
        subtitle = subtitle,
        level = HeaderLevel.Section,
        padding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        onActionClick = onAction
    )
}

@Composable
internal fun WatchingRow(
    entries: List<LibraryEntry>,
    titleLanguage: TitleLanguage,
    onMediaClick: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(entries, key = { it.mediaId }) { entry ->
            val manga = entry.type == MediaType.MANGA
            val total = if (manga) entry.totalChapters else entry.totalEpisodes
            OverviewTile(
                imageUrl = entry.cover.url() ?: entry.coverUrl,
                name = entry.getTitle(titleLanguage),
                detail = if (entry.progress == 0) {
                    stringResource(R.string.profile_watching_not_started)
                } else {
                    progressText(entry.progress, total, if (manga) MediaType.MANGA else MediaType.ANIME)
                },
                width = CoverWidth,
                height = CoverHeight,
                onClick = { onMediaClick(entry.mediaId) },
                belowImage = if (total != null && total > 0) {
                    { FlatProgress(fraction = entry.progress.toFloat() / total, modifier = Modifier.padding(top = 6.dp)) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun FlatProgress(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/** A shelf tile: the picture, what sits under it if anything, the name and one line about it. */
@Composable
private fun OverviewTile(
    imageUrl: String?,
    name: String,
    detail: String?,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    belowImage: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .width(width)
            .clip(CoverShape)
            .bouncyClickable(onClick = onClick, onClickLabel = name)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(CoverShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        belowImage?.invoke()
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
internal fun RecentUpdatesCard(
    updates: List<UserActivity>,
    onActivityClick: (Int) -> Unit,
    onMediaClick: (Int) -> Unit,
    onLikeActivity: ((Int) -> Unit)?
) {
    val zone = ZoneId.systemDefault()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            updates.forEachIndexed { index, update ->
                val day = update.dayIn(zone)
                if (index == 0 || updates[index - 1].dayIn(zone) != day) {
                    ActivityDayHeader(day, start = 16.dp)
                }
                ActivityUpdateRow(
                    activity = update,
                    shape = RoundedCornerShape(0.dp),
                    color = Color.Transparent,
                    onClick = { onActivityClick(update.id) },
                    onMediaClick = onMediaClick,
                    onLikeClick = onLikeActivity?.let { { it(update.id) } }
                )
            }
        }
    }
}

@Composable
internal fun FavouriteKindLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp)
    )
}

@Composable
internal fun FavouritePosterRow(
    entries: List<LibraryEntry>,
    titleLanguage: TitleLanguage,
    onMediaClick: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 20.dp)
    ) {
        items(entries, key = { it.mediaId }) { entry ->
            OverviewTile(
                imageUrl = entry.cover.url() ?: entry.coverUrl,
                name = entry.getTitle(titleLanguage),
                detail = entry.formatAndYear(),
                width = CoverWidth,
                height = CoverHeight,
                onClick = { onMediaClick(entry.mediaId) }
            )
        }
    }
}

internal data class FavouritePortrait(val id: Int, val imageUrl: String?, val name: String, val detail: String?)

@Composable
internal fun FavouritePortraitRow(
    items: List<FavouritePortrait>,
    onClick: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 20.dp)
    ) {
        items(items, key = { it.id }) { (id, image, name, detail) ->
            OverviewTile(
                imageUrl = image,
                name = name,
                detail = detail,
                width = PortraitWidth,
                height = PortraitHeight,
                onClick = { onClick(id) }
            )
        }
    }
}
