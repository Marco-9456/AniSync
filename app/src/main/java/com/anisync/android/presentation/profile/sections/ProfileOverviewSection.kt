package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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

/** What the Overview shows and what its parts open, worked out once for either layout. */
@Stable
internal class OverviewBlocks(
    val profile: UserProfile,
    val activityHistory: List<ActivityHistoryDay>,
    val watching: List<LibraryEntry>,
    val latestReview: MediaReview?,
    val titleLanguage: TitleLanguage,
    val viewerId: Int?,
    val onNavigateToTab: (ProfileTab) -> Unit,
    val onOpenFavorites: (ProfileFavoritesFilter) -> Unit,
    val onMediaClick: (Int) -> Unit,
    val onCharacterClick: (Int) -> Unit,
    val onStaffClick: (Int) -> Unit,
    val onUserClick: (String) -> Unit,
    val onActivityClick: (Int) -> Unit,
    val onLastReplyClick: (activityId: Int, replyId: Int) -> Unit,
    val onSubscribeClick: (Int) -> Unit,
    val onLikeActivity: ((Int) -> Unit)?,
    val onDeleteActivity: ((Int) -> Unit)?,
    val onEditActivity: ((Int) -> Unit)?,
    val onOpenWatching: () -> Unit,
    val onOpenReviews: () -> Unit,
    val onReviewClick: (MediaReview) -> Unit
) {
    val pinned = profile.activities.firstOrNull { it.isPinned }
    val updates = profile.activities.filter { it.type == ActivityType.MEDIA_LIST && !it.isPinned }.take(RECENT_UPDATES)
    val favouriteKinds = buildList {
        if (profile.favoriteAnime.isNotEmpty()) add(ProfileFavoritesFilter.ANIME)
        if (profile.favoriteMangaOverview.isNotEmpty()) add(ProfileFavoritesFilter.MANGA)
        if (profile.favoriteCharactersOverview.isNotEmpty()) add(ProfileFavoritesFilter.CHARACTERS)
        if (profile.favoriteStaffOverview.isNotEmpty()) add(ProfileFavoritesFilter.STAFF)
    }
    val isEmpty = profile.activities.isEmpty() && profile.animeCount == 0 && profile.mangaCount == 0 &&
        favouriteKinds.isEmpty()
}

/**
 * The Overview on one column. Each block is a list item of its own, so the first scroll composes
 * them as they come into view instead of the whole page at once.
 */
internal fun LazyListScope.profileOverviewBlocks(blocks: OverviewBlocks) {
    if (blocks.isEmpty) {
        item(key = "overview_empty", contentType = "overview_empty") {
            PlaceholderTabContent(message = stringResource(R.string.profile_no_recent_updates))
        }
        return
    }
    if (blocks.pinned != null) {
        item(key = "overview_pinned", contentType = "overview_pinned") { OverviewPinned(blocks) }
    }
    if (blocks.watching.isNotEmpty()) {
        item(key = "overview_watching", contentType = "overview_watching") { OverviewWatching(blocks) }
    }
    if (blocks.updates.isNotEmpty()) {
        item(key = "overview_updates", contentType = "overview_updates") { OverviewUpdates(blocks) }
    }
    if (blocks.favouriteKinds.isNotEmpty()) {
        item(key = "overview_favourites", contentType = "overview_header") { OverviewFavouritesHeader(blocks) }
        blocks.favouriteKinds.forEach { kind ->
            item(key = "overview_favourites_${kind.name}", contentType = "overview_favourites") {
                OverviewFavouriteKind(blocks, kind)
            }
        }
    }
    if (blocks.activityHistory.isNotEmpty()) {
        item(key = "overview_history", contentType = "overview_history") { OverviewHistory(blocks) }
    }
    if (blocks.latestReview != null) {
        item(key = "overview_review", contentType = "overview_review") { OverviewReview(blocks, blocks.latestReview) }
    }
    item(key = "overview_end", contentType = "overview_end") { Spacer(modifier = Modifier.height(16.dp)) }
}

/** The blocks T1 and T3 put in the first column of a wide pane, under whatever leads it. */
@Composable
internal fun OverviewColumnA(blocks: OverviewBlocks) {
    Column {
        if (blocks.updates.isNotEmpty()) OverviewUpdates(blocks)
        if (blocks.latestReview != null) OverviewReview(blocks, blocks.latestReview)
    }
}

/** The second column: the pinned post, what is being watched, favourites and the activity grid. */
@Composable
internal fun OverviewColumnB(blocks: OverviewBlocks) {
    Column {
        if (blocks.pinned != null) OverviewPinned(blocks)
        if (blocks.watching.isNotEmpty()) OverviewWatching(blocks)
        if (blocks.favouriteKinds.isNotEmpty()) {
            OverviewFavouritesHeader(blocks)
            blocks.favouriteKinds.forEach { OverviewFavouriteKind(blocks, it) }
        }
        if (blocks.activityHistory.isNotEmpty()) OverviewHistory(blocks)
    }
}

