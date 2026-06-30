package com.anisync.android.presentation.details

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.toDomain
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.ForumRepository
import com.anisync.android.domain.ForumThread
import com.anisync.android.domain.GetMediaDetailsUseCase
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.MediaFollowingEntry
import com.anisync.android.domain.Result
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.util.ShareUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaDetailsViewModel @Inject constructor(
    private val getMediaDetailsUseCase: GetMediaDetailsUseCase,
    private val detailsRepository: DetailsRepository,
    private val libraryRepository: LibraryRepository,
    private val libraryDao: LibraryDao,
    private val accountStore: com.anisync.android.data.account.AccountStore,
    private val appSettings: AppSettings,
    private val toastManager: com.anisync.android.presentation.components.alert.ToastManager,
    private val forumRepository: ForumRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // True only while an explicit pull-to-refresh network fetch is in flight (drives the PTR spinner).
    // The silent initial fetch for an uncached entry doesn't set it — DetailsUiState.Loading covers it.
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _showEditSheet = MutableStateFlow(false)
    val showEditSheet: StateFlow<Boolean> = _showEditSheet.asStateFlow()

    private val _draftEntry = MutableStateFlow<LibraryEntry?>(null)
    val draftEntry: StateFlow<LibraryEntry?> = _draftEntry.asStateFlow()

    private val _following = MutableStateFlow<List<MediaFollowingEntry>>(emptyList())
    val following: StateFlow<List<MediaFollowingEntry>> = _following.asStateFlow()

    private val _hasMoreFollowing = MutableStateFlow(false)
    val hasMoreFollowing: StateFlow<Boolean> = _hasMoreFollowing.asStateFlow()

    /** Forum threads that have this media as a `mediaCategory` (Discussions section). */
    private val _discussions = MutableStateFlow<List<ForumThread>>(emptyList())
    val discussions: StateFlow<List<ForumThread>> = _discussions.asStateFlow()

    private val _hasMoreDiscussions = MutableStateFlow(false)
    val hasMoreDiscussions: StateFlow<Boolean> = _hasMoreDiscussions.asStateFlow()

    val userScoreFormat: StateFlow<ScoreFormat> = appSettings.userScoreFormat
    
    val animeCustomLists: StateFlow<List<String>> = appSettings.animeListOrder
        .map { order -> order.filterNot { it.startsWith("status:") } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val mangaCustomLists: StateFlow<List<String>> = appSettings.mangaListOrder
        .map { order -> order.filterNot { it.startsWith("status:") } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Get the ID directly from the navigation route "details/{mediaId}"
    private val mediaId: Int = checkNotNull(savedStateHandle["mediaId"]) {
        "Media ID is required for MediaDetailsViewModel"
    }

    /**
     * Observe media details from local cache, with the viewer's note overlaid from their library
     * entry. The library row is the source of truth for notes (it's what the editor writes and what
     * sync keeps fresh), whereas the separately-cached media_details row can predate the note or lag
     * an edit — so reading the note from there would leave it blank until a manual refresh. Pulling it
     * from the library makes it appear immediately and update the instant it's edited.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailsUiState> = combine(
        getMediaDetailsUseCase(mediaId),
        accountStore.activeAccount.flatMapLatest { account ->
            libraryDao.observeEntry(account?.id ?: -1, mediaId)
        }
    ) { details, libraryEntry ->
        when {
            details == null -> DetailsUiState.Loading // No cached data yet, still loading
            // In the library → the library note is authoritative (blank/null means no note). Only
            // fall back to the media_details copy when the entry isn't cached for this account.
            libraryEntry != null -> DetailsUiState.Success(
                details.copy(listNotes = libraryEntry.notes?.takeIf { it.isNotBlank() })
            )
            else -> DetailsUiState.Success(details)
        }
    }
        .onStart { emit(DetailsUiState.Loading) }
        .catch { e -> emit(DetailsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DetailsUiState.Loading
        )

    init {
        // Offline-first + stale-while-revalidate: the Room-backed uiState flow renders the cached
        // copy instantly (and survives rotation / re-entry, so no flash), while a silent background
        // fetch refreshes it when the cached row is missing or older than its status-based TTL.
        // This is why a revisited page picks up a newly-published airing schedule, score, or cover
        // on its own — no manual pull-to-refresh required. PTR still forces [refresh].
        refreshIfStale()
        loadFollowingPreview()
        loadDiscussionsPreview()
    }

    /**
     * Background revalidation on entry. Does not drive the PTR spinner and never blocks the cache
     * render — the repository decides (by cache age + media status) whether a network call happens.
     */
    private fun refreshIfStale() {
        viewModelScope.launch {
            detailsRepository.refreshMediaDetailsIfStale(mediaId)
        }
    }

    /**
     * Loads a small preview of forum threads tagged with this media. Mirrors
     * [loadFollowingPreview]: a separate StateFlow, silent on failure so the
     * section just stays hidden. Reuses the rate-limit-safe [ForumRepository.searchThreads].
     */
    private fun loadDiscussionsPreview() {
        viewModelScope.launch {
            when (val result = forumRepository.searchThreads(
                mediaCategoryId = mediaId,
                sort = com.anisync.android.domain.ThreadSortOption.RECENTLY_REPLIED,
                page = 1
            )) {
                is Result.Success -> {
                    _discussions.value = result.data.items.take(DISCUSSIONS_PREVIEW_LIMIT)
                    _hasMoreDiscussions.value =
                        result.data.hasNextPage || result.data.items.size > DISCUSSIONS_PREVIEW_LIMIT
                }

                is Result.Error -> {
                    // Silent failure — section just stays empty.
                }
            }
        }
    }

    private fun loadFollowingPreview() {
        viewModelScope.launch {
            when (val result = detailsRepository.getMediaFollowing(
                mediaId = mediaId,
                page = 1,
                perPage = FOLLOWING_PREVIEW_LIMIT
            )) {
                is Result.Success -> {
                    val (entries, hasNext) = result.data
                    _following.value = entries
                    _hasMoreFollowing.value = hasNext
                }
                is Result.Error -> {
                    // Silent failure — section just stays empty
                }
            }
        }
    }

    /**
     * Refresh from network (called on init and can be called for pull-to-refresh).
     */
    /** Explicit user-driven refresh (pull-to-refresh) — always hits the network. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                detailsRepository.refreshMediaDetails(mediaId)
                // Result errors could be handled with a snackbar if needed
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun saveMediaListEntry(status: LibraryStatus, progress: Int) {
        viewModelScope.launch {
            _isSaving.value = true
            
            when (val result = detailsRepository.updateMediaListEntry(mediaId, status, progress)) {
                is Result.Success -> {
                    // Cache updated, Flow emits automatically
                }
                is Result.Error -> {
                    // Could emit a one-time event for error (e.g., Snackbar)
                }
            }
            
            _isSaving.value = false
        }
    }

    fun openEditSheet() {
        viewModelScope.launch {
            val details = (uiState.value as? DetailsUiState.Success)?.details ?: return@launch
            
            val existingEntry = libraryDao.getEntry(accountStore.activeAccount.value?.id ?: -1, mediaId)
            val draft = existingEntry?.toDomain() ?: LibraryEntry(
                id = 0,
                mediaId = details.id,
                titleRomaji = details.titleRomaji,
                titleEnglish = details.titleEnglish,
                titleNative = details.titleNative,
                titleUserPreferred = details.titleUserPreferred,
                coverUrl = details.coverUrl,
                progress = 0,
                totalEpisodes = details.episodes,
                totalChapters = details.chapters,
                totalVolumes = details.volumes,
                type = details.type,
                status = LibraryStatus.PLANNING,
                isPrivate = false,
                hiddenFromStatusLists = false
            )

            _draftEntry.value = draft
            _showEditSheet.value = true
        }
    }

    fun closeEditSheet() {
        _showEditSheet.value = false
        _draftEntry.value = null
    }

    fun saveLibraryEntry(entry: LibraryEntry) {
        viewModelScope.launch {
            _isSaving.value = true
            when (val result = libraryRepository.updateEntry(entry)) {
                is Result.Success -> {
                    refresh()
                    closeEditSheet()
                }
                is Result.Error -> {
                    // Handle error
                }
            }
            _isSaving.value = false
        }
    }

    fun deleteMediaListEntry() {
        viewModelScope.launch {
            val details = (uiState.value as? DetailsUiState.Success)?.details ?: return@launch
            val listEntryId = details.listEntryId ?: return@launch

            _isSaving.value = true

            when (val result = detailsRepository.deleteMediaListEntry(listEntryId, mediaId)) {
                is Result.Success -> {
                    // Refresh to update the UI
                    refresh()
                }
                is Result.Error -> {
                    // Could handle error
                }
            }

            _isSaving.value = false
        }
    }

    // Coalesces rating taps so dragging through values sends one rate + one refresh
    // instead of one pair per intermediate value. Ratings are absolute sets, so no
    // baseline seeding is needed (a re-submit of the same value is a no-op).
    private val reviewRatingCoalescer =
        com.anisync.android.presentation.util.MutationCoalescer<Int, com.anisync.android.type.ReviewRating>(viewModelScope) { reviewId, rating ->
            when (detailsRepository.rateReview(reviewId, rating)) {
                is Result.Success -> { refresh(); true }
                is Result.Error -> false
            }
        }
    private val recommendationRatingCoalescer =
        com.anisync.android.presentation.util.MutationCoalescer<Int, com.anisync.android.type.RecommendationRating>(viewModelScope) { recId, rating ->
            when (detailsRepository.rateRecommendation(mediaId, recId, rating)) {
                is Result.Success -> { refresh(); true }
                is Result.Error -> false
            }
        }

    /**
     * Toggle favourite status for the current media.
     */
    fun toggleFavourite() {
        // Favourite is a toggle endpoint and eventually-consistent on AniList, so
        // stacking toggles risks flip-flopping. Drop taps while one is in flight —
        // same in-flight guard the feed like button uses.
        if (_isSaving.value) return
        viewModelScope.launch {
            val details = (uiState.value as? DetailsUiState.Success)?.details ?: return@launch
            val mediaType = details.type ?: return@launch

            _isSaving.value = true

            when (val result = detailsRepository.toggleFavourite(mediaId, mediaType)) {
                is Result.Success -> {
                    // Cache updated via refresh, Flow emits automatically
                }
                is Result.Error -> {
                    // Could emit a one-time event for error (e.g., Snackbar)
                }
            }

            _isSaving.value = false
        }
    }

    /**
     * Share the current media via Android's share sheet.
     * Generates an AniList URL (e.g., https://anilist.co/anime/16498) for the media.
     *
     * @param context The context required to start the share activity
     */
    fun shareMedia(context: Context) {
        val details = (uiState.value as? DetailsUiState.Success)?.details ?: return

        ShareUtils.shareMedia(
            context = context,
            title = details.titleUserPreferred,
            mediaId = details.id,
            mediaType = details.type
        )
    }

    /**
     * Rate a review.
     */
    fun rateReview(reviewId: Int, rating: com.anisync.android.type.ReviewRating) {
        reviewRatingCoalescer.submit(reviewId, rating)
    }

    companion object {
        private const val FOLLOWING_PREVIEW_LIMIT = 10
        private const val DISCUSSIONS_PREVIEW_LIMIT = 5
    }

    fun rateRecommendation(recommendationId: Int, rating: com.anisync.android.type.RecommendationRating) {
        recommendationRatingCoalescer.submit(recommendationId, rating)
    }

    /**
     * Recommend a media as similar to the current one. Upvotes (creates) the
     * recommendation pairing this media -> [mediaRecommendationId], then refreshes
     * details so the new entry appears in the Recommendations section.
     */
    fun recommendMedia(mediaRecommendationId: Int) {
        viewModelScope.launch {
            when (val result = detailsRepository.rateRecommendation(
                mediaId = mediaId,
                recommendationId = mediaRecommendationId,
                rating = com.anisync.android.type.RecommendationRating.RATE_UP
            )) {
                is Result.Success -> {
                    refresh()
                    toastManager.showToast(
                        type = com.anisync.android.presentation.components.alert.ToastType.SUCCESS,
                        message = "Recommendation added"
                    )
                }
                is Result.Error -> toastManager.showResultError(result)
            }
        }
    }
}
