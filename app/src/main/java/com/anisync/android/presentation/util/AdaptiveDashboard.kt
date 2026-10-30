package com.anisync.android.presentation.util

import androidx.compose.runtime.Composable

/**
 * How many columns a section dashboard should flow into at the current width. Single column on a
 * phone (keeps the lazy, full-bleed stack), two on medium/expanded, three on very wide windows.
 * Charts stay readable because each column is still >= ~360dp at these breakpoints.
 */
@Composable
fun dashboardColumns(): Int = when (LocalAdaptiveInfo.current.widthClass) {
    WidthSizeClass.COMPACT -> 1
    WidthSizeClass.MEDIUM -> 2
    WidthSizeClass.EXPANDED -> 2
    WidthSizeClass.LARGE -> 3
}