@Composable
private fun OverviewPinned(blocks: OverviewBlocks) {
    val pinned = blocks.pinned ?: return
    val canDelete = blocks.viewerId != null && pinned.userId == blocks.viewerId
    val canEdit = canDelete && pinned.type == ActivityType.TEXT
    ActivityCard(
        activity = pinned,
        onClick = { blocks.onActivityClick(pinned.id) },
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        onUserClick = blocks.onUserClick,
        onMediaClick = blocks.onMediaClick,
        onLastReplyClick = blocks.onLastReplyClick,
        onSubscribeClick = { blocks.onSubscribeClick(pinned.id) },
        onLikeClick = blocks.onLikeActivity?.let { { it(pinned.id) } },
        onDeleteClick = if (canDelete) blocks.onDeleteActivity?.let { { it(pinned.id) } } else null,
        onEditClick = if (canEdit) blocks.onEditActivity?.let { { it(pinned.id) } } else null,
        maxBodyLines = 10,
        showAuthor = true,
        pinnedLabel = true
    )
}

@Composable
private fun OverviewWatching(blocks: OverviewBlocks) {
    Column {
        OverviewHeader(
            title = stringResource(R.string.profile_overview_watching),
            subtitle = pluralStringResourceOrDefault(
                R.plurals.profile_favorites_count_titles,
                blocks.watching.size,
                blocks.watching.size.toString()
            ),
            onAction = blocks.onOpenWatching
        )
        WatchingRow(blocks.watching, blocks.titleLanguage, blocks.onMediaClick)
    }
}

@Composable
private fun OverviewUpdates(blocks: OverviewBlocks) {
    Column {
        OverviewHeader(
            title = stringResource(R.string.section_recent_updates),
            onAction = { blocks.onNavigateToTab(ProfileTab.ACTIVITY) }
        )
        RecentUpdatesCard(blocks.updates, blocks.onActivityClick, blocks.onMediaClick, blocks.onLikeActivity)
    }
}

@Composable
private fun OverviewFavouritesHeader(blocks: OverviewBlocks) {
    OverviewHeader(
        title = stringResource(R.string.section_favorites),
        onAction = { blocks.onOpenFavorites(blocks.favouriteKinds.first()) }
    )
}

@Composable
private fun OverviewFavouriteKind(blocks: OverviewBlocks, kind: ProfileFavoritesFilter) {
    val profile = blocks.profile
    Column {
        when (kind) {
            ProfileFavoritesFilter.ANIME -> {
                FavouriteKindLabel(stringResource(R.string.media_type_anime))
                FavouritePosterRow(profile.favoriteAnime.take(FAVOURITES_PER_KIND), blocks.titleLanguage, blocks.onMediaClick)
            }
            ProfileFavoritesFilter.MANGA -> {
                FavouriteKindLabel(stringResource(R.string.media_type_manga))
                FavouritePosterRow(profile.favoriteMangaOverview.take(FAVOURITES_PER_KIND), blocks.titleLanguage, blocks.onMediaClick)
            }
            ProfileFavoritesFilter.CHARACTERS -> {
                FavouriteKindLabel(stringResource(R.string.profile_cast_characters))
                FavouritePortraitRow(
                    items = profile.favoriteCharactersOverview.take(FAVOURITES_PER_KIND).map {
                        Triple(it.id, it.imageUrl, it.nameUserPreferred to it.series?.getTitle(blocks.titleLanguage))
                    },
                    onClick = blocks.onCharacterClick
                )
            }
            ProfileFavoritesFilter.STAFF -> {
                FavouriteKindLabel(stringResource(R.string.profile_cast_staff))
                FavouritePortraitRow(
                    items = profile.favoriteStaffOverview.take(FAVOURITES_PER_KIND).map {
                        Triple(it.id, it.imageUrl, it.nameUserPreferred to it.primaryOccupations.firstOrNull())
                    },
                    onClick = blocks.onStaffClick
                )
            }
            ProfileFavoritesFilter.STUDIOS -> Unit
        }
    }
}

@Composable
private fun OverviewHistory(blocks: OverviewBlocks) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        ActivityHistorySection(
            blocks.activityHistory,
            userId = blocks.profile.id,
            onMediaClick = blocks.onMediaClick,
            onActivityClick = blocks.onActivityClick
        )
    }
}

@Composable
private fun OverviewReview(blocks: OverviewBlocks, review: MediaReview) {
    Column {
        OverviewHeader(
            title = stringResource(R.string.profile_latest_review),
            onAction = blocks.onOpenReviews
        )
        ReviewCard(
            review = review,
            onClick = { blocks.onReviewClick(review) },
            modifier = Modifier.padding(horizontal = 16.dp),
            onUserClick = blocks.onUserClick
        )
    }
}
