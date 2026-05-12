package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.FormatStat
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.ui.theme.LocalExpressiveTypography

@Composable
fun FormatsSection(formats: List<FormatStat>) {
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_formats),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                formats.forEachIndexed { index, format ->
                    FormatRow(format)
                    if (index < formats.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatRow(format: FormatStat) {
    val expressive = LocalExpressiveTypography.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFormatIcon(format.format),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatDisplayName(format.format),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = format.count.toString(),
                style = expressive.statNumericMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            RatingBadge(meanScore = format.meanScore, size = RatingBadgeSize.Small)
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "Formats — every icon path", widthDp = 360)
@Composable
private fun FormatsAllIconsPreview() {
    StatPreviewSurface(isDark = false) {
        FormatsSection(formats = previewFormats)
    }
}

@Preview(showBackground = true, name = "Formats — single TV row", widthDp = 360)
@Composable
private fun FormatsSinglePreview() {
    StatPreviewSurface(isDark = false) {
        FormatsSection(formats = listOf(previewFormats.first()))
    }
}

@Preview(showBackground = true, name = "Formats — unknown format fallback", widthDp = 360)
@Composable
private fun FormatsUnknownFallbackPreview() {
    StatPreviewSurface(isDark = false) {
        FormatsSection(formats = listOf(
            FormatStat(format = "UNKNOWN_NEW_TYPE", count = 3, meanScore = 6.5f, hoursWatched = 4f)
        ))
    }
}

@Preview(showBackground = true, name = "Formats — dark", widthDp = 360)
@Composable
private fun FormatsDarkPreview() {
    StatPreviewSurface(isDark = true) {
        FormatsSection(formats = previewFormats)
    }
}

// endregion
