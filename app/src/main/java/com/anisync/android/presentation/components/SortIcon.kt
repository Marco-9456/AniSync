package com.anisync.android.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun SortIcon(
    isAscending: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.onSurface,
    inactiveColor: Color = activeColor.copy(alpha = 0.38f) // Standard disabled alpha
) {
    Canvas(modifier = modifier.size(24.dp)) {
        // Path data for the UP arrow (Left side of SwapVert)
        // M9 3L5 6.99h3V14h2V6.99h3L9 3z
        val upPath = Path().apply {
            moveTo(9.dp.toPx(), 3.dp.toPx())
            lineTo(5.dp.toPx(), 6.99.dp.toPx())
            lineTo(8.dp.toPx(), 6.99.dp.toPx()) // h3
            lineTo(8.dp.toPx(), 14.dp.toPx())   // V14
            lineTo(10.dp.toPx(), 14.dp.toPx())  // h2
            lineTo(10.dp.toPx(), 6.99.dp.toPx()) // V6.99
            lineTo(13.dp.toPx(), 6.99.dp.toPx()) // h3
            close()
        }

        // Path data for the DOWN arrow (Right side of SwapVert)
        // M16 17.01V10h-2v7.01h-3L15 21l4-3.99h-3z
        val downPath = Path().apply {
            moveTo(16.dp.toPx(), 17.01.dp.toPx())
            lineTo(16.dp.toPx(), 10.dp.toPx())   // V10
            lineTo(14.dp.toPx(), 10.dp.toPx())   // h-2
            lineTo(14.dp.toPx(), 17.01.dp.toPx()) // v7.01
            lineTo(11.dp.toPx(), 17.01.dp.toPx()) // h-3
            lineTo(15.dp.toPx(), 21.dp.toPx())   // L15 21
            lineTo(19.dp.toPx(), 17.01.dp.toPx()) // l4 -3.99 (approx)
            lineTo(16.dp.toPx(), 17.01.dp.toPx()) // h-3
            close()
        }

        // Draw Up Arrow (Active if Ascending)
        drawPath(
            path = upPath,
            color = if (isAscending) activeColor else inactiveColor
        )

        // Draw Down Arrow (Active if Descending)
        drawPath(
            path = downPath,
            color = if (!isAscending) activeColor else inactiveColor
        )
    }
}