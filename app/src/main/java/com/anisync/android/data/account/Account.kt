package com.anisync.android.data.account

/**
 * A single signed-in AniList account.
 *
 * [token] is the OAuth access token and is **never** persisted in plain storage — it lives in
 * EncryptedSharedPreferences keyed by [id] (see [AccountStore]). The remaining fields are lightweight
 * metadata persisted as JSON so the account list can render without a network call.
 */
data class Account(
    val id: Int,
    val name: String,
    val avatarUrl: String?,
    /** Epoch millis when the token expires, or `0` if unknown (e.g. a migrated legacy token). */
    val expiresAt: Long,
    val token: String,
) {
    /** True once the stored expiry has passed. Unknown expiry (`0`) is treated as not-expired. */
    val isExpired: Boolean
        get() = expiresAt in 1 until System.currentTimeMillis()

    /**
     * A migrated legacy login whose real AniList id/name/avatar haven't been resolved yet.
     * Reconciled on the next successful `GetViewer` (see [AccountManager.reconcileActiveIfProvisional]).
     */
    val isProvisional: Boolean
        get() = id == PROVISIONAL_ID

    companion object {
        /** Placeholder id for a migrated legacy token before its real Viewer id is known. */
        const val PROVISIONAL_ID = 0
    }
}
