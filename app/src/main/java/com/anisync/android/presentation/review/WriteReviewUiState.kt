package com.anisync.android.presentation.review

import androidx.compose.runtime.Stable

@Stable
data class WriteReviewUiState(
    val mediaId: Int = 0,
    val mediaTitle: String = "",
    /** Non-null once we've loaded an existing review for this media (edit mode). */
    val existingReviewId: Int? = null,
    /** Loading the viewer's existing review on entry. */
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isDeleting: Boolean = false,
    /** Body markdown to seed the editor with (existing review when editing). */
    val initialBody: String = "",
    val body: String = "",
    val summary: String = "",
    val score: Int = 50,
    val isPrivate: Boolean = false,
    // The as-loaded meta values (create-mode defaults until an existing review loads), so "unsaved
    // changes" means "differs from what was loaded" — not "differs from a blank form", which used to
    // flag every untouched edit session as dirty.
    val initialSummary: String = "",
    val initialScore: Int = 50,
    val initialIsPrivate: Boolean = false,
    val summaryError: String? = null,
    val scoreError: String? = null,
    val bodyError: String? = null
) {
    val isEditing: Boolean get() = existingReviewId != null
}

sealed interface WriteReviewAction {
    data class OnBodyChange(val value: String) : WriteReviewAction
    data class OnSummaryChange(val value: String) : WriteReviewAction
    data class OnScoreChange(val value: Int) : WriteReviewAction
    data class OnPrivateChange(val value: Boolean) : WriteReviewAction
    data object Submit : WriteReviewAction
    data object Delete : WriteReviewAction
}

sealed interface WriteReviewEvent {
    data object Saved : WriteReviewEvent
    data object Deleted : WriteReviewEvent
}
