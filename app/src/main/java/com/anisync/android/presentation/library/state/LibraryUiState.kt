package com.anisync.android.presentation.library.state

import androidx.compose.runtime.Stable
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType

@Stable
data class LibraryUiState(
    val mediaType: MediaType = MediaType.ANIME,
    val sortOption: LibrarySort = LibrarySort.AIRING_SOON,
    val isAscending: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    val entries: List<LibraryEntry> = emptyList(),
    val groupedEntries: Map<LibraryStatus, List<LibraryEntry>> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

enum class LibrarySort {
    TITLE,
    PROGRESS,
    AIRING_SOON,
    SCORE,
    LAST_UPDATED,
    LAST_ADDED,
    START_DATE,
    RELEASE_DATE
}

sealed interface LibraryEvent {
    data object OnScreenVisible : LibraryEvent
    data object Refresh : LibraryEvent
    data class OnMediaTypeChange(val type: MediaType) : LibraryEvent
    data class OnSortOptionChange(val sort: LibrarySort, val ascending: Boolean) : LibraryEvent
    data class OnSearchQueryChange(val query: String) : LibraryEvent
    data class IncrementProgress(val mediaId: Int) : LibraryEvent
    data class DecrementProgress(val mediaId: Int) : LibraryEvent
    data class UpdateEntry(val entry: LibraryEntry) : LibraryEvent
    data class DeleteEntry(val entryId: Int, val mediaId: Int) : LibraryEvent
    data class ShowSnackbar(val message: String) : LibraryEvent
}
