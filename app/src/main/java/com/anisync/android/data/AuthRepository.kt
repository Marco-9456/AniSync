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
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
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

    /**
     * Exchange authorization code for access token.
     * This makes a POST request to AniList's token endpoint.
     */
    suspend fun exchangeCodeForToken(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://anilist.co/api/v2/oauth/token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val body = JSONObject().apply {
                put("grant_type", "authorization_code")
                put("client_id", CLIENT_ID)
                put("client_secret", CLIENT_SECRET)
                put("redirect_uri", REDIRECT_URI)
                put("code", code)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val accessToken = json.getString("access_token")
                saveToken(accessToken)
                true
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                android.util.Log.e("AuthRepository", "Token exchange failed: $error")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Token exchange error", e)
            false
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val CLIENT_ID = "32893"
        private const val CLIENT_SECRET = "hf9cEmZnPXOLg9qvfLbaCT2THZcecbSnYnquEHC6"
        private const val REDIRECT_URI = "anisync://auth"
    }
}
