package com.anisync.android.presentation.components.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LocalCoverQuality
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.ListIndicator
import com.anisync.android.presentation.components.ListIndicatorStyle
import com.anisync.android.presentation.library.components.PosterAspect
import com.anisync.android.presentation.library.components.PosterFacts
import com.anisync.android.presentation.library.components.PosterStateChip
import com.anisync.android.presentation.library.components.PosterTitle
import com.anisync.android.presentation.library.components.PosterValueText
import com.anisync.android.presentation.util.LocalLibraryStatuses
import com.anisync.android.presentation.util.TransitionKeys
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.communityScoreLabel
import com.anisync.android.presentation.util.formatAndYear
import com.anisync.android.presentation.util.selectedPaneItem
import com.anisync.android.ui.theme.ExpressiveShapes
import com.anisync.android.ui.theme.StarGold
import com.anisync.android.util.getTitle

/**
 * A title in Discover search, as the Library grid card draws one. The bottom-end corner is the list
 * tab when the title is tracked and Add to list when it is not, and the countdown takes the other
 * bottom corner on anything with a schedule.
 */
@Composable
fun DiscoverResultPoster(
    entry: LibraryEntry,
    titleLanguage: TitleLanguage,
    onClick: () -> Unit,
    onAddToList: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val title = entry.getTitle(titleLanguage)
    val listStatus = LocalLibraryStatuses.current[entry.mediaId]
    val coverUrl = entry.cover.url() ?: entry.coverUrl
    val cacheKey = TransitionKeys.imageCacheKey(TransitionKeys.DISCOVER, entry.mediaId) +
        "-" + LocalCoverQuality.current.name + TransitionKeys.coverVersion(coverUrl)

    PosterFrame(title = title, onClick = onClick, selected = selected, modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PosterAspect)
                .clip(ExpressiveShapes.mediaCover)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .placeholderMemoryCacheKey(cacheKey)
                    .memoryCacheKey(cacheKey)
                    .build(),
                contentDescription = stringResource(R.string.a11y_media_poster, title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Without a list entry nothing is waiting to be watched, so the chip only ever counts
            // down to the next episode.
            PosterStateChip(
                entry = entry,
                aired = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .widthIn(max = (maxWidth - 76.dp).coerceAtLeast(0.dp))
            )
            if (listStatus != null) {
                ListIndicator(
                    status = listStatus,
                    type = entry.type,
                    style = ListIndicatorStyle.Corner,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            } else {
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                    AddToListButton(onClick = onAddToList, size = 48.dp, corner = 16.dp)
                }
            }
        }
        PosterText(title = title) {
            val score = entry.communityScoreLabel()
            PosterFacts(
                facts = entry.formatAndYear(),
                value = score?.let { { PosterIconValue(Icons.Default.Star, StarGold, it, iconSize = 15.dp) } }
            )
        }
    }
}

/** A character or staff member, the portrait where a cover goes and favourites where a score does. */
@Composable
fun PersonResultPoster(
    name: String,
    detail: String?,
    imageUrl: String?,
    favourites: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    PosterFrame(title = name, onClick = onClick, selected = selected, modifier = modifier) {
        PersonPortrait(
            imageUrl = imageUrl,
            contentDescription = name,
            shape = ExpressiveShapes.mediaCover,
            modifier = Modifier.fillMaxWidth().aspectRatio(PosterAspect)
        )
        PosterText(title = name) {
            PosterFacts(
                facts = detail,
                value = favourites?.let { { FavouritesValue(it) } }
            )
        }
    }
}

/** A user: the avatar square across the card, the name under it. */
@Composable
fun UserResultCard(
    name: String,
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PosterFrame(title = name, onClick = onClick, modifier = modifier) {
        PersonPortrait(
            imageUrl = avatarUrl,
            contentDescription = name,
            shape = ExpressiveShapes.mediaCover,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )
        Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            PosterTitle(name, maxLines = 1)
        }
    }
}

/** A studio: the tile where its artwork would be, then the name and its favourites. */
@Composable
fun StudioResultCard(
    name: String,
    favourites: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    PosterFrame(title = name, onClick = onClick, selected = selected, modifier = modifier) {
        Box(modifier = Modifier.padding(start = 12.dp, top = 16.dp)) {
            StudioTile(size = 56.dp)
        }
        PosterText(title = name) {
            PosterFacts(
                facts = stringResource(R.string.search_result_studio),
                value = favourites?.let { { FavouritesValue(it) } }
            )
        }
    }
}

@Composable
private fun PosterFrame(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = ExpressiveShapes.mediaCover
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .selectedPaneItem(selected, shape)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = stringResource(R.string.a11y_action_open_details, title),
                clipShape = shape
            )
    ) {
        Column(content = content)
    }
}

/** The title on its two reserved lines, then one 18dp meta row. */
@Composable
private fun PosterText(title: String, meta: @Composable RowScope.() -> Unit) {
    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
        PosterTitle(title)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = meta
        )
    }
}

@Composable
private fun FavouritesValue(count: Int) {
    PosterIconValue(
        icon = Icons.Default.Favorite,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        value = formatCount(count),
        iconSize = 14.dp
    )
}

/** A glyph before a bold value: the score with its star, favourites with their heart. */
@Composable
private fun PosterIconValue(icon: ImageVector, tint: Color, value: String, iconSize: Dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
        PosterValueText(value)
    }
}
