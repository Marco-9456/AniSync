package com.anisync.android.presentation.security

import android.content.Context
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators

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
     * The unlock prompt. Class 2 plus the device credential fallback is the same
     * [authenticators] pair the support check asks about.
     */
    fun appLockRequest(title: String, subtitle: String?): AuthenticationRequest =
        AuthenticationRequest.biometricRequest(title, Biometric.Fallback.DeviceCredential) {
            setMinStrength(Biometric.Strength.Class2)
            subtitle?.let(::setSubtitle)
            // The prompt itself already stands between the user and the app; a second confirmation tap
            // after a passive face/fingerprint match just adds friction, so skip it.
            setIsConfirmationRequired(false)
        }
}
