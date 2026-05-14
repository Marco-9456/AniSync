package com.anisync.android.presentation.components.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.anisync.android.data.NavBarStyle

/**
 * Custom compact bottom navigation bar. Replaces M3's [androidx.compose.material3.NavigationBar]
 * to support:
 *  - Two visual styles via [NavBarStyle] ([NavBarStyle.ANCHORED] vs [NavBarStyle.FLOATING])
 *  - Animated per-item label show/hide (M3 default cannot fully hide labels)
 *  - Pill-shape indicator behind the icon on the selected destination
 *
 * Slot-based: caller emits one [CompactNavBarItem] per destination via [content].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompactNavBar(
    style: NavBarStyle,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 28f,
    content: @Composable RowScope.() -> Unit
) {
    val systemBarInsets = WindowInsets.navigationBars.asPaddingValues()
    val radiusDp = cornerRadius.dp

    when (style) {
        NavBarStyle.FLOATING -> {
            // Outer padding floats the pill above the system nav bar with margins.
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = systemBarInsets.calculateBottomPadding() + 12.dp,
                        top = 4.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(radiusDp),
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // M3 spec: 12dp above the indicator, 16dp below the label.
                            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        content = content
                    )
                }
            }
        }
        NavBarStyle.ANCHORED -> {
            // Anchored variant — full width, only top corners rounded. Inner bottom
            // padding consumes the system nav bar inset.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = radiusDp, topEnd = radiusDp),
                tonalElevation = 0.dp,
                modifier = modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // M3 spec: 12dp above the indicator, 16dp below the label.
                        // Bottom also consumes the system nav bar inset.
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = 12.dp,
                            bottom = systemBarInsets.calculateBottomPadding() + 16.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * A single item inside a [CompactNavBar].
 *
 * Layout: pill indicator wraps the icon; label sits beneath. The label is hoisted
 * inside an [AnimatedVisibility] so toggling [showLabel] animates the bar height
 * smoothly instead of snapping.
 *
 * @param showLabel whether labels are globally enabled. When false, labels are not
 *   rendered for any item.
 * @param badge optional badge slot — rendered in place of the bare icon when provided.
 */
@Composable
fun RowScope.CompactNavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        label = "nav-indicator-bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "nav-icon-tint"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "nav-label-color"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            // With labels hidden the item collapses to just the pill, making the
            // bar look like a thin strip. Reserve the vertical space the label
            // would have occupied so the bar keeps a balanced height.
            .then(if (!showLabel) Modifier.padding(vertical = 10.dp) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pill indicator (animates from transparent to filled). Wraps the icon so
        // it grows with the icon's natural size.
        // M3 spec: active indicator pill is 32dp tall, 56dp wide (24dp icon +
        // 16dp horizontal padding each side).
        Box(
            modifier = Modifier
                .background(indicatorColor, IndicatorShape)
                .height(32.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides iconTint) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    if (badge != null) badge() else icon()
                }
            }
        }

        AnimatedVisibility(
            visible = showLabel,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            CompositionLocalProvider(LocalContentColor provides labelColor) {
                Box(
                    modifier = Modifier.padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    label()
                }
            }
        }
    }
}

private val IndicatorShape: Shape = RoundedCornerShape(percent = 50)
