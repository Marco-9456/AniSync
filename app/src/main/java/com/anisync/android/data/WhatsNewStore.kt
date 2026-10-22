package com.anisync.android.data

import android.content.Context
import androidx.core.content.edit
import com.anisync.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Something an update added or moved that a returning user could otherwise miss. Add an entry in the
 * same change that ships the feature, with [sinceVersionCode] set to the release it ships in.
 */
enum class WhatsNewItem(val sinceVersionCode: Int, val kind: Kind, val place: Place = Place.SETTINGS) {
    SETTINGS_GENERAL(22, Kind.NEW),
    SETTINGS_LOOK_AND_FEEL(22, Kind.NEW),
    STORAGE_COVER_QUALITY(22, Kind.MOVED),
    GENERAL_MOVED_NOTICE(22, Kind.MOVED),
    LIBRARY_SEARCH_FILTERS(23, Kind.MOVED, Place.LIBRARY),
    DISCOVER_SEARCH_FILTERS(23, Kind.MOVED, Place.DISCOVER);

    enum class Kind { NEW, MOVED }

    /** The surface whose way in wears the dot, so the settings gear only points at settings. */
    enum class Place { SETTINGS, LIBRARY, DISCOVER }
}

/**
 * Decides which [WhatsNewItem]s are still worth pointing at.
 *
 * An item shows only to someone who updated from a build older than the one that added it, so a
 * fresh install never sees a page of badges. It stops once its place has been opened, and every item
 * expires [EXPIRY_DAYS] after the update whether or not it was opened.
 */
@Singleton
class WhatsNewStore @Inject constructor(
    @ApplicationContext context: Context,
    private val appSettings: AppSettings,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _active = MutableStateFlow(computeActive())
    val active: StateFlow<Set<WhatsNewItem>> = _active.asStateFlow()

    /**
     * Records the build this launch runs on. Has to run before [AppSettings.noteAppVersion], which
     * writes the current build on a fresh install and would make the install read as an update.
     */
    fun noteLaunch(now: Long = System.currentTimeMillis()) {
        val current = BuildConfig.VERSION_CODE
        val running = prefs.getInt(KEY_RUNNING, 0)
        if (running == current) return
        // Before this store existed the only record of the previous build is the support prompt's.
        val previous = if (running == 0) appSettings.lastSeenVersionCode() else running
        prefs.edit {
            putInt(KEY_RUNNING, current)
            putInt(KEY_BASELINE, if (previous == 0) current else previous)
            putLong(KEY_UPDATED_AT, now)
        }
        _active.value = computeActive(now)
    }

    fun markSeen(item: WhatsNewItem) {
        if (item !in _active.value) return
        val seen = prefs.getStringSet(KEY_SEEN, emptySet()).orEmpty() + item.name
        prefs.edit { putStringSet(KEY_SEEN, seen) }
        _active.value = _active.value - item
    }

    /** Shows every item again as if the update had just landed (Developer Tools). */
    fun reset(now: Long = System.currentTimeMillis()) {
        prefs.edit {
            putInt(KEY_BASELINE, 0)
            putLong(KEY_UPDATED_AT, now)
            remove(KEY_SEEN)
        }
        _active.value = computeActive(now)
    }

    private fun computeActive(now: Long = System.currentTimeMillis()): Set<WhatsNewItem> {
        val baseline = prefs.getInt(KEY_BASELINE, Int.MAX_VALUE)
        val updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L)
        if (now - updatedAt > TimeUnit.DAYS.toMillis(EXPIRY_DAYS)) return emptySet()
        val seen = prefs.getStringSet(KEY_SEEN, emptySet()).orEmpty()
        return WhatsNewItem.entries
            .filter { baseline < it.sinceVersionCode && it.name !in seen }
            .toSet()
    }

    private companion object {
        const val PREFS_NAME = "whats_new"
        const val KEY_RUNNING = "running_version_code"
        const val KEY_BASELINE = "baseline_version_code"
        const val KEY_UPDATED_AT = "updated_at"
        const val KEY_SEEN = "seen"
        const val EXPIRY_DAYS = 14L
    }
}
