package com.anisync.android.presentation.profile.sections

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.StaffInfo
import com.anisync.android.domain.StudioInfo
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.SegmentedTabItem
import com.anisync.android.presentation.details.CastListCard
import com.anisync.android.presentation.details.StaffListCard
import com.anisync.android.presentation.profile.ProfileFavoritesFilter
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.profile.components.ProfileMediaListCard
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.type.MediaType

@OptIn(ExperimentalSharedTransitionApi::class)
fun LazyListScope.profileFavoritesTab(
    profile: UserProfile,
    selectedFilter: ProfileFavoritesFilter,
    onFilterSelected: (ProfileFavoritesFilter) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onMediaClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onStudioClick: (Int) -> Unit = {},
    listColumns: Int = 1,
    studioColumns: Int = 2,
    modifier: Modifier = Modifier
) {
    item(key = "favorites_filters", contentType = "filters") {
        val filters = remember { ProfileFavoritesFilter.entries }
        val selectedIndex = remember(selectedFilter) { filters.indexOf(selectedFilter).coerceAtLeast(0) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(filters) { index, filter ->
                    SegmentedTabItem(
                        index = index,
                        selectedIndex = selectedIndex,
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        icon = favoritesFilterIcon(filter),
                        label = stringResource(filter.labelRes)
                    )
                }
            }
        }
    }

    when (selectedFilter) {
        ProfileFavoritesFilter.ANIME -> {
            profileFavoritesMediaList(
                items = profile.favoriteAnime,
                mediaType = MediaType.ANIME,
                emptyMessageRes = R.string.profile_placeholder_anime,
                onMediaClick = onMediaClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                transitionPrefix = "fav_anime",
                listColumns = listColumns,
                modifier = modifier
            )
        }

        ProfileFavoritesFilter.MANGA -> {
            profileFavoritesMediaList(
                items = profile.favoriteMangaOverview,
                mediaType = MediaType.MANGA,
                emptyMessageRes = R.string.profile_placeholder_manga,
                onMediaClick = onMediaClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                transitionPrefix = "fav_manga",
                listColumns = listColumns,
                modifier = modifier
            )
        }

        ProfileFavoritesFilter.CHARACTERS -> {
            // Portrait + name only — a global favourite has no single role, and AniList omits
            // per-media role/VA on the favourites path.
            profileFavoritesPersonList(
                items = profile.favoriteCharactersOverview,
                keyPrefix = "characters",
                idOf = { it.id },
                modifier = modifier
            ) { character ->
                CastListCard(
                    character = character,
                    voiceActor = null,
                    onClick = { onCharacterClick(character.id) },
                    onVoiceActorClick = {},
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        ProfileFavoritesFilter.STAFF -> {
            profileFavoritesPersonList(
                items = profile.favoriteStaffOverview,
                keyPrefix = "staff",
                idOf = { it.id },
                modifier = modifier
            ) { member ->
                StaffListCard(
                    staff = member.toStaffInfo(),
                    onClick = { onStaffClick(member.id) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
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

private fun favoritesFilterIcon(filter: ProfileFavoritesFilter): ImageVector {
    return when (filter) {
        ProfileFavoritesFilter.ANIME -> Icons.Default.Tv
        ProfileFavoritesFilter.MANGA -> Icons.AutoMirrored.Filled.MenuBook
        ProfileFavoritesFilter.CHARACTERS -> Icons.Default.Person
        ProfileFavoritesFilter.STAFF -> Icons.Default.Group
        ProfileFavoritesFilter.STUDIOS -> Icons.Default.Business
    }
}

/**
 * List rows for the favourites Anime/Manga sub-tabs, reusing [ProfileMediaListCard] in favourites
 * mode (`showViewerListData = false`): cover, title, type, and release year only — no viewer list
 * status/score/progress, which AniList doesn't surface for favourites. Chunked into [listColumns]
 * so wide windows fill the width (1-up on phones, up to 2-up on tablets).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private fun LazyListScope.profileFavoritesMediaList(
    items: List<LibraryEntry>,
    mediaType: MediaType,
    @StringRes emptyMessageRes: Int,
    onMediaClick: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    transitionPrefix: String,
    listColumns: Int,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        item(key = "fav_media_empty_$transitionPrefix", contentType = "empty") {
            Spacer(modifier = Modifier.height(16.dp))
            PlaceholderTabContent(
                message = stringResource(emptyMessageRes),
                modifier = modifier
            )
        }
        return
    }

    val rowItems = items.chunked(listColumns)
    item(key = "fav_media_top_spacer_$transitionPrefix") { Spacer(modifier = Modifier.height(16.dp)) }

    itemsIndexed(
        items = rowItems,
        key = { index, _ -> "fav_media_row_${transitionPrefix}_$index" },
        contentType = { _, _ -> "fav_media_row" }
    ) { _, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            row.forEach { entry ->
                Box(modifier = Modifier.weight(1f)) {
                    ProfileMediaListCard(
                        entry = entry,
                        mediaType = mediaType,
                        onClick = { onMediaClick(entry.mediaId) },
                        // Favourites: show type + release year only (no viewer list status/score/progress).
                        showViewerListData = false,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        transitionPrefix = transitionPrefix
                    )
                }
            }
            repeat(listColumns - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

/**
 * Single-column list rows for the favourites Characters / Staff sub-tabs, reusing the Media Details
 * cast/staff cards ([card]). [idOf] keys each row; [keyPrefix] namespaces the item keys.
 */
private fun <T> LazyListScope.profileFavoritesPersonList(
    items: List<T>,
    keyPrefix: String,
    idOf: (T) -> Int,
    modifier: Modifier,
    card: @Composable (T) -> Unit
) {
    if (items.isEmpty()) {
        item(key = "fav_empty_$keyPrefix", contentType = "empty") {
            Spacer(modifier = Modifier.height(16.dp))
            PlaceholderTabContent(
                message = stringResource(R.string.profile_placeholder_favorites),
                modifier = modifier
            )
        }
        return
    }

    item(key = "fav_top_spacer_$keyPrefix") { Spacer(modifier = Modifier.height(16.dp)) }
    items(
        count = items.size,
        key = { i -> "fav_${keyPrefix}_${idOf(items[i])}" },
        contentType = { "fav_person_row" }
    ) { i -> card(items[i]) }
    item(key = "fav_bottom_spacer_$keyPrefix") { Spacer(modifier = Modifier.height(12.dp)) }
}

/** Adapt a favourited [StaffDetails] to the [StaffInfo] the shared [StaffListCard] renders. */
private fun StaffDetails.toStaffInfo(): StaffInfo = StaffInfo(
    id = id,
    nameFull = name,
    nameNative = nativeName,
    nameUserPreferred = nameUserPreferred,
    imageUrl = imageUrl,
    role = "",
    primaryOccupations = primaryOccupations
)

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
