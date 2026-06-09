package com.anisync.android.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * A device-cached snapshot of a user's AniList account options (the `UserOptions` object plus the
 * score format from `MediaListOptions`). AniList is the source of truth; this mirrors the last-known
 * values per account so the app can respect them offline and resolve "effective" settings without a
 * round-trip. See [UserOptionsRepository] for the sync mechanic.
 */
@Immutable
@Serializable
data class AniListUserOptions(
    val titleLanguage: AniListTitleLanguage? = null,
    val displayAdultContent: Boolean = false,
    val airingNotifications: Boolean = true,
    /** AniList profile highlight color name (blue/purple/pink/orange/red/green/gray), or null. */
    val profileColor: String? = null,
    val timezone: String? = null,
    /** Minutes before separate activities merge. 0 = Never, >= 20160 (2 weeks) = Always. */
    val activityMergeTime: Int? = null,
    val staffNameLanguage: AniListStaffNameLanguage? = null,
    val restrictMessagesToFollowing: Boolean = false,
    /**
     * Per list-status flag: `true` means list updates of that status are **disabled** from creating
     * activity on AniList. Statuses absent from the map are enabled (the AniList default).
     */
    val disabledListActivity: Map<AniListListActivityStatus, Boolean> = emptyMap(),
    val scoreFormat: ScoreFormat? = null,
)

/** Mirrors AniList's `UserTitleLanguage` (6 values, including the creator-stylised variants). */
@Serializable
enum class AniListTitleLanguage {
    ROMAJI,
    ENGLISH,
    NATIVE,
    ROMAJI_STYLISED,
    ENGLISH_STYLISED,
    NATIVE_STYLISED,
}

/** Mirrors AniList's `UserStaffNameLanguage`. */
@Serializable
enum class AniListStaffNameLanguage {
    ROMAJI_WESTERN,
    ROMAJI,
    NATIVE,
}

/** The `MediaListStatus` values that can drive auto-created list activity. */
@Serializable
enum class AniListListActivityStatus {
    CURRENT,
    PLANNING,
    COMPLETED,
    DROPPED,
    PAUSED,
    REPEATING,
}

/**
 * A partial edit pushed to AniList via `UpdateUser`. Null fields are left unchanged on the server.
 */
data class UserOptionsPatch(
    val displayAdultContent: Boolean? = null,
    val titleLanguage: AniListTitleLanguage? = null,
    val staffNameLanguage: AniListStaffNameLanguage? = null,
    val scoreFormat: ScoreFormat? = null,
    val airingNotifications: Boolean? = null,
    val restrictMessagesToFollowing: Boolean? = null,
    val activityMergeTime: Int? = null,
    val profileColor: String? = null,
    val disabledListActivity: Map<AniListListActivityStatus, Boolean>? = null,
)
