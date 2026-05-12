package com.anisync.android.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.ui.theme.LocalExpressiveTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeSpentBreakdown(minutesWatched: Int) {
    val totalMinutes = minutesWatched.coerceAtLeast(0)
    val days = totalMinutes / 1440
    val hours = (totalMinutes % 1440) / 60
    val mins = totalMinutes % 60

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_time_breakdown),
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
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimeUnitBlock(days.toString(), "DAYS")
                TimeUnitBlock(hours.toString(), "HOURS")
                TimeUnitBlock(mins.toString(), "MIN")
            }
        }
    }
}

/**
 * Manga-side variant: shows chapter + volume breakdown rather than time.
 * Editorial mixed-weight FlowRow same as TimeSpentBreakdown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadVolumeBreakdown(chaptersRead: Int, volumesRead: Int) {
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_time_breakdown),
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
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimeUnitBlock(chaptersRead.toString(), "CHAPTERS")
                TimeUnitBlock(volumesRead.toString(), "VOLUMES")
            }
        }
    }
}

@Composable
private fun TimeUnitBlock(value: String, label: String) {
    val expressive = LocalExpressiveTypography.current
    Column {
        Text(
            text = value,
            style = expressive.statNumericLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = expressive.statLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// region Previews

@Preview(showBackground = true, name = "TimeSpentBreakdown — typical", widthDp = 360)
@Composable
private fun TimeSpentTypicalPreview() {
    StatPreviewSurface(isDark = false) {
        TimeSpentBreakdown(minutesWatched = 60_421)
    }
}

@Preview(showBackground = true, name = "TimeSpentBreakdown — zero", widthDp = 360)
@Composable
private fun TimeSpentZeroPreview() {
    StatPreviewSurface(isDark = false) {
        TimeSpentBreakdown(minutesWatched = 0)
    }
}

@Preview(showBackground = true, name = "TimeSpentBreakdown — dark", widthDp = 360)
@Composable
private fun TimeSpentDarkPreview() {
    StatPreviewSurface(isDark = true) {
        TimeSpentBreakdown(minutesWatched = 60_421)
    }
}

@Preview(showBackground = true, name = "ReadVolumeBreakdown — typical", widthDp = 360)
@Composable
private fun ReadVolumeTypicalPreview() {
    StatPreviewSurface(isDark = false) {
        ReadVolumeBreakdown(chaptersRead = 1432, volumesRead = 218)
    }
}

@Preview(showBackground = true, name = "ReadVolumeBreakdown — chapters only", widthDp = 360)
@Composable
private fun ReadVolumeChaptersOnlyPreview() {
    StatPreviewSurface(isDark = false) {
        ReadVolumeBreakdown(chaptersRead = 87, volumesRead = 0)
    }
}

@Preview(showBackground = true, name = "ReadVolumeBreakdown — dark", widthDp = 360)
@Composable
private fun ReadVolumeDarkPreview() {
    StatPreviewSurface(isDark = true) {
        ReadVolumeBreakdown(chaptersRead = 1432, volumesRead = 218)
    }
}

// endregion
