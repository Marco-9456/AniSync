package com.anisync.android.presentation.util

import androidx.compose.material3.MaterialTheme

/**
 * Centralized motion constants for consistent animation behavior.
 * Always prefer [MaterialTheme.motionScheme] specs over these constants when possible.
 */
object MotionConstants {
    /** Stagger delay between sequential item animations */
    const val STAGGER_DELAY_MS = 40
    
    /** Slight scale for press feedback */
    const val PRESSED_SCALE = 0.95f
    
    /** Shimmer animation duration */
    const val SHIMMER_DURATION_MS = 1200
    
    /** Background animation cycle duration (for subtle ambient motion) */
    const val AMBIENT_CYCLE_MS = 12000
}
