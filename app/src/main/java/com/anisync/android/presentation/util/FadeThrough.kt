@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.anisync.android.presentation.util

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.anisync.android.presentation.util.FadeThrough.SCALE_FACTOR

/**
 * Material Design 3 Expressive Fade Through transition utility.
 * 
 * Fade through is used for transitions between UI elements that don't have
 * a strong relationship with each other. Unlike shared axis transitions,
 * fade through doesn't imply directionality.
 * 
 * ## When to Use
 * - Switching between unrelated content (e.g., tabs with different content types)
 * - Replacing content that doesn't share a spatial relationship
 * - Crossfading between states where direction isn't meaningful
 * 
 * ## When NOT to Use
 * - Navigating forward/backward (use Shared Axis X instead)
 * - Navigating up/down hierarchy (use Shared Axis Z instead)
 * - Elements with shared identity (use Shared Element transitions)
 * 
 * ## Usage
 * ```kotlin
 * AnimatedContent(
 *     targetState = currentState,
 *     transitionSpec = { FadeThrough.contentTransform() }
 * ) { state ->
 *     when (state) {
 *         State.A -> ContentA()
 *         State.B -> ContentB()
 *     }
 * }
 * ```
 */
object FadeThrough {
    
    /**
     * Default scale factor for fade through transitions.
     * Content scales from 92% to 100% during enter.
     */
    private const val SCALE_FACTOR = 0.92f
    
    /**
     * Creates a fade through enter transition.
     * Content fades in while scaling up from [SCALE_FACTOR] to 1.0.
     * 
     * @param scaleSpec Animation spec for the scale animation
     * @param fadeSpec Animation spec for the fade animation
     */
    @Composable
    fun enter(
        scaleSpec: FiniteAnimationSpec<Float> = rememberScaleSpec(),
        fadeSpec: FiniteAnimationSpec<Float> = rememberFadeSpec()
    ): EnterTransition {
        return fadeIn(animationSpec = fadeSpec) + scaleIn(
            animationSpec = scaleSpec,
            initialScale = SCALE_FACTOR
        )
    }
    
    /**
     * Creates a fade through exit transition.
     * Content fades out while maintaining its scale.
     * 
     * @param fadeSpec Animation spec for the fade animation
     */
    @Composable
    fun exit(
        fadeSpec: FiniteAnimationSpec<Float> = rememberFadeSpec()
    ): ExitTransition {
        // Exit only fades - no scale to avoid visual competition
        return fadeOut(animationSpec = fadeSpec)
    }
    
    /**
     * Creates a complete fade through [ContentTransform] for use with AnimatedContent.
     * 
     * @param scaleSpec Animation spec for the enter scale animation
     * @param fadeSpec Animation spec for both fade animations
     */
    @Composable
    fun contentTransform(
        scaleSpec: FiniteAnimationSpec<Float> = rememberScaleSpec(),
        fadeSpec: FiniteAnimationSpec<Float> = rememberFadeSpec()
    ): ContentTransform {
        return enter(scaleSpec, fadeSpec) togetherWith exit(fadeSpec)
    }
    
    /**
     * Remembers the default scale animation spec for fade through.
     * Uses Material 3 Expressive default spatial spec.
     */
    @Composable
    fun rememberScaleSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultSpatialSpec() }
    }
    
    /**
     * Remembers the default fade animation spec for fade through.
     * Uses Material 3 Expressive default effects spec.
     */
    @Composable
    fun rememberFadeSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultEffectsSpec() }
    }
    
    /**
     * Remembers a fast scale animation spec for quick fade through transitions.
     */
    @Composable
    fun rememberFastScaleSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.fastSpatialSpec() }
    }
    
    /**
     * Remembers a fast fade animation spec for quick fade through transitions.
     */
    @Composable
    fun rememberFastFadeSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.fastEffectsSpec() }
    }
    
    /**
     * Creates a fast fade through [ContentTransform] for quick transitions.
     * Use for smaller UI elements or when responsiveness is more important than drama.
     */
    @Composable
    fun fastContentTransform(): ContentTransform {
        return contentTransform(
            scaleSpec = rememberFastScaleSpec(),
            fadeSpec = rememberFastFadeSpec()
        )
    }
}

/**
 * Extension function to easily apply fade through transition to AnimatedContent.
 * 
 * @return ContentTransform configured for fade through
 */
@Composable
fun fadeThroughTransition(): ContentTransform = FadeThrough.contentTransform()

/**
 * Extension function for fast fade through transitions.
 * 
 * @return ContentTransform configured for fast fade through
 */
@Composable
fun fastFadeThroughTransition(): ContentTransform = FadeThrough.fastContentTransform()
