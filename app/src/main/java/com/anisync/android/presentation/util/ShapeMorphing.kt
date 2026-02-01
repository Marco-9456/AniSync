@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.anisync.android.presentation.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * Shape morphing utilities for shared element transitions.
 * 
 * These utilities enable smooth shape morphing animations during shared element
 * transitions, creating visually polished effects where corners animate smoothly
 * from one shape to another.
 * 
 * ## Usage Example
 * ```kotlin
 * .sharedBoundsWithShapeMorph(
 *     sharedContentState = rememberSharedContentState(key = "item-$id"),
 *     sharedTransitionScope = sharedTransitionScope,
 *     animatedVisibilityScope = animatedVisibilityScope,
 *     restingShape = TransitionShapes.cardResting(),
 *     targetShape = TransitionShapes.cardExpanded()
 * )
 * ```
 * 
 * Based on patterns from the Androidify sample app.
 */

// =============================================================================
// MORPH OVERLAY CLIP
// =============================================================================

/**
 * Custom [OverlayClip] that clips content to a morphing shape during transitions.
 * 
 * This class interpolates between two [RoundedPolygon] shapes based on the
 * current animation progress, creating smooth shape morphing effects.
 * 
 * @param morph The [Morph] object containing start and end shapes
 * @param progress Lambda returning current animation progress (0f to 1f)
 */
class MorphOverlayClip(
    private val morph: Morph,
    private val progress: () -> Float
) : OverlayClip {
    
    // Pre-allocated objects to avoid per-frame allocations
    private val path = Path()
    private val matrix = Matrix()
    
    // Cached state to avoid redundant path computations
    private var lastProgress = Float.NaN
    private var lastBoundsWidth = Float.NaN
    private var lastBoundsHeight = Float.NaN
    
    override fun getClipPath(
        state: SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density
    ): Path {
        val currentProgress = progress()
        
        // Only recompute if progress or bounds changed
        val boundsChanged = bounds.width != lastBoundsWidth || bounds.height != lastBoundsHeight
        val progressChanged = currentProgress != lastProgress
        
        if (progressChanged || boundsChanged) {
            path.rewind()
            
            // Get the morphed path at current progress
            path.addPath(morph.toPath(currentProgress).asComposePath())
            
            // Reuse pre-allocated matrix
            matrix.reset()
            matrix.translate(bounds.center.x, bounds.center.y)
            matrix.scale(bounds.width / 2f, bounds.height / 2f)
            path.transform(matrix)
            
            // Update cached state
            lastProgress = currentProgress
            lastBoundsWidth = bounds.width
            lastBoundsHeight = bounds.height
        }
        
        return path
    }
}

// =============================================================================
// MORPH SHAPE (for regular clipping)
// =============================================================================

/**
 * A [Shape] that morphs between two [RoundedPolygon] shapes.
 * 
 * Use this for regular `Modifier.clip()` operations outside of shared transitions.
 * 
 * Performance note: Caches the outline for a given size to avoid redundant
 * path computations during animation frames.
 * 
 * @param morph The [Morph] object containing start and end shapes
 * @param progress The current animation progress (0f to 1f)
 */
class MorphShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    
    // Pre-allocated objects to avoid per-frame allocations
    private val matrix = Matrix()
    
    // Cache for outline reuse
    private var cachedOutline: Outline? = null
    private var cachedSize: Size? = null
    
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // Return cached outline if size hasn't changed
        cachedOutline?.let { cached ->
            if (cachedSize == size) return cached
        }
        
        val path = morph.toPath(progress).asComposePath()
        
        // Reuse pre-allocated matrix
        matrix.reset()
        matrix.translate(size.width / 2f, size.height / 2f)
        matrix.scale(size.width / 2f, size.height / 2f)
        path.transform(matrix)
        
        val outline = Outline.Generic(path)
        
        // Cache for reuse
        cachedOutline = outline
        cachedSize = size
        
        return outline
    }
}

// =============================================================================
// SHARED BOUNDS WITH SHAPE MORPH MODIFIER
// =============================================================================

/**
 * Enhanced [sharedBounds] modifier that includes shape morphing during transitions.
 * 
 * This modifier combines the standard shared element transition with shape morphing,
 * creating smooth corner radius animations between source and destination states.
 * 
 * @param sharedContentState The shared content state for this element
 * @param sharedTransitionScope The shared transition scope
 * @param animatedVisibilityScope The animated visibility scope for this destination
 * @param restingShape The shape when the element is at rest (e.g., in a list)
 * @param targetShape The shape when the element is expanded (e.g., in detail view)
 * @param boundsTransform The bounds transform animation specification
 * @param shapeAnimationSpec Animation spec for shape morphing. If null, uses a spring
 *        animation that closely matches the default spatial motion. For best results,
 *        pass the same animation spec used in boundsTransform.
 * @param resizeMode How to handle size differences during transition
 * @param renderInOverlayDuringTransition Whether to render in overlay during transition
 */
