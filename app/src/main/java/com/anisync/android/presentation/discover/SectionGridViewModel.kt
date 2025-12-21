package com.anisync.android.presentation.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.Result
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SectionGridScreen managing paginated section data,
 * format filtering, and infinite scroll.
 */
@HiltViewModel
class SectionGridViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Section type passed via navigation: "trending", "popular", "upcoming", "tba"
    private val sectionType: String = savedStateHandle["sectionType"] ?: "trending"

    // Media type passed via navigation, defaults to ANIME
    private val initialMediaType: MediaType = savedStateHandle.get<String>("mediaType")?.let {
        if (it == "MANGA") MediaType.MANGA else MediaType.ANIME
    } ?: MediaType.ANIME

    private val _mediaType = MutableStateFlow(initialMediaType)
    val mediaType: StateFlow<MediaType> = _mediaType.asStateFlow()

    // Accumulated items from all loaded pages
    private val _items = MutableStateFlow<List<LibraryEntry>>(emptyList())
    val items: StateFlow<List<LibraryEntry>> = _items.asStateFlow()

    // Currently selected format filter (null = all formats)
    private val _selectedFormat = MutableStateFlow<MediaFormat?>(null)
    val selectedFormat: StateFlow<MediaFormat?> = _selectedFormat.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasNextPage = MutableStateFlow(true)
    val hasNextPage: StateFlow<Boolean> = _hasNextPage.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentPage = 1

    init {
        loadInitialData()
    }

    /**
     * Load initial page of data.
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = discoverRepository.getPaginatedSection(
                sectionType = sectionType,
                type = _mediaType.value,
                page = 1,
                format = _selectedFormat.value
            )) {
                is Result.Success -> {
                    _items.value = result.data.items
                    _hasNextPage.value = result.data.hasNextPage
                    currentPage = result.data.currentPage
                }
                is Result.Error -> {
                    _errorMessage.value = result.message
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Load next page for infinite scroll.
     */
    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasNextPage.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true

            when (val result = discoverRepository.getPaginatedSection(
                sectionType = sectionType,
                type = _mediaType.value,
                page = currentPage + 1,
                format = _selectedFormat.value
            )) {
                is Result.Success -> {
                    _items.value = _items.value + result.data.items
                    _hasNextPage.value = result.data.hasNextPage
                    currentPage = result.data.currentPage
                }
                is Result.Error -> {
                    // Silent fail for pagination, don't show error
                }
            }

            _isLoadingMore.value = false
        }
    }

    /**
     * Set format filter and reload data.
     * Passing null clears the filter (shows all formats).
     */
    fun setFormatFilter(format: MediaFormat?) {
        if (_selectedFormat.value == format) return
        
        _selectedFormat.value = format
        currentPage = 1
        _items.value = emptyList()
        loadInitialData()
    }

    /**
     * Set media type and reload data.
     */
    fun setMediaType(type: MediaType) {
        if (_mediaType.value == type) return
        
        _mediaType.value = type
        _selectedFormat.value = null // Reset filter when switching media type
        currentPage = 1
        _items.value = emptyList()
        loadInitialData()
    }

    /**
     * Get available format options based on current media type.
     */
    fun getAvailableFormats(): List<MediaFormat> {
        return when (_mediaType.value) {
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

    /**
     * Retry loading data after an error.
     */
    fun retry() {
        loadInitialData()
    }
}
