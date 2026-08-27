package com.anisync.android.presentation.details

import com.anisync.android.domain.StaffDetails

sealed interface StaffDetailsUiState {
    data object Loading : StaffDetailsUiState
    data class Success(
        val details: StaffDetails,
        /** A further page of characters or credits is in flight. */
        val isLoadingMore: Boolean = false
    ) : StaffDetailsUiState
    data class Error(val message: String) : StaffDetailsUiState
}
