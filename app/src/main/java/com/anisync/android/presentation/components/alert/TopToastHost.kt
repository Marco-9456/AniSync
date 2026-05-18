package com.anisync.android.presentation.components.alert

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * Provides the singleton [ToastManager] to the composition so any screen can
 * read [ToastManager.isRateLimited] (e.g. to gate pull-to-refresh) or fire
 * a toast without threading it through every ViewModel.
 */
val LocalToastManager = compositionLocalOf<ToastManager> {
    error("LocalToastManager not provided. Wrap your hierarchy in ProvideToastManager.")
}

@Composable
fun ProvideToastManager(
    toastManager: ToastManager,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalToastManager provides toastManager, content = content)
}

/**
 * Renders the active toast in a [Popup] so it always floats above the host
 * content — including full-screen overlays like Material 3's
 * `ExpandedFullScreenSearchBar`, bottom sheets, and dialogs that would
 * otherwise obscure a regular sibling composable.
 */
@Composable
fun TopToastHost(
    toastManager: ToastManager,
    modifier: Modifier = Modifier
) {
    val currentToast by toastManager.toast.collectAsStateWithLifecycle()

    if (currentToast == null) return

    Popup(
        alignment = Alignment.TopCenter,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = currentToast != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                currentToast?.let { toast ->
                    TopAlertToast(
                        toast = toast,
                        onDismiss = { toastManager.clearToast() }
                    )

                    if (toast.countdownSeconds == null) {
                        LaunchedEffect(toast.id) {
                            delay(4000)
                            toastManager.clearToast()
                        }
                    } else {
                        // Auto-clear when the countdown hits zero so
                        // `ToastManager.isRateLimited` flips false and
                        // pull-to-refresh gates re-enable.
                        LaunchedEffect(toast.id) {
                            delay(toast.countdownSeconds * 1000)
                            toastManager.clearToast()
                        }
                    }
                }
            }
        }
    }
}