@Composable
fun Modifier.sharedBoundsWithShapeMorph(
    sharedContentState: SharedContentState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    restingShape: RoundedPolygon,
    targetShape: RoundedPolygon,
    boundsTransform: BoundsTransform,
    shapeAnimationSpec: FiniteAnimationSpec<Float>? = null,
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
    renderInOverlayDuringTransition: Boolean = true
): Modifier {
    // Create morph object - cached shapes from TransitionShapes ensure stable keys
    val morph = remember(restingShape, targetShape) {
        Morph(restingShape, targetShape)
    }
    
    // Default to a spring animation that matches typical Material motion
    val effectiveShapeSpec = shapeAnimationSpec ?: spring(
        dampingRatio = 0.9f,
        stiffness = 380f
    )
    
    // Animate progress based on visibility state
    // When entering (visible), progress goes from 0 to 1 (resting -> target)
    // When exiting (not visible), progress goes from 1 to 0 (target -> resting)
    val animatedProgress = animatedVisibilityScope.transition.animateFloat(
        label = "shapeMorphProgress",
        transitionSpec = { effectiveShapeSpec },
        targetValueByState = { enterExitState ->
            when (enterExitState) {
                androidx.compose.animation.EnterExitState.PreEnter -> 0f
                androidx.compose.animation.EnterExitState.Visible -> 1f
                androidx.compose.animation.EnterExitState.PostExit -> 0f
            }
        }
    )
    
    // Create the morphing clip
    val morphClip = remember(morph) {
        MorphOverlayClip(morph) { animatedProgress.value }
    }
    
    return with(sharedTransitionScope) {
        this@sharedBoundsWithShapeMorph.sharedBounds(
            sharedContentState = sharedContentState,
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = boundsTransform,
            resizeMode = resizeMode,
            clipInOverlayDuringTransition = morphClip,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition
        )
    }
}

/**
 * Simplified version using corner radius values instead of full [RoundedPolygon] shapes.
 * 
 * This is convenient when you just want to morph between different corner radii
 * without needing complex polygon shapes.
 * 
 * @param sharedContentState The shared content state for this element
 * @param sharedTransitionScope The shared transition scope  
 * @param animatedVisibilityScope The animated visibility scope for this destination
 * @param restingCornerRadius Corner radius when at rest (0.0 to 0.5, as fraction of size)
 * @param targetCornerRadius Corner radius when expanded (0.0 to 0.5, as fraction of size)
 * @param boundsTransform The bounds transform animation specification
 * @param shapeAnimationSpec Animation spec for shape morphing (optional)
 * @param resizeMode How to handle size differences during transition
 */
@Composable
fun Modifier.sharedBoundsWithCornerMorph(
    sharedContentState: SharedContentState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    restingCornerRadius: Float,
    targetCornerRadius: Float,
    boundsTransform: BoundsTransform,
    shapeAnimationSpec: FiniteAnimationSpec<Float>? = null,
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
): Modifier {
    val restingShape = remember(restingCornerRadius) {
        createRoundedRectanglePolygon(restingCornerRadius)
    }
    val targetShape = remember(targetCornerRadius) {
        createRoundedRectanglePolygon(targetCornerRadius)
    }
    
    return sharedBoundsWithShapeMorph(
        sharedContentState = sharedContentState,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        restingShape = restingShape,
        targetShape = targetShape,
        boundsTransform = boundsTransform,
        shapeAnimationSpec = shapeAnimationSpec,
        resizeMode = resizeMode
    )
}

// =============================================================================
// EXTENSION FUNCTIONS
// =============================================================================

/**
 * Creates a normalized rectangle [RoundedPolygon].
 * 
 * The polygon is centered at origin with vertices at ±1 on both axes,
 * which is the standard format expected by the graphics-shapes library.
 */
fun createRectanglePolygon(): RoundedPolygon {
    return RoundedPolygon(
        vertices = floatArrayOf(
            -1f, -1f,  // top-left
             1f, -1f,  // top-right
             1f,  1f,  // bottom-right
            -1f,  1f   // bottom-left
        )
    )
}

/**
 * Creates a normalized rectangle [RoundedPolygon] with rounded corners.
 * 
 * @param cornerRadius The corner radius as a fraction of the shape size (0.0 to 0.5)
 * @return A [RoundedPolygon] rectangle with rounded corners
 */
fun createRoundedRectanglePolygon(cornerRadius: Float): RoundedPolygon {
    val vertices = floatArrayOf(
        -1f, -1f,  // top-left
         1f, -1f,  // top-right
         1f,  1f,  // bottom-right
        -1f,  1f   // bottom-left
    )
    
    return if (cornerRadius > 0f) {
        RoundedPolygon(
            vertices = vertices,
            perVertexRounding = List(4) {
                androidx.graphics.shapes.CornerRounding(cornerRadius.coerceIn(0f, 0.5f))
            }
        )
    } else {
        RoundedPolygon(vertices = vertices)
    }
}
