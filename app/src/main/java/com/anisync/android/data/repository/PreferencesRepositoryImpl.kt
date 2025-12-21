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

    companion object {
        private const val PREFS_NAME = "anisync_prefs"
        private const val KEY_LAST_NOTIFIED_ID = "last_notified_id"
        private const val KEY_NOTIFIED_PLANNING = "notified_planning_media_ids"
    }
}
