package com.anisync.android.presentation.profile.sections

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.anisync.android.presentation.components.MediaPosterCard
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.R
import com.anisync.android.domain.StudioInfo
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.RailChip
import com.anisync.android.presentation.profile.ProfileFavoritesFilter
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.profile.components.ProfileTabRail
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.util.getTitle
import java.text.NumberFormat
import com.anisync.android.ui.theme.ExpressiveShapes

@OptIn(ExperimentalSharedTransitionApi::class)
fun LazyListScope.profileFavoritesTab(
    profile: UserProfile,
    selectedFilter: ProfileFavoritesFilter,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onMediaClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    studioColumns: Int = 2,
    modifier: Modifier = Modifier,
    gridColumns: Int = 3,
    titleLanguage: com.anisync.android.data.TitleLanguage = com.anisync.android.data.TitleLanguage.ROMAJI
) {
    when (selectedFilter) {
        ProfileFavoritesFilter.ANIME, ProfileFavoritesFilter.MANGA -> {
            val anime = selectedFilter == ProfileFavoritesFilter.ANIME
            val items = if (anime) profile.favoriteAnime else profile.favoriteMangaOverview
            favoritesGrid(
                items = items,
                keyPrefix = if (anime) "fav_anime" else "fav_manga",
                columns = gridColumns,
                emptyMessage = if (anime) R.string.profile_placeholder_anime else R.string.profile_placeholder_manga,
                key = { it.mediaId },
                modifier = modifier
            ) { entry, cellModifier ->
                MediaPosterCard(
                    item = entry,
                    onClick = { onMediaClick(entry.mediaId) },
                    titleLanguage = titleLanguage,
                    transitionPrefix = if (anime) "fav_anime" else "fav_manga",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = cellModifier
                )
            }
        }

        ProfileFavoritesFilter.CHARACTERS -> favoritesGrid(
            items = profile.favoriteCharactersOverview,
            keyPrefix = "fav_characters",
            columns = gridColumns,
            emptyMessage = R.string.profile_placeholder_favorites,
            key = { it.id },
            modifier = modifier
        ) { character, cellModifier ->
            FavoritePortrait(
                imageUrl = character.imageUrl,
                name = character.nameUserPreferred,
                detail = character.series?.getTitle(titleLanguage),
                onClick = { onCharacterClick(character.id) },
                modifier = cellModifier
            )
        }

        ProfileFavoritesFilter.STAFF -> favoritesGrid(
            items = profile.favoriteStaffOverview,
            keyPrefix = "fav_staff",
            columns = gridColumns,
            emptyMessage = R.string.profile_placeholder_favorites,
            key = { it.id },
            modifier = modifier
        ) { member, cellModifier ->
            FavoritePortrait(
                imageUrl = member.imageUrl,
                name = member.nameUserPreferred,
                detail = member.primaryOccupations.firstOrNull(),
                onClick = { onStaffClick(member.id) },
                modifier = cellModifier
            )
        }

        ProfileFavoritesFilter.STUDIOS -> {
            if (profile.favoriteStudiosOverview.isEmpty()) {
                item(key = "fav_empty_studios", contentType = "empty") {
                    Spacer(modifier = Modifier.height(16.dp))
                    PlaceholderTabContent(
                        message = stringResource(R.string.profile_placeholder_favorites),
                        modifier = modifier
                    )
                }
            } else {
                val rowItems = profile.favoriteStudiosOverview.chunked(studioColumns)
                item(key = "fav_top_spacer_studios") { Spacer(modifier = Modifier.height(16.dp)) }

                itemsIndexed(
                    items = rowItems,
                    key = { index, _ -> "fav_row_studios_$index" },
                    contentType = { _, _ -> "fav_row" }
                ) { _, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { studio ->
                            Box(modifier = Modifier.weight(1f)) {
                                StudioItem(
                                    studio = studio,
                                    onClick = { onStudioClick(studio.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        repeat(studioColumns - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
internal fun FavoritesFilterRail(
    selected: ProfileFavoritesFilter,
    count: Int,
    ownerName: String?,
    onSelect: (ProfileFavoritesFilter) -> Unit
) {
    Column {
        ProfileTabRail(ProfileFavoritesFilter.entries, selected) { filter, isSelected ->
            RailChip(
                label = stringResource(filter.labelRes),
                icon = favoritesFilterIcon(filter),
                selected = isSelected,
                onClick = { onSelect(filter) }
            )
        }
        if (count > 0) {
            val numbers = remember { NumberFormat.getIntegerInstance() }
            val amount = pluralStringResourceOrDefault(
                when (selected) {
                    ProfileFavoritesFilter.ANIME, ProfileFavoritesFilter.MANGA -> R.plurals.profile_favorites_count_titles
                    ProfileFavoritesFilter.CHARACTERS -> R.plurals.profile_favorites_count_characters
                    ProfileFavoritesFilter.STAFF -> R.plurals.profile_favorites_count_staff
                    ProfileFavoritesFilter.STUDIOS -> R.plurals.profile_favorites_count_studios
                },
                count,
                numbers.format(count)
            )
            val order = if (ownerName == null) {
                stringResource(R.string.profile_favorites_order_own)
            } else {
                stringResource(R.string.profile_favorites_order, ownerName)
            }
            Text(
                text = stringResource(R.string.profile_people_summary, amount, order),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

private fun <T> LazyListScope.favoritesGrid(
    items: List<T>,
    keyPrefix: String,
    columns: Int,
    @StringRes emptyMessage: Int,
    key: (T) -> Int,
    modifier: Modifier,
    cell: @Composable (T, Modifier) -> Unit
) {
    if (items.isEmpty()) {
        item(key = "${keyPrefix}_empty", contentType = "empty") {
            Spacer(modifier = Modifier.height(16.dp))
            PlaceholderTabContent(message = stringResource(emptyMessage), modifier = modifier)
        }
        return
    }
    item(key = "${keyPrefix}_top") { Spacer(modifier = Modifier.height(8.dp)) }
    itemsIndexed(
        items = items.chunked(columns),
        key = { _, row -> "${keyPrefix}_row_${key(row.first())}" },
        contentType = { _, _ -> "fav_grid_row" }
    ) { _, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            row.forEach { cell(it, Modifier.weight(1f)) }
            repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FavoritePortrait(
    imageUrl: String?,
    name: String,
    detail: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = ExpressiveShapes.mediaCover
    Column(
        modifier = modifier
            .clip(shape)
            .bouncyClickable(onClick = onClick, onClickLabel = name, clipShape = shape)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
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
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun favoritesFilterIcon(filter: ProfileFavoritesFilter): ImageVector {
    return when (filter) {
        ProfileFavoritesFilter.ANIME -> Icons.Default.Tv
        ProfileFavoritesFilter.MANGA -> Icons.AutoMirrored.Filled.MenuBook
        ProfileFavoritesFilter.CHARACTERS -> Icons.Default.Face
        ProfileFavoritesFilter.STAFF -> Icons.Default.Badge
        ProfileFavoritesFilter.STUDIOS -> Icons.Default.Business
    }
}

@Composable
private fun StudioItem(
    studio: StudioInfo,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = studio.name,
            style = MaterialTheme.typography.labelLarge.emphasis(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
