package com.anisync.android.presentation.profile

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.anisync.android.R
import com.anisync.android.domain.UserProfile

@Stable
data class ProfileUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profile: UserProfile? = null,
    val errorMessage: String? = null,
    val selectedTab: ProfileTab = ProfileTab.OVERVIEW,
    val selectedActivityFilter: ProfileActivityFilter = ProfileActivityFilter.ALL,
    val selectedSocialTab: ProfileSocialTab = ProfileSocialTab.FOLLOWING,
    val isEditProfileDialogVisible: Boolean = false,
    val isBiographySheetVisible: Boolean = false
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

sealed interface ProfileAction {
    data object Refresh : ProfileAction
    data class UpdateAbout(val about: String) : ProfileAction
    data class SelectTab(val tab: ProfileTab) : ProfileAction
    data class SelectActivityFilter(val filter: ProfileActivityFilter) : ProfileAction
    data class SelectSocialTab(val tab: ProfileSocialTab) : ProfileAction
    data class SetEditProfileDialogVisible(val visible: Boolean) : ProfileAction
    data class SetBiographySheetVisible(val visible: Boolean) : ProfileAction
}
