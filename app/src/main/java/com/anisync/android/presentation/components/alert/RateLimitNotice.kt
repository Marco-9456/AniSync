package com.anisync.android.presentation.components.alert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.anisync.android.R
import com.anisync.android.data.network.RateLimitMonitor
import com.anisync.android.data.network.RateLimitStatus
import kotlinx.coroutines.flow.distinctUntilChanged

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
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(monitor, toastManager) {
        monitor.status
            .distinctUntilChanged { old, new -> old.sameNotice(new) }
            .collect { status ->
                when (status) {
                    is RateLimitStatus.Blocked -> toastManager.showToast(
                        code = 429,
                        message = context.getString(
                            R.string.api_error_rate_limited,
                            status.secondsRemaining,
                        ),
                        countdownSeconds = status.secondsRemaining,
                    )

                    is RateLimitStatus.Pacing -> toastManager.showThrottleNotice()

                    RateLimitStatus.Clear -> Unit
                }
            }
    }
}

/**
 * Whether two states would produce the same notice.
 *
 * [RateLimitStatus.Pacing] changes on every request as the count ticks down, and re-raising the
 * same notice for each of them would leave the user staring at a toast that never settles.
 */
private fun RateLimitStatus.sameNotice(other: RateLimitStatus): Boolean = when {
    this is RateLimitStatus.Blocked && other is RateLimitStatus.Blocked ->
        retryAtElapsedMs == other.retryAtElapsedMs

    this is RateLimitStatus.Pacing && other is RateLimitStatus.Pacing -> true
    else -> this == other
}
