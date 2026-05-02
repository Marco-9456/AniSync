package com.anisync.android.presentation.components.alert

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun TopToastHost(
    toastManager: ToastManager,
    modifier: Modifier = Modifier
) {
    val currentToast by toastManager.toast.collectAsState()

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
