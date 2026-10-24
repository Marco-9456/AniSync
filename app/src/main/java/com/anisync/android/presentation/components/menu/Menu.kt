package com.anisync.android.presentation.components.menu

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.anisync.android.ui.theme.ProvideDisplaySize

/**
 * Material 3 Expressive dropdown menu. Builds on `DropdownMenuPopup` /
 * `DropdownMenuGroup` / `DropdownMenuItem` so items get segmented shapes, group
 * container colors, partial-width dividers, and refined motion automatically.
 *
 * Author content with the [MenuScope] DSL: [MenuScope.item],
 * [MenuScope.divider], [MenuScope.gap]. `gap()` splits subsequent items
 * into a new visual group with 8dp of empty space between groups.
 *
 * Destructive items use the error color triplet on text + icon; selected items
 * pick up the secondaryContainer highlight from the standard expressive tokens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable MenuScope.() -> Unit
) {
    val scope = MenuScopeImpl()
    scope.content()
    val groups = scope.groups()
    if (groups.isEmpty()) return

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            MenuAnchorPosition.Below,
            offset
        )
    ) {
        ProvideDisplaySize { MenuGroups(groups, scope.scrollingGroup()) }
    }
}

/**
 * Text-field-anchored variant of [Menu]. Selecting an option flips the
 * anchor's text. Items are authored via the same [MenuScope] DSL.
 *
 * Form-field menus skip [DropdownMenuGroup] segmentation since the text-field
 * anchor implies a single grouped surface; gaps render as empty padding rather
 * than separate Surface groups.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuField(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedLabel: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    fieldSupportingText: String? = null,
    enabled: Boolean = true,
    content: @Composable MenuScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange(it) },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = label?.let { { Text(it) } },
            supportingText = fieldSupportingText?.let { supporting ->
                {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
        )

        val scope = MenuScopeImpl()
        scope.content()
        val groups = scope.groups()
        if (groups.isEmpty()) return@ExposedDropdownMenuBox

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            ProvideDisplaySize {
                // ExposedDropdownMenu owns its surface, so flatten groups into one
                // column and use spacers between groups instead of nested Surfaces.
                val groupCount = groups.size
                groups.forEachIndexed { gIndex, group ->
                    val itemCount = group.count { it is MenuEntry.Item }
                    var itemIndex = 0
                    group.forEach { entry ->
                        when (entry) {
                            is MenuEntry.Item -> {
                                MenuItemRow(entry, itemIndex, itemCount)
                                itemIndex++
                            }
                            MenuEntry.Divider -> HorizontalDivider(
                                modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
                            )
                        }
                    }
                    if (gIndex < groupCount - 1) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnScope.MenuGroups(groups: List<List<MenuEntry>>, scrolling: ScrollingGroup?) {
    val groupCount = groups.size
    groups.forEachIndexed { gIndex, group ->
        val shapes = MenuDefaults.groupShape(gIndex, groupCount)
        if (scrolling != null && group === scrolling.entries) {
            // The scroll sits inside the group's own surface, as in Material's scrolling menu. Around
            // the surfaces it would clip their shadows flat at its top and bottom edges.
            Box(Modifier.weight(1f, fill = false)) {
                DropdownMenuGroup(shapes = shapes, containerColor = MenuDefaults.groupStandardContainerColor) {
                    ScrollingRows(scrolling.maxVisibleItems) { MenuRows(group) }
                }
            }
        } else {
            DropdownMenuGroup(shapes = shapes, containerColor = MenuDefaults.groupStandardContainerColor) {
                MenuRows(group)
            }
        }
        if (gIndex < groupCount - 1) Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MenuRows(group: List<MenuEntry>) {
    val itemCount = group.count { it is MenuEntry.Item }
    var itemIndex = 0
    group.forEach { entry ->
        when (entry) {
            is MenuEntry.Item -> {
                MenuItemRow(entry, itemIndex, itemCount)
                itemIndex++
            }
            MenuEntry.Divider -> HorizontalDivider(
                modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
            )
        }
    }
}

/**
 * Rows that show at most [maxVisible] at a time and scroll the rest. A sliver of the next row peeks
 * out under them, an edge with rows behind it fades out and a thin bar marks the position, so a list
 * cut short reads as one that scrolls.
 */
@Composable
private fun ScrollingRows(maxVisible: Int, rows: @Composable () -> Unit) {
    val scrollState = rememberScrollState()
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val measurePolicy = remember(maxVisible) { firstRowsMeasurePolicy(maxVisible) }
    Layout(
        content = rows,
        modifier = Modifier
            .scrollHints(scrollState, barColor)
            // The viewport takes the intrinsic height, which only counts the first rows and the peek.
            .height(IntrinsicSize.Max)
            .verticalScroll(scrollState),
        measurePolicy = measurePolicy
    )
}

/**
 * Stacks every row, but reports the height of the first [count], and of a sliver of the next one, as
 * its intrinsic height.
 */
private fun firstRowsMeasurePolicy(count: Int) = object : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val width = (placeables.maxOfOrNull { it.width } ?: 0)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        return layout(width, placeables.sumOf { it.height }) {
            var y = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) =
        firstRowsHeight(measurables, count) { it.minIntrinsicHeight(width) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) =
        firstRowsHeight(measurables, count) { it.maxIntrinsicHeight(width) }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) =
        measurables.maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) =
        measurables.maxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0
}

