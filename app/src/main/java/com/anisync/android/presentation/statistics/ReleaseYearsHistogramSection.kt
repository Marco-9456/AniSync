package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.anisync.android.R
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.YearUiModel
import com.anisync.android.ui.theme.LocalExpressiveTypography

@Composable
fun ReleaseYearsHistogramSection(years: List<YearUiModel>) {
    val expressive = LocalExpressiveTypography.current
    val peakYear = years.maxByOrNull { it.count }
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_by_year),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                if (peakYear != null && peakYear.count > 0) {
                    Text(
                        text = stringResource(R.string.statistics_peak_year).uppercase(),
                        style = expressive.statLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = peakYear.year.toString(),
                            style = expressive.statNumericLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${peakYear.count} titles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    years.forEach { stat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (stat.count > 0) {
                                Text(
                                    text = stat.count.toString(),
                                    style = expressive.numericMono.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(80.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(stat.heightFraction.coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "'${stat.year.toString().takeLast(2)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "ReleaseYears — typical", widthDp = 360)
@Composable
private fun ReleaseYearsTypicalPreview() {
    StatPreviewSurface(isDark = false) {
        ReleaseYearsHistogramSection(years = previewYears)
    }
}

@Preview(showBackground = true, name = "ReleaseYears — single peak", widthDp = 360)
@Composable
private fun ReleaseYearsSinglePeakPreview() {
    StatPreviewSurface(isDark = false) {
        ReleaseYearsHistogramSection(years = previewSingleYear)
    }
}

@Preview(showBackground = true, name = "ReleaseYears — empty", widthDp = 360)
@Composable
private fun ReleaseYearsEmptyPreview() {
    StatPreviewSurface(isDark = false) {
        ReleaseYearsHistogramSection(years = emptyList())
    }
}

@Preview(showBackground = true, name = "ReleaseYears — dark", widthDp = 360)
@Composable
private fun ReleaseYearsDarkPreview() {
    StatPreviewSurface(isDark = true) {
        ReleaseYearsHistogramSection(years = previewYears)
    }
}

// endregion
