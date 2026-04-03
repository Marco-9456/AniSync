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
class CharacterDetailsViewModel @Inject constructor(
    private val detailsRepository: DetailsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val characterId: Int = checkNotNull(savedStateHandle["characterId"]) {
        "Character ID is required"
    }

    private val _uiState =
        MutableStateFlow<CharacterDetailsUiState>(CharacterDetailsUiState.Loading)
    val uiState: StateFlow<CharacterDetailsUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var isFetching = false

    init {
        loadCharacterDetails()
    }

    fun loadCharacterDetails() {
        viewModelScope.launch {
            _uiState.value = CharacterDetailsUiState.Loading
            currentPage = 1
            when (val result = detailsRepository.getCharacterDetails(characterId, currentPage)) {
                is Result.Success -> {
                    _uiState.value = CharacterDetailsUiState.Success(result.data)
                }

                is Result.Error -> {
                    _uiState.value = CharacterDetailsUiState.Error(result.message)
                }
            }
        }
    }

    fun loadMoreMedia() {
        val currentState = _uiState.value as? CharacterDetailsUiState.Success ?: return
        if (isFetching || !currentState.details.hasNextPage) return

        viewModelScope.launch {
            isFetching = true
            currentPage++
            when (val result = detailsRepository.getCharacterDetails(characterId, currentPage)) {
                is Result.Success -> {
                    val mergedMedia =
                        (currentState.details.media + result.data.media).distinctBy { it.id }
                    _uiState.value = CharacterDetailsUiState.Success(
                        currentState.details.copy(
                            media = mergedMedia,
                            hasNextPage = result.data.hasNextPage
                        )
                    )
                }

                is Result.Error -> {
                    currentPage--
                }
            }
            isFetching = false
        }
    }
}