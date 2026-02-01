package com.anisync.android.presentation.util

import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon

/**
 * Centralized configuration for shape morphing transitions.
 * 
 * This object provides consistent shape definitions for shared element transitions
 * across the app. All shapes are normalized (vertices in [-1, 1] range) for
 * compatibility with the [Morph] API.
 * 
 * ## Performance
 * Shapes are lazily initialized and cached as singletons to avoid
 * object allocation during composition/animation frames.
 * 
 * ## Corner Radius Scale
 * Corner radius values are fractions of the shape size:
 * - 0.0 = no rounding (sharp corners)
 * - 0.5 = maximum rounding (becomes circular for square shapes)
 * 
 * ## Usage
 * ```kotlin
 * .sharedBoundsWithShapeMorph(
 *     restingShape = TransitionShapes.cardResting(),
 *     targetShape = TransitionShapes.cardExpanded(),
 *     // ...
 * )
 * ```
 */
object TransitionShapes {
    
    // ==========================================================================
    // CORNER RADIUS CONSTANTS
    // ==========================================================================
    
    /**
     * Standard card corner radius (~12dp equivalent).
     * Matches the RoundedCornerShape(12.dp) used throughout the app.
     */
    const val CARD_CORNER_RADIUS = 0.12f
    
    /**
     * Smaller corner radius for expanded/detail views (~4dp equivalent).
     * Creates subtle rounding while appearing nearly rectangular.
     */
    const val DETAIL_CORNER_RADIUS = 0.03f
    
    /**
     * No corner radius - sharp rectangular corners.
     */
    const val NO_CORNER_RADIUS = 0f
    
    /**
     * Large corner radius for pill-shaped elements (~16dp equivalent).
     */
    const val PILL_CORNER_RADIUS = 0.20f
    
    /**
     * Maximum corner radius for circular/capsule shapes.
     */
    const val CIRCULAR_CORNER_RADIUS = 0.5f
    
    // ==========================================================================
    // CACHED SHAPE INSTANCES (lazy-initialized singletons)
    // ==========================================================================
    
    private val _cardResting: RoundedPolygon by lazy { 
        createRoundedRectangle(CARD_CORNER_RADIUS) 
    }
    
    private val _cardExpanded: RoundedPolygon by lazy { 
        createRoundedRectangle(DETAIL_CORNER_RADIUS) 
    }
    
    private val _rectangle: RoundedPolygon by lazy { 
        createRoundedRectangle(NO_CORNER_RADIUS) 
    }
    
    private val _posterResting: RoundedPolygon by lazy { 
        createRoundedRectangle(CARD_CORNER_RADIUS) 
    }
    
    private val _posterExpanded: RoundedPolygon by lazy { 
        createRoundedRectangle(DETAIL_CORNER_RADIUS) 
    }
    
    private val _heroResting: RoundedPolygon by lazy { 
        createRoundedRectangle(PILL_CORNER_RADIUS) 
    }
    
    private val _heroExpanded: RoundedPolygon by lazy { 
        createRoundedRectangle(DETAIL_CORNER_RADIUS) 
    }
    
    private val _circular: RoundedPolygon by lazy { 
        createRoundedRectangle(CIRCULAR_CORNER_RADIUS) 
    }
    
    private val _avatarExpanded: RoundedPolygon by lazy { 
        createRoundedRectangle(CARD_CORNER_RADIUS) 
    }
    
    // ==========================================================================
    // CARD TRANSITION SHAPES
    // ==========================================================================
    
    /**
     * Shape for cards in their resting state (e.g., in a list or grid).
     * Features standard 12dp-equivalent rounded corners.
     * 
     * @return Cached RoundedPolygon with card-style rounded corners
     */
    fun cardResting(): RoundedPolygon = _cardResting
    
    /**
     * Shape for cards in their expanded state (e.g., detail screen header).
     * Features minimal rounding for a nearly rectangular appearance.
     * 
     * @return Cached RoundedPolygon with subtle rounded corners
     */
    fun cardExpanded(): RoundedPolygon = _cardExpanded
    
    /**
     * Shape for fully rectangular elements with no rounding.
     * 
     * @return Cached RoundedPolygon rectangle with sharp corners
     */
    fun rectangle(): RoundedPolygon = _rectangle
    
    // ==========================================================================
    // POSTER TRANSITION SHAPES
    // ==========================================================================
    
    /**
     * Shape for poster cards in their resting state.
     * Uses slightly larger corners than standard cards for visual distinction.
     * 
     * @return Cached RoundedPolygon with poster-style rounded corners
     */
    fun posterResting(): RoundedPolygon = _posterResting
    
    /**
     * Shape for poster cards in their expanded state.
     * 
     * @return Cached RoundedPolygon with minimal rounded corners
     */
    fun posterExpanded(): RoundedPolygon = _posterExpanded
    
    // ==========================================================================
    // HERO/CAROUSEL TRANSITION SHAPES
    // ==========================================================================
    
    /**
     * Shape for hero carousel items in their resting state.
     * Features prominent rounded corners for visual impact.
     * 
     * @return Cached RoundedPolygon with large rounded corners
     */
    fun heroResting(): RoundedPolygon = _heroResting
    
    /**
     * Shape for hero carousel items in their expanded state.
     * 
     * @return Cached RoundedPolygon with subtle rounded corners
     */
    fun heroExpanded(): RoundedPolygon = _heroExpanded
    
    // ==========================================================================
    // CIRCULAR/AVATAR TRANSITION SHAPES
    // ==========================================================================
    
    /**
     * Circular shape for avatar/profile images.
     * 
     * @return Cached RoundedPolygon approximating a circle
     */
    fun circular(): RoundedPolygon = _circular
    
    /**
     * Shape for avatars in their expanded state (e.g., full profile view).
     * Transitions from circle to rounded rectangle.
     * 
     * @return Cached RoundedPolygon with card-style rounded corners
     */
    fun avatarExpanded(): RoundedPolygon = _avatarExpanded
    
    // ==========================================================================
    // CUSTOM SHAPE FACTORY
    // ==========================================================================
    
    /**
     * Creates a custom rounded rectangle shape with specified corner radius.
     * 
     * NOTE: This creates a NEW shape instance each call. For frequently used
     * shapes, add a cached version to this object instead.
     * 
     * @param cornerRadius Corner radius as fraction (0.0 to 0.5)
     * @return RoundedPolygon with uniform corner rounding
     */
    fun custom(cornerRadius: Float): RoundedPolygon = createRoundedRectangle(cornerRadius)
    
    // ==========================================================================
    // INTERNAL HELPERS
    // ==========================================================================
    
    /**
     * Creates a normalized rounded rectangle polygon.
     * 
     * @param cornerRadius Corner radius as fraction (0.0 to 0.5)
     * @return RoundedPolygon centered at origin with vertices at ±1
     */
    private fun createRoundedRectangle(cornerRadius: Float): RoundedPolygon {
        // Clamp corner radius to valid range
        val clampedRadius = cornerRadius.coerceIn(0f, 0.5f)
        
        // Create rectangle vertices (normalized to [-1, 1])
        val vertices = floatArrayOf(
            -1f, -1f,  // top-left
             1f, -1f,  // top-right
             1f,  1f,  // bottom-right
            -1f,  1f   // bottom-left
        )
        
        // Apply uniform corner rounding
        return if (clampedRadius > 0f) {
            RoundedPolygon(
                vertices = vertices,
                perVertexRounding = List(4) { CornerRounding(clampedRadius) }
            )
        } else {
            RoundedPolygon(vertices = vertices)
        }
    }
}
