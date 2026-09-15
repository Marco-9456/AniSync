package com.anisync.android.presentation.components.alert

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.network.RateLimitMonitor
import com.anisync.android.data.network.RateLimitStatus
import kotlinx.coroutines.delay

/**
 * The rate limit state, made available to the composables that gate on it.
 *
 * Reading it from a composition local rather than from [ToastManager] is the point: the previous
 * code asked "is a rate limit toast on screen", which is a question about the UI, and answered "no"
 * the moment the user swiped the toast away even though AniList was still refusing requests.
 */
val LocalRateLimitMonitor = staticCompositionLocalOf<RateLimitMonitor?> { null }

/**
 * Turns [RateLimitMonitor] into toasts.
 *
 * This is the only place the network layer's state meets the UI. The interceptor used to hold a
 * `ToastManager` and build its own English sentences, which put a presentation type in the data
 * layer's dependency graph and made the limiter impossible to unit test.
 *
 * The notice lasts as long as the block does, not as long as one countdown. A countdown is seeded
 * from the seconds left when the status was published, so a second 429 extending the window, or a
 * `delay` stretched by Doze, used to leave the toast gone while pull-to-refresh was still swallowing
 * gestures: a dead gesture with nothing on screen to explain it. The deadline is the truth, so the
 * notice is re-raised against it until it passes.
 */
@Composable
fun RateLimitNotice(monitor: RateLimitMonitor, toastManager: ToastManager) {
    val status by monitor.status.collectAsStateWithLifecycle()
    // Through the composition, so the message follows the language chosen in the app rather than the
    // one the device is set to.
    val context = LocalContext.current

    when (val current = status) {
        is RateLimitStatus.Blocked -> {
            // Keyed on the deadline, so one timeout raises one toast however many requests bounce
            // off it, and a later deadline starts its own. The previous interceptor raised one per
            // blocked caller.
            LaunchedEffect(current.retryAtElapsedMs) {
                val key = "$RATE_LIMIT_KEY:${current.retryAtElapsedMs}"
                while (!toastManager.wasDismissed(key)) {
                    val remaining = current.secondsLeft()
                    if (remaining <= 0L) break
                    toastManager.showToast(
                        code = 429,
                        message = context.getString(R.string.api_error_rate_limited, remaining),
                        countdownSeconds = remaining,
                        key = key,
                    )
                    // Wake just after the toast has counted itself out. Still blocked means raise it
                    // again, against the seconds actually left rather than the ones it started with.
                    delay(remaining * 1_000 + RERAISE_GRACE_MS)
                }
            }
        }

        // Pacing ticks with every request, so the effect is keyed on the state being Pacing at all.
        // ToastManager throttles the notice itself on top of that.
        is RateLimitStatus.Pacing -> LaunchedEffect(Unit) { toastManager.showThrottleNotice() }

        // The window can roll over sooner than the countdown said, and a notice for a block that is
        // over has nothing to tell the user.
        RateLimitStatus.Clear -> LaunchedEffect(Unit) { toastManager.clearKeyedToast() }
    }
}

/** Seconds left on the block right now, as opposed to when the status was published. */
private fun RateLimitStatus.Blocked.secondsLeft(): Long {
    val leftMs = retryAtElapsedMs - SystemClock.elapsedRealtime()
    return if (leftMs <= 0L) 0L else (leftMs + 999) / 1_000
}

private const val RATE_LIMIT_KEY = "rate-limit"

/** Covers the drift a one-second-at-a-time countdown accumulates over a minute. */
private const val RERAISE_GRACE_MS = 1_000L
