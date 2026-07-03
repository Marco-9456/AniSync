package com.anisync.android.presentation.profile

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.NotificationBadgeStore
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.AnimeStatistics
import com.anisync.android.domain.CachePolicy
import com.anisync.android.domain.GetProfileUseCase
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MangaStatistics
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.StatisticsRepository
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

private val LIBRARY_STATUS_DISPLAY_ORDER = arrayOf(
    LibraryStatus.CURRENT,
    LibraryStatus.REPEATING,
    LibraryStatus.PAUSED,
    LibraryStatus.COMPLETED,
    LibraryStatus.PLANNING,
    LibraryStatus.DROPPED
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val profileRepository: ProfileRepository,
    private val detailsRepository: com.anisync.android.domain.DetailsRepository,
    private val statisticsRepository: StatisticsRepository,
    private val activityRepository: ActivityRepository,
    private val accountManager: com.anisync.android.data.account.AccountManager,
    private val appSettings: AppSettings,
    private val notificationBadgeStore: NotificationBadgeStore,
    private val toastManager: ToastManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val LIST_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

        /** Minimum gap between non-user-initiated refreshes (init / ON_RESUME). */
        private const val AUTO_REFRESH_COOLDOWN_MS = 15_000L

        /** Minimum gap between user-initiated refreshes (pull-to-refresh). */
        private const val GESTURE_REFRESH_COOLDOWN_MS = 5_000L
    }

    /**
     * Per-resource cooldown. Independent from the 429 toast gate — that one only
     * activates *after* AniList already returned 429. This gate prevents the
     * runaway-pull spam path that gets us there in the first place. The 429 toast
     * still overrides (its `isRateLimited` flag short-circuits the UI gesture).
     */
    private class FetchCooldown {
        private var lastAt: Long = 0L
        fun shouldFetch(userInitiated: Boolean): Boolean {
            val floor = if (userInitiated) GESTURE_REFRESH_COOLDOWN_MS else AUTO_REFRESH_COOLDOWN_MS
            val now = SystemClock.elapsedRealtime()
            return (now - lastAt >= floor).also { ok -> if (ok) lastAt = now }
        }
    }

    /** Serializes refresh() invocations so a double-pull only fans out once. */
    private val refreshMutex = Mutex()

    /** Gates the orchestrator-level refresh(); per-tab fetches keep their own dedup. */
    private val profileCooldown = FetchCooldown()

    /**
     * Tick that re-fires the follow-state collector on every pull-to-refresh so
     * the user sees fresh follow state without scheduling a parallel job inside
     * refresh() (which would double-fire follow state on cold open).
     */
    private val followStateTick = MutableStateFlow(0L)

    // Settings from AppSettings (needed for display in profile)
    val titleLanguage: StateFlow<com.anisync.android.data.TitleLanguage> = appSettings.titleLanguage

    // Multi-account quick-switch (own-profile header).
    val accounts: StateFlow<List<com.anisync.android.data.account.Account>> = accountManager.accounts
    val activeAccountId: StateFlow<Int?> = accountManager.activeAccount
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.Eagerly, accountManager.activeAccount.value?.id)

    /**
     * Switches the active account. The keyed MainScreen subtree rebuilds on the resulting session
     * epoch bump, so no explicit screen reload is needed here.
     */
    fun switchAccount(id: Int) {
        accountManager.switch(id)
    }

    private data class ProfileUiLocalState(
        val selectedTab: ProfileTab = ProfileTab.OVERVIEW,
        val selectedActivityFilter: ProfileActivityFilter = ProfileActivityFilter.ALL,
        val selectedFavoritesFilter: ProfileFavoritesFilter = ProfileFavoritesFilter.ANIME,
        val selectedAnimeStatus: LibraryStatus = LibraryStatus.CURRENT,
        val selectedMangaStatus: LibraryStatus = LibraryStatus.CURRENT,
        val selectedSocialTab: ProfileSocialTab = ProfileSocialTab.FOLLOWING,
        val selectedStatsType: ProfileStatsType = ProfileStatsType.ANIME,
        val isEditProfileDialogVisible: Boolean = false,
        val isBiographySheetVisible: Boolean = false,
        val selectedReview: com.anisync.android.domain.MediaReview? = null,
        val isMessageComposerVisible: Boolean = false,
        val isSendingMessage: Boolean = false,
        val messageSendError: String? = null,
        val messageSentEvent: Long? = null,
        val isRefreshing: Boolean = false,
        val isFollowingUser: Boolean = false,
        val isFollowerOfViewer: Boolean = false,
        val isFollowLoading: Boolean = false,
        val editingActivity: com.anisync.android.domain.UserActivity? = null,
        val isSavingActivityEdit: Boolean = false
    )

    private val localState = MutableStateFlow(ProfileUiLocalState())

    private data class SocialState(
        val isSocialLoading: Boolean = false,
        val socialFollowing: List<com.anisync.android.domain.SocialUser> = emptyList(),
        val socialFollowers: List<com.anisync.android.domain.SocialUser> = emptyList(),
        val socialThreads: List<com.anisync.android.domain.ForumThread> = emptyList(),
        val socialComments: List<com.anisync.android.domain.SocialThreadComment> = emptyList(),
        val socialErrorMessage: String? = null,
        val hasFetchedSocialData: Boolean = false,
        val socialPage: Int = 1,
        val followingHasNextPage: Boolean = false,
        val followersHasNextPage: Boolean = false,
        val threadsHasNextPage: Boolean = false,
        val commentsHasNextPage: Boolean = false,
        val isSocialPaginating: Boolean = false
    )

    private val socialState = MutableStateFlow(SocialState())

    private data class ReviewsState(
        val isReviewsLoading: Boolean = false,
        val reviews: List<com.anisync.android.domain.MediaReview> = emptyList(),
        val reviewsErrorMessage: String? = null,
        val hasFetchedReviews: Boolean = false,
        val reviewsPage: Int = 1,
        val reviewsHasNextPage: Boolean = false,
        val isReviewsPaginating: Boolean = false
    )

    private val reviewsState = MutableStateFlow(ReviewsState())

    private data class StatsState(
        val isStatsLoading: Boolean = false,
        val statsData: StatisticsUiModel? = null,
        val statsErrorMessage: String? = null,
        val hasFetchedStats: Boolean = false
    )

    private val statsState = MutableStateFlow(StatsState())

    private data class MediaListState(
        val userAnimeList: List<LibraryEntry> = emptyList(),
        val userAnimeListByStatus: Map<LibraryStatus, List<LibraryEntry>> = emptyMap(),
        val isUserAnimeListLoading: Boolean = false,
        val hasFetchedAnimeList: Boolean = false,
        val lastAnimeListFetchAtMs: Long = 0L,
        val userMangaList: List<LibraryEntry> = emptyList(),
        val userMangaListByStatus: Map<LibraryStatus, List<LibraryEntry>> = emptyMap(),
        val isUserMangaListLoading: Boolean = false,
        val hasFetchedMangaList: Boolean = false,
        val lastMangaListFetchAtMs: Long = 0L
    )

    private val mediaListState = MutableStateFlow(MediaListState())

    /**
     * Full favourite-anime list, loaded lazily when the Favorites tab opens. Null
     * until first fetch — the UI falls back to the page-1 preview the profile
     * itself carries. Keeping it out of the profile until populated means a normal
     * profile load/refresh never fans out across favourite pages.
     */
    private data class FavoritesState(
        // The favourite-anime list is paginated, so it's fetched lazily (all pages) when the tab
        // opens; the profile fetch only carries page 1. Manga/characters/staff show their page-1
        // overview directly (objective media fields only — no viewer-list enrichment).
        val fullAnime: List<LibraryEntry>? = null,
        val isAnimeLoading: Boolean = false,
        val animeFetched: Boolean = false
    )

    private val favoritesState = MutableStateFlow(FavoritesState())

    /**
     * Older activity pages loaded on demand as the user scrolls the Activity tab past
     * the initial batch the profile fetch carries. Starts empty with [hasNextPage] true
     * (optimistic — the profile fetch returns no activity pageInfo, so the first
     * load-more authoritatively sets it). [loadMoreActivities] appends pages; the
     * uiState combine merges them into the profile feed (deduped, newest-first).
     */
    private data class ActivityPaginationState(
        val appended: List<com.anisync.android.domain.UserActivity> = emptyList(),
        val page: Int = 1,
        val hasNextPage: Boolean = true,
        val isPaginating: Boolean = false,
        // Which profile filter this stream is paging. Pagination is server-side per type, so a
        // filter change re-seeds a fresh stream (see [seedActivityPagination]).
        val filter: ProfileActivityFilter = ProfileActivityFilter.ALL
    )

    private val activityPaginationState = MutableStateFlow(ActivityPaginationState())

    // Overlay for optimistic activity subscription toggles: activityId -> isSubscribed
    private val activitySubscriptionOverrides = MutableStateFlow<Map<Int, Boolean>>(emptyMap())

    // Overlay for optimistic activity like toggles: activityId -> (isLiked, likeCount).
    private val activityLikeOverrides =
        MutableStateFlow<Map<Int, Pair<Boolean, Int>>>(emptyMap())

    // Coalesces rapid like taps into a single network toggle. Each tap flips the
    // optimistic override instantly; only the settled state is sent, so a
    // like→unlike→like burst hits the network at most once (or not at all).
    private val activityLikeCoalescer =
        com.anisync.android.presentation.util.MutationCoalescer<Int, Boolean>(viewModelScope) { activityId, _ ->
            when (val result = activityRepository.toggleActivityLike(activityId)) {
                is Result.Success -> {
                    val server = result.data
                    activityLikeOverrides.update { it + (activityId to (server.isLiked to server.likeCount)) }
                    true
                }
                is Result.Error -> {
                    activityLikeOverrides.update { it - activityId }
                    toastManager.showResultError(result)
                    false
                }
            }
        }

    // Optimistically-deleted activity ids hidden from the UI until confirmed.
    private val deletedActivityIds = MutableStateFlow<Set<Int>>(emptySet())

    private val viewerIdFlow = MutableStateFlow<Int?>(null)

    private val targetUsername: String? = savedStateHandle.get<String>("username")
        ?.let(Uri::decode)
        ?.trim()
        ?.removePrefix("@")
        ?.takeIf { it.isNotBlank() }
    // Payload `forceNetwork` controls Apollo fetch policy for the target-profile
    // path (other users). `false` is used by the cold-open auto-refresh so the
    // normalized cache renders instantly; `true` is used by pull-to-refresh.
    private val targetRefreshSignal = MutableSharedFlow<Boolean>(replay = 0, extraBufferCapacity = 1)

    /**
     * Last successfully-loaded target profile. Lets a refresh (e.g. the ON_RESUME
     * re-fetch) skip the Loading skeleton, so the content stays mounted and the
     * scroll position / selected tab survive re-entry instead of being reset.
     */
    private var lastLoadedTargetProfile: com.anisync.android.domain.UserProfile? = null

    private val profileState = if (targetUsername.isNullOrBlank()) {
        getProfileUseCase()
            .map { profileResult ->
                if (profileResult != null) {
                    ProfileUiState(
                        isLoading = false,
                        profile = profileResult
                    )
                } else {
                    ProfileUiState(isLoading = true)
                }
            }
            // Keep the cached account name/avatar (account switcher + AniList settings) in sync with
            // the freshly-loaded own profile, so a picture changed on AniList shows up here too.
            .onEach { state ->
                state.profile?.let { accountManager.updateActiveDetails(it.name, it.avatarUrl) }
            }
            .onStart { emit(ProfileUiState(isLoading = true)) }
            .catch { e -> emit(ProfileUiState(isLoading = false, errorMessage = e.message ?: "Unknown error")) }
    } else {
        targetRefreshSignal
            .onStart { emit(false) }
            .flatMapLatest { forceNetwork ->
                kotlinx.coroutines.flow.flow {
                    // Skeleton only on the first load. On a refresh keep the loaded
                    // profile on screen so ProfileContent stays mounted and scroll /
                    // tab state are preserved (don't flash a Loading state on enter).
                    if (lastLoadedTargetProfile == null) {
                        emit(ProfileUiState(isLoading = true))
                    }
                    when (val result = profileRepository.fetchUserProfile(targetUsername, forceNetwork)) {
                        is Result.Success -> {
                            lastLoadedTargetProfile = result.data
                            emit(ProfileUiState(isLoading = false, profile = result.data))
                        }
                        is Result.Error -> {
                            val cached = lastLoadedTargetProfile
                            if (cached != null) {
                                // Refresh failed but the profile is already on screen — keep it.
                                emit(ProfileUiState(isLoading = false, profile = cached))
                            } else {
                                val message = if (
                                    result.message.contains("not found", ignoreCase = true)
                                ) {
                                    "Could not load @$targetUsername right now. The account may be private, temporarily unavailable, or rate-limited."
                                } else {
                                    result.message
                                }
                                emit(ProfileUiState(isLoading = false, errorMessage = message))
                            }
                        }
                    }
                }
            }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        profileState,
        localState,
        socialState,
        reviewsState,
        statsState,
        mediaListState,
        activitySubscriptionOverrides,
        activityLikeOverrides,
        deletedActivityIds,
        viewerIdFlow,
        notificationBadgeStore.unreadCount,
        favoritesState,
        activityPaginationState
    ) { params ->
        val remote = params[0] as ProfileUiState
        val local = params[1] as ProfileUiLocalState
        val social = params[2] as SocialState
        val reviews = params[3] as ReviewsState
        val stats = params[4] as StatsState
        val mediaLists = params[5] as MediaListState
        @Suppress("UNCHECKED_CAST")
        val subOverrides = params[6] as Map<Int, Boolean>
        @Suppress("UNCHECKED_CAST")
        val likeOverrides = params[7] as Map<Int, Pair<Boolean, Int>>
        @Suppress("UNCHECKED_CAST")
        val deletedIds = params[8] as Set<Int>
        val viewerId = params[9] as Int?
        val unreadCount = params[10] as Int
        val favoritesOverlay = params[11] as FavoritesState
        val pagination = params[12] as ActivityPaginationState

        // Fold lazily-loaded older activity pages into the profile's initial feed
        // (deduped, newest-first) before overlays apply, so a like/subscribe/delete
        // also patches an appended item. No-op until the user paginates.
        val mergedRemote = if (remote.profile != null && pagination.appended.isNotEmpty()) {
            val p = remote.profile
            val merged = (p.activities + pagination.appended)
                .distinctBy { it.id }
                .sortedWith(
                    compareByDescending<com.anisync.android.domain.UserActivity> { it.isPinned }
                        .thenByDescending { it.timestamp }
                )
            remote.copy(profile = p.copy(activities = merged))
        } else {
            remote
        }

        val needsOverlay = mergedRemote.profile != null &&
            (subOverrides.isNotEmpty() || likeOverrides.isNotEmpty() || deletedIds.isNotEmpty())
        val remoteWithOverrides = if (!needsOverlay) {
            mergedRemote
        } else {
            val profile = mergedRemote.profile!!
            val patched = profile.activities
                .asSequence()
                .filterNot { deletedIds.contains(it.id) }
                .map { a ->
                    var next = a
                    subOverrides[a.id]?.let { sub -> next = next.copy(isSubscribed = sub) }
                    likeOverrides[a.id]?.let { (liked, count) ->
                        next = next.copy(isLiked = liked, likeCount = count)
                    }
                    next
                }
                .toList()
            mergedRemote.copy(profile = profile.copy(activities = patched))
        }

        // Swap in the lazily-loaded full favourite-anime list once available; until then the page-1
        // preview carried by the profile fetch stands.
        val withFavorites = favoritesOverlay.fullAnime?.let { full ->
            remoteWithOverrides.profile?.let { p ->
                remoteWithOverrides.copy(profile = p.copy(favoriteAnime = full))
            }
        } ?: remoteWithOverrides

        withFavorites.copy(
            isRefreshing = local.isRefreshing,
            isFollowingUser = local.isFollowingUser,
            isFollowerOfViewer = local.isFollowerOfViewer,
            isFollowLoading = local.isFollowLoading,
            selectedTab = local.selectedTab,
            selectedActivityFilter = local.selectedActivityFilter,
            selectedFavoritesFilter = local.selectedFavoritesFilter,
            selectedAnimeStatus = local.selectedAnimeStatus,
            selectedMangaStatus = local.selectedMangaStatus,
            selectedSocialTab = local.selectedSocialTab,
            selectedStatsType = local.selectedStatsType,
            isEditProfileDialogVisible = local.isEditProfileDialogVisible,
            isBiographySheetVisible = local.isBiographySheetVisible,
            selectedReview = local.selectedReview,
            isMessageComposerVisible = local.isMessageComposerVisible,
            isSendingMessage = local.isSendingMessage,
            messageSendError = local.messageSendError,
            messageSentEvent = local.messageSentEvent,
            editingActivity = local.editingActivity,
            isSavingActivityEdit = local.isSavingActivityEdit,
            socialFollowing = social.socialFollowing,
            socialFollowers = social.socialFollowers,
            socialThreads = social.socialThreads,
            socialComments = social.socialComments,
            isSocialLoading = social.isSocialLoading,
            socialErrorMessage = social.socialErrorMessage,
            followingHasNextPage = social.followingHasNextPage,
            followersHasNextPage = social.followersHasNextPage,
            threadsHasNextPage = social.threadsHasNextPage,
            commentsHasNextPage = social.commentsHasNextPage,
            isSocialPaginating = social.isSocialPaginating,
            reviews = reviews.reviews,
            isReviewsLoading = reviews.isReviewsLoading,
            reviewsErrorMessage = reviews.reviewsErrorMessage,
            reviewsHasNextPage = reviews.reviewsHasNextPage,
            isReviewsPaginating = reviews.isReviewsPaginating,
            statsData = stats.statsData,
            isStatsLoading = stats.isStatsLoading,
            statsErrorMessage = stats.statsErrorMessage,
            userAnimeList = mediaLists.userAnimeList,
            userAnimeListByStatus = mediaLists.userAnimeListByStatus,
            isUserAnimeListLoading = mediaLists.isUserAnimeListLoading,
            userMangaList = mediaLists.userMangaList,
            userMangaListByStatus = mediaLists.userMangaListByStatus,
            isUserMangaListLoading = mediaLists.isUserMangaListLoading,
            viewerId = viewerId,
            unreadNotificationCount = unreadCount,
            activitiesHasNextPage = pagination.hasNextPage,
            isActivitiesPaginating = pagination.isPaginating
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ProfileUiState(isLoading = true)
    )

    init {
        observeTargetProfileForFollowState()

        viewModelScope.launch {
            viewerIdFlow.value = activityRepository.getViewerId()
        }

        // Stale-while-revalidate: the cached profile (Apollo normalized cache for a
        // visited user, Room for the own profile) paints instantly via the cache-first
        // initial load, then this deferred pass forces the network so the activity feed
        // and stats reflect reality — without the user having to pull-to-refresh.
        // Previously this revalidation was also cache-first, so a revisited profile kept
        // showing the last visit's snapshot (looked inactive) until a manual refresh.
        // The deferred timing keeps the first frame instant; the cooldown gate prevents
        // back-to-back VM constructions (rapid screen back-then-in) from re-firing it.
        viewModelScope.launch {
            delay(500)
            if (profileCooldown.shouldFetch(userInitiated = false)) {
                refresh(forceNetwork = true)
            }
        }
    }

    /** Pulls the current `Viewer.unreadNotificationCount`; called on screen resume. */
    fun refreshNotificationBadge() {
        if (!targetUsername.isNullOrBlank()) return
        viewModelScope.launch { notificationBadgeStore.refresh() }
    }

    /**
     * Optimistically zero the badge when the user taps the bell. The
     * server-side reset rides on NotificationsScreen's first ALL fetch
     * (`resetNotificationCount=true`); the next on-resume refresh
     * reconciles either way.
     */
    fun onNotificationsOpened() {
        notificationBadgeStore.clearOptimistically()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.Refresh -> refresh(forceNetwork = action.forceNetwork)
            is ProfileAction.ToggleFollow -> toggleFollow()
            is ProfileAction.UpdateAbout -> updateAbout(action.about)
            is ProfileAction.SelectTab -> {
                localState.update {
                    it.copy(selectedTab = action.tab)
                }
                if (action.tab == ProfileTab.SOCIAL && !socialState.value.hasFetchedSocialData) {
                    fetchSocialData()
                } else if (action.tab == ProfileTab.REVIEWS && !reviewsState.value.hasFetchedReviews) {
                    fetchReviews()
                } else if ((action.tab == ProfileTab.STATS || action.tab == ProfileTab.OVERVIEW) &&
                    !statsState.value.hasFetchedStats
                ) {
                    // Overview shows the activity heatmap, which rides on the stats payload, so
                    // it warms the same cache the Stats tab reuses.
                    fetchStats()
                } else if (action.tab == ProfileTab.FAVORITES && !favoritesState.value.animeFetched) {
                    fetchFullFavoriteAnime()
                } else if (action.tab == ProfileTab.ANIME &&
                    shouldRefreshAnimeList() &&
                    !mediaListState.value.isUserAnimeListLoading
                ) {
                    fetchUserAnimeList(forceRefresh = true)
                } else if (action.tab == ProfileTab.MANGA &&
                    shouldRefreshMangaList() &&
                    !mediaListState.value.isUserMangaListLoading
                ) {
                    fetchUserMangaList(forceRefresh = true)
                }
            }

            is ProfileAction.SelectStatsType -> {
                localState.update {
                    it.copy(selectedStatsType = action.type)
                }
            }

            is ProfileAction.SelectActivityFilter -> {
                if (localState.value.selectedActivityFilter != action.filter) {
                    localState.update {
                        it.copy(selectedActivityFilter = action.filter)
                    }
                    // Server-side per-type pagination: a new filter is a new stream, so drop
                    // the previous filter's appended pages and re-seed. Otherwise its
                    // hasNextPage/page counter would carry over and the footer loader would
                    // page the wrong (or an exhausted) stream.
                    seedActivityPagination(action.filter)
                }
            }

            is ProfileAction.SelectFavoritesFilter -> {
                localState.update {
                    it.copy(selectedFavoritesFilter = action.filter)
                }
            }

            is ProfileAction.SelectAnimeStatus -> {
                localState.update {
                    it.copy(selectedAnimeStatus = action.status)
                }
            }

            is ProfileAction.SelectMangaStatus -> {
                localState.update {
                    it.copy(selectedMangaStatus = action.status)
                }
            }

            is ProfileAction.SelectSocialTab -> {
                localState.update {
                    it.copy(selectedSocialTab = action.tab)
                }
            }

            is ProfileAction.SetEditProfileDialogVisible -> {
                localState.update {
                    it.copy(isEditProfileDialogVisible = action.visible)
                }
            }

            is ProfileAction.SetBiographySheetVisible -> {
                localState.update {
                    it.copy(isBiographySheetVisible = action.visible)
                }
            }

            is ProfileAction.SelectReview -> {
                localState.update {
                    it.copy(selectedReview = action.review)
                }
            }

            is ProfileAction.RateReview -> rateReview(action.reviewId, action.rating)

            is ProfileAction.ShowMessageComposer -> {
                localState.update {
                    it.copy(
                        isMessageComposerVisible = true,
                        messageSendError = null
                    )
                }
            }

            is ProfileAction.HideMessageComposer -> {
                localState.update {
                    it.copy(
                        isMessageComposerVisible = false,
                        messageSendError = null
                    )
                }
            }

            is ProfileAction.SendMessage -> sendMessage(action.text, action.isPrivate)

            is ProfileAction.ConsumeMessageSentEvent -> {
                localState.update { it.copy(messageSentEvent = null) }
            }

            is ProfileAction.LoadMoreActivities -> loadMoreActivities()
            is ProfileAction.LoadMoreSocial -> loadMoreSocial()
            is ProfileAction.LoadMoreReviews -> loadMoreReviews()
            is ProfileAction.ToggleActivitySubscription -> toggleActivitySubscription(action.activityId)
            is ProfileAction.ToggleActivityLike -> toggleActivityLike(action.activityId)
            is ProfileAction.DeleteActivity -> deleteActivity(action.activityId)
            is ProfileAction.EditActivity -> openActivityEdit(action.activityId)
            is ProfileAction.DismissActivityEdit -> {
                if (!localState.value.isSavingActivityEdit) {
                    localState.update { it.copy(editingActivity = null) }
                }
            }
            is ProfileAction.SubmitActivityEdit -> submitActivityEdit(action.text)
            is ProfileAction.ConsumeActivitySnackbar -> Unit
        }
    }

    private fun openActivityEdit(activityId: Int) {
        val target = uiState.value.profile?.activities?.firstOrNull { it.id == activityId } ?: return
        localState.update { it.copy(editingActivity = target) }
    }

    private fun submitActivityEdit(text: String) {
        val target = localState.value.editingActivity ?: return
        if (localState.value.isSavingActivityEdit) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        localState.update { it.copy(isSavingActivityEdit = true) }
        viewModelScope.launch {
            val result = when (target.type) {
                com.anisync.android.domain.ActivityType.TEXT ->
                    activityRepository.saveTextActivity(trimmed, id = target.id)
                com.anisync.android.domain.ActivityType.MESSAGE -> {
                    val recipientId = target.recipientId
                    if (recipientId == null) {
                        Result.Error("Missing recipient")
                    } else {
                        activityRepository.saveMessageActivity(
                            id = target.id,
                            recipientId = recipientId,
                            message = trimmed,
                            isPrivate = target.isPrivate
                        )
                    }
                }
                else -> Result.Error("Cannot edit this activity type")
            }
            when (result) {
                is Result.Success -> {
                    toastManager.showToast(ToastType.SUCCESS, message = "Activity updated")
                    localState.update {
                        it.copy(isSavingActivityEdit = false, editingActivity = null)
                    }
                    refresh()
                }
                is Result.Error -> {
                    localState.update { it.copy(isSavingActivityEdit = false) }
                    toastManager.showResultError(result)
                }
            }
        }
    }

    private fun toggleActivityLike(activityId: Int) {
        val current = uiState.value.profile?.activities?.firstOrNull { it.id == activityId } ?: return
        val wasLiked = current.isLiked
        // Baseline = server-side liked state (recorded once); each tap flips the
        // optimistic override and the coalescer sends only the settled result.
        activityLikeCoalescer.seed(activityId, wasLiked)
        val nextLiked = !wasLiked
        val nextCount = (current.likeCount + if (wasLiked) -1 else 1).coerceAtLeast(0)
        activityLikeOverrides.update { it + (activityId to (nextLiked to nextCount)) }
        activityLikeCoalescer.submit(activityId, nextLiked)
    }

    private fun deleteActivity(activityId: Int) {
        if (deletedActivityIds.value.contains(activityId)) return
        deletedActivityIds.update { it + activityId }

        viewModelScope.launch {
            when (val result = activityRepository.deleteActivity(activityId)) {
                is Result.Success -> {
                    toastManager.showToast(ToastType.SUCCESS, message = "Activity deleted")
                }
                is Result.Error -> {
                    deletedActivityIds.update { it - activityId }
                    toastManager.showResultError(result)
                }
            }
        }
    }

    private fun toggleActivitySubscription(activityId: Int) {
        val current = uiState.value.profile?.activities?.firstOrNull { it.id == activityId } ?: return
        val next = !current.isSubscribed
        activitySubscriptionOverrides.update { it + (activityId to next) }
        viewModelScope.launch {
            when (activityRepository.toggleSubscription(activityId, next)) {
                is Result.Success -> Unit
                is Result.Error -> {
                    activitySubscriptionOverrides.update { it + (activityId to current.isSubscribed) }
                }
            }
        }
    }

    private fun sendMessage(text: String, isPrivate: Boolean) {
        val recipientId = uiState.value.profile?.id ?: return
        viewModelScope.launch {
            localState.update {
                it.copy(isSendingMessage = true, messageSendError = null)
            }
            when (val result = profileRepository.sendMessageActivity(
                recipientId = recipientId,
                message = text,
                isPrivate = isPrivate
            )) {
                is Result.Success -> {
                    localState.update {
                        it.copy(
                            isSendingMessage = false,
                            isMessageComposerVisible = false,
                            messageSendError = null,
                            messageSentEvent = System.currentTimeMillis()
                        )
                    }
                    toastManager.showToast(ToastType.SUCCESS, message = "Message sent")
                }
                is Result.Error -> {
                    localState.update {
                        it.copy(
                            isSendingMessage = false,
                            messageSendError = result.message
                        )
                    }
                }
            }
        }
    }

    private fun refresh(forceNetwork: Boolean = true) {
        // Synchronous early-return so a second pull-to-refresh while the first
        // is still in flight is dropped, not stacked. The 429 toast gate only
        // fires AFTER a rate limit happens; this prevents the burst from
        // queuing in the first place.
        if (!refreshMutex.tryLock()) return
        localState.update { it.copy(isRefreshing = true) }
        // A refresh replaces the activity seed with a fresh page 1, so drop any
        // lazily-loaded older pages — they'd misalign or duplicate atop the new seed.
        // Keep the active filter so pagination keeps paging that type's stream.
        seedActivityPagination(localState.value.selectedActivityFilter)

        viewModelScope.launch {
            Trace.beginSection("AniSync.Profile.Refresh.Total")
            val totalStart = SystemClock.elapsedRealtime()
            val selectedTab = localState.value.selectedTab

            var profileTimings: com.anisync.android.domain.ProfileRefreshTimings? = null
            var refreshJobMs = 0L

            try {
                val refreshJob = launch {
                    Trace.beginSection("AniSync.Profile.Refresh.RefreshJob")
                    val s = SystemClock.elapsedRealtime()
                    try {
                        if (targetUsername.isNullOrBlank()) {
                            when (val r = profileRepository.refreshProfileTimed("", forceNetwork = forceNetwork)) {
                                is Result.Success -> profileTimings = r.data
                                is Result.Error -> Unit
                            }
                        } else {
                            targetRefreshSignal.tryEmit(forceNetwork)
                        }
                    } finally {
                        refreshJobMs = SystemClock.elapsedRealtime() - s
                        Trace.endSection()
                    }
                }

                var activeTabMs = 0L
                val activeTabJob = launch {
                    Trace.beginSection("AniSync.Profile.Refresh.ActiveTabJob.$selectedTab")
                    val s = SystemClock.elapsedRealtime()
                    try {
                        when (selectedTab) {
                            ProfileTab.SOCIAL -> fetchSocialData(forceRefresh = true)
                            ProfileTab.REVIEWS -> fetchReviews(forceRefresh = true)
                            ProfileTab.STATS -> fetchStats(forceRefresh = true)
                            // The heatmap rides the stats payload. An explicit pull forces the
                            // network (like the other tabs); a soft refresh stays network-first
                            // with a cache fallback so the heatmap still revalidates (#88).
                            ProfileTab.OVERVIEW -> fetchStats(forceRefresh = forceNetwork)
                            ProfileTab.FAVORITES -> fetchFullFavoriteAnime(forceRefresh = true)
                            ProfileTab.ANIME -> fetchUserAnimeList(forceRefresh = shouldRefreshAnimeList())
                            ProfileTab.MANGA -> fetchUserMangaList(forceRefresh = shouldRefreshMangaList())
                            else -> Unit
                        }
                    } finally {
                        activeTabMs = SystemClock.elapsedRealtime() - s
                        Trace.endSection()
                    }
                }

                // Follow-state is fired via the tick instead of an inline job — the
                // observer combine() below picks up the tick and runs fetchFollowState.
                // Only tick on user-initiated (forceNetwork=true) so cold-open doesn't
                // double-fire follow-state alongside the observer's initial emission.
                if (!targetUsername.isNullOrBlank() && forceNetwork) {
                    followStateTick.value = SystemClock.elapsedRealtime()
                }

                refreshJob.join()
                activeTabJob.join()

                val totalMs = SystemClock.elapsedRealtime() - totalStart
                Log.i(
                    "AniSyncPerf",
                    "profile.refresh own=${targetUsername.isNullOrBlank()} tab=$selectedTab " +
                        "total=${totalMs}ms refreshJob=${refreshJobMs}ms " +
                        "profileQuery=${profileTimings?.profileQueryMs ?: -1}ms " +
                        "activities=${profileTimings?.activitiesQueryMs ?: -1}ms " +
                        "favoritesTotal=${profileTimings?.favoritesTotalMs ?: -1}ms " +
                        "favoritesFirstPage=${profileTimings?.favoritesFirstPageMs ?: -1}ms " +
                        "favoritesRest=${profileTimings?.favoritesRestMs ?: -1}ms " +
                        "favoritesPages=${profileTimings?.favoritesPageCount ?: -1} " +
                        "activeTab=${activeTabMs}ms followState=async " +
                        "forceNetwork=$forceNetwork"
                )
            } finally {
                localState.update { it.copy(isRefreshing = false) }
                Trace.endSection()
                refreshMutex.unlock()
            }
        }
    }

    private fun observeTargetProfileForFollowState() {
        if (targetUsername.isNullOrBlank()) return

        viewModelScope.launch {
            // Combine: re-emits when EITHER the profile id changes OR the
            // followStateTick fires. Initial emission uses the default (cache-friendly)
            // policy; pull-to-refresh ticks force a fresh network query.
            combine(
                uiState.map { it.profile?.id }.filterNotNull().distinctUntilChanged(),
                followStateTick
            ) { id, tick -> id to tick }
                .collect { (userId, tick) ->
                    fetchFollowState(
                        userId = userId,
                        // First emission (tick==0L) is cold-open: prefer cache fallback;
                        // subsequent ticks are user-initiated refresh: force network.
                        policy = if (tick == 0L) CachePolicy.NetworkFirst else CachePolicy.NetworkOnly
                    )
                }
        }
    }

    private fun fetchFollowState(userId: Int, policy: CachePolicy = CachePolicy.NetworkFirst) {
        viewModelScope.launch {
            localState.update { it.copy(isFollowLoading = true) }
            when (val result = profileRepository.getFollowState(userId, policy)) {
                is Result.Success -> {
                    localState.update {
                        it.copy(
                            isFollowingUser = result.data.isFollowing,
                            isFollowerOfViewer = result.data.isFollower,
                            isFollowLoading = false
                        )
                    }
                }

                is Result.Error -> {
                    localState.update { it.copy(isFollowLoading = false) }
                }
            }
        }
    }

    private fun toggleFollow() {
        if (targetUsername.isNullOrBlank()) return

        viewModelScope.launch {
            val userId = uiState.value.profile?.id ?: return@launch
            localState.update { it.copy(isFollowLoading = true) }
            when (val result = profileRepository.toggleFollow(userId)) {
                is Result.Success -> {
                    localState.update {
                        it.copy(
                            isFollowingUser = result.data,
                            isFollowLoading = false
                        )
                    }
                }

                is Result.Error -> {
                    localState.update { it.copy(isFollowLoading = false) }
                }
            }
        }
    }

    private fun fetchReviews(forceRefresh: Boolean = false) {
        if (reviewsState.value.isReviewsLoading) return
        if (!forceRefresh && reviewsState.value.hasFetchedReviews) return

        viewModelScope.launch {
            reviewsState.update { it.copy(isReviewsLoading = true, reviewsErrorMessage = null) }
            val userId = uiState.value.profile?.id
            if (userId == null) {
                reviewsState.update { it.copy(isReviewsLoading = false) }
                return@launch
            }

            val policy = if (forceRefresh) CachePolicy.NetworkOnly else CachePolicy.NetworkFirst
            when (val result = profileRepository.getUserReviews(userId, page = 1, policy = policy)) {
                is Result.Success -> {
                    reviewsState.update {
                        it.copy(
                            isReviewsLoading = false,
                            reviews = result.data.reviews,
                            hasFetchedReviews = true,
                            reviewsPage = 1,
                            reviewsHasNextPage = result.data.hasNextPage
                        )
                    }
                }
                is Result.Error -> {
                    reviewsState.update {
                        it.copy(
                            isReviewsLoading = false,
                            reviewsErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadMoreReviews() {
        val current = reviewsState.value
        if (current.isReviewsLoading || current.isReviewsPaginating || !current.reviewsHasNextPage) return

        viewModelScope.launch {
            reviewsState.update { it.copy(isReviewsPaginating = true) }
            val userId = uiState.value.profile?.id
            if (userId == null) {
                reviewsState.update { it.copy(isReviewsPaginating = false) }
                return@launch
            }
            val nextPage = current.reviewsPage + 1
            when (val result = profileRepository.getUserReviews(userId, page = nextPage)) {
                is Result.Success -> {
                    reviewsState.update {
                        it.copy(
                            isReviewsPaginating = false,
                            reviewsPage = nextPage,
                            // Dedupe by id so a review carried over between pages doesn't crash
                            // the list with a duplicate key.
                            reviews = (it.reviews + result.data.reviews).distinctBy { r -> r.id },
                            reviewsHasNextPage = result.data.hasNextPage
                        )
                    }
                }
                is Result.Error -> {
                    reviewsState.update { it.copy(isReviewsPaginating = false) }
                }
            }
        }
    }

    /**
     * Loads the next older page of the user's activity feed and appends it. Triggered by
     * the Activity tab's infinite scroll. The combine merges/dedupes the appended pages
     * into the profile feed, so this only tracks the cursor and in-flight/has-more flags.
     */
    private fun loadMoreActivities() {
        val current = activityPaginationState.value
        if (current.isPaginating || !current.hasNextPage) return
        val userId = uiState.value.profile?.id?.takeIf { it > 0 } ?: return

        viewModelScope.launch {
            activityPaginationState.update { it.copy(isPaginating = true) }
            val nextPage = current.page + 1
            val types = activityTypesFor(current.filter)
            when (val result = profileRepository.getUserActivitiesPage(userId, nextPage, types)) {
                is Result.Success -> activityPaginationState.update {
                    it.copy(
                        isPaginating = false,
                        page = nextPage,
                        appended = it.appended + result.data.activities,
                        hasNextPage = result.data.hasNextPage
                    )
                }
                is Result.Error -> activityPaginationState.update { it.copy(isPaginating = false) }
            }
        }
    }

    /**
     * Reset activity pagination to a fresh stream for [filter]. ALL treats the profile's
     * initial batch as page 1 and pages onward from page 2; a narrower filter seeds page 0 so
     * its first load-more re-fetches page 1 of that type's stream (the initial batch drew that
     * type from a different, shorter query, so its page-1 overlap is deduped rather than skipped).
     */
    private fun seedActivityPagination(filter: ProfileActivityFilter) {
        activityPaginationState.value = ActivityPaginationState(
            filter = filter,
            page = if (filter == ProfileActivityFilter.ALL) 1 else 0
        )
    }

    private fun activityTypesFor(
        filter: ProfileActivityFilter
    ): List<com.anisync.android.domain.ActivityType> = when (filter) {
        ProfileActivityFilter.ALL -> listOf(
            com.anisync.android.domain.ActivityType.TEXT,
            com.anisync.android.domain.ActivityType.MESSAGE,
            com.anisync.android.domain.ActivityType.MEDIA_LIST
        )
        ProfileActivityFilter.STATUS -> listOf(com.anisync.android.domain.ActivityType.TEXT)
        ProfileActivityFilter.MESSAGES -> listOf(com.anisync.android.domain.ActivityType.MESSAGE)
        ProfileActivityFilter.LISTS -> listOf(com.anisync.android.domain.ActivityType.MEDIA_LIST)
    }

    /**
     * Up/down-vote the review currently open in the detail sheet. Applies an optimistic count
     * update to both the open sheet and the underlying reviews list, then reconciles with the
     * authoritative counts the mutation returns (or rolls back on failure).
     */
    private fun rateReview(reviewId: Int, rating: com.anisync.android.type.ReviewRating) {
        val current = localState.value.selectedReview?.takeIf { it.id == reviewId } ?: return

        val oldRating = current.userRating
        val newRatingStr = if (rating == com.anisync.android.type.ReviewRating.NO_VOTE) null else rating.name

        var updatedRating = current.rating
        var updatedAmount = current.ratingAmount
        when (oldRating) {
            "UP_VOTE" -> updatedRating -= 1
            "DOWN_VOTE" -> updatedRating += 1
        }
        if (oldRating != null && newRatingStr == null) updatedAmount -= 1
        if (oldRating == null && newRatingStr != null) updatedAmount += 1
        when (newRatingStr) {
            "UP_VOTE" -> updatedRating += 1
            "DOWN_VOTE" -> updatedRating -= 1
        }

        val optimistic = current.copy(
            userRating = newRatingStr,
            rating = updatedRating,
            ratingAmount = updatedAmount
        )
        applyReviewUpdate(reviewId, optimistic)

        viewModelScope.launch {
            when (val result = detailsRepository.rateReview(reviewId, rating)) {
                is Result.Success -> {
                    val server = result.data
                    applyReviewPatch(reviewId) {
                        it.copy(
                            rating = server.rating,
                            ratingAmount = server.ratingAmount,
                            userRating = server.userRating
                        )
                    }
                }
                is Result.Error -> applyReviewUpdate(reviewId, current) // roll back
            }
        }
    }

    /** Replaces the review wherever it currently lives (open sheet + reviews list). */
    private fun applyReviewUpdate(
        reviewId: Int,
        updated: com.anisync.android.domain.MediaReview
    ) {
        localState.update {
            if (it.selectedReview?.id == reviewId) it.copy(selectedReview = updated) else it
        }
        reviewsState.update { st ->
            st.copy(reviews = st.reviews.map { if (it.id == reviewId) updated else it })
        }
    }

    private inline fun applyReviewPatch(
        reviewId: Int,
        patch: (com.anisync.android.domain.MediaReview) -> com.anisync.android.domain.MediaReview
    ) {
        localState.update {
            val r = it.selectedReview
            if (r?.id == reviewId) it.copy(selectedReview = patch(r)) else it
        }
        reviewsState.update { st ->
            st.copy(reviews = st.reviews.map { if (it.id == reviewId) patch(it) else it })
        }
    }

    /**
     * Loads the full favourite-anime list (all pages) on demand — triggered when the Favorites tab
     * opens, or re-run on pull-to-refresh while it's active. The profile fetch only carries page 1,
     * so this is the only place the favourites fan-out runs. Manga/characters/staff show their
     * page-1 overview directly, so they need no lazy fetch.
     */
    private fun fetchFullFavoriteAnime(forceRefresh: Boolean = false) {
        if (favoritesState.value.isAnimeLoading) return
        if (!forceRefresh && favoritesState.value.animeFetched) return

        viewModelScope.launch {
            val userId = uiState.value.profile?.id?.takeIf { it > 0 } ?: return@launch
            favoritesState.update { it.copy(isAnimeLoading = true) }
            val policy = if (forceRefresh) CachePolicy.NetworkOnly else CachePolicy.NetworkFirst
            val result = profileRepository.getFavoriteAnime(userId, policy)
            favoritesState.update {
                it.copy(
                    fullAnime = (result as? Result.Success)?.data ?: it.fullAnime,
                    isAnimeLoading = false,
                    animeFetched = true
                )
            }
        }
    }

    private fun fetchStats(forceRefresh: Boolean = false) {
        if (statsState.value.isStatsLoading) return
        if (!forceRefresh && statsState.value.hasFetchedStats) return

        viewModelScope.launch {
            statsState.update { it.copy(isStatsLoading = true, statsErrorMessage = null) }
            // The Overview tab's cold-open auto-refresh fires this before the profile is
            // necessarily in uiState (the stats fetch races the profile load). Bailing on a
            // null id left the activity-history heatmap empty until a manual pull-to-refresh;
            // instead, await the first non-null id so it populates on its own.
            val userId = uiState.value.profile?.id
                ?: uiState.map { it.profile?.id }.filterNotNull().first()

            // Force the network on an explicit refresh; otherwise network-first with a
            // cache fallback (matches the other profile tabs). A cache-first default here
            // froze the activity-history heatmap at first fetch — the normalized cache
            // never revalidates on its own, so new activity never showed (#88).
            val policy = if (forceRefresh) CachePolicy.NetworkOnly else CachePolicy.NetworkFirst
            when (val result = statisticsRepository.getUserStatistics(userId, policy)) {
                is Result.Success -> {
                    // Process data on default dispatcher
                    val processedData = kotlinx.coroutines.withContext(Dispatchers.Default) {
                        val animeUi = processAnimeStats(result.data.scoreFormat, result.data.animeStats)
                        val mangaUi = result.data.mangaStats?.let { processMangaStats(it, result.data.scoreFormat) }
                        StatisticsUiModel(animeUi, mangaUi, activityHistory = result.data.activityHistory)
                    }

                    statsState.update {
                        it.copy(
                            isStatsLoading = false,
                            statsData = processedData,
                            hasFetchedStats = true
                        )
                    }
                }
                is Result.Error -> {
                    statsState.update {
                        it.copy(
                            isStatsLoading = false,
                            statsErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun processAnimeStats(scoreFormat: ScoreFormat?, stats: AnimeStatistics): AnimeStatisticsUi {
        val effectiveScoreFormat = scoreFormat ?: inferScoreFormat(stats.scoreDistribution.map { it.score })
        val scoreUi = bucketScores(effectiveScoreFormat, stats.scoreDistribution.map { it.score to it.count })

        val sortedReleaseYears = stats.releaseYearDistribution.sortedBy { it.year }.takeLast(12)
        val maxReleaseYearCount = sortedReleaseYears.maxOfOrNull { it.count } ?: 1
        val releaseYearsUi = sortedReleaseYears.map {
            YearUiModel(it.year, it.count, it.count.toFloat() / maxReleaseYearCount.coerceAtLeast(1))
        }

        val sortedStartYears = stats.startYearDistribution.sortedBy { it.year }.takeLast(12)
        val maxStartYearCount = sortedStartYears.maxOfOrNull { it.count } ?: 1
        val startYearsUi = sortedStartYears.map {
            YearUiModel(it.year, it.count, it.count.toFloat() / maxStartYearCount.coerceAtLeast(1))
        }

        val maxLengthCount = stats.lengthDistribution.maxOfOrNull { it.count } ?: 1
        val lengthsUi = stats.lengthDistribution.map {
            LengthUiModel(it.length, it.count, it.count.toFloat() / maxLengthCount.coerceAtLeast(1))
        }

        val statusesUi = stats.statusDistribution.toStatusUi()

        return AnimeStatisticsUi(
            totalCount = stats.totalCount,
            daysWatched = stats.daysWatched.toDouble(),
            meanScore = stats.meanScore.toDouble(),
            standardDeviation = stats.standardDeviation.toDouble(),
            episodesWatched = stats.episodesWatched,
            minutesWatched = stats.minutesWatched,
            statusDistribution = statusesUi,
            scoreDistribution = scoreUi,
            genreDistribution = stats.genreDistribution.take(20),
            tagDistribution = stats.tagDistribution.take(25),
            formatDistribution = stats.formatDistribution,
            releaseYearDistribution = releaseYearsUi,
            startYearDistribution = startYearsUi,
            lengthDistribution = lengthsUi,
            studioDistribution = stats.studioDistribution.take(20),
            voiceActorDistribution = stats.voiceActorDistribution.take(10),
            staffDistribution = stats.staffDistribution.take(10),
            countryDistribution = stats.countryDistribution
        )
    }

    private fun processMangaStats(stats: MangaStatistics, scoreFormat: ScoreFormat?): MangaStatisticsUi {
        val effectiveScoreFormat = scoreFormat ?: inferScoreFormat(stats.scoreDistribution.map { it.score })
        val scoreUi = bucketScores(effectiveScoreFormat, stats.scoreDistribution.map { it.score to it.count })

        val sortedReleaseYears = stats.releaseYearDistribution.sortedBy { it.year }.takeLast(12)
        val maxReleaseYearCount = sortedReleaseYears.maxOfOrNull { it.count } ?: 1
        val releaseYearsUi = sortedReleaseYears.map {
            YearUiModel(it.year, it.count, it.count.toFloat() / maxReleaseYearCount.coerceAtLeast(1))
        }

        val sortedStartYears = stats.startYearDistribution.sortedBy { it.year }.takeLast(12)
        val maxStartYearCount = sortedStartYears.maxOfOrNull { it.count } ?: 1
        val startYearsUi = sortedStartYears.map {
            YearUiModel(it.year, it.count, it.count.toFloat() / maxStartYearCount.coerceAtLeast(1))
        }

        val maxLengthCount = stats.lengthDistribution.maxOfOrNull { it.count } ?: 1
        val lengthsUi = stats.lengthDistribution.map {
            LengthUiModel(it.length, it.count, it.count.toFloat() / maxLengthCount.coerceAtLeast(1))
        }

        val statusesUi = stats.statusDistribution.toStatusUi()

        return MangaStatisticsUi(
            totalCount = stats.totalCount,
            chaptersRead = stats.chaptersRead,
            volumesRead = stats.volumesRead,
            meanScore = stats.meanScore.toDouble(),
            standardDeviation = stats.standardDeviation.toDouble(),
            statusDistribution = statusesUi,
            scoreDistribution = scoreUi,
            genreDistribution = stats.genreDistribution.take(20),
            tagDistribution = stats.tagDistribution.take(25),
            formatDistribution = stats.formatDistribution,
            releaseYearDistribution = releaseYearsUi,
            startYearDistribution = startYearsUi,
            lengthDistribution = lengthsUi,
            staffDistribution = stats.staffDistribution.take(10),
            countryDistribution = stats.countryDistribution
        )
    }

    private fun bucketScores(
        format: ScoreFormat,
        scores: List<Pair<Int, Int>>
    ): List<ScoreUiModel> {
        val bucketCount = when (format) {
            ScoreFormat.POINT_100, ScoreFormat.POINT_10_DECIMAL, ScoreFormat.POINT_10 -> 10
            ScoreFormat.POINT_5 -> 5
            ScoreFormat.POINT_3 -> 3
        }
        val maxRawScore = when (format) {
            ScoreFormat.POINT_100, ScoreFormat.POINT_10_DECIMAL -> 100
            ScoreFormat.POINT_10 -> 10
            ScoreFormat.POINT_5 -> 5
            ScoreFormat.POINT_3 -> 3
        }
        val counts = IntArray(bucketCount)
        scores.forEach { (rawScore, count) ->
            val s = rawScore.coerceIn(1, maxRawScore)
            val bucketIndex = when (format) {
                ScoreFormat.POINT_100, ScoreFormat.POINT_10_DECIMAL -> ((s - 1) / 10).coerceIn(0, bucketCount - 1)
                else -> (s - 1).coerceIn(0, bucketCount - 1)
            }
            counts[bucketIndex] += count
        }
        val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
        return (1..bucketCount).map { bucket ->
            val count = counts[bucket - 1]
            val label = when (format) {
                ScoreFormat.POINT_100 -> (bucket * 10).toString()
                ScoreFormat.POINT_10_DECIMAL -> "$bucket.0"
                else -> bucket.toString()
            }
            val normalized = when (format) {
                ScoreFormat.POINT_100 -> (bucket * 10) / 100f
                ScoreFormat.POINT_10_DECIMAL, ScoreFormat.POINT_10 -> bucket / 10f
                ScoreFormat.POINT_5 -> bucket / 5f
                ScoreFormat.POINT_3 -> bucket / 3f
            }
            ScoreUiModel(
                score = bucket,
                label = label,
                normalizedScore = normalized,
                count = count,
                heightFraction = count.toFloat() / maxCount
            )
        }
    }

    private fun List<com.anisync.android.domain.StatusStat>.toStatusUi(): List<StatusUiModel> {
        val total = sumOf { it.count }.coerceAtLeast(1)
        // Stable color role per status for theme-driven palette
        val statusOrder = listOf("CURRENT", "COMPLETED", "PLANNING", "PAUSED", "DROPPED", "REPEATING")
        return sortedBy { statusOrder.indexOf(it.status).let { idx -> if (idx < 0) Int.MAX_VALUE else idx } }
            .mapIndexed { index, stat ->
                StatusUiModel(
                    status = stat.status,
                    count = stat.count,
                    fraction = stat.count.toFloat() / total,
                    colorRoleIndex = index % 5
                )
            }
    }

    private fun inferScoreFormat(scores: List<Int>): ScoreFormat {
        val maxScore = scores.maxOrNull() ?: 10
        return when {
            maxScore > 10 -> ScoreFormat.POINT_100
            maxScore > 5 -> ScoreFormat.POINT_10
            maxScore > 3 -> ScoreFormat.POINT_5
            else -> ScoreFormat.POINT_3
        }
    }

    private fun fetchUserAnimeList(forceRefresh: Boolean = false) {
        if (mediaListState.value.isUserAnimeListLoading) return
        if (!forceRefresh && mediaListState.value.hasFetchedAnimeList) return

        viewModelScope.launch {
            mediaListState.update { it.copy(isUserAnimeListLoading = true) }
            val username = uiState.value.profile?.name
            if (username.isNullOrBlank()) {
                mediaListState.update { it.copy(isUserAnimeListLoading = false) }
                return@launch
            }

            when (val result = profileRepository.getUserAnimeList(username)) {
                is Result.Success -> {
                    val grouped = groupEntriesByStatus(result.data)
                    mediaListState.update {
                        it.copy(
                            isUserAnimeListLoading = false,
                            userAnimeList = result.data,
                            userAnimeListByStatus = grouped,
                            hasFetchedAnimeList = true,
                            lastAnimeListFetchAtMs = System.currentTimeMillis()
                        )
                    }
                }
                is Result.Error -> {
                    mediaListState.update { it.copy(isUserAnimeListLoading = false) }
                }
            }
        }
    }

    private fun fetchUserMangaList(forceRefresh: Boolean = false) {
        if (mediaListState.value.isUserMangaListLoading) return
        if (!forceRefresh && mediaListState.value.hasFetchedMangaList) return

        viewModelScope.launch {
            mediaListState.update { it.copy(isUserMangaListLoading = true) }
            val username = uiState.value.profile?.name
            if (username.isNullOrBlank()) {
                mediaListState.update { it.copy(isUserMangaListLoading = false) }
                return@launch
            }

            when (val result = profileRepository.getUserMangaList(username)) {
                is Result.Success -> {
                    val grouped = groupEntriesByStatus(result.data)
                    mediaListState.update {
                        it.copy(
                            isUserMangaListLoading = false,
                            userMangaList = result.data,
                            userMangaListByStatus = grouped,
                            hasFetchedMangaList = true,
                            lastMangaListFetchAtMs = System.currentTimeMillis()
                        )
                    }
                }
                is Result.Error -> {
                    mediaListState.update { it.copy(isUserMangaListLoading = false) }
                }
            }
        }
    }

    private fun fetchSocialData(forceRefresh: Boolean = false) {
        if (socialState.value.isSocialLoading) return
        if (!forceRefresh && socialState.value.hasFetchedSocialData) return

        viewModelScope.launch {
            socialState.update { it.copy(isSocialLoading = true, socialErrorMessage = null) }
            val userId = uiState.value.profile?.id
            if (userId == null) {
                socialState.update { it.copy(isSocialLoading = false) }
                return@launch
            }

            val policy = if (forceRefresh) CachePolicy.NetworkOnly else CachePolicy.NetworkFirst
            when (val result = profileRepository.getSocialData(userId, page = 1, policy = policy)) {
                is Result.Success -> {
                    val page = result.data
                    socialState.update {
                        it.copy(
                            isSocialLoading = false,
                            socialFollowing = page.data.following,
                            socialFollowers = page.data.followers,
                            socialThreads = page.data.threads,
                            socialComments = page.data.comments,
                            hasFetchedSocialData = true,
                            socialPage = 1,
                            followingHasNextPage = page.followingHasNextPage,
                            followersHasNextPage = page.followersHasNextPage,
                            threadsHasNextPage = page.threadsHasNextPage,
                            commentsHasNextPage = page.commentsHasNextPage
                        )
                    }
                }
                is Result.Error -> {
                    socialState.update {
                        it.copy(
                            isSocialLoading = false,
                            socialErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadMoreSocial() {
        val current = socialState.value
        if (current.isSocialLoading || current.isSocialPaginating) return
        val activeTab = localState.value.selectedSocialTab
        val hasNext = when (activeTab) {
            ProfileSocialTab.FOLLOWING -> current.followingHasNextPage
            ProfileSocialTab.FOLLOWERS -> current.followersHasNextPage
            ProfileSocialTab.FORUM_THREADS -> current.threadsHasNextPage
            ProfileSocialTab.FORUM_COMMENTS -> current.commentsHasNextPage
        }
        if (!hasNext) return

        viewModelScope.launch {
            socialState.update { it.copy(isSocialPaginating = true) }
            val userId = uiState.value.profile?.id
            if (userId == null) {
                socialState.update { it.copy(isSocialPaginating = false) }
                return@launch
            }

            val nextPage = current.socialPage + 1
            when (val result = profileRepository.getSocialData(userId, page = nextPage)) {
                is Result.Success -> {
                    val p = result.data
                    socialState.update {
                        it.copy(
                            isSocialPaginating = false,
                            socialPage = nextPage,
                            // Dedupe by id: threads/comments are keyed by id in the list, so a
                            // carried-over item would crash with a duplicate key; users render in
                            // index-keyed rows but are deduped too to avoid showing twice.
                            socialFollowing = (it.socialFollowing + p.data.following).distinctBy { u -> u.id },
                            socialFollowers = (it.socialFollowers + p.data.followers).distinctBy { u -> u.id },
                            socialThreads = (it.socialThreads + p.data.threads).distinctBy { t -> t.id },
                            socialComments = (it.socialComments + p.data.comments).distinctBy { c -> c.id },
                            followingHasNextPage = p.followingHasNextPage,
                            followersHasNextPage = p.followersHasNextPage,
                            threadsHasNextPage = p.threadsHasNextPage,
                            commentsHasNextPage = p.commentsHasNextPage
                        )
                    }
                }
                is Result.Error -> {
                    socialState.update { it.copy(isSocialPaginating = false) }
                }
            }
        }
    }

    private fun updateAbout(about: String) {
        viewModelScope.launch {
            if (profileRepository.updateAbout(about) is Result.Error) {
                // In a real app, send a one-off UI event (e.g. Snackbar) here
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            accountManager.logoutActive()
            onComplete()
        }
    }

    private fun groupEntriesByStatus(entries: List<LibraryEntry>): Map<LibraryStatus, List<LibraryEntry>> {
        val grouped = entries.groupBy { it.status }
        val ordered = LinkedHashMap<LibraryStatus, List<LibraryEntry>>(LIBRARY_STATUS_DISPLAY_ORDER.size + 1)
        for (status in LIBRARY_STATUS_DISPLAY_ORDER) {
            ordered[status] = grouped[status].orEmpty()
        }
        val unknownItems = grouped[LibraryStatus.UNKNOWN].orEmpty()
        if (unknownItems.isNotEmpty()) {
            ordered[LibraryStatus.UNKNOWN] = unknownItems
        }
        return ordered
    }

    private fun shouldRefreshAnimeList(): Boolean {
        val state = mediaListState.value
        if (!state.hasFetchedAnimeList) return true
        return System.currentTimeMillis() - state.lastAnimeListFetchAtMs >= LIST_REFRESH_INTERVAL_MS
    }

    private fun shouldRefreshMangaList(): Boolean {
        val state = mediaListState.value
        if (!state.hasFetchedMangaList) return true
        return System.currentTimeMillis() - state.lastMangaListFetchAtMs >= LIST_REFRESH_INTERVAL_MS
    }
}
