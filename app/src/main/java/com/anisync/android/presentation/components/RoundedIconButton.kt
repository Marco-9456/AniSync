package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.util.bouncyClickable

/**
 * A reusable icon button with rounded square shape per MD3 specs.
 * Uses 40dp container with 8dp corner radius (MD3 standard icon button shape).
 *
 * @param icon The icon to display inside the button
 * @param contentDescription Accessibility description for the icon
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun RoundedIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp), // MD3 standard icon button shape
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        modifier = modifier
            .size(40.dp)
            .bouncyClickable(pressedScale = 0.9f, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * @deprecated Use [RoundedIconButton] instead. This alias is kept for backward compatibility.
 */
@Deprecated(
    message = "Use RoundedIconButton instead",
    replaceWith = ReplaceWith("RoundedIconButton(icon, contentDescription, onClick, modifier)")
)
@Composable
fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = RoundedIconButton(icon, contentDescription, onClick, modifier)

// --- PREVIEWS ---

@Preview(showBackground = true)
@Composable
private fun RoundedIconButtonSortPreview() {
    MaterialTheme {
        RoundedIconButton(
            icon = Icons.AutoMirrored.Filled.Sort,
            contentDescription = "Sort",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoundedIconButtonGridPreview() {
    MaterialTheme {
        RoundedIconButton(
            icon = Icons.Default.GridView,
            contentDescription = "Toggle view",
            onClick = {}
        )
    }
}
