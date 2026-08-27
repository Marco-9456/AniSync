package com.anisync.android.presentation.details

import com.anisync.android.domain.CharacterDetails

sealed interface CharacterDetailsUiState {
    data object Loading : CharacterDetailsUiState
    data class Success(
        val details: CharacterDetails,
        /** A further page of appearances is in flight, so the footer shows progress. */
        val isLoadingMore: Boolean = false
    ) : CharacterDetailsUiState
    data class Error(val message: String) : CharacterDetailsUiState
}