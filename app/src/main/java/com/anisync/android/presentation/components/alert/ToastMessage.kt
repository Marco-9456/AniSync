package com.anisync.android.presentation.components.alert

import java.util.concurrent.atomic.AtomicInteger

private val toastIdGenerator = AtomicInteger(0)

data class ToastMessage(
    val id: Int = toastIdGenerator.incrementAndGet(),
    val type: ToastType,
    val title: String?,
    val message: String,
    val countdownSeconds: Long? = null,
    /**
     * Identity of the underlying cause, stable across re-raises of the same one.
     *
     * A toast raised for something that outlives its own countdown is re-raised until the cause
     * clears, and a swipe on a keyed toast has to stay dismissed rather than come straight back.
     * Only [RateLimitNotice] sets this today.
     */
    val key: String? = null,
)
