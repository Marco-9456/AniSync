package com.anisync.android.presentation.components.alert

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@Composable
fun TopToastHost(
    toastManager: ToastManager,
    modifier: Modifier = Modifier
) {
    val currentToast by toastManager.toast.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
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
                }
            }
        }
    }
}