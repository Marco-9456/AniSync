@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.anisync.android.presentation.util

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Provides memoized access to Material 3 Expressive motion specs.
 * 
 * These specs are derived from [MaterialTheme.motionScheme] and are 
 * automatically memoized to prevent recreation on recomposition.
 * 
 * ## Usage
 * ```kotlin
 * val spatialSpec = AppMotion.rememberSpatialSpec()
 * ```
 * 
 * ## Motion Spec Types
 * - **Spatial**: For position/size animations (can overshoot)
 * - **Effects**: For opacity/color animations (no overshoot)
 * 
 * ## Speeds
 * - **Default**: Standard motion for most UI elements
 * - **Fast**: Quick, responsive feedback (buttons, small elements)
 * - **Slow**: Emphasized, dramatic transitions (hero moments)
 */
object AppMotion {
    
    // ==================== SPATIAL SPECS (for movement) ====================
    
    /**
     * Default spatial motion spec for Rect transformations.
     * Use for shared element bounds, container transforms.
     */
    @Composable
    fun rememberSpatialSpec(): FiniteAnimationSpec<Rect> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultSpatialSpec() }
    }
    
    /**
     * Fast spatial motion for quick, responsive feedback.
     * Use for button presses, tab switches, small element movements.
     */
    @Composable
    fun rememberFastSpatialSpec(): FiniteAnimationSpec<Rect> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.fastSpatialSpec() }
    }
    
    /**
     * Slow spatial motion for emphasized transitions.
     * Use for hero moments, dramatic reveals.
     */
    @Composable
    fun rememberSlowSpatialSpec(): FiniteAnimationSpec<Rect> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.slowSpatialSpec() }
    }
    
    /**
     * Default spatial motion for IntOffset (translation) animations.
     * Use for slide transitions, position changes.
     */
    @Composable
    fun rememberOffsetSpatialSpec(): FiniteAnimationSpec<IntOffset> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultSpatialSpec() }
    }
    
    /**
     * Fast spatial motion for IntOffset animations.
     * Use for quick slide transitions.
     */
    @Composable
    fun rememberFastOffsetSpatialSpec(): FiniteAnimationSpec<IntOffset> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.fastSpatialSpec() }
    }
    
    /**
     * Default spatial motion for IntSize animations.
     * Use for size changes, expand/collapse.
     */
    @Composable
    fun rememberSizeSpatialSpec(): FiniteAnimationSpec<IntSize> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultSpatialSpec() }
    }
    
    /**
     * Default spatial motion for Float values.
     * Use for scale, rotation animations.
     */
    @Composable
    fun rememberFloatSpatialSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultSpatialSpec() }
    }
    
    /**
     * Fast spatial motion for Float values.
     * Use for quick scale/rotation feedback.
     */
    @Composable
    fun rememberFastFloatSpatialSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.fastSpatialSpec() }
    }
    
    /**
     * Slow spatial motion for Float values.
     * Use for emphasized scale animations.
     */
    @Composable
    fun rememberSlowFloatSpatialSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.slowSpatialSpec() }
    }
    
    // ==================== EFFECTS SPECS (for opacity/color) ====================
    
    /**
     * Default effects motion spec for Float values.
     * Use for fade in/out, alpha transitions.
     */
    @Composable
    fun rememberEffectsSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultEffectsSpec() }
    }
    
    /**
     * Fast effects motion spec.
     * Use for quick appearance/disappearance.
     */
    @Composable
    fun rememberFastEffectsSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.fastEffectsSpec() }
    }
    
    /**
     * Slow effects motion spec.
     * Use for subtle, ambient transitions.
     */
    @Composable
    fun rememberSlowEffectsSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.slowEffectsSpec() }
    }
    
    /**
     * Default effects motion spec for Color values.
     * Use for color transitions, tint animations.
     */
    @Composable
    fun rememberColorEffectsSpec(): FiniteAnimationSpec<Color> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultEffectsSpec() }
    }
    
    /**
     * Fast effects motion spec for Color values.
     * Use for quick color changes (press states).
     */
    @Composable
    fun rememberFastColorEffectsSpec(): FiniteAnimationSpec<Color> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.fastEffectsSpec() }
    }
}
