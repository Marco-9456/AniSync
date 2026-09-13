package com.anisync.android.data.network

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers an active 429 timeout across process death.
 *
 * AniList's penalty for exceeding the window is a full minute, and a force stop or a low-memory
 * kill in the middle of it used to hand the next launch a clean slate that walked straight back
 * into the limiter. This is the only piece of rate limit state worth persisting: everything else is
 * re-learned from the first response's headers.
 */
interface RateLimitPersistence {

    /** Milliseconds still to wait, or 0 when there is no stored timeout or it has passed. */
    fun readBlockedForMs(): Long

    /** Stores a timeout that has [remainingMs] left to run. Zero or less clears it. */
    fun saveBlockedFor(remainingMs: Long)

    /** Drops any stored timeout without waiting for it to expire. */
    fun clear()

    /** For tests and for any build that would rather not keep the state. */
    object None : RateLimitPersistence {
        override fun readBlockedForMs(): Long = 0
        override fun saveBlockedFor(remainingMs: Long) = Unit
        override fun clear() = Unit
    }
}

/**
 * Wall clock backed store.
 *
 * The runtime gate works in monotonic time, which resets when the device reboots, so the deadline
 * has to be written as a wall clock instant and converted back on read. A clock that jumped
 * backwards would make the stored deadline look far away, so the value read back is capped at the
 * longest timeout AniList can hand out.
 */
@Singleton
class SharedPreferencesRateLimitPersistence @Inject constructor(
    @ApplicationContext context: Context,
) : RateLimitPersistence {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun readBlockedForMs(): Long {
        val deadline = prefs.getLong(KEY_BLOCKED_UNTIL, 0L)
        if (deadline <= 0L) return 0L
        val remaining = deadline - System.currentTimeMillis()
        if (remaining <= 0L) {
            clear()
            return 0L
        }
        return remaining.coerceAtMost(RateLimitWindow.MAX_RETRY_AFTER_SECONDS * 1000L)
    }

    override fun saveBlockedFor(remainingMs: Long) {
        if (remainingMs <= 0L) {
            clear()
            return
        }
        prefs.edit { putLong(KEY_BLOCKED_UNTIL, System.currentTimeMillis() + remainingMs) }
    }

    override fun clear() {
        prefs.edit { remove(KEY_BLOCKED_UNTIL) }
    }

    private companion object {
        const val FILE = "rate_limit"
        const val KEY_BLOCKED_UNTIL = "blocked_until"
    }
}
