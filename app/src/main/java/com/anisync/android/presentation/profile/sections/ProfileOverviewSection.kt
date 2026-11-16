package com.anisync.android.presentation.profile.sections

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.ActivityHistoryDay
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.ReviewCard
import com.anisync.android.presentation.profile.ProfileFavoritesFilter
import com.anisync.android.presentation.profile.ProfileTab
import com.anisync.android.presentation.profile.components.ActivityCard
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.statistics.ActivityHistorySection
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.util.getTitle

private const val RECENT_UPDATES = 5
private const val FAVOURITES_PER_KIND = 10

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileOverviewSection(
    profile: UserProfile,
    activityHistory: List<ActivityHistoryDay> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onNavigateToTab: (ProfileTab) -> Unit = {},
    onOpenFavorites: (ProfileFavoritesFilter) -> Unit = {},
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
    modifier: Modifier = Modifier,
    watching: List<LibraryEntry> = emptyList(),
    latestReview: MediaReview? = null,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    onOpenWatching: () -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onReviewClick: (MediaReview) -> Unit = {}
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

    val pinned = remember(profile.activities) { profile.activities.firstOrNull { it.isPinned } }
    val updates = remember(profile.activities) {
        profile.activities.filter { it.type == ActivityType.MEDIA_LIST && !it.isPinned }.take(RECENT_UPDATES)
    }

    Column(modifier = modifier) {
        if (pinned != null) {
            val canDelete = viewerId != null && pinned.userId == viewerId
            val canEdit = canDelete && pinned.type == ActivityType.TEXT
            ActivityCard(
                activity = pinned,
                onClick = { onActivityClick(pinned.id) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                onUserClick = onUserClick,
                onMediaClick = onMediaClick,
                onLastReplyClick = onLastReplyClick,
                onSubscribeClick = { onSubscribeClick(pinned.id) },
                onLikeClick = onLikeActivity?.let { { it(pinned.id) } },
                onDeleteClick = if (canDelete) onDeleteActivity?.let { { it(pinned.id) } } else null,
                onEditClick = if (canEdit) onEditActivity?.let { { it(pinned.id) } } else null,
                maxBodyLines = 10,
                showAuthor = true,
                pinnedLabel = true
            )
        }

        if (watching.isNotEmpty()) {
            OverviewHeader(
                title = stringResource(R.string.profile_overview_watching),
                subtitle = pluralStringResourceOrDefault(R.plurals.profile_favorites_count_titles, watching.size, watching.size.toString()),
                onAction = onOpenWatching
            )
            WatchingRow(watching, titleLanguage, onMediaClick)
        }

        if (updates.isNotEmpty()) {
            OverviewHeader(
                title = stringResource(R.string.section_recent_updates),
                onAction = { onNavigateToTab(ProfileTab.ACTIVITY) }
            )
            RecentUpdatesCard(updates, onActivityClick, onMediaClick, onLikeActivity)
        }

        val hasFavourites = profile.favoriteAnime.isNotEmpty() || profile.favoriteMangaOverview.isNotEmpty() ||
            profile.favoriteCharactersOverview.isNotEmpty() || profile.favoriteStaffOverview.isNotEmpty()
        if (hasFavourites) {
            val firstKind = when {
                profile.favoriteAnime.isNotEmpty() -> ProfileFavoritesFilter.ANIME
                profile.favoriteMangaOverview.isNotEmpty() -> ProfileFavoritesFilter.MANGA
                profile.favoriteCharactersOverview.isNotEmpty() -> ProfileFavoritesFilter.CHARACTERS
                else -> ProfileFavoritesFilter.STAFF
            }
            OverviewHeader(
                title = stringResource(R.string.section_favorites),
                onAction = { onOpenFavorites(firstKind) }
            )
            if (profile.favoriteAnime.isNotEmpty()) {
                FavouriteKindLabel(stringResource(R.string.media_type_anime))
                FavouritePosterRow(profile.favoriteAnime.take(FAVOURITES_PER_KIND), titleLanguage, onMediaClick)
            }
            if (profile.favoriteMangaOverview.isNotEmpty()) {
                FavouriteKindLabel(stringResource(R.string.media_type_manga))
                FavouritePosterRow(profile.favoriteMangaOverview.take(FAVOURITES_PER_KIND), titleLanguage, onMediaClick)
            }
            if (profile.favoriteCharactersOverview.isNotEmpty()) {
                FavouriteKindLabel(stringResource(R.string.profile_cast_characters))
                FavouritePortraitRow(
                    items = profile.favoriteCharactersOverview.take(FAVOURITES_PER_KIND).map {
                        Triple(it.id, it.imageUrl, it.nameUserPreferred to it.series?.getTitle(titleLanguage))
                    },
                    onClick = onCharacterClick
                )
            }
            if (profile.favoriteStaffOverview.isNotEmpty()) {
                FavouriteKindLabel(stringResource(R.string.profile_cast_staff))
                FavouritePortraitRow(
                    items = profile.favoriteStaffOverview.take(FAVOURITES_PER_KIND).map {
                        Triple(it.id, it.imageUrl, it.nameUserPreferred to it.primaryOccupations.firstOrNull())
                    },
                    onClick = onStaffClick
                )
            }
        }

        if (activityHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ActivityHistorySection(
                activityHistory,
                userId = profile.id,
                onMediaClick = onMediaClick,
                onActivityClick = onActivityClick
            )
        }

        if (latestReview != null) {
            OverviewHeader(
                title = stringResource(R.string.profile_latest_review),
                onAction = onOpenReviews
            )
            ReviewCard(
                review = latestReview,
                onClick = { onReviewClick(latestReview) },
                modifier = Modifier.padding(horizontal = 16.dp),
                onUserClick = onUserClick
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
