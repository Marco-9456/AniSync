package com.anisync.android.widget.designsystem.tokens

import androidx.compose.ui.unit.dp

/**
 * Dimension tokens for widget layouts.
 * Centralizes all hardcoded dp values used across widgets.
 */
object WidgetDimensions {

    // === Padding ===
    val paddingXSmall = 4.dp
    val paddingSmall = 8.dp
    val paddingMedium = 12.dp
    val paddingLarge = 16.dp

    // === Corner Radius ===
    val cornerRadiusSmall = 8.dp
    val cornerRadiusMedium = 12.dp
    val cornerRadiusLarge = 16.dp
    val cornerRadiusPill = 20.dp

    // === Poster/Image Sizes ===
    object Poster {
        val widthCompact = 40.dp
        val widthMedium = 48.dp
        val widthExpanded = 80.dp
        
        val heightCompact = 56.dp
        val heightMedium = 64.dp
        val heightExpanded = 110.dp
    }

    // === Icon Sizes ===
    object Icon {
        val small = 16.dp
        val medium = 20.dp
        val large = 24.dp
        val xlarge = 48.dp
    }

    // === Spacer Sizes ===
    object Spacer {
        val xsmall = 4.dp
        val small = 8.dp
        val medium = 12.dp
        val large = 16.dp
    }

    // === Component Heights ===
    val segmentedControlHeight = 40.dp
    val timelineItemHeight = 80.dp
    val progressBarHeight = 4.dp

    // === Badge Sizes ===
    object Badge {
        val paddingHorizontal = 8.dp
        val paddingVertical = 4.dp
        val cornerRadius = 8.dp
    }

    // === Timeline ===
    object Timeline {
        val lineWidth = 2.dp
        val columnWidth = 70.dp
    }
}
