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

    // Swallow Back while locked so it can't drive the hidden UI underneath; send the app to the
    // background instead, the way a real lock screen behaves.
    BackHandler { activity?.moveTaskToBack(true) }

    fun prompt() {
        // Fail open: if we can't resolve the host activity, or the device lock was removed while we
        // were away, never trap the user behind a lock they can't clear.
        val host = activity
        if (host == null || !context.isAppLockSupported()) {
            appLockManager.unlock()
            return
        }
        // No re-entrancy guard: the system dialog owns focus while it's up, so this only ever fires
        // when nothing is showing (auto-launch on appear, or a tap on the Unlock button).
        appLockManager.onUnlockStarted()
        host.authenticateForAppLock(
            title = context.getString(R.string.app_lock_title),
            subtitle = context.getString(R.string.app_lock_subtitle),
            onSuccess = { appLockManager.unlock() },
            onError = { appLockManager.onUnlockDismissed() },
        )
    }

    // Auto-prompt once when the gate first appears (i.e. each time the app locks).
    LaunchedEffect(Unit) { prompt() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(44.dp),
                )
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.app_lock_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            Button(
                onClick = { prompt() },
                modifier = Modifier.padding(top = 32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.app_lock_unlock),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
