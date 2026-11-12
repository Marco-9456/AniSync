package com.anisync.android.data

import android.content.Context
import androidx.core.content.edit
import com.anisync.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The support card's state across launches, applying [SupportPromptRules]. Main thread only, like
 * the screens that drive it.
 */
@Singleton
class SupportPromptStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    appSettings: AppSettings,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var state = read(legacyVersionCode = appSettings.lastSeenVersionCode())

    private val _visible = MutableStateFlow(false)

    /** True while the card is up. */
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(state.remindersOn)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    /** Arms the card when this launch is the first on a feature update. */
    fun noteLaunch(now: Long = System.currentTimeMillis()) {
        save(SupportPromptRules.onLaunch(state, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, installedAt(), now))
    }

    /** A list edit reached AniList, the moment an armed card waits for. */
    fun onListSaved() {
        if (SupportPromptRules.canShow(state)) _visible.value = true
    }

    fun onNotNow(now: Long = System.currentTimeMillis()) {
        _visible.value = false
        save(SupportPromptRules.onNotNow(state, now))
    }

    fun onTipOpened(now: Long = System.currentTimeMillis()) {
        _visible.value = false
        save(SupportPromptRules.onTipOpened(state, now))
    }

    fun setRemindersEnabled(enabled: Boolean) {
        if (!enabled) _visible.value = false
        save(SupportPromptRules.withReminders(state, enabled))
    }

    /** Raises the card now, without an update behind it (Developer Tools). */
    fun show() {
        _visible.value = true
    }

    /** Arms the card for the next list edit, past every wait (Developer Tools). */
    fun arm() {
        save(state.copy(pending = true, quietUntil = 0L, notNowStreak = 0, remindersOn = true))
    }

    private fun installedAt(): Long = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
    }.getOrDefault(0L)

    private fun read(legacyVersionCode: Int) = SupportPromptState(
        // Before this store existed the old prompt kept the last build, so an update from one of
        // those builds still reads as an update and not as a fresh install.
        versionCode = prefs.getInt(KEY_VERSION_CODE, legacyVersionCode),
        versionName = prefs.getString(KEY_VERSION_NAME, null),
        pending = prefs.getBoolean(KEY_PENDING, false),
        quietUntil = prefs.getLong(KEY_QUIET_UNTIL, 0L),
        notNowStreak = prefs.getInt(KEY_NOT_NOW_STREAK, 0),
        remindersOn = prefs.getBoolean(KEY_REMINDERS, true),
    )

    private fun save(next: SupportPromptState) {
        if (next == state) return
        state = next
        _remindersEnabled.value = next.remindersOn
        prefs.edit {
            putInt(KEY_VERSION_CODE, next.versionCode)
            putString(KEY_VERSION_NAME, next.versionName)
            putBoolean(KEY_PENDING, next.pending)
            putLong(KEY_QUIET_UNTIL, next.quietUntil)
            putInt(KEY_NOT_NOW_STREAK, next.notNowStreak)
            putBoolean(KEY_REMINDERS, next.remindersOn)
        }
    }

    private companion object {
        const val PREFS_NAME = "support_prompt"
        const val KEY_VERSION_CODE = "version_code"
        const val KEY_VERSION_NAME = "version_name"
        const val KEY_PENDING = "pending"
        const val KEY_QUIET_UNTIL = "quiet_until"
        const val KEY_NOT_NOW_STREAK = "not_now_streak"
        const val KEY_REMINDERS = "reminders_on"
    }
}
