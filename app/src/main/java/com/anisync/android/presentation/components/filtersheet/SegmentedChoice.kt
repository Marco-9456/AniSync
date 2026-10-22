package com.anisync.android.presentation.components.filtersheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.bouncyClickable

private val OuterCorner = 14.dp
private val SeamCorner = 6.dp

/**
 * A small connected selector: 28dp segments 2dp apart, the selected one fully round on primary.
 * Every either-or control inside a filter sheet is this, from the sort direction to a threshold's
 * mode, so they read as one family.
 */
@Composable
fun SegmentedChoice(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    idleColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    Row(
        modifier = modifier.then(if (enabled) Modifier else Modifier.alpha(0.38f)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val shape = if (selected) CircleShape else segmentShape(index, labels.lastIndex)
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primary else idleColor,
                shape = shape,
                modifier = Modifier
                    .height(28.dp)
                    .bouncyClickable(
                        onClick = { onSelect(index) },
                        enabled = enabled,
                        role = Role.RadioButton,
                        clipShape = shape
                    )
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Ascending and Descending. A column with no direction, such as Best match or Relevance, leaves it
 * disabled at 38% rather than hiding it, so the sheet does not jump as columns change.
 */
@Composable
fun DirectionToggle(
    isAscending: Boolean,
    onChange: (ascending: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SegmentedChoice(
        labels = listOf(stringResource(R.string.ascending), stringResource(R.string.descending)),
        selectedIndex = if (isAscending && enabled) 0 else 1,
        onSelect = { onChange(it == 0) },
        enabled = enabled,
        modifier = modifier
    )
}

private fun segmentShape(index: Int, lastIndex: Int): Shape = when (index) {
    0 -> RoundedCornerShape(
        topStart = OuterCorner,
        bottomStart = OuterCorner,
        topEnd = SeamCorner,
        bottomEnd = SeamCorner
    )
    lastIndex -> RoundedCornerShape(
        topStart = SeamCorner,
        bottomStart = SeamCorner,
        topEnd = OuterCorner,
        bottomEnd = OuterCorner
    )
    else -> RoundedCornerShape(SeamCorner)
}
