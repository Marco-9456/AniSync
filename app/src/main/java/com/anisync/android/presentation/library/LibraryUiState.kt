package com.anisync.android.presentation.library

import androidx.compose.runtime.Stable
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.type.MediaType

@Stable
data class LibraryUiState(
    val mediaType: MediaType = MediaType.ANIME,
    val sortOption: LibrarySort = LibrarySort.AIRING_SOON,
    val isAscending: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    val userScoreFormat: ScoreFormat = ScoreFormat.POINT_100,
    val entries: List<LibraryEntry> = emptyList(),
    val groupedEntries: Map<LibraryStatus, List<LibraryEntry>> = emptyMap(),
    val customListNames: List<String> = emptyList(),
    val customListEntries: Map<String, List<LibraryEntry>> = emptyMap(),
    val favoriteEntries: List<LibraryEntry> = emptyList(),
    val hiddenListNames: Set<String> = emptySet(),
    val listOrder: List<String> = emptyList(),
    val showPrivateEntries: Boolean = true,
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

sealed interface LibraryAction {
    data object OnScreenVisible : LibraryAction
    data object Refresh : LibraryAction
    data class OnMediaTypeChange(val type: MediaType) : LibraryAction
    data class OnSortOptionChange(val sort: LibrarySort, val ascending: Boolean) : LibraryAction
    data class OnSearchQueryChange(val query: String) : LibraryAction
    data class IncrementProgress(val mediaId: Int) : LibraryAction
    data class DecrementProgress(val mediaId: Int) : LibraryAction
    data class UpdateEntry(val entry: LibraryEntry) : LibraryAction
    data class DeleteEntry(val entryId: Int, val mediaId: Int) : LibraryAction
    data class ShowSnackbar(val message: String) : LibraryAction
    data class ToggleListVisibility(val listName: String, val hidden: Boolean) : LibraryAction
    data class MoveListUp(val listName: String) : LibraryAction
    data class MoveListDown(val listName: String) : LibraryAction
    data class CreateCustomList(val listName: String, val type: MediaType) : LibraryAction
    data class DeleteCustomList(val listName: String) : LibraryAction
    data class TogglePrivateVisibility(val show: Boolean) : LibraryAction
}
