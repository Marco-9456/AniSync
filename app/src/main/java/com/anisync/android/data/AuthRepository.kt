@file:Suppress("DEPRECATION")

package com.anisync.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isLoggedIn = MutableStateFlow(getToken() != null)
    val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

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
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
