package com.anisync.android.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Reads and writes the active account's AniList options, treating AniList as the source of truth.
 *
 * - [cachedOptions] is the last-known snapshot for the **active** account (reloaded on account switch).
 * - [fetchOptions] pulls fresh values from AniList, persists them per account, and mirrors the ones
 *   that map to local app behavior into [com.anisync.android.data.AppSettings] (honoring the device
 *   override flags).
 * - [updateOptions] pushes a partial edit via `UpdateUser` and refreshes the cache from the response.
 */
interface UserOptionsRepository {
    val cachedOptions: StateFlow<AniListUserOptions?>

    suspend fun fetchOptions(): Result<AniListUserOptions>

    suspend fun updateOptions(patch: UserOptionsPatch): Result<AniListUserOptions>
}
