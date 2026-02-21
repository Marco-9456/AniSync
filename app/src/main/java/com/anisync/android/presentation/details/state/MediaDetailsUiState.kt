package com.anisync.android.presentation.details.state

import com.anisync.android.domain.MediaDetails

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(val details: MediaDetails) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}
