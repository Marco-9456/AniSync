package com.anisync.android.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.MediaTheme
import com.anisync.android.domain.MediaThemesRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Openings and endings for one title, owned separately from the media details it sits under.
 *
 * Its own ViewModel rather than another flow on [MediaDetailsViewModel] because the full list
 * is a route of its own: sharing the details ViewModel there would build a second copy of it,
 * re-check the media details cache and fetch the discussion and following previews, all to
 * draw a list that was already in Room.
 *
 * [start] is called with what the page knows about the title, so a manga page never spends a
 * request on a service that only carries anime.
 */
@HiltViewModel
class MediaThemesViewModel @Inject constructor(
    private val repository: MediaThemesRepository,
    private val appSettings: AppSettings,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaId: Int = checkNotNull(savedStateHandle["mediaId"]) {
        "Media ID is required for MediaThemesViewModel"
    }

    private val _state = MutableStateFlow(MediaThemesState())
    val state: StateFlow<MediaThemesState> = _state.asStateFlow()

    private var started = false

    /** Begins the lookup once, and only for anime. Safe to call on every recomposition. */
    fun start(isAnime: Boolean) {
        if (started || !isAnime) return
        started = true

        viewModelScope.launch {
            repository.observeThemes(mediaId).collect { cached ->
                if (cached == null) return@collect
                _state.update {
                    it.copy(
                        animeSlug = cached.animeSlug,
                        themes = cached.themes.filterAdult(),
                        hasLoaded = true
                    )
                }
            }
        }

        viewModelScope.launch {
            if (repository.isStale(mediaId)) fetch() else _state.update { it.copy(hasLoaded = true) }
        }
    }

    /** Retry from the notice the section shows when the lookup failed. */
    fun retry() {
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        _state.update { it.copy(isLoading = true, errorMessage = null, retryAfterSeconds = null) }
        when (val result = repository.refreshThemes(mediaId)) {
            is Result.Success -> _state.update {
                it.copy(
                    animeSlug = result.data.animeSlug,
                    themes = result.data.themes.filterAdult(),
                    isLoading = false,
                    hasLoaded = true
                )
            }

            is Result.Error -> _state.update {
                it.copy(
                    isLoading = false,
                    hasLoaded = true,
                    errorMessage = result.message,
                    retryAfterSeconds = result.countdownSeconds
                )
            }
        }
    }

    private fun List<MediaTheme>.filterAdult(): List<MediaTheme> =
        if (appSettings.showAdultContent.value) this else filterNot { it.isAdult }
}
