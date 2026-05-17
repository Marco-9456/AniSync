package com.anisync.android.presentation.details

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.local.dao.LibraryDao
import com.anisync.android.data.local.toDomain
import com.anisync.android.domain.DetailsRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private val appSettings: AppSettings,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _showEditSheet = MutableStateFlow(false)
    val showEditSheet: StateFlow<Boolean> = _showEditSheet.asStateFlow()

    private val _draftEntry = MutableStateFlow<LibraryEntry?>(null)
    val draftEntry: StateFlow<LibraryEntry?> = _draftEntry.asStateFlow()

    private val _following = MutableStateFlow<List<MediaFollowingEntry>>(emptyList())
    val following: StateFlow<List<MediaFollowingEntry>> = _following.asStateFlow()

    private val _hasMoreFollowing = MutableStateFlow(false)
    val hasMoreFollowing: StateFlow<Boolean> = _hasMoreFollowing.asStateFlow()

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
     * Observe media details from local cache via Flow.
     */
    val uiState: StateFlow<DetailsUiState> = getMediaDetailsUseCase(mediaId)
        .map<MediaDetails?, DetailsUiState> { details ->
            if (details != null) {
                DetailsUiState.Success(details)
            } else {
                // No cached data yet, still loading
                DetailsUiState.Loading
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
        // Trigger network refresh to get fresh data
        refresh()
        loadFollowingPreview()
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
    fun refresh() {
        viewModelScope.launch {
            detailsRepository.refreshMediaDetails(mediaId)
            // Result errors could be handled with a snackbar if needed
        }
    }

    // Kept for compatibility with DetailsScreen's LaunchedEffect
    fun loadMedia(id: Int) {
        // Since we use stateIn with mediaId from constructor,
        // this is mostly a no-op but can trigger a refresh
        if (id == mediaId) {
            refresh()
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
            
            val existingEntry = libraryDao.getEntry(mediaId)
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

    /**
     * Toggle favourite status for the current media.
     */
    fun toggleFavourite() {
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
        viewModelScope.launch {
            when (val result = detailsRepository.rateReview(reviewId, rating)) {
                is Result.Success -> {
                    // Update cache for immediate feedback
                    refresh()
                }
                is Result.Error -> {
                    // Handled implicitly or via snackbar later
                }
            }
        }
    }

    companion object {
        private const val FOLLOWING_PREVIEW_LIMIT = 10
    }

    fun rateRecommendation(recommendationId: Int, rating: com.anisync.android.type.RecommendationRating) {
        viewModelScope.launch {
            when (detailsRepository.rateRecommendation(mediaId, recommendationId, rating)) {
                is Result.Success -> refresh()
                is Result.Error -> {}
            }
        }
    }
}
