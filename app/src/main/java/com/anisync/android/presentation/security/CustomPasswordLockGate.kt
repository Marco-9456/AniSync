package com.anisync.android.presentation.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.presentation.security.AppLockAuthenticator.authenticateForAppLock
import com.anisync.android.presentation.security.AppLockAuthenticator.authenticateWithBiometrics
import com.anisync.android.presentation.security.AppLockAuthenticator.isBiometricSupported
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CustomPasswordLockGate(
    title: String = stringResource(R.string.app_name),
    subtitle: String = "Enter 4-digit PIN",
    icon: ImageVector = Icons.Rounded.Lock,
    biometricsEnabled: Boolean = true,
    hasCustomPassword: Boolean = false,
    onVerifyPassword: (String) -> Boolean,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val scope = rememberCoroutineScope()

    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    val isBiometricAvailable = remember(context) { context.isBiometricSupported() }
    val canUseBiometrics = biometricsEnabled && isBiometricAvailable

    fun triggerShake() {
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -10f at 250
                    10f at 300
                    -5f at 350
                    0f at 400
                }
            )
        }
    }

    fun promptBiometrics() {
        val host = activity ?: return
        if (canUseBiometrics) {
            host.authenticateWithBiometrics(
                title = title,
                subtitle = subtitle,
                negativeButtonText = if (hasCustomPassword) "Use PIN" else "Cancel",
                onSuccess = { onUnlockSuccess() },
                onError = { }
            )
        } else if (!hasCustomPassword) {
            host.authenticateForAppLock(
                title = title,
                subtitle = subtitle,
                onSuccess = { onUnlockSuccess() },
                onError = { }
            )
        }
    }

    // Auto-prompt biometrics once on gate appearance
    LaunchedEffect(Unit) {
        if (canUseBiometrics || !hasCustomPassword) {
            promptBiometrics()
        }
    }

    fun onDigitPress(digit: String) {
        if (pinInput.length < 4) {
            val newPin = pinInput + digit
            pinInput = newPin
            errorMessage = null

            if (newPin.length == 4) {
                scope.launch {
                    delay(50)
                    val isSuccess = onVerifyPassword(newPin)
                    if (isSuccess) {
                        onUnlockSuccess()
                    } else {
                        errorMessage = "Incorrect PIN"
                        triggerShake()
                        delay(200)
                        pinInput = ""
                    }
                }
            }
        }
    }

    fun onDeletePress() {
        if (pinInput.isNotEmpty()) {
            pinInput = pinInput.dropLast(1)
            errorMessage = null
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            if (hasCustomPassword) {
                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        errorMessage != null -> MaterialTheme.colorScheme.error
                                        isFilled -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                )
                        )
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Built-in Number Keypad (3x4)
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("BIO", "0", "DEL")
                    )

                    for (row in rows) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (item in row) {
                                when (item) {
                                    "BIO" -> {
                                        if (canUseBiometrics) {
                                            KeypadIconButton(
                                                icon = Icons.Rounded.Fingerprint,
                                                onClick = { promptBiometrics() }
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(64.dp))
                                        }
                                    }
                                    "DEL" -> {
                                        KeypadIconButton(
                                            icon = Icons.AutoMirrored.Rounded.Backspace,
                                            onClick = { onDeletePress() }
                                        )
                                    }
                                    else -> {
                                        KeypadDigitButton(
                                            digit = item,
                                            onClick = { onDigitPress(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = { promptBiometrics() },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (canUseBiometrics) Icons.Rounded.Fingerprint else Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.app_lock_unlock), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
