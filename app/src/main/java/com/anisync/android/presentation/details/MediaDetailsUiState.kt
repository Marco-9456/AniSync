package com.anisync.android.presentation.details

import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.MediaTheme

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(val details: MediaDetails) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

/**
 * Openings and endings, loaded separately from the AniList page they sit on.
 *
 * [hasLoaded] tells an empty list from one that has not arrived yet, which is what decides
 * between drawing the skeleton rail and drawing no section at all.
 */
data class MediaThemesState(
    val animeSlug: String? = null,
    val themes: List<MediaTheme> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Seconds AnimeThemes asked us to wait, from a 429's Retry-After. Null for other failures. */
    val retryAfterSeconds: Long? = null,
    val hasLoaded: Boolean = false
)
