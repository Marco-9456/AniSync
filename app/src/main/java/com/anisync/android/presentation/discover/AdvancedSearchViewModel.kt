package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.MediaTag
import com.anisync.android.domain.Result
import com.anisync.android.domain.SearchFilters
import com.anisync.android.domain.SearchRepository
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvancedSearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val appSettings: AppSettings,
    private val bridge: AdvancedSearchBridge
) : ViewModel() {

    private val _state = MutableStateFlow(AdvancedSearchState())
    val state: StateFlow<AdvancedSearchState> = _state.asStateFlow()

    val showAdultContent: StateFlow<Boolean> = appSettings.showAdultContent

    private val countTrigger = MutableStateFlow<CountTrigger?>(null)

    init {
        observeCount()
        loadTaxonomy()
        bridge.consumePending()?.let { input ->
            _state.update {
                it.copy(
                    initialized = true,
                    draft = input.filters,
                    query = input.query,
                    mediaType = input.type
                )
            }
            triggerCount()
        }
    }

    fun submit() {
        bridge.emitResult(_state.value.draft)
    }

    /** Fallback initializer used when the bridge had no pending input. */
    fun initializeIfNeeded(filters: SearchFilters, query: String, type: MediaType) {
        if (_state.value.initialized) return
        _state.update {
            it.copy(
                initialized = true,
                draft = filters,
                query = query,
                mediaType = type
            )
        }
        triggerCount()
    }

    fun onAction(action: AdvancedSearchAction) {
        when (action) {
            is AdvancedSearchAction.UpdateFilters -> {
                _state.update { it.copy(draft = action.filters) }
                triggerCount()
            }
            AdvancedSearchAction.ResetFilters -> {
                _state.update { it.copy(draft = SearchFilters()) }
                triggerCount()
            }
            is AdvancedSearchAction.SetAdultContent -> {
                appSettings.setShowAdultContent(action.enabled)
                if (!action.enabled) {
                    val current = _state.value.draft
                    val nsfwTags = _state.value.tags.filter { it.isAdult }.map { it.name }.toSet()
                    val cleanedTagsIn = current.tagsIncluded - nsfwTags
                    val cleanedTagsOut = current.tagsExcluded - nsfwTags
                    val onlyAdult = if (current.onlyAdult == true) null else current.onlyAdult
                    if (cleanedTagsIn != current.tagsIncluded ||
                        cleanedTagsOut != current.tagsExcluded ||
                        onlyAdult != current.onlyAdult
                    ) {
                        _state.update {
                            it.copy(
                                draft = current.copy(
                                    tagsIncluded = cleanedTagsIn,
                                    tagsExcluded = cleanedTagsOut,
                                    onlyAdult = onlyAdult
                                )
                            )
                        }
                        triggerCount()
                    }
                }
            }
        }
    }

    private fun loadTaxonomy() {
        viewModelScope.launch {
            val genresDeferred = async { searchRepository.getGenres() }
            val tagsDeferred = async { searchRepository.getTags() }
            val genres = (genresDeferred.await() as? Result.Success)?.data.orEmpty()
            val tags = (tagsDeferred.await() as? Result.Success)?.data.orEmpty()
            _state.update {
                it.copy(genres = genres, tags = tags, taxonomyLoaded = true)
            }
        }
    }

    private fun triggerCount() {
        val s = _state.value
        countTrigger.value = CountTrigger(s.query, s.mediaType, s.draft)
    }

    @OptIn(FlowPreview::class)
    private fun observeCount() {
        viewModelScope.launch {
            countTrigger
                .filterNotNull()
                .debounce(400L)
                .distinctUntilChanged()
                .collectLatest { trigger ->
                    _state.update { it.copy(isLoadingCount = true) }
                    val result = searchRepository.searchMedia(
                        query = trigger.query,
                        type = trigger.type,
                        filters = trigger.filters,
                        page = 1,
                        perPage = 1,
                        countOnly = true
                    )
                    val total = (result as? Result.Success)?.data?.total
                    _state.update { it.copy(isLoadingCount = false, liveCount = total) }
                }
        }
    }

    private data class CountTrigger(
        val query: String,
        val type: MediaType,
        val filters: SearchFilters
    )
}

data class AdvancedSearchState(
    val initialized: Boolean = false,
    val query: String = "",
    val mediaType: MediaType = MediaType.ANIME,
    val draft: SearchFilters = SearchFilters(),
    val genres: List<String> = emptyList(),
    val tags: List<MediaTag> = emptyList(),
    val taxonomyLoaded: Boolean = false,
    val liveCount: Int? = null,
    val isLoadingCount: Boolean = false
)

sealed interface AdvancedSearchAction {
    data class UpdateFilters(val filters: SearchFilters) : AdvancedSearchAction
    data object ResetFilters : AdvancedSearchAction
    data class SetAdultContent(val enabled: Boolean) : AdvancedSearchAction
}
