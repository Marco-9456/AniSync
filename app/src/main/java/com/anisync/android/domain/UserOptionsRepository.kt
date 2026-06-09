package com.anisync.android.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Local-first store of the active account's AniList options.
 *
 * Edits apply to [cachedOptions] immediately (optimistic) and are coalesced into a pending patch that
 * is flushed to AniList in the background. [pull] reconciles with the server; when a field changed both
 * locally and on the website a [conflict] is surfaced for the user to resolve via [resolveConflict].
 */
interface UserOptionsRepository {
    /** The local working copy for the active account (what the UI shows). Null when signed out. */
    val cachedOptions: StateFlow<AniListUserOptions?>

    /** A pending conflict awaiting the user's choice, or null. */
    val conflict: StateFlow<ConflictState?>

    /** Pull from AniList and reconcile: clean fields take the server value; a dirty field whose server
     *  value also changed becomes a [conflict]. */
    suspend fun pull(): Result<Unit>

    /** Apply an edit to the local copy immediately, mirror it, and schedule a background flush. */
    fun applyEdit(patch: UserOptionsPatch)

    /** Push pending local edits to AniList in one request. Guarded, coalesced, and a no-op when there
     *  is nothing safe to push. */
    suspend fun flush()

    /** Resolve the current conflict. [keepLocal] = push the device's values over the website;
     *  otherwise discard the local edits and adopt the website's values. */
    fun resolveConflict(keepLocal: Boolean)
}
