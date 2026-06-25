package com.anisync.android.presentation.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Bridges a screen's desired status-bar protection color up to the global scrim painted in
 * [com.anisync.android.MainActivity]. That scrim is a *sibling* of the screen content — it sits over
 * the status-bar strip, above everything — so screens can't color it directly. Instead MainActivity
 * provides this holder, the scrim reads it, and the active layer writes it.
 *
 * Value semantics: [Color.Unspecified] (the default) means "use the M3 system-bar protection tone"
 * (`surfaceContainer`), which matches top app bars and the navigation rail frame. A specified color
 * paints the strip in that color instead — used by the flat tab roots whose top edge is the plain
 * page background (Library/Discover/Feed/Forum, set in MainScreen) and by [CollapsingTopBarScaffold],
 * which publishes its live bar color so the strip tracks the hero bar: `background` while expanded,
 * lerping to `surfaceContainer` as it collapses.
 *
 * The default factory returns a throwaway holder so reads outside MainActivity's provider (previews,
 * tests) are harmless no-ops rather than crashes.
 */
val LocalStatusBarColor = staticCompositionLocalOf<MutableState<Color>> {
    mutableStateOf(Color.Unspecified)
}