private fun IntrinsicMeasureScope.firstRowsHeight(
    measurables: List<IntrinsicMeasurable>,
    count: Int,
    height: (IntrinsicMeasurable) -> Int,
): Int {
    val shown = measurables.take(count).sumOf(height)
    val next = measurables.getOrNull(count) ?: return shown
    return shown + minOf(NextRowPeek.roundToPx(), height(next))
}

/** Offscreen so the fade masks the rows alone, not the group surface behind them. */
private fun Modifier.scrollHints(state: ScrollState, barColor: Color): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (state.maxValue == 0) return@drawWithContent
        val fade = ScrollFadeLength.toPx()
        if (state.canScrollBackward) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), endY = fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn
            )
        }
        if (state.canScrollForward) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black, Color.Transparent),
                    startY = size.height - fade,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn
            )
        }
        val inset = ScrollBarInset.toPx()
        val barWidth = ScrollBarWidth.toPx()
        val track = size.height - inset * 2
        val thumb = (track * size.height / (size.height + state.maxValue))
            .coerceIn(minOf(ScrollBarMinLength.toPx(), track), track)
        val x = if (layoutDirection == LayoutDirection.Ltr) size.width - inset - barWidth else inset
        drawRoundRect(
            color = barColor,
            topLeft = Offset(x, inset + (track - thumb) * state.value / state.maxValue),
            size = Size(barWidth, thumb),
            cornerRadius = CornerRadius(barWidth / 2)
        )
    }

private val ScrollFadeLength = 16.dp

// Taller than the fade, so the top of the next row's text shows before it fades out.
private val NextRowPeek = 28.dp

private val ScrollBarWidth = 4.dp
private val ScrollBarInset = 4.dp
private val ScrollBarMinLength = 24.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuItemRow(entry: MenuEntry.Item, index: Int, count: Int) {
    val errorColor = MaterialTheme.colorScheme.error
    val colors = if (entry.destructive) {
        MenuDefaults.selectableItemColors(
            textColor = errorColor,
            leadingIconColor = errorColor,
            trailingContentColor = errorColor
        )
    } else {
        MenuDefaults.selectableItemColors()
    }
    SelectableDropdownMenuItem(
        selected = entry.selected,
        onClick = entry.onClick,
        text = { Text(entry.text, maxLines = entry.maxLines, overflow = TextOverflow.Ellipsis) },
        shapes = MenuDefaults.itemShape(index, count),
        leadingIcon = entry.leadingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null) }
        },
        trailingContent = entry.trailingLabel?.let { trailing ->
            {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        enabled = entry.enabled,
        colors = colors,
        supportingText = entry.supportingText?.let { supporting ->
            { Text(supporting, maxLines = entry.maxLines, overflow = TextOverflow.Ellipsis) }
        }
    )
}

interface MenuScope {
    fun item(
        text: String,
        onClick: () -> Unit,
        leadingIcon: ImageVector? = null,
        trailingLabel: String? = null,
        supportingText: String? = null,
        destructive: Boolean = false,
        enabled: Boolean = true,
        selected: Boolean = false,
        maxLines: Int = Int.MAX_VALUE
    )

    fun divider()

    /** Splits subsequent items into a new visual group separated by 8dp of empty space. */
    fun gap()

    /**
     * Lets the current group scroll inside its own surface. It shows at most [maxVisibleItems] rows and
     * a sliver of the next, and fewer when the menu has less room, while the other groups stay in view.
     * One group per menu.
     */
    fun scrollable(maxVisibleItems: Int = Int.MAX_VALUE)
}

private sealed interface MenuEntry {
    data class Item(
        val text: String,
        val onClick: () -> Unit,
        val leadingIcon: ImageVector?,
        val trailingLabel: String?,
        val supportingText: String?,
        val destructive: Boolean,
        val enabled: Boolean,
        val selected: Boolean,
        val maxLines: Int
    ) : MenuEntry

    data object Divider : MenuEntry
}

private class ScrollingGroup(val entries: List<MenuEntry>, val maxVisibleItems: Int)

private class MenuScopeImpl : MenuScope {
    private val groups = mutableListOf(mutableListOf<MenuEntry>())
    private var scrolling: ScrollingGroup? = null

    override fun item(
        text: String,
        onClick: () -> Unit,
        leadingIcon: ImageVector?,
        trailingLabel: String?,
        supportingText: String?,
        destructive: Boolean,
        enabled: Boolean,
        selected: Boolean,
        maxLines: Int
    ) {
        groups.last().add(
            MenuEntry.Item(
                text = text,
                onClick = onClick,
                leadingIcon = leadingIcon,
                trailingLabel = trailingLabel,
                supportingText = supportingText,
                destructive = destructive,
                enabled = enabled,
                selected = selected,
                maxLines = maxLines
            )
        )
    }

    override fun divider() {
        groups.last().add(MenuEntry.Divider)
    }

    override fun gap() {
        if (groups.last().isNotEmpty()) {
            groups.add(mutableListOf())
        }
    }

    override fun scrollable(maxVisibleItems: Int) {
        scrolling = ScrollingGroup(groups.last(), maxVisibleItems)
    }

    fun groups(): List<List<MenuEntry>> = groups.filter { it.isNotEmpty() }

    fun scrollingGroup(): ScrollingGroup? = scrolling?.takeIf { it.entries.isNotEmpty() }
}
