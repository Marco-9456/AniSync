package com.anisync.android.widget.core

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Represents the size class of a widget based on its current dimensions.
 * Used for responsive layout decisions across all widgets.
 */
enum class SizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/**
 * Size breakpoints for widget responsive layouts.
 * These values are used to determine which SizeClass a widget falls into.
 */
object SizeBreakpoints {
    val compactMaxHeight = 110.dp
    val mediumMaxHeight = 200.dp
}

/**
 * Extension function to convert a DpSize to a SizeClass.
 * Used in all widgets to determine which layout variant to display.
 */
fun DpSize.toSizeClass(): SizeClass = when {
    height <= SizeBreakpoints.compactMaxHeight -> SizeClass.COMPACT
    height <= SizeBreakpoints.mediumMaxHeight -> SizeClass.MEDIUM
    else -> SizeClass.EXPANDED
}
