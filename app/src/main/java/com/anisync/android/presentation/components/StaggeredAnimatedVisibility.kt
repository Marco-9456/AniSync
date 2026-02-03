@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.anisync.android.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.anisync.android.presentation.util.AppMotion
import kotlinx.coroutines.delay

/**
 * Default delay in milliseconds between each item's staggered animation.
 */
const val DefaultStaggerDelayPerItem = 40

/**
 * Direction for staggered animations.
 */
enum class StaggerDirection {
    /** Items slide in from bottom to top */
    UP,
    /** Items slide in from top to bottom */
    DOWN,
    /** Items slide in from right to left */
    START,
    /** Items slide in from left to right */
    END
}

/**
 * A composable that provides staggered animation for content sections.
 * Each item fades in and slides up with a delay based on its index,
 * creating a cascading reveal effect.
 *
 * The animation only plays once per screen session. Scrolling items in/out
 * of view will NOT restart the animation. The animation resets when the
 * user navigates away and returns to the screen.
 *
 * @param key Unique identifier for this animated item (e.g., "info_cards", "genres").
 *            Used to persist animation state across recompositions.
 * @param index Position in the stagger sequence (0, 1, 2, ...). Higher indices animate later.
 * @param delayPerItem Delay in milliseconds between each item's animation start.
 * @param direction Direction from which items slide in (default UP).
 * @param exitEnabled Whether to animate exit (default false for backward compatibility).
 * @param content The composable content to animate.
 */
@Composable
fun StaggeredAnimatedVisibility(
    key: String,
    index: Int,
    delayPerItem: Int = DefaultStaggerDelayPerItem,
    direction: StaggerDirection = StaggerDirection.UP,
    exitEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    // Track whether this item has already animated using rememberSaveable
    // This persists across recompositions (scroll) but resets on navigation
    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    
    // Local visibility state - starts as true if already animated
    var visible by remember { mutableStateOf(hasAnimated) }

    LaunchedEffect(key) {
        if (!hasAnimated) {
            // Only delay and animate if this is the first time
            delay((index * delayPerItem).toLong())
            visible = true
            hasAnimated = true
        }
    }

    // Use memoized motion specs from AppMotion
    val effectsSpec = AppMotion.rememberEffectsSpec()
    val spatialSpec = AppMotion.rememberOffsetSpatialSpec()
    
    // Calculate enter/exit transitions based on direction
    val enterTransition: EnterTransition = when (direction) {
        StaggerDirection.UP -> fadeIn(animationSpec = effectsSpec) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = spatialSpec
        )
        StaggerDirection.DOWN -> fadeIn(animationSpec = effectsSpec) + slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = spatialSpec
        )
        StaggerDirection.START -> fadeIn(animationSpec = effectsSpec) + slideInHorizontally(
            initialOffsetX = { it / 4 },
            animationSpec = spatialSpec
        )
        StaggerDirection.END -> fadeIn(animationSpec = effectsSpec) + slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = spatialSpec
        )
    }
    
    val exitTransition: ExitTransition = if (exitEnabled) {
        when (direction) {
            StaggerDirection.UP -> fadeOut(animationSpec = effectsSpec) + slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = spatialSpec
            )
            StaggerDirection.DOWN -> fadeOut(animationSpec = effectsSpec) + slideOutVertically(
                targetOffsetY = { -it / 4 },
                animationSpec = spatialSpec
            )
            StaggerDirection.START -> fadeOut(animationSpec = effectsSpec) + slideOutHorizontally(
                targetOffsetX = { it / 4 },
                animationSpec = spatialSpec
            )
            StaggerDirection.END -> fadeOut(animationSpec = effectsSpec) + slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = spatialSpec
            )
        }
    } else {
        ExitTransition.None
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
        exit = exitTransition
    ) {
        content()
    }
}

/**
 * Simplified version for horizontal staggered animations (e.g., horizontal lists).
 * 
 * @param key Unique identifier for this animated item.
 * @param index Position in the stagger sequence.
 * @param delayPerItem Delay in milliseconds between each item.
 * @param fromEnd If true, items slide in from the end (left-to-right). If false, from start (right-to-left).
 * @param content The composable content to animate.
 */
@Composable
fun HorizontalStaggeredAnimatedVisibility(
    key: String,
    index: Int,
    delayPerItem: Int = DefaultStaggerDelayPerItem,
    fromEnd: Boolean = false,
    content: @Composable () -> Unit
) {
    StaggeredAnimatedVisibility(
        key = key,
        index = index,
        delayPerItem = delayPerItem,
        direction = if (fromEnd) StaggerDirection.END else StaggerDirection.START,
        content = content
    )
}
