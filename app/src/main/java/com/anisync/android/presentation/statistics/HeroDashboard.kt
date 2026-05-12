package com.anisync.android.presentation.statistics

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.ui.theme.LocalExpressiveTypography

/**
 * MD3 Expressive HeroDashboard.
 *
 * Editorial layout (per m3.material.io/styles/typography/editorial-treatments):
 *  - eyebrow label (caps, wide tracking, low size)
 *  - mixed-weight baseline-aligned row: huge W900 numeric + W400 unit
 *  - optional editorial-lead sentence ("≈ 42 days of your life")
 *  - divider
 *  - up to three sub-stats using tabular figures
 */
@Composable
fun HeroDashboard(
    primaryValue: String,
    primaryUnit: String,
    primaryLabel: String,
    secondaryRow: List<EditorialStat>,
    accentText: String? = null,
    modifier: Modifier = Modifier
) {
    val expressive = LocalExpressiveTypography.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = 28.dp,
                bottom = 24.dp
            )
        ) {
            Text(
                text = primaryLabel.uppercase(),
                style = expressive.statLabel,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            val heroStyle = when {
                primaryValue.length > 6 -> expressive.heroNumeric.copy(fontSize = 56.sp, lineHeight = 56.sp)
                primaryValue.length > 4 -> expressive.heroNumeric.copy(fontSize = 72.sp, lineHeight = 72.sp)
                else -> expressive.heroNumeric
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = primaryValue,
                    style = heroStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = primaryUnit,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    softWrap = true,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(bottom = 14.dp)
                )
            }
            accentText?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = expressive.editorialLead,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
            if (secondaryRow.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    secondaryRow.forEach { stat ->
                        EditorialStatBlock(stat, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialStatBlock(stat: EditorialStat, modifier: Modifier = Modifier) {
    val expressive = LocalExpressiveTypography.current
    Column(modifier) {
        if (stat.icon != null) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = stat.value,
            style = expressive.statNumericMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1
        )
        Text(
            text = stat.label.uppercase(),
            style = expressive.statLabel,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            maxLines = 2
        )
    }
}

// region Previews

@Preview(showBackground = true, name = "HeroDashboard — typical anime", widthDp = 360)
@Composable
private fun HeroDashboardTypicalPreview() {
    StatPreviewSurface(isDark = false) {
        HeroDashboard(
            primaryValue = "1,248",
            primaryUnit = "episodes",
            primaryLabel = "Watched",
            accentText = "≈ 42 days of your life",
            secondaryRow = listOf(
                EditorialStat("128", "Total", Icons.Default.PlayArrow),
                EditorialStat("8.1", "Mean", Icons.Default.Star),
                EditorialStat("1.4", "σ")
            )
        )
    }
}

@Preview(showBackground = true, name = "HeroDashboard — no accent + long unit", widthDp = 360)
@Composable
private fun HeroDashboardNoAccentLongUnitPreview() {
    StatPreviewSurface(isDark = false) {
        HeroDashboard(
            primaryValue = "42",
            primaryUnit = "ridiculously-long-unit-name",
            primaryLabel = "Read",
            accentText = null,
            secondaryRow = listOf(
                EditorialStat("12", "Volumes"),
                EditorialStat("7.9", "Mean")
            )
        )
    }
}

@Preview(showBackground = true, name = "HeroDashboard — empty secondary row", widthDp = 360)
@Composable
private fun HeroDashboardEmptySecondaryPreview() {
    StatPreviewSurface(isDark = false) {
        HeroDashboard(
            primaryValue = "0",
            primaryUnit = "entries",
            primaryLabel = "Library",
            secondaryRow = emptyList()
        )
    }
}

@Preview(showBackground = true, name = "HeroDashboard — dark", widthDp = 360, heightDp = 320)
@Composable
private fun HeroDashboardDarkPreview() {
    StatPreviewSurface(isDark = true) {
        HeroDashboard(
            primaryValue = "1,248",
            primaryUnit = "episodes",
            primaryLabel = "Watched",
            accentText = "≈ 42 days of your life",
            secondaryRow = listOf(
                EditorialStat("128", "Total", Icons.Default.PlayArrow),
                EditorialStat("8.1", "Mean", Icons.Default.Star),
                EditorialStat("1.4", "σ")
            )
        )
    }
}

// endregion
