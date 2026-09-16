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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The app's single overlay alert.
 *
 * One component in two densities, chosen by [ToastType.density]. A strip explains a slowdown nobody
 * has to act on. An alert reports a failure, and grows to fit what that failure actually carries: a
 * countdown against a real deadline, AniList's own per-field messages, or an action.
 *
 * What it deliberately no longer does is print the HTTP status in a badge, or paint itself from a
 * table of fixed hex colours that ignored the palette the rest of the app was built from.
 */
@Composable
fun TopAlertToast(
    toast: ToastMessage,
    onDismiss: () -> Unit,
    onCountdownFinished: () -> Unit = onDismiss,
    modifier: Modifier = Modifier
) {
    val accent = toast.type.accentColor()
    val swipe = rememberSwipeToDismiss(onDismiss)

    // Capped rather than filled. On a tablet fillMaxWidth drew a banner the width of the whole
    // window for a one-line message.
    val frame = modifier
        .widthIn(max = MAX_WIDTH)
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .then(swipe)
        .semantics { liveRegion = LiveRegionMode.Polite }

    when (toast.type.density) {
        ToastDensity.STRIP -> ToastStrip(toast, frame)
        ToastDensity.ALERT -> ToastAlert(toast, accent, onCountdownFinished, frame)
    }
}

/**
 * The quiet density: one line, no title, no action, nothing to decide.
 *
 * Pacing earns the wavy mark because something is still in flight. Deferred does not, because
 * nothing was sent.
 */
@Composable
private fun ToastStrip(toast: ToastMessage, modifier: Modifier) {
    // No fillMaxWidth: a strip is as wide as its one line, so it reads as a notice rather than a bar.
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = toast.type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = toast.message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (toast.type == ToastType.PACING) {
                LinearWavyProgressIndicator(modifier = Modifier.width(46.dp))
            }
        }
    }
}

@Composable
private fun ToastAlert(
    toast: ToastMessage,
    accent: Color,
    onCountdownFinished: () -> Unit,
    modifier: Modifier
) {
    val remaining = toast.countdown?.let { rememberCountdown(it, onCountdownFinished) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 13.dp, bottom = 13.dp),
                verticalAlignment = if (toast.details.isEmpty()) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accent.copy(alpha = PUCK_TINT), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = toast.type.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    toast.title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = accent
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    if (toast.details.isEmpty()) {
                        Text(
                            text = toast.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        FieldMessages(toast, accent)
                    }
                }

                if (remaining != null) {
                    CountdownLabel(remaining, accent)
                } else if (toast.action != null && !toast.action.filled) {
                    TextButton(onClick = toast.action.onClick) {
                        Text(toast.action.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            toast.action?.takeIf { it.filled }?.let { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    toast.secondaryAction?.let { secondary ->
                        TextButton(onClick = secondary.onClick) {
                            Text(
                                text = secondary.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = action.onClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(action.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (remaining != null) {
                val total = ((toast.countdown?.totalSeconds ?: 1L) * 1_000).coerceAtLeast(1L).toFloat()
                LinearProgressIndicator(
                    // Read in the lambda so the bar redraws without recomposing the toast around it.
                    progress = { (remaining.value / total).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PROGRESS_HEIGHT),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
    }
}

/**
 * AniList's own words for each field it rejected.
 *
 * Their mutation docs say these are written to be shown to the user, and the app used to keep the
 * first and throw the rest away.
 */
@Composable
private fun FieldMessages(toast: ToastMessage, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        toast.details.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(4.dp)
                        .background(accent.copy(alpha = BULLET_TINT), CircleShape)
                )
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (toast.overflow > 0) {
            Text(
                text = pluralStringResource(R.plurals.toast_more_fields, toast.overflow, toast.overflow),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Isolated so only these four characters recompose while the clock runs, and derived so that
 * happens once a second rather than once a frame.
 */
@Composable
private fun CountdownLabel(remaining: State<Long>, accent: Color) {
    val seconds by remember(remaining) {
        derivedStateOf { ((remaining.value + 999) / 1_000).coerceAtLeast(0L) }
    }
    val spoken = stringResource(R.string.alert_retrying_in, seconds)
    // The app ships locales whose digits are not Latin, so the clock is formatted in the one the
    // composition is running under rather than the process default.
    val locale = LocalConfiguration.current.locales[0]
    Text(
        text = String.format(locale, "%d:%02d", seconds / 60, seconds % 60),
        style = MaterialTheme.typography.labelLarge,
        color = accent,
        modifier = Modifier
            .padding(start = 4.dp, end = 8.dp)
            .semantics { contentDescription = spoken },
    )
}

/**
 * Milliseconds left, measured against the deadline on every tick.
 *
 * Not a counter that decrements once a second: that drifts over a minute, and keeps running when the
 * wait it describes has already been extended or cut short.
 */
@Composable
private fun rememberCountdown(countdown: ToastCountdown, onFinished: () -> Unit): State<Long> {
    val remaining = remember(countdown) { mutableLongStateOf(countdown.millisLeft()) }
    LaunchedEffect(countdown) {
        while (true) {
            val left = countdown.millisLeft()
            remaining.longValue = left
            if (left <= 0L) break
            // Per frame, so the progress bar moves smoothly. The label derives seconds from this
            // and recomposes only when they change.
            withFrameMillis { }
        }
        onFinished()
    }
    return remaining
}

private fun ToastCountdown.millisLeft(): Long =
    (retryAtElapsedMs - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)

/** Swipe up or sideways to dismiss, fading out with the distance travelled. */
@Composable
private fun rememberSwipeToDismiss(onDismiss: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    return Modifier
        .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
        .graphicsLayer {
            // Deferring state reads to the draw phase
            val maxOffset = 500f
            val currentOffset = maxOf(abs(offsetY.value), abs(offsetX.value))
            alpha = (1f - (currentOffset / maxOffset)).coerceIn(0f, 1f)
        }
        .draggable(
            state = rememberDraggableState { delta ->
                scope.launch { offsetX.snapTo(offsetX.value + delta) }
            },
            orientation = Orientation.Horizontal,
            onDragStopped = {
                if (abs(offsetX.value) > 300) onDismiss()
                else offsetX.animateTo(0f, tween(300))
            }
        )
        .draggable(
            state = rememberDraggableState { delta ->
                scope.launch {
                    if (offsetY.value + delta < 50f) offsetY.snapTo(offsetY.value + delta)
                }
            },
            orientation = Orientation.Vertical,
            onDragStopped = {
                if (offsetY.value < -200) onDismiss()
                else offsetY.animateTo(0f, tween(300))
            }
        )
}

/** Wide enough for three lines of message, narrow enough to stay an overlay on a tablet. */
private val MAX_WIDTH: Dp = 480.dp
private val PROGRESS_HEIGHT: Dp = 3.dp
private const val PUCK_TINT = 0.16f
private const val BULLET_TINT = 0.7f
