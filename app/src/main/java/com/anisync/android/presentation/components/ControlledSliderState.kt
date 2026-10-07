package com.anisync.android.presentation.components

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

/**
 * A [SliderState] for a slider whose value lives outside it, in a ViewModel or local state.
 *
 * Pass it to the [Slider] overload that also takes `onValueChange`. That overload hands each drag
 * to the callback and leaves the state alone, so the thumb only moves once [value] comes back
 * changed. This copies [value] in after every composition, which also keeps the thumb in step when
 * something else changes the value, a reset or a stepper next to it.
 *
 * `rememberSliderState` reads its value once, at creation, so on its own it would ignore both.
 */
@Composable
fun rememberControlledSliderState(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
): SliderState {
    val state = remember(steps, valueRange) { SliderState(value, steps, valueRange) }
    SideEffect { if (state.value != value) state.value = value }
    return state
}
