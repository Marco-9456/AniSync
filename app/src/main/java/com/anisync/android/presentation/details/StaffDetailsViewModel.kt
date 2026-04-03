package com.anisync.android.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffDetailsViewModel @Inject constructor(
    private val detailsRepository: DetailsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val staffId: Int = checkNotNull(savedStateHandle["staffId"]) {
        "Staff ID is required"
    }

    private val _uiState = MutableStateFlow<StaffDetailsUiState>(StaffDetailsUiState.Loading)
    val uiState: StateFlow<StaffDetailsUiState> = _uiState.asStateFlow()

    init {
        loadStaffDetails()
    }

    fun loadStaffDetails() {
        viewModelScope.launch {
            _uiState.value = StaffDetailsUiState.Loading
            when (val result = detailsRepository.getStaffDetails(staffId)) {
                is Result.Success -> {
                    _uiState.value = StaffDetailsUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = StaffDetailsUiState.Error(result.message)
                }
            }
        }
    }
}
