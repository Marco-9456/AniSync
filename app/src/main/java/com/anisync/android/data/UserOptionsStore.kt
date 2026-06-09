package com.anisync.android.data

import android.content.Context
import com.anisync.android.domain.AniListUserOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-account on-device cache of [AniListUserOptions], persisted as JSON in SharedPreferences and
 * keyed by AniList account id. Mirrors the JSON-blob persistence pattern used for typography
 * overrides in [AppSettings]; one small blob per account avoids a migration when the option set grows.
 */
@Singleton
class UserOptionsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun read(accountId: Int): AniListUserOptions? {
        val raw = prefs.getString(key(accountId), null) ?: return null
        return runCatching { json.decodeFromString<AniListUserOptions>(raw) }.getOrNull()
    }

    fun write(accountId: Int, options: AniListUserOptions) {
        prefs.edit().putString(key(accountId), json.encodeToString(options)).apply()
    }

    fun clear(accountId: Int) {
        prefs.edit().remove(key(accountId)).apply()
    }

    private fun key(accountId: Int): String = "options_$accountId"

    companion object {
        private const val PREFS_NAME = "anisync_user_options"
    }
}
