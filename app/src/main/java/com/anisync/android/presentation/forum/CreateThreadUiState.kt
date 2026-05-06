package com.anisync.android.presentation.forum

import androidx.compose.runtime.Stable
import com.anisync.android.domain.ForumCategory
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.type.MediaType

@Stable
data class CreateThreadUiState(
    val title: String = "",
    val body: String = "",
    val selectedCategoryIds: Set<Int> = emptySet(),
    val availableCategories: List<ForumCategory> = defaultCategories,
    val isSubmitting: Boolean = false,
    val isPreviewMode: Boolean = false,
    val titleError: String? = null,
    val bodyError: String? = null,
    val categoryError: String? = null,
    val mediaSearchType: MediaType = MediaType.ANIME,
    val mediaSearchQuery: String = "",
    val mediaSearchResults: List<LibraryEntry> = emptyList(),
    val isMediaSearching: Boolean = false,
    val mediaSearchError: String? = null
) {
    val isValid: Boolean get() = title.isNotBlank() && body.isNotBlank() && selectedCategoryIds.isNotEmpty()
    val hasUnsavedChanges: Boolean get() = title.isNotBlank() || body.isNotBlank() || selectedCategoryIds.isNotEmpty()
}

sealed interface CreateThreadAction {
    data class OnTitleChange(val value: String) : CreateThreadAction
    data class OnBodyChange(val value: String) : CreateThreadAction
    data class ToggleCategory(val categoryId: Int) : CreateThreadAction
    data object TogglePreview : CreateThreadAction
    data object Submit : CreateThreadAction
    data object NavigateUp : CreateThreadAction
    data class OnMediaSearchQueryChange(val query: String) : CreateThreadAction
    data class OnMediaSearchTypeChange(val type: MediaType) : CreateThreadAction
}

/**
 * AniList forum category IDs verified from the live site. AniList doesn't expose
 * a category list query, so they're hardcoded here.
 */
val defaultCategories = listOf(
    ForumCategory(1, "Anime"),
    ForumCategory(2, "Manga"),
    ForumCategory(3, "Light Novels"),
    ForumCategory(4, "Visual Novels"),
    ForumCategory(5, "Release Discussion"),
    ForumCategory(7, "General"),
    ForumCategory(8, "News"),
    ForumCategory(9, "Music"),
    ForumCategory(10, "Gaming"),
    ForumCategory(11, "Site Feedback"),
    ForumCategory(12, "Bug Reports"),
    ForumCategory(13, "Site Announcements"),
    ForumCategory(15, "Recommendations"),
    ForumCategory(16, "Forum Games"),
    ForumCategory(17, "Misc"),
    ForumCategory(18, "AniList Apps")
)
