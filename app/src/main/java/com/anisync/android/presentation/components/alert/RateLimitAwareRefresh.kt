package com.anisync.android.presentation.components.alert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Wraps a pull-to-refresh `onRefresh` callback so that gestures triggered
 * while a 429 toast countdown is active become no-ops. The toast is already
 * on screen with a live countdown, so the user has the feedback they need —
 * silently dropping the refresh avoids piling up more rate-limited requests
 * and prevents the spinner from re-arming behind the toast.
 */
@Composable
fun rememberRateLimitedRefresh(onRefresh: () -> Unit): () -> Unit {
    val toastManager = LocalToastManager.current
    val isRateLimited by toastManager.isRateLimited.collectAsStateWithLifecycle()
    return remember(isRateLimited, onRefresh) {
        {
            if (!isRateLimited) onRefresh()
        }
    }
}
