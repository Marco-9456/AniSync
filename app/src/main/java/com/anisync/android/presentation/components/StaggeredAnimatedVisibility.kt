package com.anisync.android.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay

/**
 * Default delay in milliseconds between each item's staggered animation.
 */
const val DefaultStaggerDelayPerItem = 40

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
 * @param content The composable content to animate.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StaggeredAnimatedVisibility(
    key: String,
    index: Int,
    delayPerItem: Int = DefaultStaggerDelayPerItem,
    content: @Composable () -> Unit
) {
    // Track whether this item has already animated using rememberSaveable
    // This persists across recompositions (scroll) but resets on navigation
    var hasAnimated by rememberSaveable(key = "stagger_anim_$key") { mutableStateOf(false) }
    
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

    // Use spring physics for both fade and slide to match shared element transitions
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = effectsSpec) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = spatialSpec
        )
    ) {
        content()
    }
}

