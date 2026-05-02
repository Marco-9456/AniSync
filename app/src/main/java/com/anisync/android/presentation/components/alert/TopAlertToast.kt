package com.anisync.android.presentation.components.alert

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    var remainingSeconds by remember(toast.id) { 
        mutableLongStateOf(toast.countdownSeconds ?: 0L) 
    }

    // Drag states for swiping
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    if (toast.countdownSeconds != null) {
        LaunchedEffect(toast.id) {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
            }
            onDismiss()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                // Fade out as it's swiped away
                val maxOffset = 500f
                val currentOffset = if (abs(offsetY.value) > abs(offsetX.value)) abs(offsetY.value) else abs(offsetX.value)
                alpha = (1f - (currentOffset / maxOffset)).coerceIn(0f, 1f)
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        offsetX.snapTo(offsetX.value + delta)
                    }
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (abs(offsetX.value) > 300) {
                        onDismiss()
                    } else {
                        offsetX.animateTo(0f, tween(300))
                    }
                }
            )
            .draggable(
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        // Only allow dragging up, or a little bit down for bounce
                        if (offsetY.value + delta < 50f) {
                            offsetY.snapTo(offsetY.value + delta)
                        }
                    }
                },
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (offsetY.value < -200) {
                        onDismiss()
                    } else {
                        offsetY.animateTo(0f, tween(300))
                    }
                }
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (toast.type.code != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(toast.type.color.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = toast.type.code.toString(),
                                color = toast.type.color,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (toast.countdownSeconds != null) {
                        Text(
                            text = "Retrying in ${remainingSeconds}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (toast.type.code != null || toast.countdownSeconds != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (toast.title != null) {
                    Text(
                        text = toast.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = toast.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(toast.type.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = toast.type.icon,
                    contentDescription = null,
                    tint = toast.type.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
