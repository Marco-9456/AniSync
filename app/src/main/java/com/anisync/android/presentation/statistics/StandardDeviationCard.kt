package com.anisync.android.presentation.statistics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.util.formatDecimal
import com.anisync.android.ui.theme.LocalExpressiveTypography

@Composable
fun StandardDeviationCard(standardDeviation: Double, meanScore: Double) {
    if (standardDeviation <= 0.0) return
    val expressive = LocalExpressiveTypography.current
    val interpretation = when {
        standardDeviation < 1.0 -> stringResource(R.string.statistics_taste_focused)
        standardDeviation < 2.0 -> stringResource(R.string.statistics_taste_balanced)
        else -> stringResource(R.string.statistics_taste_eclectic)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.statistics_std_dev).uppercase(),
                style = expressive.statLabel,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatDecimal(standardDeviation),
                    style = expressive.statNumericLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "vs mean ${formatDecimal(meanScore)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = interpretation,
                style = expressive.editorialLead,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "StdDev — focused (<1)", widthDp = 360)
@Composable
private fun StdDevFocusedPreview() {
    StatPreviewSurface(isDark = false) {
        StandardDeviationCard(standardDeviation = 0.7, meanScore = 8.1)
    }
}

@Preview(showBackground = true, name = "StdDev — balanced (<2)", widthDp = 360)
@Composable
private fun StdDevBalancedPreview() {
    StatPreviewSurface(isDark = false) {
        StandardDeviationCard(standardDeviation = 1.4, meanScore = 7.4)
    }
}

@Preview(showBackground = true, name = "StdDev — eclectic (>=2)", widthDp = 360)
@Composable
private fun StdDevEclecticPreview() {
    StatPreviewSurface(isDark = false) {
        StandardDeviationCard(standardDeviation = 2.6, meanScore = 6.8)
    }
}

@Preview(showBackground = true, name = "StdDev — zero (early-return)", widthDp = 360, heightDp = 80)
@Composable
private fun StdDevZeroPreview() {
    StatPreviewSurface(isDark = false) {
        Column(Modifier.padding(16.dp)) {
            StandardDeviationCard(standardDeviation = 0.0, meanScore = 0.0)
            Text(
                text = "(intentionally renders nothing — early return on std-dev ≤ 0)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "StdDev — dark", widthDp = 360)
@Composable
private fun StdDevDarkPreview() {
    StatPreviewSurface(isDark = true) {
        StandardDeviationCard(standardDeviation = 1.4, meanScore = 7.4)
    }
}

// endregion
