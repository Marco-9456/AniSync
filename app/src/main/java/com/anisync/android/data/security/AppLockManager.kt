package com.anisync.android.data.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.anisync.android.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the app-lock state: whether the app is currently locked and must be unlocked before its
 * content is shown. Device-local privacy gate — no encryption, it only decides UI visibility.
 *
 * Lock lifecycle:
 *  - **Cold start**: [locked] starts at [AppSettings.appLockEnabled]'s value, so a fresh process is
 *    locked whenever the feature is on.
 *  - **Background → foreground**: [onStop] re-arms the lock when the whole app is backgrounded, so
 *    returning to it requires authentication again.
 *  - The system credential/biometric prompt is a separate activity that drives the app through
 *    ON_STOP/ON_START itself; [isAuthenticating] suppresses the re-arm during that round trip so the
 *    prompt isn't fighting a lock underneath it.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val appSettings: AppSettings,
) : DefaultLifecycleObserver {

    private val _locked = MutableStateFlow(appSettings.appLockEnabled.value)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    /** Mirrors the persisted toggle, so the gate can react without touching [AppSettings] directly. */
    val enabled: StateFlow<Boolean> = appSettings.appLockEnabled

    @Volatile
    var isAuthenticating: Boolean = false

    /** Registers the app-level background/foreground observer. Call once from the Application. */
    fun bindTo(processLifecycle: Lifecycle) {
        processLifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        if (enabled.value && !isAuthenticating) _locked.value = true
    }

    /** Marks the app unlocked for this foreground session (called after a successful auth). */
    fun unlock() {
        _locked.value = false
    }
}
