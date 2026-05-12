package com.anisync.android.presentation.profile.sections

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.R
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.StudioInfo
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.profile.ProfileFavoritesFilter
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.util.bouncyClickable

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
                    AnimatedTab(
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
            profileMediaTab(
                items = profile.favoriteAnime,
                emptyMessageRes = R.string.profile_placeholder_anime,
                onMediaClick = onMediaClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                transitionPrefix = "fav_anime"
            )
        }

        ProfileFavoritesFilter.MANGA -> {
            profileMediaTab(
                items = profile.favoriteMangaOverview,
                emptyMessageRes = R.string.profile_placeholder_manga,
                onMediaClick = onMediaClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                transitionPrefix = "fav_manga"
            )
        }

        ProfileFavoritesFilter.CHARACTERS -> {
            if (profile.favoriteCharactersOverview.isEmpty()) {
                item(key = "fav_empty_characters", contentType = "empty") {
                    Spacer(modifier = Modifier.height(16.dp))
                    PlaceholderTabContent(
                        message = stringResource(R.string.profile_placeholder_favorites),
                        modifier = modifier
                    )
                }
            } else {
                val rowItems = profile.favoriteCharactersOverview.chunked(3)
                item(key = "fav_top_spacer_characters") { Spacer(modifier = Modifier.height(16.dp)) }

                itemsIndexed(
                    items = rowItems,
                    key = { index, _ -> "fav_row_characters_$index" },
                    contentType = { _, _ -> "fav_row" }
                ) { _, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { character ->
                            Box(modifier = Modifier.weight(1f)) {
                                CharacterItem(
                                    character = character,
                                    onClick = { onCharacterClick(character.id) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        ProfileFavoritesFilter.STAFF -> {
            if (profile.favoriteStaffOverview.isEmpty()) {
                item(key = "fav_empty_staff", contentType = "empty") {
                    Spacer(modifier = Modifier.height(16.dp))
                    PlaceholderTabContent(
                        message = stringResource(R.string.profile_placeholder_favorites),
                        modifier = modifier
                    )
                }
            } else {
                val rowItems = profile.favoriteStaffOverview.chunked(3)
                item(key = "fav_top_spacer_staff") { Spacer(modifier = Modifier.height(16.dp)) }

                itemsIndexed(
                    items = rowItems,
                    key = { index, _ -> "fav_row_staff_$index" },
                    contentType = { _, _ -> "fav_row" }
                ) { _, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { staff ->
                            Box(modifier = Modifier.weight(1f)) {
                                CastStaffItem(
                                    staff = staff,
                                    onClick = { onStaffClick(staff.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
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
                val rowItems = profile.favoriteStudiosOverview.chunked(2)
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
                        repeat(2 - row.size) {
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

@Composable
private fun CastStaffItem(
    staff: StaffDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .clip(imageShape)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = staff.nameUserPreferred
            )
            .padding(bottom = dimensionResource(R.dimen.spacing_small))
    ) {
        AsyncImage(
            model = staff.imageUrl,
            contentDescription = staff.nameUserPreferred,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(dimensionResource(R.dimen.character_image_height))
                .fillMaxWidth()
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
        Text(
            text = staff.nameUserPreferred,
            style = MaterialTheme.typography.labelMedium.emphasis(),
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary
        )
        if (staff.primaryOccupations.isNotEmpty()) {
            Text(
                text = staff.primaryOccupations.first(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
