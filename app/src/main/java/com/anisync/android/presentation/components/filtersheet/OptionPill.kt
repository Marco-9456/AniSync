package com.anisync.android.presentation.components.filtersheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.bouncyClickable

private val PillShape = RoundedCornerShape(22.dp)

/**
 * The one option for every closed list in a filter sheet: a 48dp pill that turns secondaryContainer
 * and gains a check when picked. Inside a [FilterOptionGroup] the idle fill steps up to
 * surfaceContainerHighest so the pill still reads against the group.
 */
@Composable
fun OptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    idleColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    role: Role = Role.RadioButton
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else idleColor,
        shape = PillShape,
        modifier = modifier
            .height(48.dp)
            .bouncyClickable(onClick = onClick, role = role, clipShape = PillShape)
            .semantics { this.selected = selected }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The label takes the slack so the tick lands on the pill's trailing edge.
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** [options] as [OptionPill]s two to a row, a lone last option keeping half the width. */
@Composable
fun <T> OptionPillGrid(
    options: List<T>,
    label: @Composable (T) -> String,
    isSelected: (T) -> Boolean,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    idleColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    role: Role = Role.RadioButton
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { option ->
                    OptionPill(
                        label = label(option),
                        selected = isSelected(option),
                        onClick = { onClick(option) },
                        idleColor = idleColor,
                        role = role,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
