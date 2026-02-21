package com.anisync.android.presentation.discover.state

import androidx.compose.runtime.Stable
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.type.MediaFormat
import com.anisync.android.type.MediaType

@Stable
data class SectionGridUiState(
    val mediaType: MediaType = MediaType.ANIME,
    val items: List<LibraryEntry> = emptyList(),
    val selectedFormat: MediaFormat? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = true,
    val errorMessage: String? = null
)

sealed interface SectionGridEvent {
    data object LoadNextPage : SectionGridEvent
    data class SetFormatFilter(val format: MediaFormat?) : SectionGridEvent
    data class SetMediaType(val type: MediaType) : SectionGridEvent
    data object Retry : SectionGridEvent
}
