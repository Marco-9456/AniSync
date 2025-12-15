package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * A reusable circular icon button with bouncy click interaction.
 * Follows Material 3 design system with secondaryContainer styling.
 *
 * @param icon The icon to display inside the button
 * @param contentDescription Accessibility description for the icon
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
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

// --- PREVIEWS ---

@Preview(showBackground = true)
@Composable
private fun CircularIconButtonSortPreview() {
    MaterialTheme {
        CircularIconButton(
            icon = Icons.AutoMirrored.Filled.Sort,
            contentDescription = "Sort",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CircularIconButtonGridPreview() {
    MaterialTheme {
        CircularIconButton(
            icon = Icons.Default.GridView,
            contentDescription = "Toggle view",
            onClick = {}
        )
    }
}
