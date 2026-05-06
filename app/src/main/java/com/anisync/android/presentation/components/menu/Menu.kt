package com.anisync.android.presentation.components.menu

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

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
        offset = offset
    ) {
        MenuGroups(groups)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuGroups(groups: List<List<MenuEntry>>) {
    val groupCount = groups.size
    groups.forEachIndexed { gIndex, group ->
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(gIndex, groupCount),
            containerColor = MenuDefaults.groupStandardContainerColor
        ) {
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
        if (gIndex < groupCount - 1) Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuItemRow(entry: MenuEntry.Item, index: Int, count: Int) {
    val errorColor = MaterialTheme.colorScheme.error
    val colors = if (entry.destructive) {
        MenuDefaults.selectableItemColors(
            textColor = errorColor,
            leadingIconColor = errorColor,
            trailingIconColor = errorColor
        )
    } else {
        MenuDefaults.selectableItemColors()
    }
    DropdownMenuItem(
        selected = entry.selected,
        onClick = entry.onClick,
        text = { Text(entry.text) },
        shapes = MenuDefaults.itemShape(index, count),
        leadingIcon = entry.leadingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null) }
        },
        trailingIcon = entry.trailingLabel?.let { trailing ->
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
            { Text(supporting) }
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
        selected: Boolean = false
    )

    fun divider()

    /** Splits subsequent items into a new visual group separated by 8dp of empty space. */
    fun gap()
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
        val selected: Boolean
    ) : MenuEntry

    data object Divider : MenuEntry
}

private class MenuScopeImpl : MenuScope {
    private val groups = mutableListOf(mutableListOf<MenuEntry>())

    override fun item(
        text: String,
        onClick: () -> Unit,
        leadingIcon: ImageVector?,
        trailingLabel: String?,
        supportingText: String?,
        destructive: Boolean,
        enabled: Boolean,
        selected: Boolean
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
                selected = selected
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

    fun groups(): List<List<MenuEntry>> = groups.filter { it.isNotEmpty() }
}
