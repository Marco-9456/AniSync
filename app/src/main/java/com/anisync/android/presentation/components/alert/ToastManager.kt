package com.anisync.android.presentation.components.alert

import android.os.SystemClock
import com.anisync.android.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToastManager @Inject constructor() {
    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    /**
     * True while a 429 rate-limit countdown toast is on screen. Pull-to-refresh
     * gates read this so users can't spam refresh while the AniList retry
     * window is still ticking. Flipped false by [clearToast] (which the
     * countdown auto-fires when it hits zero).
     */
    private val _isRateLimited = MutableStateFlow(false)
    val isRateLimited: StateFlow<Boolean> = _isRateLimited.asStateFlow()

    /** Last time a throttle notice was shown; rate-limits the notice itself. */
    @Volatile private var lastThrottleNoticeAt: Long = 0L

    /**
     * True while a modal surface (bottom sheet / dialog) is hosting its own
     * [com.anisync.android.presentation.components.alert.OverlayToastHost]. Those
     * render in a platform window *above* the app window, so the global
     * [com.anisync.android.presentation.components.alert.TopToastHost] popup would
     * land behind their scrim. While this is true the global host stands down and
     * the modal's own host draws the toast above the scrim.
     */
    private val _overlayHostActive = MutableStateFlow(false)
    val overlayHostActive: StateFlow<Boolean> = _overlayHostActive.asStateFlow()
    private var overlayHostCount = 0

    @Synchronized
    fun acquireOverlayHost() {
        overlayHostCount++
        _overlayHostActive.value = overlayHostCount > 0
    }

    @Synchronized
    fun releaseOverlayHost() {
        overlayHostCount = (overlayHostCount - 1).coerceAtLeast(0)
        _overlayHostActive.value = overlayHostCount > 0
    }

    fun showToast(type: ToastType, title: String? = null, message: String, countdownSeconds: Long? = null) {
        _toast.value = ToastMessage(type = type, title = title, message = message, countdownSeconds = countdownSeconds)
    }

    fun showToast(code: Int, message: String, countdownSeconds: Long? = null) {
        val type = ToastType.fromCode(code)
        val title = when(code) {
            400 -> "Validation Error"
            401 -> "Unauthorized"
            404 -> "Not Found"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            else -> null
        }
        if (code == 429 && countdownSeconds != null && countdownSeconds > 0) {
            _isRateLimited.value = true
        }
        showToast(type, title, message, countdownSeconds)
    }

    fun clearToast() {
        _toast.value = null
        _isRateLimited.value = false
    }

    /**
     * Shows a toast for a [Result.Error], preserving both the HTTP status code
     * (so the correct icon/title render) and any `countdownSeconds`
     * (so 429 errors display a live timer and gate pull-to-refresh).
     *
     * Replaces the duplicated `if (code != null) showToast(code,…) else
     * showToast(INFO,…)` block that every ViewModel used to repeat.
     */
    fun showResultError(error: Result.Error) {
        if (error.code != null) {
            showToast(error.code, error.message, error.countdownSeconds)
        } else {
            showToast(ToastType.INFO, message = error.message)
        }
    }

    /**
     * Brief, non-blocking notice that the app is deliberately pacing requests to
     * stay under AniList's rate limit. Unlike the 429 countdown toast it does NOT
     * set [isRateLimited], so it never gates pull-to-refresh — it just explains a
     * momentary slowdown instead of leaving the user on an unexplained spinner
     * (the "feels broken/slow" complaint). Self-throttled to at most once per
     * [THROTTLE_NOTICE_INTERVAL_MS], and suppressed while a 429 countdown is up so
     * it never clobbers the more important rate-limit toast.
     */
    fun showThrottleNotice() {
        if (_isRateLimited.value) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastThrottleNoticeAt < THROTTLE_NOTICE_INTERVAL_MS) return
        lastThrottleNoticeAt = now
        showToast(ToastType.INFO, message = "Slowing down to stay within AniList's rate limit…")
    }

    private companion object {
        const val THROTTLE_NOTICE_INTERVAL_MS = 6_000L
    }
}
