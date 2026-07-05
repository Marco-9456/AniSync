package com.anisync.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.anisync.android.domain.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Notification dedup state in plain SharedPreferences, keyed per account (`<base>_<accountId>`) so
 * each signed-in account tracks its own high-water marks independently.
 */
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun key(base: String, accountId: Int) = "${base}_$accountId"

    override suspend fun getLastNotifiedId(accountId: Int): Int = withContext(Dispatchers.IO) {
        prefs.getInt(key(KEY_LAST_NOTIFIED_ID, accountId), 0)
    }

    override suspend fun setLastNotifiedId(accountId: Int, id: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(key(KEY_LAST_NOTIFIED_ID, accountId), id).apply()
    }

    override suspend fun getNotifiedPlanningMediaIds(accountId: Int): Set<Int> = withContext(Dispatchers.IO) {
        prefs.getStringSet(key(KEY_NOTIFIED_PLANNING, accountId), emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }

    override suspend fun markPlanningMediaAsNotified(accountId: Int, mediaId: Int) = withContext(Dispatchers.IO) {
        val k = key(KEY_NOTIFIED_PLANNING, accountId)
        val current = prefs.getStringSet(k, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(mediaId.toString())
        prefs.edit().putStringSet(k, current).apply()
    }

    override suspend fun cleanupOrphanedPlanningIds(accountId: Int, currentPlanningIds: Set<Int>) = withContext(Dispatchers.IO) {
        val k = key(KEY_NOTIFIED_PLANNING, accountId)
        val notifiedIds = prefs.getStringSet(k, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()

        // Keep only IDs that are still in the planning list
        val validIds = notifiedIds.intersect(currentPlanningIds)

        if (validIds.size != notifiedIds.size) {
            prefs.edit()
                .putStringSet(k, validIds.map { it.toString() }.toSet())
                .apply()
        }
    }

    // ---- Upcoming airing notifications ----

    override suspend fun getNotifiedUpcomingAiringIds(accountId: Int): Set<Int> = withContext(Dispatchers.IO) {
        prefs.getStringSet(key(KEY_NOTIFIED_UPCOMING, accountId), emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }

    override suspend fun markUpcomingAiringNotified(accountId: Int, airingId: Int) = withContext(Dispatchers.IO) {
        val k = key(KEY_NOTIFIED_UPCOMING, accountId)
        val current = prefs.getStringSet(k, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(airingId.toString())
        prefs.edit().putStringSet(k, current).apply()
    }

    override suspend fun cleanupOldUpcomingAirings(accountId: Int, currentValidIds: Set<Int>) = withContext(Dispatchers.IO) {
        val k = key(KEY_NOTIFIED_UPCOMING, accountId)
        val notifiedIds = prefs.getStringSet(k, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()

        // Keep only IDs that are still valid (upcoming)
        val validIds = notifiedIds.intersect(currentValidIds)

        if (validIds.size != notifiedIds.size) {
            prefs.edit()
                .putStringSet(k, validIds.map { it.toString() }.toSet())
                .apply()
        }

        // Prune the advance_/imminent_ dedup keys of airings that left the upcoming window too.
        // They're never consulted again once the airing is out of the window, and without this the
        // set only shrinks via the size cap, which evicts arbitrary entries (StringSet is unordered)
        // and can re-fire alerts whose key got dropped.
        val keysKey = key(KEY_NOTIFICATION_KEYS, accountId)
        val notificationKeys = prefs.getStringSet(keysKey, emptySet()) ?: emptySet()
        val validKeys = notificationKeys.filterTo(mutableSetOf()) { entry ->
            entry.substringAfterLast('_').toIntOrNull() in currentValidIds
        }
        if (validKeys.size != notificationKeys.size) {
            prefs.edit().putStringSet(keysKey, validKeys).apply()
        }
    }

    override suspend fun hasNotificationsEverRun(accountId: Int): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(key(KEY_HAS_EVER_RUN, accountId), false)
    }

    override suspend fun markNotificationsHaveRun(accountId: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(key(KEY_HAS_EVER_RUN, accountId), true).apply()
    }

    // ---- Key-based notification tracking for two-tier system ----

    override suspend fun hasNotifiedWithKey(accountId: Int, notificationKey: String): Boolean = withContext(Dispatchers.IO) {
        val keys = prefs.getStringSet(key(KEY_NOTIFICATION_KEYS, accountId), emptySet()) ?: emptySet()
        notificationKey in keys
    }

    override suspend fun markNotifiedWithKey(accountId: Int, notificationKey: String) = withContext(Dispatchers.IO) {
        val storeKey = key(KEY_NOTIFICATION_KEYS, accountId)
        val current = prefs.getStringSet(storeKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(notificationKey)

        // Clean up old keys to prevent unbounded growth
        if (current.size > MAX_NOTIFICATION_KEYS) {
            val keysToKeep = current.toList().takeLast(MAX_NOTIFICATION_KEYS).toSet()
            prefs.edit().putStringSet(storeKey, keysToKeep).apply()
        } else {
            prefs.edit().putStringSet(storeKey, current).apply()
        }
    }

    // ---- Social/Forum notification tracking ----

    override suspend fun getLastSocialNotifiedId(accountId: Int): Int = withContext(Dispatchers.IO) {
        prefs.getInt(key(KEY_LAST_SOCIAL_NOTIFIED_ID, accountId), 0)
    }

    override suspend fun setLastSocialNotifiedId(accountId: Int, id: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(key(KEY_LAST_SOCIAL_NOTIFIED_ID, accountId), id).apply()
    }

    override suspend fun clearForAccount(accountId: Int) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            remove(key(KEY_LAST_NOTIFIED_ID, accountId))
            remove(key(KEY_NOTIFIED_PLANNING, accountId))
            remove(key(KEY_NOTIFIED_UPCOMING, accountId))
            remove(key(KEY_HAS_EVER_RUN, accountId))
            remove(key(KEY_NOTIFICATION_KEYS, accountId))
            remove(key(KEY_LAST_SOCIAL_NOTIFIED_ID, accountId))
        }.apply()
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
