package com.anisync.android.presentation.components.alert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.network.RateLimitMonitor
import com.anisync.android.data.network.RateLimitStatus

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
 */
@Composable
fun RateLimitNotice(monitor: RateLimitMonitor, toastManager: ToastManager) {
    val status by monitor.status.collectAsStateWithLifecycle()

    when (val current = status) {
        is RateLimitStatus.Blocked -> {
            val message = stringResource(
                R.string.api_error_rate_limited,
                current.secondsRemaining,
            )
            // Keyed on the deadline, so one timeout raises one toast however many requests bounce
            // off it. The previous interceptor raised one per blocked caller.
            LaunchedEffect(current.retryAtElapsedMs) {
                toastManager.showToast(
                    code = 429,
                    message = message,
                    countdownSeconds = current.secondsRemaining,
                )
            }
        }

        // Pacing ticks with every request, so the effect is keyed on the state being Pacing at all.
        // ToastManager throttles the notice itself on top of that.
        is RateLimitStatus.Pacing -> LaunchedEffect(Unit) { toastManager.showThrottleNotice() }

        RateLimitStatus.Clear -> Unit
    }
}
