@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.anisync.android.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.rememberHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Animated tab with horizontal stretch effect and neighbor displacement.
 *
 * The scrollable-row sibling of [SegmentedTabGroup] in the app's segmented-tab family: a single
 * animated pill the caller loops inside a `ScrollableTabRow`/`LazyRow`, whereas [SegmentedTabGroup]
 * renders a whole connected segmented group on its own. Shared naming, not a composition.
 * 
 * Based on PixelPlayer's TabAnimation implementation:
 * - Selected tab stretches horizontally (scaleX 1.0 → 1.15 → 1.0)
 * - Neighbor tabs slide away (translationX ±12px) and return
 * - Smooth color transitions for selected/unselected states
 *
 * @param index The index of this tab
 * @param selectedIndex The currently selected tab index
 * @param selected Whether this tab is selected
 * @param onClick Callback when tab is clicked
 * @param icon The icon to display
 * @param label The text label to display
 * @param count Optional entry count shown as a small trailing badge pill
 */
@Composable
fun SegmentedTabItem(
    modifier: Modifier = Modifier,
    index: Int = 0,
    selectedIndex: Int = 0,
    selected: Boolean = false,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    count: Int? = null
) {
    val hapticFeedback = rememberHapticFeedback()
    val isSelected = selected

    // Stretch animation (scaleX for horizontal expansion)
    val scale = remember { Animatable(1f) }
    // Neighbor displacement
    val offsetX = remember { Animatable(0f) }

    var isInitialRenderScale by remember { androidx.compose.runtime.mutableStateOf(true) }
    var isInitialRenderOffset by remember { androidx.compose.runtime.mutableStateOf(true) }

    // Use M3 Expressive motion specs instead of hardcoded tween
    val motionScheme = MaterialTheme.motionScheme
    val scaleAnimationSpec = remember(motionScheme) { motionScheme.fastSpatialSpec<Float>() }
    val colorAnimationSpec = remember(motionScheme) { motionScheme.defaultEffectsSpec<Color>() }

    // Color transitions using M3 Expressive effects spec
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = colorAnimationSpec,
        label = "TabBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = colorAnimationSpec,
        label = "TabContent"
    )

    // Handles the "stretch" animation for the selected tab
    LaunchedEffect(isSelected) {
        if (isInitialRenderScale) {
            isInitialRenderScale = false
            scale.snapTo(1f)
        } else if (isSelected) {
            launch {
                scale.animateTo(1.15f, animationSpec = scaleAnimationSpec)
                scale.animateTo(1f, animationSpec = scaleAnimationSpec)
            }
        } else {
            // Instantly reset scale for non-selected tabs
            scale.snapTo(1f)
        }
    }

    // Handles the offset for neighbor tabs when selection changes
    LaunchedEffect(selectedIndex) {
        if (isInitialRenderOffset) {
            isInitialRenderOffset = false
            offsetX.snapTo(0f)
        } else if (!isSelected) {
            val distance = index - selectedIndex
            if (abs(distance) == 1) {
                // Only affect direct neighbors
                val direction = if (distance > 0) 1 else -1
                val offsetValue = 12f * direction
                launch {
                    offsetX.animateTo(offsetValue, animationSpec = scaleAnimationSpec)
                    offsetX.animateTo(0f, animationSpec = scaleAnimationSpec)
                }
            } else {
                // Instantly reset offset for non-neighbor tabs
                offsetX.snapTo(0f)
            }
        } else {
            // Ensure the selected tab itself has no offset
            offsetX.snapTo(0f)
        }
    }

    Tab(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .graphicsLayer {
                scaleX = scale.value
                translationX = offsetX.value
            }
            .clip(CircleShape)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(50)
            )
            .clearAndSetSemantics {
                role = Role.Tab
                this.selected = isSelected
                contentDescription = buildString {
                    append(label)
                    if (count != null) append(", $count")
                    if (isSelected) append(", selected")
                }
            },
        selected = isSelected,
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        selectedContentColor = contentColor,
        unselectedContentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (count != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = contentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
