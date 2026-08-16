package com.anisync.android.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.presentation.security.AppLockAuthenticator.isBiometricSupported
import com.anisync.android.presentation.util.LocalAppSettings

@Composable
fun SettingsSecurityScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appSettings = LocalAppSettings.current
    val context = LocalContext.current

    val appLockEnabled by appSettings.appLockEnabled.collectAsStateWithLifecycle()
    val biometricsEnabled by appSettings.appLockBiometricsEnabled.collectAsStateWithLifecycle()
    val passwordHash by appSettings.appLockPasswordHash.collectAsStateWithLifecycle()
    val hasPassword = !passwordHash.isNullOrEmpty()

    val isBiometricHardwareAvailable = remember(context) { context.isBiometricSupported() }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showPasswordDialog) {
        SetPinDialog(
            hasExisting = hasPassword,
            onDismiss = { showPasswordDialog = false },
            onSavePin = { newPin ->
                appSettings.setAppLockPassword(newPin)
                showPasswordDialog = false
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            title = { Text("Remove PIN?") },
            text = { Text("App lock and hidden lists will fall back to system device lock (if enabled).") },
            confirmButton = {
                Button(
                    onClick = {
                        appSettings.clearAppLockPassword()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    SettingsScreenScaffold(
        title = "Security & Privacy",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsSectionLabel("App Lock")
        SettingsGroup {
            SwitchSettingsItem(
                title = "App Lock Gate",
                subtitle = "Require authentication to open the app on cold start and background return",
                checked = appLockEnabled,
                onCheckedChange = { appSettings.setAppLockEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionLabel("4-Digit PIN & Biometrics")
        SettingsGroup {
            SettingsItem(
                title = if (hasPassword) "Change 4-Digit PIN" else "Set 4-Digit PIN",
                subtitle = if (hasPassword) "Custom 4-digit PIN is active for App Lock and Hidden List" else "Configure a 4-digit PIN for in-app privacy",
                icon = Icons.Rounded.Password,
                onClick = { showPasswordDialog = true }
            )

            if (hasPassword) {
                SettingsItem(
                    title = "Remove Custom PIN",
                    subtitle = "Clear your custom in-app PIN",
                    icon = Icons.Rounded.Key,
                    onClick = { showClearConfirmDialog = true }
                )
            }

            if (isBiometricHardwareAvailable) {
                SwitchSettingsItem(
                    title = "Biometric Authentication",
                    subtitle = "Allow unlocking with fingerprint or face recognition alongside PIN",
                    checked = biometricsEnabled,
                    onCheckedChange = { appSettings.setAppLockBiometricsEnabled(it) }
                )
            }
        }
    }
}

@Composable
private fun SetPinDialog(
    hasExisting: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Enter PIN, 2: Confirm PIN
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun onDigit(d: String) {
        if (step == 1) {
            if (firstPin.length < 4) {
                val next = firstPin + d
                firstPin = next
                errorMessage = null
                if (next.length == 4) {
                    step = 2
                }
            }
        } else {
            if (confirmPin.length < 4) {
                val next = confirmPin + d
                confirmPin = next
                errorMessage = null
                if (next.length == 4) {
                    if (next == firstPin) {
                        onSavePin(next)
                    } else {
                        errorMessage = "PINs do not match. Try again."
                        confirmPin = ""
                        firstPin = ""
                        step = 1
                    }
                }
            }
        }
    }

    fun onDel() {
        if (step == 1) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                step = 1
                firstPin = firstPin.dropLast(1)
            }
        }
        errorMessage = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (step == 1) (if (hasExisting) "Enter New 4-Digit PIN" else "Set 4-Digit PIN")
                else "Confirm 4-Digit PIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (step == 1) "Enter a 4-digit PIN for App Lock and Hidden List" else "Re-enter the 4-digit PIN to confirm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentPin = if (step == 1) firstPin else confirmPin
                    for (i in 0 until 4) {
                        val isFilled = i < currentPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                        )
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Number keypad
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "DEL")
                    )

                    for (row in rows) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (item in row) {
                                when (item) {
                                    "" -> Spacer(modifier = Modifier.size(56.dp))
                                    "DEL" -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .clickable { onDel() }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.AutoMirrored.Rounded.Backspace,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .clickable { onDigit(item) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = item,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
