package com.anisync.android.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.CharacterDetails
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CharacterDetailsUiState {
    data object Loading : CharacterDetailsUiState
    data class Success(val details: CharacterDetails) : CharacterDetailsUiState
    data class Error(val message: String) : CharacterDetailsUiState
}

@HiltViewModel
class CharacterDetailsViewModel @Inject constructor(
    private val detailsRepository: DetailsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val characterId: Int = checkNotNull(savedStateHandle["characterId"]) {
        "Character ID is required"
    }

    private val _uiState = MutableStateFlow<CharacterDetailsUiState>(CharacterDetailsUiState.Loading)
    val uiState: StateFlow<CharacterDetailsUiState> = _uiState.asStateFlow()

    init {
        loadCharacterDetails()
    }

    fun loadCharacterDetails() {
        viewModelScope.launch {
            _uiState.value = CharacterDetailsUiState.Loading
            when (val result = detailsRepository.getCharacterDetails(characterId)) {
                is Result.Success -> {
                    _uiState.value = CharacterDetailsUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = CharacterDetailsUiState.Error(result.message)
                }
            }
        }
    }
}
