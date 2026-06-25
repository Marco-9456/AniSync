package com.anisync.android.presentation.notes

import androidx.compose.runtime.Immutable
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry

/**
 * UI state for the Notes journal — every library entry the viewer has written a note on, optionally
 * narrowed by [query]. [totalCount] is the number of noted entries regardless of search, so the
 * screen can tell "no notes at all" apart from "no notes match your search".
 */
@Immutable
data class NotesJournalUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val totalCount: Int = 0,
    val entries: List<LibraryEntry> = emptyList(),
    val titleLanguage: TitleLanguage = TitleLanguage.ROMAJI
)
