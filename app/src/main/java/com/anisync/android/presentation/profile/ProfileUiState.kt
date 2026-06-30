package com.anisync.android.presentation.profile

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.anisync.android.R
import com.anisync.android.domain.ActivityHistoryDay
import com.anisync.android.domain.CountryStat
import com.anisync.android.domain.FormatStat
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.GenreStat
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.SocialThreadComment
import com.anisync.android.domain.SocialUser
import com.anisync.android.domain.StaffStat
import com.anisync.android.domain.StudioStat
import com.anisync.android.domain.TagStat
import com.anisync.android.domain.UserProfile
import com.anisync.android.domain.VoiceActorStat

@Stable
data class ProfileUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profile: UserProfile? = null,
    val errorMessage: String? = null,
    val isFollowingUser: Boolean = false,
    /** Whether the viewed user follows the authenticated viewer back (drives the Follows you / Mutual chip). */
    val isFollowerOfViewer: Boolean = false,
    val isFollowLoading: Boolean = false,
    val selectedTab: ProfileTab = ProfileTab.OVERVIEW,
    val selectedActivityFilter: ProfileActivityFilter = ProfileActivityFilter.ALL,
    val selectedFavoritesFilter: ProfileFavoritesFilter = ProfileFavoritesFilter.ANIME,
    val selectedAnimeStatus: LibraryStatus = LibraryStatus.CURRENT,
    val selectedMangaStatus: LibraryStatus = LibraryStatus.CURRENT,
    val selectedSocialTab: ProfileSocialTab = ProfileSocialTab.FOLLOWING,
    val selectedStatsType: ProfileStatsType = ProfileStatsType.ANIME,
    val isEditProfileDialogVisible: Boolean = false,
    val isBiographySheetVisible: Boolean = false,
    val userAnimeList: List<LibraryEntry> = emptyList(),
    val userAnimeListByStatus: Map<LibraryStatus, List<LibraryEntry>> = emptyMap(),
    val isUserAnimeListLoading: Boolean = false,
    val userMangaList: List<LibraryEntry> = emptyList(),
    val userMangaListByStatus: Map<LibraryStatus, List<LibraryEntry>> = emptyMap(),
    val isUserMangaListLoading: Boolean = false,
    val socialFollowing: List<SocialUser> = emptyList(),
    val socialFollowers: List<SocialUser> = emptyList(),
    val socialThreads: List<ForumThread> = emptyList(),
    val socialComments: List<SocialThreadComment> = emptyList(),
    val isSocialLoading: Boolean = false,
    val socialErrorMessage: String? = null,
    val followingHasNextPage: Boolean = false,
    val followersHasNextPage: Boolean = false,
    val threadsHasNextPage: Boolean = false,
    val commentsHasNextPage: Boolean = false,
    val isSocialPaginating: Boolean = false,
    val reviews: List<MediaReview> = emptyList(),
    val isReviewsLoading: Boolean = false,
    val reviewsErrorMessage: String? = null,
    val reviewsHasNextPage: Boolean = false,
    val isReviewsPaginating: Boolean = false,
    val selectedReview: MediaReview? = null,
    val isMessageComposerVisible: Boolean = false,
    val isSendingMessage: Boolean = false,
    val messageSendError: String? = null,
    val messageSentEvent: Long? = null,
    val statsData: StatisticsUiModel? = null,
    val isStatsLoading: Boolean = false,
    val statsErrorMessage: String? = null,
    val viewerId: Int? = null,
    val unreadNotificationCount: Int = 0,
    /** Whether older activities remain to be loaded into the Activity tab. */
    val activitiesHasNextPage: Boolean = false,
    /** True while an older activity page is being fetched (drives the inline spinner). */
    val isActivitiesPaginating: Boolean = false,
    /** Activity currently being edited via the inline compose sheet, or null if none. */
    val editingActivity: com.anisync.android.domain.UserActivity? = null,
    val isSavingActivityEdit: Boolean = false
)

data class StatisticsUiModel(
    val animeStats: AnimeStatisticsUi,
    val mangaStats: MangaStatisticsUi?,
    val activityHistory: List<ActivityHistoryDay> = emptyList()
)

data class AnimeStatisticsUi(
    val totalCount: Int,
    val daysWatched: Double,
    val meanScore: Double,
    val standardDeviation: Double,
    val episodesWatched: Int,
    val minutesWatched: Int,
    val statusDistribution: List<StatusUiModel>,
    val scoreDistribution: List<ScoreUiModel>,
    val genreDistribution: List<GenreStat>,
    val tagDistribution: List<TagStat>,
    val formatDistribution: List<FormatStat>,
    val releaseYearDistribution: List<YearUiModel>,
    val startYearDistribution: List<YearUiModel>,
    val lengthDistribution: List<LengthUiModel>,
    val studioDistribution: List<StudioStat>,
    val voiceActorDistribution: List<VoiceActorStat>,
    val staffDistribution: List<StaffStat>,
    val countryDistribution: List<CountryStat>
)

