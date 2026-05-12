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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.YearUiModel
import com.anisync.android.ui.theme.LocalExpressiveTypography

@Composable
fun YearComparisonSection(
    release: List<YearUiModel>,
    start: List<YearUiModel>
) {
    val expressive = LocalExpressiveTypography.current
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_start_year),
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
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    LegendDot(MaterialTheme.colorScheme.primary, "STARTED")
                    LegendDot(MaterialTheme.colorScheme.tertiaryContainer, "AIRED")
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxLen = maxOf(release.size, start.size)
                    val releasePadded = release.padTo(maxLen)
                    val startPadded = start.padTo(maxLen)
                    for (i in 0 until maxLen) {
                        val s = startPadded.getOrNull(i)
                        val r = releasePadded.getOrNull(i)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.height(80.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .fillMaxHeight(
                                            (s?.heightFraction ?: 0f).coerceAtLeast(0.02f)
                                        )
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .fillMaxHeight(
                                            (r?.heightFraction ?: 0f).coerceAtLeast(0.02f)
                                        )
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "'${(s?.year ?: r?.year ?: 0).toString().takeLast(2)}",
                                style = expressive.numericMono.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun <T> List<T>.padTo(size: Int): List<T?> =
    (0 until size).map { i -> getOrNull(i) }

@Composable
private fun LegendDot(color: Color, label: String) {
    val expressive = LocalExpressiveTypography.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = expressive.statLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// region Previews

@Preview(showBackground = true, name = "YearComparison — typical", widthDp = 360)
@Composable
private fun YearComparisonTypicalPreview() {
    StatPreviewSurface(isDark = false) {
        YearComparisonSection(release = previewYears, start = previewStartYears)
    }
}

@Preview(showBackground = true, name = "YearComparison — uneven lengths", widthDp = 360)
@Composable
private fun YearComparisonUnevenPreview() {
    StatPreviewSurface(isDark = false) {
        YearComparisonSection(release = previewYears, start = previewStartYears.take(2))
    }
}

@Preview(showBackground = true, name = "YearComparison — start empty", widthDp = 360)
@Composable
private fun YearComparisonStartEmptyPreview() {
    StatPreviewSurface(isDark = false) {
        YearComparisonSection(release = previewYears, start = emptyList())
    }
}

@Preview(showBackground = true, name = "YearComparison — dark", widthDp = 360)
@Composable
private fun YearComparisonDarkPreview() {
    StatPreviewSurface(isDark = true) {
        YearComparisonSection(release = previewYears, start = previewStartYears)
    }
}

// endregion
