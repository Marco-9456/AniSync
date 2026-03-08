package com.anisync.android.presentation.discover

import androidx.compose.runtime.Stable
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType

@Stable
data class SectionGridUiState(
    val mediaType: MediaType = MediaType.ANIME,
    val items: List<LibraryEntry> = emptyList(),
    val knownIds: Set<Int> = emptySet(),
    val selectedFormat: MediaFormat? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null,
    val currentPage: Int = 1
)

sealed interface SectionGridAction {
    data object LoadNextPage : SectionGridAction
    data class SetFormatFilter(val format: MediaFormat?) : SectionGridAction
    data class SetMediaType(val type: MediaType) : SectionGridAction
    data object Retry : SectionGridAction
}