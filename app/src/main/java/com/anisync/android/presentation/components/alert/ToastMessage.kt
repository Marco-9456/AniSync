package com.anisync.android.presentation.components.alert

import java.util.concurrent.atomic.AtomicInteger

private val toastIdGenerator = AtomicInteger(0)

/**
 * A wait the toast counts down.
 *
 * The deadline, not a number of seconds, because the seconds are only true at the moment they are
 * read. A toast seeded with a count and left to decrement drifts, and keeps ticking after the wait
 * it describes has been extended or cut short.
 *
 * @param retryAtElapsedMs monotonic instant the wait ends, comparable to
 *   [android.os.SystemClock.elapsedRealtime]
 * @param totalSeconds the whole wait, so the progress track has something to be a fraction of
 */
data class ToastCountdown(
    val retryAtElapsedMs: Long,
    val totalSeconds: Long,
)

/** A button on the toast. [filled] marks the one thing the user is being asked to do. */
data class ToastAction(
    val label: String,
    val filled: Boolean = false,
    val onClick: () -> Unit,
)

data class ToastMessage(
    val id: Int = toastIdGenerator.incrementAndGet(),
    val type: ToastType,
    val title: String?,
    val message: String,
    /**
     * One line per rejected field, from AniList's `validation` object. These are written to be read
     * by the user, and the app used to keep only the first.
     */
    val details: List<String> = emptyList(),
    /** How many further [details] were left out, for the "N more" line. */
    val overflow: Int = 0,
    val countdown: ToastCountdown? = null,
    val action: ToastAction? = null,
    /**
     * Identity of the underlying cause, stable across re-raises of the same one.
     *
     * A toast raised for something that outlives its own countdown is re-raised until the cause
     * clears, and a swipe on a keyed toast has to stay dismissed rather than come straight back.
     * Only [RateLimitNotice] sets this today.
     */
    val key: String? = null,
)
