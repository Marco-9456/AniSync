package com.anisync.android.presentation.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Thin wrapper over AndroidX Biometric that authenticates the user with **whatever screen lock the
 * phone already uses** — a Class 2 (weak) biometric *or* the device credential (PIN / pattern /
 * password). The system UI picks the method and enforces its own rules; we never store or verify
 * anything ourselves. Modelled on Mihon's app-lock helper.
 */
object AppLockAuthenticator {

    /** Class-2 biometric OR device credential — i.e. any lock the user has set up. */
    private val authenticators = Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL

    /** True when a usable screen lock exists (a biometric is enrolled, or a PIN/pattern/password). */
    fun Context.isAppLockSupported(): Boolean =
        BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows the system unlock prompt. [onSuccess] fires once the user proves it's them; [onError]
     * fires on an unrecoverable error or when they dismiss it (per-attempt failures are handled by
     * the system UI and don't call back here).
     */
    fun FragmentActivity.authenticateForAppLock(
        title: String,
        subtitle: String?,
        onSuccess: () -> Unit,
        onError: (CharSequence) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                    onSuccess()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    onError(errString)
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            // The prompt itself already stands between the user and the app; a second confirmation tap
            // after a passive face/fingerprint match just adds friction, so skip it.
            .setConfirmationRequired(false)
            .build()
        prompt.authenticate(promptInfo)
    }
}

/** Unwraps the [FragmentActivity] backing a Compose [Context], or null if there isn't one. */
fun Context.findFragmentActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}
