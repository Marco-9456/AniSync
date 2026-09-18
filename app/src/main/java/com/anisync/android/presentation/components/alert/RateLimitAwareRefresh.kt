package com.anisync.android.presentation.components.alert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.data.network.RateLimitStatus
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Wraps a pull-to-refresh callback so gestures made while AniList is refusing requests become
 * no-ops. The countdown toast is already on screen, so silently dropping the gesture avoids piling
 * up requests that cannot succeed and stops the spinner re-arming behind the toast.
 *
 * The gate is the server's timeout, not the toast. Keying it on the toast, as this did before, let
 * a swipe-to-dismiss re-open the floodgates while the timeout still had fifty seconds to run.
 */
@Composable
fun rememberRateLimitedRefresh(onRefresh: () -> Unit): () -> Unit {
    val monitor = LocalRateLimitMonitor.current
    val status by (monitor?.status ?: remember { MutableStateFlow(RateLimitStatus.Clear) })
        .collectAsStateWithLifecycle()
    val blocked = status is RateLimitStatus.Blocked
    return remember(blocked, onRefresh) {
        {
            if (!blocked) onRefresh()
        }
    }
}
