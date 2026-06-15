package com.anisync.android.presentation.profile.sections

import com.anisync.android.ui.theme.emphasis
import com.anisync.android.domain.url

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.ActivityHistoryDay
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.PosterCard
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.profile.ProfileTab
import com.anisync.android.presentation.profile.RecentUpdatesSection
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.statistics.ActivityHeatmapSection
import com.anisync.android.presentation.statistics.GenreCardModern
import com.anisync.android.presentation.util.bouncyClickable

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileOverviewSection(
    profile: UserProfile,
    activityHistory: List<ActivityHistoryDay> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onNavigateToTab: (ProfileTab) -> Unit = {},
    onMediaClick: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onActivityClick: (Int) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    onSubscribeClick: (Int) -> Unit = {},
    onLikeActivity: ((Int) -> Unit)? = null,
    onDeleteActivity: ((Int) -> Unit)? = null,
    onEditActivity: ((Int) -> Unit)? = null,
    viewerId: Int? = null,
    modifier: Modifier = Modifier
) {
    val isProfileEmpty = profile.activities.isEmpty() &&
            profile.animeCount == 0 &&
            profile.mangaCount == 0 &&
            profile.favoriteAnime.isEmpty() &&
            profile.favoriteMangaOverview.isEmpty() &&
            profile.favoriteCharactersOverview.isEmpty() &&
            profile.favoriteStaffOverview.isEmpty()

    if (isProfileEmpty) {
        PlaceholderTabContent(
            message = stringResource(R.string.profile_no_recent_updates),
            modifier = modifier
        )
        return
    }

    val hasLibrary = profile.animeCount > 0 || profile.mangaCount > 0

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))

        // Featured activity leads the tab: the pinned status if there is one, otherwise the latest.
        // The list arrives sorted pinned-first then newest, but pick explicitly so intent survives
        // any upstream re-sort. Capped to a compact teaser; the header's expand opens full Activity.
        if (profile.activities.isNotEmpty()) {
            val featured = remember(profile.activities) {
                profile.activities.firstOrNull { it.isPinned } ?: profile.activities.first()
            }
            RecentUpdatesSection(
                activities = listOf(featured),
                maxItems = 1,
                maxBodyLines = 10,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                onActionClick = { onNavigateToTab(ProfileTab.ACTIVITY) },
                onUserClick = onUserClick,
                onActivityClick = onActivityClick,
                onMediaClick = onMediaClick,
                onLastReplyClick = onLastReplyClick,
                onSubscribeClick = onSubscribeClick,
                onLikeClick = onLikeActivity,
                onDeleteClick = onDeleteActivity,
                onEditClick = onEditActivity,
                viewerId = viewerId
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Activity heatmap — leads the tab as the most glanceable "what they've been up to".
        if (activityHistory.isNotEmpty()) {
            ActivityHeatmapSection(activityHistory)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Library snapshot — the at-a-glance summary; the header's expand button opens full Stats.
        if (hasLibrary) {
            SectionHeader(
                title = stringResource(R.string.statistics_title),
                level = HeaderLevel.Section,
                padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onActionClick = { onNavigateToTab(ProfileTab.STATS) }
            )
            ProfileLibrarySnapshot(profile = profile)

            if (profile.topGenres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = stringResource(R.string.statistics_top_genres),
                    level = HeaderLevel.Section,
                    padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    onActionClick = { onNavigateToTab(ProfileTab.STATS) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profile.topGenres.take(5), key = { it.genre }) { genre ->
                        GenreCardModern(genre = genre)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Favorites: Anime
        if (profile.favoriteAnime.isNotEmpty()) {
            HorizontalFavoritesSection(
                title = stringResource(R.string.media_type_anime),
                onActionClick = { onNavigateToTab(ProfileTab.ANIME) }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profile.favoriteAnime.take(5), key = { it.mediaId }) { media ->
                        Box(modifier = Modifier.width(110.dp)) {
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                PosterCard(
                                    item = media,
                                    onClick = { onMediaClick(media.mediaId) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    transitionPrefix = "overview_anime"
                                )
                            } else {
                                PosterCardFallback(
                                    coverUrl = media.coverUrl,
                                    title = media.titleRomaji ?: media.titleEnglish ?: "",
                                    onClick = { onMediaClick(media.mediaId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Favorites: Manga
        if (profile.favoriteMangaOverview.isNotEmpty()) {
            HorizontalFavoritesSection(
                title = stringResource(R.string.media_type_manga),
                onActionClick = { onNavigateToTab(ProfileTab.MANGA) }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profile.favoriteMangaOverview.take(5), key = { it.mediaId }) { media ->
                        Box(modifier = Modifier.width(110.dp)) {
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                PosterCard(
                                    item = media,
                                    onClick = { onMediaClick(media.mediaId) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    transitionPrefix = "overview_manga"
                                )
                            } else {
                                PosterCardFallback(
                                    coverUrl = media.coverUrl,
                                    title = media.titleRomaji ?: media.titleEnglish ?: "",
                                    onClick = { onMediaClick(media.mediaId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Favorites: Characters
        if (profile.favoriteCharactersOverview.isNotEmpty()) {
            HorizontalFavoritesSection(
                title = stringResource(R.string.profile_cast_characters),
                onActionClick = { onNavigateToTab(ProfileTab.FAVORITES) }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profile.favoriteCharactersOverview.take(5), key = { it.id }) { character ->
                        CharacterItem(
                            character = character,
                            onClick = { onCharacterClick(character.id) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }
        }

        // Favorites: Staff
        if (profile.favoriteStaffOverview.isNotEmpty()) {
            HorizontalFavoritesSection(
                title = stringResource(R.string.profile_cast_staff),
                onActionClick = { onNavigateToTab(ProfileTab.FAVORITES) }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profile.favoriteStaffOverview.take(5), key = { it.id }) { staff ->
                        StaffItem(
                            staff = staff,
                            onClick = { onStaffClick(staff.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HorizontalFavoritesSection(
    title: String,
    onActionClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        SectionHeader(
            title = title,
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            onActionClick = onActionClick
        )
        content()
    }
}

@Composable
private fun PosterCardFallback(
    coverUrl: String?,
    cover: com.anisync.android.domain.CoverImage? = null,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .bouncyClickable(onClick = onClick)
    ) {
        AsyncImage(
            model = cover.url() ?: coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.emphasis(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StaffItem(
    staff: StaffDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .width(dimensionResource(R.dimen.character_item_width))
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
