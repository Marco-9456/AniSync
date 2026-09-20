package com.anisync.android.presentation.feed

import androidx.compose.runtime.Immutable
import com.anisync.android.domain.FeedFilter
import com.anisync.android.domain.FeedMediaType
import com.anisync.android.domain.FeedScope
import com.anisync.android.domain.UserActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class FeedUiState(
    val isLoading: Boolean = true,
    val isPaginating: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: ImmutableList<UserActivity> = persistentListOf(),
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val filter: FeedFilter = FeedFilter.ALL,
    val scope: FeedScope = FeedScope.GLOBAL,
    val mediaType: FeedMediaType = FeedMediaType.ANIME,
    val isAuthenticated: Boolean = true,
    val viewerId: Int? = null,
    /** Collapse a run of list updates from one person into one card (feed display choice). */
    val groupListUpdates: Boolean = true,
    /** The account's AniList `activityMergeTime`, shown on the feed menu. Null until options load. */
    val activityMergeMinutes: Int? = null,
    /** Activities a background refresh pulled in above what the reader has already seen. */
    val newActivityCount: Int = 0,
    /** Bumped when the Feed tab is reselected, asking the list to scroll back to the top. */
    val scrollToTopRequest: Long = 0L,
    val errorMessage: String? = null,
    /** HTTP-ish code behind [errorMessage]; 429 gets its own wording rather than the offline one. */
    val errorCode: Int? = null,
    val pendingLikeIds: ImmutableSet<Int> = persistentSetOf(),
    val pendingDeleteIds: ImmutableSet<Int> = persistentSetOf(),
    /**
     * Activity currently being edited via the inline compose sheet, or null if none.
     * Holds the full activity so we know whether it's TEXT or MESSAGE (for the right
     * mutation) and what to prefill the editor with.
     */
    val editingActivity: UserActivity? = null,
    val isSavingEdit: Boolean = false
)

sealed interface FeedAction {
    data object Refresh : FeedAction
    data object LoadMore : FeedAction
    data class OnFilterChange(val filter: FeedFilter) : FeedAction
    data class OnScopeChange(val scope: FeedScope) : FeedAction
    /** The Anime and Manga chips: list activity of one type, in one step. */
    data class OnListTypeChange(val mediaType: FeedMediaType) : FeedAction
    data object ToggleGroupListUpdates : FeedAction
    data object DismissNewActivity : FeedAction
    data class ToggleSubscribe(val activityId: Int) : FeedAction
    data class ToggleLike(val activityId: Int) : FeedAction
    data class DeleteActivity(val activityId: Int) : FeedAction
    data class EditActivity(val activityId: Int) : FeedAction
    data object DismissEdit : FeedAction
    data class SubmitEdit(val text: String) : FeedAction
}
