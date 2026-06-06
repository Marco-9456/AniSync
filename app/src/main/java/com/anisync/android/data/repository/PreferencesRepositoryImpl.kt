package com.anisync.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.anisync.android.domain.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun getLastNotifiedId(): Int = withContext(Dispatchers.IO) {
        prefs.getInt(KEY_LAST_NOTIFIED_ID, 0)
    }

    override suspend fun setLastNotifiedId(id: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(KEY_LAST_NOTIFIED_ID, id).apply()
    }

    override suspend fun getNotifiedPlanningMediaIds(): Set<Int> = withContext(Dispatchers.IO) {
        prefs.getStringSet(KEY_NOTIFIED_PLANNING, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }

    override suspend fun markPlanningMediaAsNotified(mediaId: Int) = withContext(Dispatchers.IO) {
        val current = prefs.getStringSet(KEY_NOTIFIED_PLANNING, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(mediaId.toString())
        prefs.edit().putStringSet(KEY_NOTIFIED_PLANNING, current).apply()
    }

    override suspend fun cleanupOrphanedPlanningIds(currentPlanningIds: Set<Int>) = withContext(Dispatchers.IO) {
        val notifiedIds = prefs.getStringSet(KEY_NOTIFIED_PLANNING, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
        
        // Keep only IDs that are still in the planning list
        val validIds = notifiedIds.intersect(currentPlanningIds)
        
        if (validIds.size != notifiedIds.size) {
            prefs.edit()
                .putStringSet(KEY_NOTIFIED_PLANNING, validIds.map { it.toString() }.toSet())
                .apply()
        }
    }

    // ---- Upcoming airing notifications ----

    override suspend fun getNotifiedUpcomingAiringIds(): Set<Int> = withContext(Dispatchers.IO) {
        prefs.getStringSet(KEY_NOTIFIED_UPCOMING, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }

    override suspend fun markUpcomingAiringNotified(airingId: Int) = withContext(Dispatchers.IO) {
        val current = prefs.getStringSet(KEY_NOTIFIED_UPCOMING, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(airingId.toString())
        prefs.edit().putStringSet(KEY_NOTIFIED_UPCOMING, current).apply()
    }

    override suspend fun cleanupOldUpcomingAirings(currentValidIds: Set<Int>) = withContext(Dispatchers.IO) {
        val notifiedIds = prefs.getStringSet(KEY_NOTIFIED_UPCOMING, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
        
        // Keep only IDs that are still valid (upcoming)
        val validIds = notifiedIds.intersect(currentValidIds)
        
        if (validIds.size != notifiedIds.size) {
            prefs.edit()
                .putStringSet(KEY_NOTIFIED_UPCOMING, validIds.map { it.toString() }.toSet())
                .apply()
        }
    }

    override suspend fun hasNotificationsEverRun(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(KEY_HAS_EVER_RUN, false)
    }

    override suspend fun markNotificationsHaveRun() = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_HAS_EVER_RUN, true).apply()
    }

    // ---- Key-based notification tracking for two-tier system ----

    override suspend fun hasNotifiedWithKey(key: String): Boolean = withContext(Dispatchers.IO) {
        val keys = prefs.getStringSet(KEY_NOTIFICATION_KEYS, emptySet()) ?: emptySet()
        key in keys
    }

    override suspend fun markNotifiedWithKey(key: String) = withContext(Dispatchers.IO) {
        val current = prefs.getStringSet(KEY_NOTIFICATION_KEYS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(key)
        
        // Clean up old keys to prevent unbounded growth
        // Keep only keys from the last 7 days worth of airings (assuming ~50 per day max)
        if (current.size > MAX_NOTIFICATION_KEYS) {
            val keysToKeep = current.toList().takeLast(MAX_NOTIFICATION_KEYS).toSet()
            prefs.edit().putStringSet(KEY_NOTIFICATION_KEYS, keysToKeep).apply()
        } else {
            prefs.edit().putStringSet(KEY_NOTIFICATION_KEYS, current).apply()
        }
    }

    // ---- Social/Forum notification tracking ----

    override suspend fun getLastSocialNotifiedId(): Int = withContext(Dispatchers.IO) {
        prefs.getInt(KEY_LAST_SOCIAL_NOTIFIED_ID, 0)
    }

    override suspend fun setLastSocialNotifiedId(id: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(KEY_LAST_SOCIAL_NOTIFIED_ID, id).apply()
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "anisync_prefs"
        private const val KEY_LAST_NOTIFIED_ID = "last_notified_id"
        private const val KEY_NOTIFIED_PLANNING = "notified_planning_media_ids"
        private const val KEY_NOTIFIED_UPCOMING = "notified_upcoming_airing_ids"
        private const val KEY_HAS_EVER_RUN = "notifications_have_ever_run"
        private const val KEY_NOTIFICATION_KEYS = "notification_keys"
        private const val KEY_LAST_SOCIAL_NOTIFIED_ID = "last_social_notified_id"
        private const val MAX_NOTIFICATION_KEYS = 350 // ~50 notifications per day * 7 days
    }
}
