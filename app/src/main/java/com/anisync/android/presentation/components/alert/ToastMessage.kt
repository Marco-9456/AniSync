package com.anisync.android.presentation.components.alert

import java.util.concurrent.atomic.AtomicInteger

private val toastIdGenerator = AtomicInteger(0)

data class ToastMessage(
    val id: Int = toastIdGenerator.incrementAndGet(),
    val type: ToastType,
    val title: String?,
    val message: String,
    val countdownSeconds: Long? = null
)