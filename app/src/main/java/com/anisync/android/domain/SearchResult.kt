package com.anisync.android.domain

import androidx.compose.runtime.Immutable

@Immutable
sealed interface SearchResult {
    val id: Int
    val displayName: String
    val imageUrl: String?

    @Immutable
    data class MediaResult(
        val entry: LibraryEntry
    ) : SearchResult {
        override val id: Int get() = entry.mediaId
        override val displayName: String get() = entry.titleUserPreferred
        override val imageUrl: String? get() = entry.coverUrl
    }

    @Immutable
    data class CharacterResult(
        override val id: Int,
        override val displayName: String,
        val nativeName: String?,
        override val imageUrl: String?,
        val favourites: Int?
    ) : SearchResult

    @Immutable
    data class StaffResult(
        override val id: Int,
        override val displayName: String,
        val nativeName: String?,
        override val imageUrl: String?,
        val primaryOccupations: List<String>,
        val favourites: Int?
    ) : SearchResult

    @Immutable
    data class UserResult(
        override val id: Int,
        override val displayName: String,
        override val imageUrl: String?
    ) : SearchResult

    @Immutable
    data class StudioResult(
        override val id: Int,
        override val displayName: String,
        val favourites: Int?
    ) : SearchResult {
        override val imageUrl: String? get() = null
    }
}

@Immutable
data class GroupedSearchResults(
    val anime: List<SearchResult.MediaResult> = emptyList(),
    val manga: List<SearchResult.MediaResult> = emptyList(),
    val characters: List<SearchResult.CharacterResult> = emptyList(),
    val staff: List<SearchResult.StaffResult> = emptyList(),
    val users: List<SearchResult.UserResult> = emptyList(),
    val studios: List<SearchResult.StudioResult> = emptyList()
) {
    val isEmpty: Boolean
        get() = anime.isEmpty() && manga.isEmpty() && characters.isEmpty() &&
                staff.isEmpty() && users.isEmpty() && studios.isEmpty()
}
