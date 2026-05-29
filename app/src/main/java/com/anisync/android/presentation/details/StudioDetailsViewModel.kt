package com.anisync.android.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.StudioMediaEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudioDetailsViewModel @Inject constructor(
    private val detailsRepository: DetailsRepository,
    appSettings: com.anisync.android.data.AppSettings,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage

    private val studioId: Int = checkNotNull(savedStateHandle["studioId"]) {
        "Studio ID is required"
    }

    private val _uiState = MutableStateFlow<StudioDetailsUiState>(StudioDetailsUiState.Loading)
    val uiState: StateFlow<StudioDetailsUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var isFetching = false

    init {
        loadStudioDetails()
    }

    fun loadStudioDetails() {
        viewModelScope.launch {
            _uiState.value = StudioDetailsUiState.Loading
            currentPage = 1
            when (val result = detailsRepository.getStudioDetails(studioId, currentPage)) {
                is Result.Success -> {
                    _uiState.value = StudioDetailsUiState.Success(result.data)
                }

                is Result.Error -> {
                    _uiState.value = StudioDetailsUiState.Error(result.message)
                }
            }
        }
    }

    fun loadMoreMedia() {
        val currentState = _uiState.value as? StudioDetailsUiState.Success ?: return
        if (isFetching || !currentState.details.hasNextPage) return

        viewModelScope.launch {
            isFetching = true
            currentPage++
            when (val result = detailsRepository.getStudioDetails(studioId, currentPage)) {
                is Result.Success -> {
                    val merged = mergeMedia(currentState.details.media, result.data.media)
                    _uiState.value = StudioDetailsUiState.Success(
                        currentState.details.copy(
                            media = merged,
                            hasNextPage = result.data.hasNextPage
                        )
                    )
                }

                is Result.Error -> {
                    currentPage--
                }
            }
            isFetching = false
        }
    }

    private fun mergeMedia(
        oldList: List<StudioMediaEntry>,
        newList: List<StudioMediaEntry>
    ): List<StudioMediaEntry> {
        val seen = oldList.mapTo(mutableSetOf()) { it.mediaId }
        return oldList + newList.filter { seen.add(it.mediaId) }
    }

    @Volatile private var isTogglingFavourite = false

    fun toggleFavourite() {
        // Drop taps while a toggle is in flight; favourite is eventually-consistent
        // on AniList, so stacking requests risks flip-flopping the state.
        if (isTogglingFavourite) return
        isTogglingFavourite = true
        viewModelScope.launch {
            try {
                val currentState =
                    _uiState.value as? StudioDetailsUiState.Success ?: return@launch
                val newState = !currentState.details.isFavourite
                _uiState.value = StudioDetailsUiState.Success(
                    currentState.details.copy(isFavourite = newState)
                )
                when (detailsRepository.toggleStudioFavourite(studioId, newState)) {
                    is Result.Success -> {
                        // Keep optimistic state. Mutation success is sufficient;
                        // the paged response payload cannot be trusted to derive the new flag.
                    }

                    is Result.Error -> {
                        _uiState.value = currentState
                    }
                }
            } finally {
                isTogglingFavourite = false
            }
        }
    }

    fun shareStudio(context: android.content.Context) {
        val details = (_uiState.value as? StudioDetailsUiState.Success)?.details ?: return
        com.anisync.android.util.ShareUtils.shareStudio(context, details.name, studioId)
    }
}
