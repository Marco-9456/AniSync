package com.anisync.android.data.network

/**
 * The slice of the session the network layer needs: a token to sign requests with, and somewhere to
 * report that it stopped working.
 *
 * The interceptors took the whole `AuthRepository` before, which pulled the account store, and with
 * it Android's encrypted preferences, into any test that wanted to exercise them.
 */
interface SessionTokens {

    /** The active account's bearer token, or null when signed out. Read per request. */
    fun getToken(): String?

    /** The active account's token was rejected. Clears it and starts the signed-out flow. */
    fun onSessionExpired()
}
