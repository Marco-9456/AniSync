package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.anisync.android.R

/**
 * A small, reusable badge component for displaying status information.
 * Follows Material 3 design system.
 *
 * @param text The text to display inside the badge
 * @param containerColor The background color of the badge
 * @param contentColor The text color of the badge
 * @param modifier Modifier for the composable
 * @param announceChanges If true, announces changes to screen readers via liveRegion
 */
@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    announceChanges: Boolean = false
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .height(14.dp)
            .then(
                if (announceChanges) {
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                color = contentColor
            )
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, showSystemUi = false,
    device = "spec:width=1080px,height=2340px,dpi=640"
)
@Composable
private fun StatusBadgeUpToDatePreview() {
    MaterialTheme {
        StatusBadge(
            text = stringResource(R.string.badge_up_to_date),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgeBehindPreview() {
    MaterialTheme {
        StatusBadge(
            text = stringResource(R.string.episodes_behind, 3),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
