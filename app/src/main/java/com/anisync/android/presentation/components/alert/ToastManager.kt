package com.anisync.android.presentation.components.alert

import android.content.Context
import android.os.SystemClock
import com.anisync.android.R
import com.anisync.android.data.network.findApiError
import com.anisync.android.data.util.ApiError
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
     * The last keyed toast the user swiped away.
     *
     * One entry is enough: keyed toasts are raised for a cause that is live at the time, and a new
     * cause carries a new key, so an older dismissal can never suppress it.
     */
    @Volatile private var dismissedKey: String? = null

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

    fun showToast(
        type: ToastType,
        title: String? = null,
        message: String,
        details: List<String> = emptyList(),
        overflow: Int = 0,
        countdown: ToastCountdown? = null,
        action: ToastAction? = null,
        key: String? = null,
    ) {
        _toast.value = ToastMessage(
            type = type,
            title = title,
            message = message,
            details = details,
            overflow = overflow,
            countdown = countdown,
            action = action,
            key = key,
        )
    }

    /** Takes the toast off screen. The cause, if there is one, is left to raise it again. */
    fun clearToast() {
        _toast.value = null
    }

    /**
     * The user swiped the toast away.
     *
     * Distinct from [clearToast] because a keyed toast is raised for as long as its cause lasts.
     * Swiping says the user has read it and wants the screen back, so it stays dismissed until the
     * cause itself changes, while a countdown simply running out does not.
     */
    fun dismissToast() {
        _toast.value?.key?.let { dismissedKey = it }
        clearToast()
    }

    /** True if the user swiped away the toast raised for [key]. */
    fun wasDismissed(key: String): Boolean = dismissedKey == key

    /**
     * Clears the toast only if it is a keyed one, which is how a cause that ends early takes its own
     * notice down. An ordinary toast that landed on top of it in the meantime is left alone.
     */
    fun clearKeyedToast() {
        if (_toast.value?.key != null) clearToast()
    }

    /**
     * Shows a toast for a [Result.Error].
     *
     * The typed [ApiError] is already sitting on [Result.Error.exception], put there by
     * `safeApiCall`, so reading it costs nothing and tells the toast far more than the HTTP status
     * did: which of AniList's failures this is, every field message a rejected mutation came back
     * with, and whether there is anything to wait for. The status code is the fallback for a failure
     * that never reached the classifier.
     */
    fun showResultError(error: Result.Error, action: ToastAction? = null) {
        val api = error.exception?.findApiError()
        val type = api?.let { ToastType.of(it) } ?: ToastType.fromCode(error.code)
        val strings = AppLocale.wrap(context)

        val details = (api as? ApiError.Validation)?.messages.orEmpty()
        showToast(
            type = type,
            title = titleFor(type),
            message = bodyFor(type, error, strings),
            details = details.take(MAX_FIELD_MESSAGES),
            overflow = (details.size - MAX_FIELD_MESSAGES).coerceAtLeast(0),
            countdown = (api as? ApiError.RateLimited)?.let { countdownOf(it.retryAfterSeconds) },
            action = action,
        )
    }

    /**
     * Raises the rate limit notice against the instant the block ends.
     *
     * Called from [RateLimitNotice], which owns the decision to raise it at all and re-raises it for
     * as long as the block lasts.
     */
    fun showRateLimit(retryAtElapsedMs: Long, totalSeconds: Long, key: String) {
        showToast(
            type = ToastType.RATE_LIMITED,
            title = titleFor(ToastType.RATE_LIMITED),
            message = AppLocale.wrap(context).getString(R.string.alert_rate_limited_body),
            countdown = ToastCountdown(retryAtElapsedMs, totalSeconds),
            key = key,
        )
    }

    /**
     * Brief, non-blocking notice that the app is deliberately pacing requests to stay under
     * AniList's rate limit. It explains a momentary slowdown instead of leaving the user on an
     * unexplained spinner (the "feels broken/slow" complaint). Self-throttled to at most once per
     * [THROTTLE_NOTICE_INTERVAL_MS], and suppressed while a keyed toast is up so it never clobbers
     * the rate limit countdown, which is the more important of the two.
     */
    fun showThrottleNotice() {
        if (_toast.value?.key != null) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastThrottleNoticeAt < THROTTLE_NOTICE_INTERVAL_MS) return
        lastThrottleNoticeAt = now
        // Read through AppLocale: this runs outside the composition, and the application context
        // keeps the system locale even after the app's own language has been set.
        showToast(
            ToastType.PACING,
            message = AppLocale.wrap(context).getString(R.string.alert_rate_limit_notice),
        )
    }

    /** The app's own title for a kind, also read by the developer tools preview. */
    fun titleFor(type: ToastType): String? {
        val res = when (type) {
            ToastType.RATE_LIMITED -> R.string.toast_title_rate_limited
            ToastType.OFFLINE -> R.string.toast_title_offline
            ToastType.TIMEOUT -> R.string.toast_title_timeout
            ToastType.SERVER_ERROR -> R.string.toast_title_server_error
            ToastType.VALIDATION_ERROR -> R.string.toast_title_validation
            ToastType.PERMISSION_DENIED -> R.string.toast_title_permission_denied
            ToastType.SESSION_EXPIRED -> R.string.toast_title_unauthorized
            ToastType.API_DISABLED -> R.string.toast_title_api_disabled
            ToastType.NOT_FOUND -> R.string.toast_title_not_found
            ToastType.ERROR -> R.string.toast_title_error
            // A strip has no room for one, and the two friendly kinds are titled by their caller.
            ToastType.PACING, ToastType.DEFERRED, ToastType.SUCCESS, ToastType.INFO -> null
        }
        return res?.let { AppLocale.wrap(context).getString(it) }
    }

    /**
     * The sentence under the title.
     *
     * Kinds AniList writes itself (a validation message, a permission reason, the API notice) keep
     * the server's words. The rest read better as a short line that does not repeat the title, which
     * `ApiErrorMessages` cannot do because it has to work as a standalone sentence elsewhere.
     */
    private fun bodyFor(type: ToastType, error: Result.Error, strings: Context): String =
        when (type) {
            ToastType.RATE_LIMITED -> strings.getString(R.string.alert_rate_limited_body)
            ToastType.DEFERRED -> strings.getString(R.string.alert_deferred_notice)
            ToastType.OFFLINE -> strings.getString(R.string.alert_offline_body)
            ToastType.TIMEOUT -> strings.getString(R.string.alert_timeout_body)
            else -> error.message
        }

    private fun countdownOf(seconds: Long): ToastCountdown? {
        if (seconds <= 0L) return null
        return ToastCountdown(SystemClock.elapsedRealtime() + seconds * 1_000, seconds)
    }

    private companion object {
        const val THROTTLE_NOTICE_INTERVAL_MS = 6_000L

        /** Enough to be useful without turning the toast into a form. The rest are counted. */
        const val MAX_FIELD_MESSAGES = 3
    }
}
