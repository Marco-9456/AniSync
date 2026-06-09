package com.anisync.android.data

import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.AniListStaffNameLanguage
import com.anisync.android.domain.AniListTitleLanguage
import com.anisync.android.domain.UserActivity

/**
 * Pure mapping/resolution helpers shared by the search repository, the options repository, and the
 * AniList options screen. Kept free of Android/Apollo types so they can be unit-tested directly.
 */

/**
 * Resolves the `isAdult` search filter. Returns `null` to leave the filter off (show everything),
 * `false` to hide 18+, or `true` to show only 18+. The per-search [mode] wins; when it is
 * [AdultMode.ANY] the global [showAdultContent] preference acts as a floor that hides adult media.
 */
fun resolveAdultIsAdult(mode: AdultMode, showAdultContent: Boolean): Boolean? = when (mode) {
    AdultMode.ANY -> if (showAdultContent) null else false
    AdultMode.HIDE -> false
    AdultMode.ONLY -> true
}

/** Collapses AniList's 6-value title language onto the app's 3-value enum (stylised → base). */
fun AniListTitleLanguage.toLocalTitleLanguage(): TitleLanguage = when (this) {
    AniListTitleLanguage.ENGLISH, AniListTitleLanguage.ENGLISH_STYLISED -> TitleLanguage.ENGLISH
    AniListTitleLanguage.NATIVE, AniListTitleLanguage.NATIVE_STYLISED -> TitleLanguage.NATIVE
    else -> TitleLanguage.ROMAJI
}

/** Maps the AniList staff-name language option onto the local enum (1:1). */
fun AniListStaffNameLanguage.toLocalStaffNameLanguage(): StaffNameLanguage = when (this) {
    AniListStaffNameLanguage.ROMAJI_WESTERN -> StaffNameLanguage.ROMAJI_WESTERN
    AniListStaffNameLanguage.ROMAJI -> StaffNameLanguage.ROMAJI
    AniListStaffNameLanguage.NATIVE -> StaffNameLanguage.NATIVE
}

/**
 * Drops activity for 18+ media when adult content is off, matching the AniList website. Text and
 * message activities carry no media (so [UserActivity.mediaIsAdult] is false) and always pass.
 */
fun List<UserActivity>.filterAdultActivities(showAdultContent: Boolean): List<UserActivity> =
    if (showAdultContent) this else filterNot { it.mediaIsAdult }
