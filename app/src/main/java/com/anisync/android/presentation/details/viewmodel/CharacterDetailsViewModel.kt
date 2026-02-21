package com.anisync.android.presentation.details.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.Result
import com.anisync.android.presentation.details.state.CharacterDetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
