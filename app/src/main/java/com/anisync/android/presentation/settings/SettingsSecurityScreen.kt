package com.anisync.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
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
        SetPasswordDialog(
            hasExisting = hasPassword,
            onDismiss = { showPasswordDialog = false },
            onSavePassword = { newPassword ->
                appSettings.setAppLockPassword(newPassword)
                showPasswordDialog = false
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            title = { Text("Remove Custom Password?") },
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

        SettingsSectionLabel("Custom Password & Biometrics")
        SettingsGroup {
            SettingsItem(
                title = if (hasPassword) "Change Custom Password" else "Set Custom Password",
                subtitle = if (hasPassword) "Custom password/PIN is active for App Lock and Hidden List" else "Configure a custom password for in-app privacy",
                icon = Icons.Rounded.Password,
                onClick = { showPasswordDialog = true }
            )

            if (hasPassword) {
                SettingsItem(
                    title = "Remove Custom Password",
                    subtitle = "Clear your custom in-app password",
                    icon = Icons.Rounded.Key,
                    onClick = { showClearConfirmDialog = true }
                )
            }

            if (isBiometricHardwareAvailable) {
                SwitchSettingsItem(
                    title = "Biometric Authentication",
                    subtitle = "Allow unlocking with fingerprint or face recognition alongside password",
                    checked = biometricsEnabled,
                    onCheckedChange = { appSettings.setAppLockBiometricsEnabled(it) }
                )
            }
        }
    }
}

@Composable
private fun SetPasswordDialog(
    hasExisting: Boolean,
    onDismiss: () -> Unit,
    onSavePassword: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasExisting) "Change Password" else "Set Custom Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "This password will be used to unlock the app and access your Hidden List.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password / PIN") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        errorMessage = "Password cannot be empty"
                    } else if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                    } else {
                        onSavePassword(password)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
