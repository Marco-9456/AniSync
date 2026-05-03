package com.anisync.android.presentation.components.alert

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToastManager @Inject constructor() {
    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    fun showToast(type: ToastType, title: String? = null, message: String, countdownSeconds: Long? = null) {
        _toast.value = ToastMessage(type = type, title = title, message = message, countdownSeconds = countdownSeconds)
    }

    fun showToast(code: Int, message: String, countdownSeconds: Long? = null) {
        val type = ToastType.fromCode(code)
        val title = when(code) {
            400 -> "Validation Error"
            401 -> "Unauthorized"
            404 -> "Not Found"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            else -> null
        }
        showToast(type, title, message, countdownSeconds)
    }

    fun clearToast() {
        _toast.value = null
    }
}