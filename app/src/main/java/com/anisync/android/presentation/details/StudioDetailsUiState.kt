package com.anisync.android.presentation.details

import com.anisync.android.domain.StudioDetails

sealed interface StudioDetailsUiState {
    data object Loading : StudioDetailsUiState
    data class Success(val details: StudioDetails) : StudioDetailsUiState
    data class Error(val message: String) : StudioDetailsUiState
}
