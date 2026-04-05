package com.anisync.android.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.VoicedCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffDetailsViewModel @Inject constructor(
    private val detailsRepository: DetailsRepository,
    appSettings: com.anisync.android.data.AppSettings,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage

    private val staffId: Int = checkNotNull(savedStateHandle["staffId"]) {
        "Staff ID is required"
    }

    private val _uiState = MutableStateFlow<StaffDetailsUiState>(StaffDetailsUiState.Loading)
    val uiState: StateFlow<StaffDetailsUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var isFetching = false

    init {
        loadStaffDetails()
    }

    fun loadStaffDetails() {
        viewModelScope.launch {
            _uiState.value = StaffDetailsUiState.Loading
            currentPage = 1
            when (val result = detailsRepository.getStaffDetails(staffId, currentPage)) {
                is Result.Success -> {
                    _uiState.value = StaffDetailsUiState.Success(result.data)
                }

                is Result.Error -> {
                    _uiState.value = StaffDetailsUiState.Error(result.message)
                }
            }
        }
    }

    fun loadMoreMedia() {
        val currentState = _uiState.value as? StaffDetailsUiState.Success ?: return
        if (isFetching || !currentState.details.hasNextPage) return

        viewModelScope.launch {
            isFetching = true
            currentPage++
            when (val result = detailsRepository.getStaffDetails(staffId, currentPage)) {
                is Result.Success -> {
                    val newVoicedCharacters = mergeVoicedCharacters(
                        currentState.details.voicedCharacters,
                        result.data.voicedCharacters
                    )
                    _uiState.value = StaffDetailsUiState.Success(
                        currentState.details.copy(
                            voicedCharacters = newVoicedCharacters,
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

    private fun mergeVoicedCharacters(
        oldList: List<VoicedCharacter>,
        newList: List<VoicedCharacter>
    ): List<VoicedCharacter> {
        val map = linkedMapOf<Int, VoicedCharacter>()
        oldList.forEach { map[it.characterId] = it }
        newList.forEach { newVc ->
            val existing = map[newVc.characterId]
            if (existing != null) {
                val mergedAppearances =
                    (existing.mediaAppearances + newVc.mediaAppearances).distinctBy { it.mediaId }
                map[newVc.characterId] = existing.copy(mediaAppearances = mergedAppearances)
            } else {
                map[newVc.characterId] = newVc
            }
        }
        return map.values.toList()
    }

    fun toggleFavourite() {
        viewModelScope.launch {
            val currentState =
                _uiState.value as? StaffDetailsUiState.Success ?: return@launch
            // Optimistic update
            _uiState.value = StaffDetailsUiState.Success(
                currentState.details.copy(isFavourite = !currentState.details.isFavourite)
            )
            when (val result = detailsRepository.toggleStaffFavourite(staffId)) {
                is Result.Success -> {
                    val state =
                        _uiState.value as? StaffDetailsUiState.Success ?: return@launch
                    _uiState.value = StaffDetailsUiState.Success(
                        state.details.copy(isFavourite = result.data)
                    )
                }

                is Result.Error -> {
                    // Revert optimistic update
                    _uiState.value = currentState
                }
            }
        }
    }

    fun shareStaff(context: android.content.Context) {
        val details = (_uiState.value as? StaffDetailsUiState.Success)?.details ?: return
        val name = details.name
        com.anisync.android.util.ShareUtils.shareStaff(context, name, staffId)
    }
}