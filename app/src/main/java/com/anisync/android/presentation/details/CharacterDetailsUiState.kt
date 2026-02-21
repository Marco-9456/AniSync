package com.anisync.android.presentation.details

import com.anisync.android.domain.CharacterDetails

sealed interface CharacterDetailsUiState {
    data object Loading : CharacterDetailsUiState
    data class Success(val details: CharacterDetails) : CharacterDetailsUiState
    data class Error(val message: String) : CharacterDetailsUiState
}
