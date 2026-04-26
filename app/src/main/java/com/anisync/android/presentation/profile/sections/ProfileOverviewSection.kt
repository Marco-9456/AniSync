package com.anisync.android.presentation.profile.sections

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.StaffDetails
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.PosterCard
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.details.components.CharacterItem
import com.anisync.android.presentation.profile.ProfileTab
import com.anisync.android.presentation.profile.RecentUpdatesSection
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.statistics.GenreCardModern
import com.anisync.android.presentation.statistics.HeroDashboard
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.formatDecimal

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileOverviewSection(
    profile: UserProfile,
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

    Column(modifier = modifier) {
        // Recent Updates
        if (profile.activities.isNotEmpty()) {
            RecentUpdatesSection(
                activities = profile.activities.take(5),
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                onUserClick = onUserClick,
                onActivityClick = onActivityClick,
                onMediaClick = onMediaClick,
                onLastReplyClick = onLastReplyClick,
                onSubscribeClick = onSubscribeClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Statistics (Hero Dashboard + Top Genres)
        if (profile.animeCount > 0 || profile.mangaCount > 0) {
            SectionHeader(
                title = stringResource(R.string.statistics_title),
                level = HeaderLevel.Section,
                padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                onActionClick = { onNavigateToTab(ProfileTab.STATS) }
            )
            HeroDashboard(
                count = profile.animeCount.toString(),
                countLabel = stringResource(R.string.statistics_total_anime),
                subStat1Value = formatDecimal(profile.daysWatched),
                subStat1Label = stringResource(R.string.statistics_days_watched),
                subStat1Icon = Icons.Default.Tv,
                subStat2Value = formatDecimal(profile.meanScore),
                subStat2Label = stringResource(R.string.statistics_mean_score),
                subStat2Icon = Icons.Default.Star,
                episodes = profile.chaptersRead.takeIf { it > 0 }
            )

            if (profile.topGenres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
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
            model = coverUrl,
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
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
