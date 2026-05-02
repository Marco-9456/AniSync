package com.anisync.android.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.NotificationBadgeStore
import com.anisync.android.domain.ActivityRepository
import com.anisync.android.domain.AnimeStatistics
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val statisticsRepository: StatisticsRepository,
    private val activityRepository: ActivityRepository,
    private val authRepository: com.anisync.android.data.AuthRepository,
    private val appSettings: AppSettings,
    private val notificationBadgeStore: NotificationBadgeStore,
    private val toastManager: ToastManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val LIST_REFRESH_INTERVAL_MS = 5 * 60 * 1000L
    }

    // Settings from AppSettings (needed for display in profile)
    val titleLanguage: StateFlow<com.anisync.android.data.TitleLanguage> = appSettings.titleLanguage

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
        val isFollowLoading: Boolean = false
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

    // Overlay for optimistic activity subscription toggles: activityId -> isSubscribed
    private val activitySubscriptionOverrides = MutableStateFlow<Map<Int, Boolean>>(emptyMap())

    // Overlay for optimistic activity like toggles: activityId -> (isLiked, likeCount).
    private val activityLikeOverrides =
        MutableStateFlow<Map<Int, Pair<Boolean, Int>>>(emptyMap())

    // Optimistically-deleted activity ids hidden from the UI until confirmed.
    private val deletedActivityIds = MutableStateFlow<Set<Int>>(emptySet())

    private val viewerIdFlow = MutableStateFlow<Int?>(null)

    private val targetUsername: String? = savedStateHandle.get<String>("username")
        ?.let(Uri::decode)
        ?.trim()
        ?.removePrefix("@")
        ?.takeIf { it.isNotBlank() }
    private val targetRefreshSignal = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

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
            .onStart { emit(ProfileUiState(isLoading = true)) }
            .catch { e -> emit(ProfileUiState(isLoading = false, errorMessage = e.message ?: "Unknown error")) }
    } else {
        targetRefreshSignal
            .onStart { emit(Unit) }
            .flatMapLatest {
                kotlinx.coroutines.flow.flow {
                    emit(ProfileUiState(isLoading = true))
                    when (val result = profileRepository.fetchUserProfile(targetUsername)) {
                        is Result.Success -> emit(ProfileUiState(isLoading = false, profile = result.data))
                        is Result.Error -> {
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
        notificationBadgeStore.unreadCount
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

        val needsOverlay = remote.profile != null &&
            (subOverrides.isNotEmpty() || likeOverrides.isNotEmpty() || deletedIds.isNotEmpty())
        val remoteWithOverrides = if (!needsOverlay) {
            remote
        } else {
            val profile = remote.profile!!
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
            remote.copy(profile = profile.copy(activities = patched))
        }

        remoteWithOverrides.copy(
            isRefreshing = local.isRefreshing,
            isFollowingUser = local.isFollowingUser,
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
            unreadNotificationCount = unreadCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ProfileUiState(isLoading = true)
    )

    init {
        // Trigger initial refresh
        onAction(ProfileAction.Refresh)

        observeTargetProfileForFollowState()

        viewModelScope.launch {
            viewerIdFlow.value = activityRepository.getViewerId()
        }

        if (targetUsername.isNullOrBlank()) {
            refreshNotificationBadge()
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
            is ProfileAction.Refresh -> refresh()
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
                } else if (action.tab == ProfileTab.STATS && !statsState.value.hasFetchedStats) {
                    fetchStats()
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
                localState.update {
                    it.copy(selectedActivityFilter = action.filter)
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

            is ProfileAction.LoadMoreSocial -> loadMoreSocial()
            is ProfileAction.LoadMoreReviews -> loadMoreReviews()
            is ProfileAction.ToggleActivitySubscription -> toggleActivitySubscription(action.activityId)
            is ProfileAction.ToggleActivityLike -> toggleActivityLike(action.activityId)
            is ProfileAction.DeleteActivity -> deleteActivity(action.activityId)
            is ProfileAction.ConsumeActivitySnackbar -> Unit
        }
    }

    private fun toggleActivityLike(activityId: Int) {
        val current = uiState.value.profile?.activities?.firstOrNull { it.id == activityId } ?: return
        val wasLiked = current.isLiked
        val nextLiked = !wasLiked
        val nextCount = (current.likeCount + if (wasLiked) -1 else 1).coerceAtLeast(0)
        activityLikeOverrides.update { it + (activityId to (nextLiked to nextCount)) }

        viewModelScope.launch {
            when (val result = activityRepository.toggleActivityLike(activityId)) {
                is Result.Success -> {
                    val server = result.data
                    activityLikeOverrides.update {
                        it + (activityId to (server.isLiked to server.likeCount))
                    }
                }
            is Result.Error -> {
                activityLikeOverrides.update { it - activityId }
                if (result.code != null) {
                    toastManager.showToast(result.code, result.message)
                } else {
                    toastManager.showToast(ToastType.INFO, message = result.message)
                }
            }
            }
        }
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
                    if (result.code != null) {
                        toastManager.showToast(result.code, result.message)
                    } else {
                        toastManager.showToast(ToastType.INFO, message = result.message)
                    }
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

    private fun refresh() {
        viewModelScope.launch {
            localState.update { it.copy(isRefreshing = true) }
            val selectedTab = localState.value.selectedTab

            val refreshJob = launch {
                if (targetUsername.isNullOrBlank()) {
                    profileRepository.refreshProfile("")
                } else {
                    targetRefreshSignal.tryEmit(Unit)
                }
            }

            val activeTabJob = launch {
                when (selectedTab) {
                    ProfileTab.SOCIAL -> fetchSocialData(forceRefresh = true)
                    ProfileTab.REVIEWS -> fetchReviews(forceRefresh = true)
                    ProfileTab.STATS -> fetchStats(forceRefresh = true)
                    ProfileTab.ANIME -> fetchUserAnimeList(forceRefresh = shouldRefreshAnimeList())
                    ProfileTab.MANGA -> fetchUserMangaList(forceRefresh = shouldRefreshMangaList())
                    else -> Unit
                }
            }

            refreshJob.join()
            activeTabJob.join()

            if (!targetUsername.isNullOrBlank()) {
                uiState.value.profile?.id?.let { userId ->
                    fetchFollowState(userId)
                }
            }
            
            localState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun observeTargetProfileForFollowState() {
        if (targetUsername.isNullOrBlank()) return

        viewModelScope.launch {
            uiState
                .map { it.profile?.id }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { userId ->
                    fetchFollowState(userId)
                }
        }
    }

    private fun fetchFollowState(userId: Int) {
        viewModelScope.launch {
            localState.update { it.copy(isFollowLoading = true) }
            when (val result = profileRepository.getFollowState(userId)) {
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

            when (val result = profileRepository.getUserReviews(userId, page = 1)) {
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
                            reviews = it.reviews + result.data.reviews,
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

    private fun fetchStats(forceRefresh: Boolean = false) {
        if (statsState.value.isStatsLoading) return
        if (!forceRefresh && statsState.value.hasFetchedStats) return

        viewModelScope.launch {
            statsState.update { it.copy(isStatsLoading = true, statsErrorMessage = null) }
            val userId = uiState.value.profile?.id
            if (userId == null) {
                statsState.update { it.copy(isStatsLoading = false) }
                return@launch
            }

            when (val result = statisticsRepository.getUserStatistics(userId)) {
                is Result.Success -> {
                    // Process data on default dispatcher
                    val processedData = kotlinx.coroutines.withContext(Dispatchers.Default) {
                        val animeUi = processAnimeStats(result.data.scoreFormat, result.data.animeStats)
                        val mangaUi = result.data.mangaStats?.let { processMangaStats(it) }
                        StatisticsUiModel(animeUi, mangaUi)
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
        val effectiveScoreFormat = scoreFormat ?: inferScoreFormat(stats)

        val bucketCount = when (effectiveScoreFormat) {
            ScoreFormat.POINT_100, ScoreFormat.POINT_10_DECIMAL, ScoreFormat.POINT_10 -> 10
            ScoreFormat.POINT_5 -> 5
            ScoreFormat.POINT_3 -> 3
        }

        val maxRawScore = when (effectiveScoreFormat) {
            ScoreFormat.POINT_100, ScoreFormat.POINT_10_DECIMAL -> 100
            ScoreFormat.POINT_10 -> 10
            ScoreFormat.POINT_5 -> 5
            ScoreFormat.POINT_3 -> 3
        }

        val countsByBucket = IntArray(bucketCount)
        stats.scoreDistribution.forEach { stat ->
            val rawScore = stat.score.coerceIn(1, maxRawScore)
            val bucketIndex = when (effectiveScoreFormat) {
                ScoreFormat.POINT_100, ScoreFormat.POINT_10_DECIMAL -> ((rawScore - 1) / 10).coerceIn(0, bucketCount - 1)
                ScoreFormat.POINT_10, ScoreFormat.POINT_5, ScoreFormat.POINT_3 -> (rawScore - 1).coerceIn(0, bucketCount - 1)
            }
            countsByBucket[bucketIndex] += stat.count
        }

        val maxScoreCount = countsByBucket.maxOrNull()?.coerceAtLeast(1) ?: 1
        val fullScoreDistribution = (1..bucketCount).map { bucket ->
            val count = countsByBucket[bucket - 1]
            val label = when (effectiveScoreFormat) {
                ScoreFormat.POINT_100 -> (bucket * 10).toString()
                ScoreFormat.POINT_10_DECIMAL -> "$bucket.0"
                ScoreFormat.POINT_10, ScoreFormat.POINT_5, ScoreFormat.POINT_3 -> bucket.toString()
            }
            val normalizedScore = when (effectiveScoreFormat) {
                ScoreFormat.POINT_100 -> (bucket * 10) / 100f
                ScoreFormat.POINT_10_DECIMAL, ScoreFormat.POINT_10 -> bucket / 10f
                ScoreFormat.POINT_5 -> bucket / 5f
                ScoreFormat.POINT_3 -> bucket / 3f
            }

            ScoreUiModel(
                score = bucket,
                label = label,
                normalizedScore = normalizedScore,
                count = count,
                heightFraction = count.toFloat() / maxScoreCount
            )
        }

        val sortedYears = stats.releaseYearDistribution
            .sortedBy { it.year }
            .takeLast(10)

        val maxYearCount = sortedYears.maxOfOrNull { it.count } ?: 1
        val processedYears = sortedYears.map {
            YearUiModel(it.year, it.count, it.count.toFloat() / maxYearCount.coerceAtLeast(1))
        }

        val topGenres = stats.genreDistribution.take(20)
        val topStudios = stats.studioDistribution.take(20)

        return AnimeStatisticsUi(
            totalCount = stats.totalCount,
            daysWatched = stats.daysWatched.toDouble(),
            meanScore = stats.meanScore.toDouble(),
            episodesWatched = stats.episodesWatched,
            scoreDistribution = fullScoreDistribution,
            genreDistribution = topGenres,
            formatDistribution = stats.formatDistribution,
            releaseYearDistribution = processedYears,
            studioDistribution = topStudios
        )
    }

    private fun processMangaStats(stats: MangaStatistics): MangaStatisticsUi {
        return MangaStatisticsUi(
            totalCount = stats.totalCount,
            chaptersRead = stats.chaptersRead,
            meanScore = stats.meanScore.toDouble()
        )
    }

    private fun inferScoreFormat(stats: AnimeStatistics): ScoreFormat {
        val maxScore = stats.scoreDistribution.maxOfOrNull { it.score } ?: 10
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

            when (val result = profileRepository.getSocialData(userId, page = 1)) {
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
                            socialFollowing = it.socialFollowing + p.data.following,
                            socialFollowers = it.socialFollowers + p.data.followers,
                            socialThreads = it.socialThreads + p.data.threads,
                            socialComments = it.socialComments + p.data.comments,
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
            authRepository.logout()
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
