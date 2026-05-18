package com.anisync.android.presentation.components.alert

import com.anisync.android.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToastManager @Inject constructor() {
    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    /**
     * True while a 429 rate-limit countdown toast is on screen. Pull-to-refresh
     * gates read this so users can't spam refresh while the AniList retry
     * window is still ticking. Flipped false by [clearToast] (which the
     * countdown auto-fires when it hits zero).
     */
    private val _isRateLimited = MutableStateFlow(false)
    val isRateLimited: StateFlow<Boolean> = _isRateLimited.asStateFlow()

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
        if (code == 429 && countdownSeconds != null && countdownSeconds > 0) {
            _isRateLimited.value = true
        }
        showToast(type, title, message, countdownSeconds)
    }

    fun clearToast() {
        _toast.value = null
        _isRateLimited.value = false
    }

    /**
     * Shows a toast for a [Result.Error], preserving both the HTTP status code
     * (so the correct icon/title render) and any `countdownSeconds`
     * (so 429 errors display a live timer and gate pull-to-refresh).
     *
     * Replaces the duplicated `if (code != null) showToast(code,…) else
     * showToast(INFO,…)` block that every ViewModel used to repeat.
     */
    fun showResultError(error: Result.Error) {
        if (error.code != null) {
            showToast(error.code, error.message, error.countdownSeconds)
        } else {
            showToast(ToastType.INFO, message = error.message)
        }
    }
}
