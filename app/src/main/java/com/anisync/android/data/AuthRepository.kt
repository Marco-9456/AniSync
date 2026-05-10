@file:Suppress("DEPRECATION")

package com.anisync.android.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = createPrefs()

    private fun buildMasterKey(): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private fun createPrefs(): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                buildMasterKey(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { it.contains(KEY_ACCESS_TOKEN) } // force decrypt to surface AEADBadTagException now
        } catch (t: Throwable) {
            // Keystore key invalidated (OS upgrade, backup/restore, lockscreen change, etc.)
            // Wipe corrupt prefs file + master key alias, then recreate. Token is lost; user re-logs in.
            Log.w(TAG, "EncryptedSharedPreferences unreadable, resetting", t)
            resetEncryptedStore()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                buildMasterKey(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    private fun resetEncryptedStore() {
        runCatching { context.deleteSharedPreferences(PREFS_NAME) }
        runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            if (ks.containsAlias(MASTER_KEY_ALIAS)) ks.deleteEntry(MASTER_KEY_ALIAS)
        }
    }

    private val _isLoggedIn = MutableStateFlow(getToken() != null)
    val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

    /**
     * Emitted when the API returns HTTP 401 (token expired/revoked).
     * The UI should collect this to show a "session expired" dialog
     * and redirect the user to the login screen.
     */
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /**
     * Called by [AuthorizationInterceptor] when a 401 is received.
     * Clears the stored token and emits a session-expired event.
     */
    fun onSessionExpired() {
        clearToken()
        _sessionExpired.tryEmit(Unit)
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
        _isLoggedIn.value = true
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun clearToken() {
        sharedPreferences.edit().remove(KEY_ACCESS_TOKEN).apply()
        _isLoggedIn.value = false
    }

    /**
     * Full logout: clears token and resets login state.
     * Note: Apollo cache is not cleared here as we don't use normalized caching.
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        // Clear encrypted preferences
        sharedPreferences.edit().clear().apply()
        
        // Reset login state
        _isLoggedIn.value = false
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val PREFS_NAME = "auth_prefs"
        private const val MASTER_KEY_ALIAS = MasterKey.DEFAULT_MASTER_KEY_ALIAS
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
