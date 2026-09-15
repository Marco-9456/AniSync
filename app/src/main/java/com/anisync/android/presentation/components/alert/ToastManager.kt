package com.anisync.android.presentation.components.alert

import android.content.Context
import android.os.SystemClock
import com.anisync.android.R
import com.anisync.android.data.util.AppLocale
import com.anisync.android.domain.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToastManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
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
        val titleRes = when (code) {
            400 -> R.string.toast_title_validation
            401 -> R.string.toast_title_unauthorized
            404 -> R.string.toast_title_not_found
            429 -> R.string.toast_title_rate_limited
            500 -> R.string.toast_title_server_error
            else -> null
        }
        val title = titleRes?.let { AppLocale.wrap(context).getString(it) }
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
        // Read through AppLocale: this runs outside the composition, and the application context
        // keeps the system locale even after the app's own language has been set.
        showToast(
            ToastType.INFO,
            message = AppLocale.wrap(context).getString(R.string.alert_rate_limit_notice),
        )
    }

    private companion object {
        const val THROTTLE_NOTICE_INTERVAL_MS = 6_000L
    }
}
