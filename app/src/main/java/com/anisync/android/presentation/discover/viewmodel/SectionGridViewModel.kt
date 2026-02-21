package com.anisync.android.presentation.discover.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.Result
import com.anisync.android.presentation.discover.state.SectionGridEvent
import com.anisync.android.presentation.discover.state.SectionGridUiState
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SectionGridViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    savedStateHandle: SavedStateHandle,
    private val appSettings: com.anisync.android.data.AppSettings
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage

    // Section type passed via navigation: "trending", "popular", "upcoming", "tba"
    private val sectionType: String = savedStateHandle["sectionType"] ?: "trending"

    // Media type passed via navigation, defaults to ANIME
    private val initialMediaType: MediaType = savedStateHandle.get<String>("mediaType")?.let {
        if (it == "MANGA") MediaType.MANGA else MediaType.ANIME
    } ?: MediaType.ANIME

    private val _uiState = MutableStateFlow(SectionGridUiState(mediaType = initialMediaType))
    val uiState: StateFlow<SectionGridUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    init {
        loadInitialData()
    }

    fun onEvent(event: SectionGridEvent) {
        when (event) {
            is SectionGridEvent.LoadNextPage -> loadNextPage()
            is SectionGridEvent.SetFormatFilter -> setFormatFilter(event.format)
            is SectionGridEvent.SetMediaType -> setMediaType(event.type)
            is SectionGridEvent.Retry -> retry()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val currentState = _uiState.value

            when (val result = discoverRepository.getPaginatedSection(
                sectionType = sectionType,
                type = currentState.mediaType,
                page = 1,
                format = currentState.selectedFormat
            )) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            items = result.data.items,
                            hasNextPage = result.data.hasNextPage,
                            isLoading = false
                        )
                    }
                    currentPage = result.data.currentPage
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasNextPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            when (val result = discoverRepository.getPaginatedSection(
                sectionType = sectionType,
                type = currentState.mediaType,
                page = currentPage + 1,
                format = currentState.selectedFormat
            )) {
                is Result.Success -> {
                    // Deduplicate by mediaId
                    val existingIds = currentState.items.map { it.mediaId }.toSet()
                    val newItems = result.data.items.filter { it.mediaId !in existingIds }
                    
                    _uiState.update {
                        it.copy(
                            items = it.items + newItems,
                            hasNextPage = result.data.hasNextPage,
                            isLoadingMore = false
                        )
                    }
                    currentPage = result.data.currentPage
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    private fun setFormatFilter(format: MediaFormat?) {
        if (_uiState.value.selectedFormat == format) return
        
        _uiState.update { it.copy(selectedFormat = format, items = emptyList()) }
        currentPage = 1
        loadInitialData()
    }

    private fun setMediaType(type: MediaType) {
        if (_uiState.value.mediaType == type) return
        
        _uiState.update { 
            it.copy(
                mediaType = type, 
                selectedFormat = null, // Reset format filter when switching type
                items = emptyList()
            ) 
        }
        currentPage = 1
        loadInitialData()
    }

    fun getAvailableFormats(): List<MediaFormat> {
        return when (_uiState.value.mediaType) {
            MediaType.ANIME -> listOf(
                MediaFormat.TV,
                MediaFormat.MOVIE,
                MediaFormat.OVA,
                MediaFormat.SPECIAL,
                MediaFormat.ONA
            )
            MediaType.MANGA -> listOf(
                MediaFormat.MANGA,
                MediaFormat.NOVEL,
                MediaFormat.ONE_SHOT
            )
            else -> emptyList()
        }
    }

    private fun retry() {
        loadInitialData()
    }
}
