package com.anisync.android.presentation.settings

import com.anisync.android.domain.AniListListActivityStatus
import com.anisync.android.domain.AniListStaffNameLanguage
import com.anisync.android.domain.AniListTitleLanguage
import com.anisync.android.domain.AniListUserOptions
import com.anisync.android.domain.ConflictState
import com.anisync.android.domain.ScoreFormat

/**
 * State for the AniList Account options screen. [options] is the local working copy (edits apply to
 * it instantly and sync in the background). [conflict] is non-null when the same option changed both
 * on this device and on the website and the user must choose a direction.
 */
data class AniListOptionsUiState(
    val isLoading: Boolean = true,
    val isSignedIn: Boolean = true,
    val error: String? = null,
    val options: AniListUserOptions? = null,
    val conflict: ConflictState? = null,
)

sealed interface AniListOptionsAction {
    data object Refresh : AniListOptionsAction

    // Content & titles
    data class SetAdultContent(val enabled: Boolean) : AniListOptionsAction
    data class SetTitleLanguage(val language: AniListTitleLanguage) : AniListOptionsAction
    data class SetStaffNameLanguage(val language: AniListStaffNameLanguage) : AniListOptionsAction
    data class SetScoreFormat(val format: ScoreFormat) : AniListOptionsAction

    // Social & activity
    data class SetAiringNotifications(val enabled: Boolean) : AniListOptionsAction
    data class SetRestrictMessagesToFollowing(val enabled: Boolean) : AniListOptionsAction
    data class SetActivityMergeTime(val minutes: Int) : AniListOptionsAction
    data class SetListActivityDisabled(
        val status: AniListListActivityStatus,
        val disabled: Boolean,
    ) : AniListOptionsAction

    // Profile
    data class SetProfileColor(val color: String) : AniListOptionsAction

    /** Resolve a sync conflict: keepLocal pushes this device's values, else adopts the website's. */
    data class ResolveConflict(val keepLocal: Boolean) : AniListOptionsAction
}
