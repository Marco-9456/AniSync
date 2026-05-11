package com.anisync.android.presentation.components

import com.anisync.android.ui.theme.emphasis
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A badge component that displays a score with semantic color coding.
 * Uses Material 3 theme tokens for proper theming support.
 *
 * Score tiers:
 * - High (≥75): Primary colors (positive)
 * - Medium (≥60): Tertiary colors (neutral)
 * - Low (<60): Error colors (warning)
 *
 * @param score The score value (0-100)
 * @param modifier Modifier for the composable
 */
@Composable
fun ScoreBadge(
    score: Int,
    modifier: Modifier = Modifier
) {
    // Semantic colors based on score using M3 tokens
    val (containerColor, contentColor) = when {
        score >= 75 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        score >= 60 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$score%",
                style = MaterialTheme.typography.labelSmall.emphasis(),
                color = contentColor
            )
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "High Score (≥75)")
@Composable
private fun ScoreBadgeHighPreview() {
    MaterialTheme {
        ScoreBadge(score = 85)
    }
}

@Preview(showBackground = true, name = "Medium Score (≥60)")
@Composable
private fun ScoreBadgeMediumPreview() {
    MaterialTheme {
        ScoreBadge(score = 68)
    }
}

@Preview(showBackground = true, name = "Low Score (<60)")
@Composable
private fun ScoreBadgeLowPreview() {
    MaterialTheme {
        ScoreBadge(score = 45)
    }
}
