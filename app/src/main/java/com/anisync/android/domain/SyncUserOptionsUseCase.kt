package com.anisync.android.domain

import javax.inject.Inject

/**
 * Pulls the active account's AniList options and mirrors them into local settings. Safe to call on
 * app start and on account switch; it no-ops (returns an error result) when signed out.
 */
class SyncUserOptionsUseCase @Inject constructor(
    private val repository: UserOptionsRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.pull()
}
