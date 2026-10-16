package com.anisync.android.presentation.library.components

import android.view.View
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.withTimeoutOrNull

private const val SETTLE_TIMEOUT_MS = 600L
private const val STABLE_FRAMES = 2

/**
 * Material's DatePicker flickers on the way from text input back to the calendar (b/497072444).
 * Its size animation runs while the dialog window is still resizing around the closing keyboard,
 * and even once the keyboard is gone the clipped spring squeezes the month grid under the weekday
 * row. So that one switch waits for the keyboard and the window to settle, then rebuilds the picker
 * straight into calendar mode instead of animating. The way into text input keeps its animation.
 *
 * Call it inside the dialog's content so it watches the dialog window, not the activity's.
 */
@Composable
internal fun CalendarAfterKeyboard(
    state: DatePickerState,
    content: @Composable (DatePickerState) -> Unit,
) {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var calendarRequested by remember { mutableStateOf(false) }
    var calendarEntries by remember { mutableIntStateOf(0) }

    LaunchedEffect(calendarRequested) {
        if (!calendarRequested) return@LaunchedEffect
        focusManager.clearFocus()
        keyboard?.hide()
        awaitWindowSettled(view)
        state.displayMode = DisplayMode.Picker
        calendarEntries++
        calendarRequested = false
    }

    val deferred = remember(state) { DeferredCalendarState(state) { calendarRequested = true } }
    key(calendarEntries) { content(deferred) }
}

private class DeferredCalendarState(
    private val delegate: DatePickerState,
    private val onCalendarRequested: () -> Unit,
) : DatePickerState by delegate {
    override var displayMode: DisplayMode
        get() = delegate.displayMode
        set(value) {
            if (value == DisplayMode.Picker && delegate.displayMode == DisplayMode.Input) {
                onCalendarRequested()
            } else {
                delegate.displayMode = value
            }
        }
}

private suspend fun awaitWindowSettled(view: View) {
    withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
        val location = IntArray(2)
        var lastHeight = -1
        var lastTop = Int.MIN_VALUE
        var stableFrames = 0
        while (stableFrames < STABLE_FRAMES) {
            withFrameNanos { }
            val imeVisible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            view.getLocationOnScreen(location)
            val height = view.rootView.height
            val settled = !imeVisible && height == lastHeight && location[1] == lastTop
            stableFrames = if (settled) stableFrames + 1 else 0
            lastHeight = height
            lastTop = location[1]
        }
    }
}
