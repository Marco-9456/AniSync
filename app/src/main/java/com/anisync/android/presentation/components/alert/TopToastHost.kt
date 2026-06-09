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
import androidx.compose.runtime.DisposableEffect
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
 * App-level toast host. Renders the active toast in a [Popup] so it floats above
 * ordinary host content.
 *
 * A Material 3 [androidx.compose.material3.ModalBottomSheet] (and dialogs) render
 * in their own platform window *above* this app window, so this popup would land
 * behind their scrim. While such a modal mounts an [OverlayToastHost] this host
 * stands down (see [ToastManager.overlayHostActive]) and the modal draws the toast
 * itself — above the scrim.
 */
@Composable
fun TopToastHost(
    toastManager: ToastManager,
    modifier: Modifier = Modifier
) {
    val overlayHostActive by toastManager.overlayHostActive.collectAsStateWithLifecycle()
    if (overlayHostActive) return

    ToastPopup(toastManager = toastManager, modifier = modifier)
}

/**
 * Toast host meant to live *inside* a modal surface (Material 3 `ModalBottomSheet`,
 * dialogs). Mount it within the modal's content: while mounted it suppresses the
 * global [TopToastHost] and renders the toast within the modal's own window, so it
 * appears above the modal's scrim instead of behind it.
 */
@Composable
fun OverlayToastHost(
    toastManager: ToastManager,
    modifier: Modifier = Modifier
) {
    DisposableEffect(toastManager) {
        toastManager.acquireOverlayHost()
        onDispose { toastManager.releaseOverlayHost() }
    }
    ToastPopup(toastManager = toastManager, modifier = modifier)
}

@Composable
private fun ToastPopup(
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
