package com.anisync.android.presentation.components.alert

import java.util.UUID

data class ToastMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: ToastType,
    val title: String?,
    val message: String,
    val countdownSeconds: Long? = null
)