data class MangaStatisticsUi(
    val totalCount: Int,
    val chaptersRead: Int,
    val volumesRead: Int,
    val meanScore: Double,
    val standardDeviation: Double,
    val statusDistribution: List<StatusUiModel>,
    val scoreDistribution: List<ScoreUiModel>,
    val genreDistribution: List<GenreStat>,
    val tagDistribution: List<TagStat>,
    val formatDistribution: List<FormatStat>,
    val releaseYearDistribution: List<YearUiModel>,
    val startYearDistribution: List<YearUiModel>,
    val lengthDistribution: List<LengthUiModel>,
    val staffDistribution: List<StaffStat>,
    val countryDistribution: List<CountryStat>
)

data class ScoreUiModel(
    val score: Int,
    val label: String,
    val normalizedScore: Float,
    val count: Int,
    val heightFraction: Float
)

data class YearUiModel(
    val year: Int,
    val count: Int,
    val heightFraction: Float
)

data class StatusUiModel(
    val status: String,
    val count: Int,
    val fraction: Float,
    /** 0..4 ordinal mapped to color roles (primary/secondary/tertiary/error/outline). */
    val colorRoleIndex: Int
)

data class LengthUiModel(
    val label: String,
    val count: Int,
    val heightFraction: Float
)

@Immutable
enum class ProfileTab(@StringRes val titleRes: Int) {
    OVERVIEW(R.string.profile_tab_overview),
    ACTIVITY(R.string.profile_tab_activity),
    ANIME(R.string.media_type_anime),
    MANGA(R.string.media_type_manga),
    FAVORITES(R.string.section_favorites),
    SOCIAL(R.string.profile_tab_social),
    REVIEWS(R.string.section_reviews),
    STATS(R.string.statistics_title)
}

@Immutable
enum class ProfileFavoritesFilter(@StringRes val labelRes: Int) {
    ANIME(R.string.media_type_anime),
    MANGA(R.string.media_type_manga),
    CHARACTERS(R.string.profile_cast_characters),
    STAFF(R.string.profile_cast_staff),
    STUDIOS(R.string.profile_favorites_studios)
}

@Immutable
enum class ProfileActivityFilter(@StringRes val labelRes: Int) {
    ALL(R.string.profile_activity_all),
    STATUS(R.string.profile_activity_status),
    MESSAGES(R.string.profile_activity_messages),
    LISTS(R.string.profile_activity_lists)
}

@Immutable
enum class ProfileSocialTab(@StringRes val labelRes: Int) {
    FOLLOWING(R.string.profile_social_following),
    FOLLOWERS(R.string.profile_social_followers),
    FORUM_THREADS(R.string.profile_social_forum_threads),
    FORUM_COMMENTS(R.string.profile_social_forum_comments)
}

@Immutable
enum class ProfileStatsType(@StringRes val labelRes: Int) {
    ANIME(R.string.statistics_anime),
    MANGA(R.string.statistics_manga)
}

sealed interface ProfileAction {
    /**
     * [forceNetwork] = true is the user-pull behavior (hit network unconditionally).
     * [forceNetwork] = false serves Apollo's normalized cache when available and
     * is used by the screen's auto-refresh on entry so cold opens render instantly.
     */
    data class Refresh(val forceNetwork: Boolean = true) : ProfileAction
    data object ToggleFollow : ProfileAction
    data class UpdateAbout(val about: String) : ProfileAction
    data class SelectTab(val tab: ProfileTab) : ProfileAction
    data class SelectActivityFilter(val filter: ProfileActivityFilter) : ProfileAction
    data class SelectFavoritesFilter(val filter: ProfileFavoritesFilter) : ProfileAction
    data class SelectAnimeStatus(val status: LibraryStatus) : ProfileAction
    data class SelectMangaStatus(val status: LibraryStatus) : ProfileAction
    data class SelectSocialTab(val tab: ProfileSocialTab) : ProfileAction
    data class SelectStatsType(val type: ProfileStatsType) : ProfileAction
    data class SetEditProfileDialogVisible(val visible: Boolean) : ProfileAction
    data class SetBiographySheetVisible(val visible: Boolean) : ProfileAction
    data class SelectReview(val review: MediaReview?) : ProfileAction
    data class RateReview(
        val reviewId: Int,
        val rating: com.anisync.android.type.ReviewRating
    ) : ProfileAction
    data object ShowMessageComposer : ProfileAction
    data object HideMessageComposer : ProfileAction
    data class SendMessage(val text: String, val isPrivate: Boolean) : ProfileAction
    data object ConsumeMessageSentEvent : ProfileAction
    data object LoadMoreActivities : ProfileAction
    data object LoadMoreSocial : ProfileAction
    data object LoadMoreReviews : ProfileAction
    data class ToggleActivitySubscription(val activityId: Int) : ProfileAction
    data class ToggleActivityLike(val activityId: Int) : ProfileAction
    data class DeleteActivity(val activityId: Int) : ProfileAction
    data class EditActivity(val activityId: Int) : ProfileAction
    data object DismissActivityEdit : ProfileAction
    data class SubmitActivityEdit(val text: String) : ProfileAction
    data object ConsumeActivitySnackbar : ProfileAction
}
