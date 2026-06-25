package com.anisync.android.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.type.MediaType
import com.anisync.android.util.getTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Backs the Notes journal. Reads the viewer's cached anime + manga library (offline-first — the same
 * `observeLibrary("")` the library tab uses), keeps only entries with a non-blank note, and exposes
 * them newest-edited first, narrowable by a free-text [onQueryChange] over title or note text.
 *
 * No new data layer: notes already live on [com.anisync.android.domain.LibraryEntry].
 */
@HiltViewModel
class NotesJournalViewModel @Inject constructor(
    libraryRepository: LibraryRepository,
    appSettings: AppSettings
) : ViewModel() {

    private val query = MutableStateFlow("")

    // Noted entries derived from the cached library only — so it is recomputed (and re-sorted) when
    // the library changes, not on every keystroke. The query/title filter is layered on top below.
    private val notedEntries = combine(
        libraryRepository.observeLibrary("", MediaType.ANIME),
        libraryRepository.observeLibrary("", MediaType.MANGA)
    ) { anime, manga ->
        (anime + manga)
            .filter { !it.notes.isNullOrBlank() }
            .distinctBy { it.mediaId }
            .sortedByDescending { it.updatedAt ?: 0L }
    }

    val uiState: StateFlow<NotesJournalUiState> = combine(
        notedEntries,
        query,
        appSettings.titleLanguage
    ) { noted, search, language ->
        val trimmed = search.trim()
        val filtered = if (trimmed.isBlank()) {
            noted
        } else {
            noted.filter { entry ->
                entry.getTitle(language).contains(trimmed, ignoreCase = true) ||
                    entry.notes?.contains(trimmed, ignoreCase = true) == true
            }
        }

        NotesJournalUiState(
            isLoading = false,
            query = search,
            totalCount = noted.size,
            entries = filtered,
            titleLanguage = language
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesJournalUiState()
    )

    fun onQueryChange(value: String) {
        query.value = value
    }
}
