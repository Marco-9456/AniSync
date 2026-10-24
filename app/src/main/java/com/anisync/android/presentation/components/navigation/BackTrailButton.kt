package com.anisync.android.presentation.components.navigation

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import com.anisync.android.R
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.navigation.BackTrailHost
import com.anisync.android.presentation.navigation.LocalBackTrailHost
import com.anisync.android.presentation.navigation.TrailSnapshot
import com.anisync.android.presentation.navigation.rememberBackTrail
import com.anisync.android.presentation.navigation.tabRow
import com.anisync.android.presentation.navigation.trailRow
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.ui.theme.ProvideDisplaySize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
    val appSettings = LocalAppSettings.current
    val trailEnabled by appSettings.backTrailEnabled.collectAsStateWithLifecycle()
    val snapshot = rememberBackTrail(host, entry).takeIf { trailEnabled }
    val trail = snapshot?.trail
    var expanded by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()
    val openTrail = {
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        appSettings.noteBackTrailOpened()
        expanded = true
    }
    val trailLabel = stringResource(R.string.a11y_back_trail)
    var anchorBottom by remember { mutableIntStateOf(0) }
    val shape = IconButtonDefaults.standardShape

    val tooltipState = rememberTooltipState(isPersistent = true)
    var tip by remember { mutableStateOf("") }
    val rowCount = trail?.rowCount ?: 0
    val firstTip = stringResource(R.string.back_trail_tip)
    val tabName = snapshot?.let { it.trail.tab?.let(it::entry) }?.tabRow()?.title
        ?: host?.list?.let { stringResource(it.label) }
    val tabTip = tabName?.let { stringResource(R.string.back_trail_tip_tab, it) }
    val streakHere = entry != null && host?.streakEntryId == entry.id
    // The first time a page has three rows behind it, and after two quick backs that still leave
    // three, once the page has settled. A navigation host keeps an entering page short of RESUMED
    // until its transition ends, so a tip never rides the motion.
    LaunchedEffect(entry, rowCount >= TipRowCount, streakHere) {
        if (entry == null || rowCount < TipRowCount) return@LaunchedEffect
        val first = !appSettings.backTrailFirstTipShown()
        val text = (if (first) firstTip else tabTip.takeIf { streakHere }) ?: return@LaunchedEffect
        if (!appSettings.backTrailTipsAllowed()) return@LaunchedEffect
        entry.lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED }
        if (!appSettings.backTrailTipsAllowed()) return@LaunchedEffect
        appSettings.noteBackTrailTip(first)
        tip = text
        withTimeoutOrNull(TipDurationMillis) { tooltipState.show() }
    }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            anchorBottom = (coordinates.positionInWindow().y + coordinates.size.height).roundToInt()
        }
    ) {
        TooltipBox(
            positionProvider = rememberDiscAlignedPositionProvider(),
            tooltip = {
                ProvideDisplaySize {
                    PlainTooltip(maxWidth = TipMaxWidth) { Text(tip) }
                }
            },
            state = tooltipState,
            // Never in the way: touches pass through, and any tap or scroll puts it away.
            focusable = false,
            enableUserInput = false,
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
        }

        if (host != null && snapshot != null) {
            BackTrailMenu(
                expanded = expanded,
                onDismiss = { expanded = false },
                host = host,
                snapshot = snapshot,
                anchorBottom = anchorBottom,
            )
        }
    }
}

private const val TipRowCount = 3
private const val TrailVisiblePages = 4
private const val TipDurationMillis = 4_000L

// Room for the tip on one line. Plain tooltips stop at 200dp, a few short of this one.
private val TipMaxWidth = 280.dp

/** Below the arrow, lined up with its 40dp disc rather than the 48dp target around it. */
@Composable
private fun rememberDiscAlignedPositionProvider(): PopupPositionProvider {
    val density = LocalDensity.current
    val inset = with(density) { ((48.dp - IconButtonDefaults.smallContainerSize().width) / 2).roundToPx() }
    val spacing = with(density) { 4.dp.roundToPx() }
    return remember(inset, spacing) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = if (layoutDirection == LayoutDirection.Ltr) {
                    anchorBounds.left + inset
                } else {
                    anchorBounds.right - inset - popupContentSize.width
                }
                val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                return IntOffset(x.coerceIn(0, maxX), anchorBounds.bottom + spacing)
            }
        }
    }
}

@Composable
private fun BackTrailMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    host: BackTrailHost,
    snapshot: TrailSnapshot,
    anchorBottom: Int,
) {
    // A long trail shows a few pages at a time and scrolls the rest, so it never takes over the
    // screen, and the tab stays in view under them. When even that does not fit below the arrow, as
    // in landscape, the pages give up more room. The room is measured the way the popup measures it:
    // against the window's visible frame, which leaves out the system bars, less the margin Material
    // keeps to its edge. A taller menu would be moved up over the arrow.
    val view = LocalView.current
    val margin = with(LocalDensity.current) { 48.dp.roundToPx() }
    val visibleHeight = remember(view, expanded, anchorBottom) {
        Rect().also(view::getWindowVisibleDisplayFrame).height()
    }
    val maxHeight = (visibleHeight - anchorBottom - margin).coerceAtLeast(margin)

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
            },
        offset = DpOffset(4.dp, 0.dp),
    ) {
        scrollable(maxVisibleItems = TrailVisiblePages)
        snapshot.trail.pages.forEach { node ->
            val entry = snapshot.entry(node) ?: return@forEach
            val row = entry.trailRow()
            item(
                text = row.title,
                supportingText = row.supporting,
                leadingIcon = row.icon,
                maxLines = 1,
                onClick = {
                    onDismiss()
                    host.popTo(node.entryId)
                }
            )
        }
        gap()
        val tab = snapshot.trail.tab?.let(snapshot::entry)
        val list = host.list
        if (tab != null) {
            val row = tab.tabRow()
            item(
                text = row.title,
                leadingIcon = row.icon,
                maxLines = 1,
                onClick = {
                    onDismiss()
                    host.popTo(tab.id)
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
