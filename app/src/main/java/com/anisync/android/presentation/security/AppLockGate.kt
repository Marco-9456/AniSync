package com.anisync.android.presentation.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.security.AppLockManager
import com.anisync.android.presentation.security.AppLockAuthenticator.authenticateForAppLock
import com.anisync.android.presentation.security.AppLockAuthenticator.isAppLockSupported

/**
 * Full-screen privacy gate drawn on top of everything while the app is locked. It hides the app
 * content and auto-launches the system unlock prompt. Renders nothing when the feature is off or
 * already unlocked. (Keeping the app out of the recents preview is handled separately in
 * [com.anisync.android.MainActivity] via `setRecentsScreenshotEnabled` on Android 13+.)
 */
@Composable
fun AppLockGate(
    appLockManager: AppLockManager,
    modifier: Modifier = Modifier,
) {
    // Plain collectAsState (not lifecycle-aware) so the lock state stays current while the app is
    // stopped: ProcessLifecycleOwner flips `locked` true ~700ms into the background, and we need the
    // gate on the FIRST resumed frame — otherwise the last screen shows, frozen, until the gate lands.
    val enabled by appLockManager.enabled.collectAsState()
    val locked by appLockManager.locked.collectAsState()
    if (!enabled || !locked) return

    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometricsEnabled by appLockManager.biometricsEnabled.collectAsState()

    // Swallow Back while locked so it can't drive the hidden UI underneath; send the app to the
    // background instead, the way a real lock screen behaves.
    BackHandler { activity?.moveTaskToBack(true) }

    CustomPasswordLockGate(
        title = stringResource(R.string.app_name),
        subtitle = stringResource(R.string.app_lock_message),
        icon = Icons.Rounded.Lock,
        biometricsEnabled = biometricsEnabled,
        hasCustomPassword = appLockManager.hasCustomPassword,
        onVerifyPassword = { password ->
            appLockManager.verifyPassword(password)
        },
        onUnlockSuccess = {
            appLockManager.unlock()
        },
        modifier = modifier
    )
}
