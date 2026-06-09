package com.anisync.android.presentation.settings

import com.anisync.android.data.StaffNameLanguage
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.AniListListActivityStatus
import com.anisync.android.domain.AniListStaffNameLanguage
import com.anisync.android.domain.AniListTitleLanguage
import com.anisync.android.domain.AniListUserOptions
import com.anisync.android.domain.ScoreFormat

/**
 * State for the AniList Account options screen. [options] is the cached account snapshot (source of
 * truth for everything that pushes to AniList); the `local*` fields back the two device-override
 * controls (adult content, title language) which only take effect when their override flag is on.
 */
data class AniListOptionsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSignedIn: Boolean = true,
    val error: String? = null,
    val options: AniListUserOptions? = null,

    // Device override switches + their local values.
    val adultOverrideEnabled: Boolean = false,
    val localShowAdultContent: Boolean = false,
    val titleLanguageOverrideEnabled: Boolean = false,
    val localTitleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    val localStaffNameLanguage: StaffNameLanguage = StaffNameLanguage.ROMAJI_WESTERN,
) {
    /** Effective adult-content value shown by the primary switch (override value or account value). */
    val effectiveShowAdult: Boolean
        get() = if (adultOverrideEnabled) localShowAdultContent else options?.displayAdultContent == true
}

sealed interface AniListOptionsAction {
    data object Refresh : AniListOptionsAction

    // Content & titles
    data class SetAdultContent(val enabled: Boolean) : AniListOptionsAction
    data class SetAdultOverrideEnabled(val enabled: Boolean) : AniListOptionsAction
    data class SetTitleLanguageAccount(val language: AniListTitleLanguage) : AniListOptionsAction
    data class SetTitleLanguageOverrideEnabled(val enabled: Boolean) : AniListOptionsAction
    data class SetLocalTitleLanguage(val language: TitleLanguage) : AniListOptionsAction
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
}
