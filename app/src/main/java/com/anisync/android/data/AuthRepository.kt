package com.anisync.android.data

import androidx.datastore.core.DataStore
import com.anisync.android.data.proto.AuthToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing authentication tokens securely.
 *
 * This repository uses Proto DataStore with Google Tink encryption to store
 * authentication tokens securely. It provides reactive access to the token
 * state via Kotlin Flow and follows modern Android security best practices.
 *
 * Key features:
 * - Asynchronous, main-thread-safe operations using coroutines
 * - Type-safe storage using Protocol Buffers
 * - Full-file encryption using AES-256-GCM with HKDF
 * - Reactive token state via Flow
 * - Automatic token expiry tracking
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authDataStore: DataStore<AuthToken>
) {

    // Reactive login state - derived from token presence
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

    // Reactive access to the token
    val token: Flow<String?> = authDataStore.data
        .map { authToken ->
            authToken.accessToken.takeIf { it.isNotEmpty() }
        }
        .onEach { tokenValue ->
            _isLoggedIn.value = tokenValue != null
        }

    /**
     * Saves an access token with optional expiry information.
     *
     * @param token The access token to save
     * @param expiresInSeconds Optional token expiry time in seconds from now
     */
    suspend fun saveToken(token: String, expiresInSeconds: Long? = null) {
        val currentTime = System.currentTimeMillis()
        val expiresAt = expiresInSeconds?.let { currentTime + (it * 1000) } ?: 0L

        authDataStore.updateData { currentAuthToken ->
            currentAuthToken.toBuilder()
                .setAccessToken(token)
                .setIssuedAt(currentTime)
                .setExpiresAt(expiresAt)
                .build()
        }
        _isLoggedIn.value = true
    }

    /**
     * Retrieves the current access token synchronously.
     *
     * Note: This should be used sparingly. Prefer the reactive [token] Flow
     * for observing token changes.
     *
     * @return The current access token, or null if not logged in
     */
    suspend fun getToken(): String? {
        return authDataStore.data
            .map { it.accessToken.takeIf { token -> token.isNotEmpty() } }
            .firstOrNull()
    }

    /**
     * Checks if the current token is expired.
     *
     * @return true if the token has expired, false otherwise
     */
    suspend fun isTokenExpired(): Boolean {
        return authDataStore.data
            .map { authToken ->
                if (authToken.expiresAt == 0L) {
                    false // Token doesn't expire
                } else {
                    System.currentTimeMillis() > authToken.expiresAt
                }
            }
            .firstOrNull() ?: false
    }

    /**
     * Clears the stored token and resets the login state.
     * This is a lightweight operation that just removes the token.
     */
    suspend fun clearToken() {
        authDataStore.updateData { AuthToken.getDefaultInstance() }
        _isLoggedIn.value = false
    }

    /**
     * Full logout: clears token and resets login state.
     * Note: Apollo cache is not cleared here as we don't use normalized caching.
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        clearToken()
    }
}
