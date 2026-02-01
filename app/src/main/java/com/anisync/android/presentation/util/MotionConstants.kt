package com.anisync.android.presentation.util

import androidx.compose.material3.MaterialTheme

/**
 * Centralized motion constants for consistent animation behavior across the app.
 * 
 * ## Usage Guidelines
 * 
 * **IMPORTANT**: Always prefer [MaterialTheme.motionScheme] specs over hardcoded values
 * when possible. Use [AppMotion] for memoized access to theme motion specs.
 * 
 * These constants are intended for:
 * - Values not provided by the motion scheme (like stagger delays)
 * - Infinite/ambient animations that don't fit motion scheme patterns
 * - UI feedback constants (like press scale)
 * 
 * ## Motion Scheme Priority
 * 
 * Use this hierarchy when choosing animation specs:
 * 1. **AppMotion.rememberXxxSpec()** - For shared element transitions, item animations
 * 2. **MaterialTheme.motionScheme.xxxSpec()** - Direct theme access in composables
 * 3. **MotionConstants** - Only for values not covered above
 * 
 * ## M3 Expressive Motion Specs
 * 
 * The motion scheme provides these specs (accessible via [AppMotion]):
 * - **Spatial specs** - For movement/position changes (shared elements, navigation)
 *   - `defaultSpatialSpec()` / `fastSpatialSpec()` / `slowSpatialSpec()`
 * - **Effects specs** - For non-spatial changes (fade, scale, color)
 *   - `defaultEffectsSpec()` / `fastEffectsSpec()` / `slowEffectsSpec()`
 * 
 * @see AppMotion for memoized motion spec access
 * @see FadeThrough for fade through transition utilities
 */
object MotionConstants {
    /**
     * Stagger delay between sequential item animations.
     * Used in [StaggeredAnimatedVisibility] for cascading entrance effects.
     * 
     * This value creates a subtle wave effect when multiple items animate
     * sequentially. Adjust based on the number of items being animated.
     */
    const val STAGGER_DELAY_MS = 40
    
    /**
     * Scale factor for press/touch feedback.
     * Applied during press interactions to provide tactile feedback.
     * 
     * Value of 0.95f provides subtle shrink effect without being distracting.
     */
    const val PRESSED_SCALE = 0.95f
    
    /**
     * Duration for shimmer loading animations.
     * Used by [shimmerEffect] modifier for loading placeholders.
     * 
     * This is an infinite animation, so it doesn't use motion scheme specs.
     */
    const val SHIMMER_DURATION_MS = 1200
    
    /**
     * Duration for ambient/background animation cycles.
     * Used for subtle continuous motion like the login screen mesh background.
     * 
     * Long duration ensures the motion is calming rather than distracting.
     * This is an infinite animation, so it doesn't use motion scheme specs.
     */
    const val AMBIENT_CYCLE_MS = 12000
}
