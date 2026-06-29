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
    /**
     * Media attached to the thread as `mediaCategories` on the SaveThread mutation.
     * AniList treats these as a separate signal from forum [selectedCategoryIds]:
     * "what media is this thread about" rather than "which forum bucket."
     */
    val selectedMediaCategories: List<LibraryEntry> = emptyList(),
    /**
     * Media ids pre-attached on entry ("start discussion" from a media page). Dirty tracking
     * compares against these so an untouched prefilled form doesn't trip the discard prompt.
     */
    val prefilledMediaCategoryIds: List<Int> = emptyList(),
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

    /**
     * The standalone category currently selected (Misc / AniList Apps /
     * Bug Reports / Site Feedback), or null. Only one can ever be selected at a
     * time since picking one clears the rest.
     */
    val selectedExclusiveCategoryId: Int? get() = selectedCategoryIds.firstOrNull { it in exclusiveCategoryIds }

    /**
     * Whether [categoryId] can be toggled. Selecting a standalone category
     * disables every other category — they can't be combined with it — rather
     * than hiding them, so the picker doesn't flicker as chips appear/disappear.
     */
    fun isCategoryEnabled(categoryId: Int): Boolean {
        val exclusive = selectedExclusiveCategoryId ?: return true
        return categoryId == exclusive
    }

    /** AniList Apps threads carry no related media, so the picker is hidden. */
    val isMediaPickerHidden: Boolean get() = ANILIST_APPS_CATEGORY_ID in selectedCategoryIds

    /** Whether another forum category may still be selected (cap [MAX_THREAD_CATEGORIES]). */
    val canSelectMoreCategories: Boolean get() = selectedCategoryIds.size < MAX_THREAD_CATEGORIES

    /** Whether another media category may still be attached (cap [MAX_THREAD_MEDIA_CATEGORIES]). */
    val canAttachMoreMedia: Boolean get() = selectedMediaCategories.size < MAX_THREAD_MEDIA_CATEGORIES
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
    data class AddMediaCategory(val entry: LibraryEntry) : CreateThreadAction
    data class RemoveMediaCategory(val mediaId: Int) : CreateThreadAction
}

/** AniList caps a thread at three forum categories. */
const val MAX_THREAD_CATEGORIES = 3

/** AniList caps a thread at two attached media (`mediaCategories`). */
const val MAX_THREAD_MEDIA_CATEGORIES = 2

/** AniList Apps category id — threads here carry no related media. */
const val ANILIST_APPS_CATEGORY_ID = 18

/**
 * Standalone AniList forum sections. Selecting one is mutually exclusive with
 * every other category: a Bug Reports / Site Feedback / Misc / AniList Apps
 * thread can't also live under a content category, so the rest are hidden.
 */
val exclusiveCategoryIds: Set<Int> = setOf(
    11, // Site Feedback
    12, // Bug Reports
    17, // Misc
    18  // AniList Apps
)

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
