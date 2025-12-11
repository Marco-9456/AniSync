package com.anisync.android.presentation.util

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect

/**
 * Material 3 Motion Tokens
 * Centralized motion constants for consistent animations across the app.
 * 
 * Reference: https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
 */
object MotionTokens {
    
    // -------------------------------------------------------------------------
    // EASING CURVES
    // -------------------------------------------------------------------------
    
    /** Standard M3 emphasized easing - use for most transitions */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    /** Accelerate easing - use for elements exiting the screen */
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    
    /** Decelerate easing - use for elements entering the screen */
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    
    /** Standard easing - use for subtle, non-emphasized transitions */
    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    // -------------------------------------------------------------------------
    // DURATION TOKENS (in milliseconds)
    // -------------------------------------------------------------------------
    
    const val DurationShort1 = 50
    const val DurationShort2 = 100
    const val DurationShort3 = 150
    const val DurationShort4 = 200
    
    const val DurationMedium1 = 250
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350
    const val DurationMedium4 = 400
    
    const val DurationLong1 = 450
    const val DurationLong2 = 500
    const val DurationLong3 = 550
    const val DurationLong4 = 600
    
    // -------------------------------------------------------------------------
    // SPRING PHYSICS (M3 Physics Motion System)
    // -------------------------------------------------------------------------
    
    /**
     * Spring configurations based on Material 3 motion system.
     * 
     * Choose speed based on animation size/distance:
     * - Fast: Small components (switches, buttons)
     * - Default: Partial screen (bottom sheets, cards)
     * - Slow: Full screen (navigation, page transitions)
     * 
     * Choose type based on property:
     * - Spatial: Position, size, shape (can overshoot)
     * - Effects: Color, opacity (should NOT overshoot)
     */
    object Springs {
        // Fast - for small components like switches and buttons
        const val FastSpatialDamping = 0.9f
        const val FastSpatialStiffness = 1400f
        const val FastEffectsDamping = 1f
        const val FastEffectsStiffness = 3800f
        
        // Default - for partial screen animations (cards, dialogs)
        const val DefaultSpatialDamping = 0.9f
        const val DefaultSpatialStiffness = 700f
        const val DefaultEffectsDamping = 1f
        const val DefaultEffectsStiffness = 1600f
        
        // Slow - for full screen animations and transitions
        const val SlowSpatialDamping = 0.9f
        const val SlowSpatialStiffness = 300f
        const val SlowEffectsDamping = 1f
        const val SlowEffectsStiffness = 800f
    }
    
    // -------------------------------------------------------------------------
    // SHARED ELEMENT TRANSITION HELPERS
    // -------------------------------------------------------------------------
    
    /**
     * Default bounds transform for shared element transitions.
     * Uses M3 default spatial spring for natural motion.
     */
    fun sharedElementBoundsTransform() = { _: Rect, _: Rect ->
        spring<Rect>(
            dampingRatio = Springs.DefaultSpatialDamping,
            stiffness = Springs.DefaultSpatialStiffness
        )
    }
    
    /**
     * Slow bounds transform for larger shared elements (e.g., hero images).
     */
    fun sharedElementSlowBoundsTransform() = { _: Rect, _: Rect ->
        spring<Rect>(
            dampingRatio = Springs.SlowSpatialDamping,
            stiffness = Springs.SlowSpatialStiffness
        )
    }
    
    // -------------------------------------------------------------------------
    // STAGGERED ANIMATION HELPERS  
    // -------------------------------------------------------------------------
    
    /** Default delay between staggered items (40ms for snappier feel) */
    const val StaggerDelayPerItem = 40
    
    /** Calculate delay for staggered animation by index */
    fun staggerDelay(index: Int, delayPerItem: Int = StaggerDelayPerItem): Int {
        return index * delayPerItem
    }
    
    // -------------------------------------------------------------------------
    // PRESS ANIMATION CONSTANTS
    // -------------------------------------------------------------------------
    
    /** Scale factor when a card/component is pressed */
    const val PressedScale = 0.97f
    
    /** Default scale factor (no press) */
    const val DefaultScale = 1f
}
