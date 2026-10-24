package com.anisync.android.presentation.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import com.anisync.android.R
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.navigation.BackTrail
import com.anisync.android.presentation.navigation.BackTrailHost
import com.anisync.android.presentation.navigation.LocalBackTrailHost
import com.anisync.android.presentation.navigation.popBackStackTo
import com.anisync.android.presentation.navigation.rememberBackTrail
import com.anisync.android.presentation.navigation.tabRow
import com.anisync.android.presentation.navigation.trailRow
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.rememberHapticFeedback
import kotlin.math.roundToInt

/**
 * The back arrow of every browsing screen. A tap goes back one page. With two or more pages behind
 * this one, a long press opens the trail: those pages newest first, then the tab they started from,
 * and a row pops straight back to its page.
 *
 * [androidx.compose.material3.IconButton] takes no long click, so this is built the same way: a 40dp
 * visual inside a 48dp target. The page finds its own entry through the ViewModel store owner, which
 * a navigation host sets to the entry it is drawing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackTrailButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    contentDescription: String = stringResource(R.string.back),
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    iconSize: Dp = 24.dp,
) {
    val host = LocalBackTrailHost.current
    val entry = LocalViewModelStoreOwner.current as? NavBackStackEntry
    val trailEnabled by LocalAppSettings.current.backTrailEnabled.collectAsStateWithLifecycle()
    val trail = rememberBackTrail(host, entry).takeIf { trailEnabled }
    var expanded by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()
    val openTrail = {
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        expanded = true
    }
    val trailLabel = stringResource(R.string.a11y_back_trail)
    var anchorBottom by remember { mutableIntStateOf(0) }
    val shape = IconButtonDefaults.standardShape

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            anchorBottom = (coordinates.positionInWindow().y + coordinates.size.height).roundToInt()
        }
    ) {
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(IconButtonDefaults.smallContainerSize())
                .clip(shape)
                .background(color = colors.containerColor, shape = shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (trail != null) openTrail else null,
                    onLongClickLabel = if (trail != null) trailLabel else null,
                    role = Role.Button,
                    // The trail plays its own tick, through the app's haptics switch.
                    hapticFeedbackEnabled = false,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(focusRingShape = shape),
                )
                .then(
                    if (trail != null) {
                        Modifier.semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(trailLabel) {
                                    openTrail()
                                    true
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = colors.contentColor,
                modifier = Modifier.size(iconSize)
            )
        }

        if (host != null && trail != null) {
            BackTrailMenu(
                expanded = expanded,
                onDismiss = { expanded = false },
                host = host,
                trail = trail,
                anchorBottom = anchorBottom,
            )
        }
    }
}

@Composable
private fun BackTrailMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    host: BackTrailHost,
    trail: BackTrail,
    anchorBottom: Int,
) {
    // A long trail scrolls inside the menu rather than climbing over the arrow it hangs from. The
    // margin is the one Material keeps between a menu and the window edge.
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val margin = with(LocalDensity.current) { 48.dp.roundToPx() }
    val maxHeight = (windowHeight - anchorBottom - margin).coerceAtLeast(margin)
    val navController = host.navController

    Menu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    constraints.copy(
                        maxHeight = maxHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
                    )
                )
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .verticalScroll(rememberScrollState()),
        offset = DpOffset(4.dp, 0.dp),
    ) {
        val stack = navController.currentBackStack.value
        trail.pages.forEach { node ->
            val entry = stack.firstOrNull { it.id == node.entryId } ?: return@forEach
            val row = entry.trailRow()
            item(
                text = row.title,
                supportingText = row.supporting,
                leadingIcon = row.icon,
                maxLines = 1,
                onClick = {
                    onDismiss()
                    navController.popBackStackTo(node.entryId)
                }
            )
        }
        gap()
        val tab = trail.tab?.let { node -> stack.firstOrNull { it.id == node.entryId } }
        val list = host.list
        if (tab != null) {
            val row = tab.tabRow()
            item(
                text = row.title,
                leadingIcon = row.icon,
                maxLines = 1,
                onClick = {
                    onDismiss()
                    navController.popBackStackTo(tab.id)
                }
            )
        } else if (list != null) {
            item(
                text = stringResource(list.label),
                leadingIcon = list.icon,
                maxLines = 1,
                onClick = {
                    onDismiss()
                    list.onSelect()
                }
            )
        }
    }
}
