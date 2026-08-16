package com.anisync.android.presentation.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.startClass2BiometricOrCredentialAuthentication
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Thin wrapper over AndroidX Biometric that authenticates the user with **whatever screen lock the
 * phone already uses** — a Class 2 (weak) biometric *or* the device credential (PIN / pattern /
 * password). The system UI picks the method and enforces its own rules; we never store or verify
 * anything ourselves. Modelled on Mihon's app-lock helper.
 */
object AppLockAuthenticator {

    private val authenticators = Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
    private val biometricAuthenticators = Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK

    /** True when a usable screen lock exists (a biometric is enrolled, or a PIN/pattern/password). */
    fun Context.isAppLockSupported(): Boolean =
        BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    /** True when biometric hardware and enrolled biometrics are available. */
    fun Context.isBiometricSupported(): Boolean =
        BiometricManager.from(this).canAuthenticate(biometricAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows a biometric-only unlock prompt with a fallback negative button (e.g. "Use Password").
     */
    fun FragmentActivity.authenticateWithBiometrics(
        title: String,
        subtitle: String?,
        negativeButtonText: String = "Use Password",
        onSuccess: () -> Unit,
        onError: (CharSequence) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(biometricAuthenticators)
            .setConfirmationRequired(false)
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString)
                }
            }
        )
        biometricPrompt.authenticate(promptInfo)
    }

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
        startClass2BiometricOrCredentialAuthentication(
            title = title,
            subtitle = subtitle,
            // The prompt itself already stands between the user and the app; a second confirmation tap
            // after a passive face/fingerprint match just adds friction, so skip it.
            confirmationRequired = false,
            executor = ContextCompat.getMainExecutor(this),
            callback = object : AuthPromptCallback() {
                override fun onAuthenticationSucceeded(
                    activity: FragmentActivity?,
                    result: BiometricPrompt.AuthenticationResult,
                ) = onSuccess()

                override fun onAuthenticationError(
                    activity: FragmentActivity?,
                    errorCode: Int,
                    errString: CharSequence,
                ) = onError(errString)
            },
        )
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
