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
 * @param index Position in the stagger sequence (0, 1, 2, ...). Higher indices animate later.
 * @param delayPerItem Delay in milliseconds between each item's animation start.
 * @param content The composable content to animate.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StaggeredAnimatedVisibility(
    index: Int,
    delayPerItem: Int = DefaultStaggerDelayPerItem,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * delayPerItem).toLong())
        visible = true
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
