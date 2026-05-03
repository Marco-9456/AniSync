package com.anisync.android.presentation.components.alert

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TopAlertToast(
    toast: ToastMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Drag states for swiping
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                // Deferring state reads to the draw phase
                val maxOffset = 500f
                val currentOffset = maxOf(abs(offsetY.value), abs(offsetX.value))
                alpha = (1f - (currentOffset / maxOffset)).coerceIn(0f, 1f)
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    coroutineScope.launch { offsetX.snapTo(offsetX.value + delta) }
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (abs(offsetX.value) > 300) onDismiss()
                    else offsetX.animateTo(0f, tween(300))
                }
            )
            .draggable(
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        if (offsetY.value + delta < 50f) offsetY.snapTo(offsetY.value + delta)
                    }
                },
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (offsetY.value < -200) onDismiss()
                    else offsetY.animateTo(0f, tween(300))
                }
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(toast.type.surfaceColor), // Pre-calculated in enum
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = toast.type.icon,
                    contentDescription = toast.type.accessibilityLabel,
                    tint = toast.type.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (toast.countdownSeconds != null) {
                    // Isolated composable prevents the rest of the Toast from recomposing
                    CountdownTimerText(
                        initialSeconds = toast.countdownSeconds,
                        onTimerFinished = onDismiss
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (toast.title != null) {
                        Text(
                            text = toast.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (toast.type.code != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(toast.type.codeBackgroundColor) // Pre-calculated
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = toast.type.code.toString(),
                                color = toast.type.color,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (toast.type.code != null || toast.title != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = toast.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Isolates the countdown state so that ONLY this text recomposes every second,
 * rather than invalidating the entire Toast's Surface/Row/Icon/Column structure.
 */
@Composable
private fun CountdownTimerText(
    initialSeconds: Long,
    onTimerFinished: () -> Unit
) {
    var remainingSeconds by remember(initialSeconds) { mutableLongStateOf(initialSeconds) }

    LaunchedEffect(initialSeconds) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
        onTimerFinished()
    }

    Text(
        text = "Retrying in ${remainingSeconds}s",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Medium
    )
}