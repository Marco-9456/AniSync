package com.anisync.android.domain

import kotlinx.serialization.Serializable

/**
 * The individually-editable AniList account options. Used to track which fields the user changed
 * locally ("dirty") and to detect per-field conflicts against the server during sync.
 */
@Serializable
enum class UserOptionField {
    DISPLAY_ADULT_CONTENT,
    TITLE_LANGUAGE,
    STAFF_NAME_LANGUAGE,
    SCORE_FORMAT,
    AIRING_NOTIFICATIONS,
    RESTRICT_MESSAGES_TO_FOLLOWING,
    ACTIVITY_MERGE_TIME,
    PROFILE_COLOR,
    DISABLED_LIST_ACTIVITY,
}

/**
 * Per-account local-first sync state, persisted between launches.
 *
 * - [serverSnapshot]: the option values last known to match the AniList server (the conflict baseline).
 * - [local]: the working copy the app reads and the UI shows (may be ahead of the server).
 * - [dirty]: fields edited locally that still need pushing.
 * - [conflict]: a detected divergence awaiting the user's choice (null when none).
 */
@Serializable
data class LocalOptionsState(
    val serverSnapshot: AniListUserOptions = AniListUserOptions(),
    val local: AniListUserOptions = AniListUserOptions(),
    val dirty: Set<UserOptionField> = emptySet(),
    val conflict: ConflictState? = null,
)

/** A field-level conflict: [fields] changed both locally and on the server; [serverValues] is fresh. */
@Serializable
data class ConflictState(
    val fields: Set<UserOptionField>,
    val serverValues: AniListUserOptions,
)

/** Fields whose values differ between two snapshots. */
fun AniListUserOptions.differingFields(other: AniListUserOptions): Set<UserOptionField> =
    UserOptionField.entries.filterTo(mutableSetOf()) { !valueEquals(other, it) }

private fun AniListUserOptions.valueEquals(other: AniListUserOptions, field: UserOptionField): Boolean =
    when (field) {
        UserOptionField.DISPLAY_ADULT_CONTENT -> displayAdultContent == other.displayAdultContent
        UserOptionField.TITLE_LANGUAGE -> titleLanguage == other.titleLanguage
        UserOptionField.STAFF_NAME_LANGUAGE -> staffNameLanguage == other.staffNameLanguage
        UserOptionField.SCORE_FORMAT -> scoreFormat == other.scoreFormat
        UserOptionField.AIRING_NOTIFICATIONS -> airingNotifications == other.airingNotifications
        UserOptionField.RESTRICT_MESSAGES_TO_FOLLOWING ->
            restrictMessagesToFollowing == other.restrictMessagesToFollowing
        UserOptionField.ACTIVITY_MERGE_TIME -> activityMergeTime == other.activityMergeTime
        UserOptionField.PROFILE_COLOR -> profileColor == other.profileColor
        UserOptionField.DISABLED_LIST_ACTIVITY -> disabledListActivity == other.disabledListActivity
    }

/** The fields a patch sets (its non-null members). */
fun UserOptionsPatch.affectedFields(): Set<UserOptionField> = buildSet {
    if (displayAdultContent != null) add(UserOptionField.DISPLAY_ADULT_CONTENT)
    if (titleLanguage != null) add(UserOptionField.TITLE_LANGUAGE)
    if (staffNameLanguage != null) add(UserOptionField.STAFF_NAME_LANGUAGE)
    if (scoreFormat != null) add(UserOptionField.SCORE_FORMAT)
    if (airingNotifications != null) add(UserOptionField.AIRING_NOTIFICATIONS)
    if (restrictMessagesToFollowing != null) add(UserOptionField.RESTRICT_MESSAGES_TO_FOLLOWING)
    if (activityMergeTime != null) add(UserOptionField.ACTIVITY_MERGE_TIME)
    if (profileColor != null) add(UserOptionField.PROFILE_COLOR)
    if (disabledListActivity != null) add(UserOptionField.DISABLED_LIST_ACTIVITY)
}

/** Returns a copy with the patch's non-null fields applied. */
fun AniListUserOptions.applyPatch(patch: UserOptionsPatch): AniListUserOptions = copy(
    displayAdultContent = patch.displayAdultContent ?: displayAdultContent,
    titleLanguage = patch.titleLanguage ?: titleLanguage,
    staffNameLanguage = patch.staffNameLanguage ?: staffNameLanguage,
    scoreFormat = patch.scoreFormat ?: scoreFormat,
    airingNotifications = patch.airingNotifications ?: airingNotifications,
    restrictMessagesToFollowing = patch.restrictMessagesToFollowing ?: restrictMessagesToFollowing,
    activityMergeTime = patch.activityMergeTime ?: activityMergeTime,
    profileColor = patch.profileColor ?: profileColor,
    disabledListActivity = patch.disabledListActivity ?: disabledListActivity,
)

/** Builds a patch carrying this snapshot's values for exactly [fields] (for pushing dirty fields). */
fun AniListUserOptions.patchForFields(fields: Set<UserOptionField>): UserOptionsPatch = UserOptionsPatch(
    displayAdultContent = displayAdultContent.takeIf { UserOptionField.DISPLAY_ADULT_CONTENT in fields },
    titleLanguage = titleLanguage.takeIf { UserOptionField.TITLE_LANGUAGE in fields },
    staffNameLanguage = staffNameLanguage.takeIf { UserOptionField.STAFF_NAME_LANGUAGE in fields },
    scoreFormat = scoreFormat.takeIf { UserOptionField.SCORE_FORMAT in fields },
    airingNotifications = airingNotifications.takeIf { UserOptionField.AIRING_NOTIFICATIONS in fields },
    restrictMessagesToFollowing =
        restrictMessagesToFollowing.takeIf { UserOptionField.RESTRICT_MESSAGES_TO_FOLLOWING in fields },
    activityMergeTime = activityMergeTime.takeIf { UserOptionField.ACTIVITY_MERGE_TIME in fields },
    profileColor = profileColor.takeIf { UserOptionField.PROFILE_COLOR in fields },
    disabledListActivity = disabledListActivity.takeIf { UserOptionField.DISABLED_LIST_ACTIVITY in fields },
)

/** Returns a copy with [fields] taken from [from], other fields unchanged. */
fun AniListUserOptions.takeFields(fields: Set<UserOptionField>, from: AniListUserOptions): AniListUserOptions =
    applyPatch(from.patchForFields(fields))
