package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.IntRangeFilter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeSliderWithLabels(
    value: IntRangeFilter,
    bounds: IntRange,
    step: Int = 1,
    onValueChange: (IntRangeFilter) -> Unit,
    formatValue: (Int) -> String = { it.toString() },
    modifier: Modifier = Modifier
) {
    val minBound = bounds.first.toFloat()
    val maxBound = bounds.last.toFloat()
    var floatRange by remember(value, bounds) {
        mutableStateOf(
            (value.min?.toFloat() ?: minBound)..(value.max?.toFloat() ?: maxBound)
        )
    }
    val steps = if (step > 0) ((maxBound - minBound) / step).toInt() - 1 else 0

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val low = floatRange.start.roundToInt()
            val high = floatRange.endInclusive.roundToInt()
            val display = if (low == bounds.first && high == bounds.last) {
                "Any"
            } else {
                "${formatValue(low)} – ${formatValue(high)}"
            }
            Text(
                text = display,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (value.isActive) {
                IconButton(onClick = {
                    floatRange = minBound..maxBound
                    onValueChange(IntRangeFilter())
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Reset range")
                }
            }
        }
        RangeSlider(
            value = floatRange,
            onValueChange = { floatRange = it },
            valueRange = minBound..maxBound,
            steps = steps.coerceAtLeast(0),
            onValueChangeFinished = {
                val low = floatRange.start.roundToInt()
                val high = floatRange.endInclusive.roundToInt()
                val newMin = if (low <= bounds.first) null else low
                val newMax = if (high >= bounds.last) null else high
                onValueChange(IntRangeFilter(newMin, newMax))
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatValue(bounds.first),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatValue(bounds.last),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
