package com.anisync.android.widget.designsystem.tokens

import androidx.compose.ui.unit.sp

/**
 * Widget typography tokens.
 *
 * Names mirror MD3 Expressive type roles (Title / Body / Label) per
 * m3.material.io/styles/typography/applying-type. Sizes are widget-tuned
 * (denser than the main app scale) because Glance surfaces have tighter
 * cells and no scrolling — falling back to the standard MD3 sp values would
 * overflow the constrained widget layouts.
 */
object WidgetTypography {

    object Title {
        val large = 18.sp   // matches MD3 titleLarge slot (scaled down for widgets)
        val medium = 16.sp  // titleMedium
        val small = 14.sp   // titleSmall
    }

    object Body {
        val large = 14.sp   // bodyMedium tier
        val medium = 12.sp  // bodySmall tier
    }

    object Label {
        val large = 12.sp   // labelMedium tier
        val medium = 10.sp  // labelSmall tier
    }

    val badge = 12.sp
    val countdown = 14.sp
}
