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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.LengthUiModel
import com.anisync.android.ui.theme.LocalExpressiveTypography

@Composable
fun EpisodeLengthDistributionSection(
    lengths: List<LengthUiModel>,
    title: String = stringResource(R.string.statistics_length_distribution)
) {
    if (lengths.isEmpty()) return
    val expressive = LocalExpressiveTypography.current
    Column {
        SectionHeader(
            title = title,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(24.dp),
                horizontalArrangement = if (lengths.size <= 2) Arrangement.spacedBy(
                    24.dp,
                    Alignment.CenterHorizontally
                ) else Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                lengths.forEach { len ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = if (lengths.size <= 2) Modifier.width(64.dp) else Modifier.weight(
                            1f
                        )
                    ) {
                        Text(
                            text = len.count.toString(),
                            style = expressive.numericMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .widthIn(max = 48.dp)
                                .fillMaxWidth(0.7f)
                                .height(90.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(len.heightFraction.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = len.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "EpisodeLength — >2 buckets (weight branch)", widthDp = 360)
@Composable
private fun EpisodeLengthManyPreview() {
    StatPreviewSurface(isDark = false) {
        EpisodeLengthDistributionSection(lengths = previewLengths)
    }
}

@Preview(showBackground = true, name = "EpisodeLength — <=2 buckets (fixed-width branch)", widthDp = 360)
@Composable
private fun EpisodeLengthFewPreview() {
    StatPreviewSurface(isDark = false) {
        EpisodeLengthDistributionSection(lengths = previewLengthsShort)
    }
}

@Preview(showBackground = true, name = "EpisodeLength — empty (early-return)", widthDp = 360, heightDp = 80)
@Composable
private fun EpisodeLengthEmptyPreview() {
    StatPreviewSurface(isDark = false) {
        Column(Modifier.padding(16.dp)) {
            EpisodeLengthDistributionSection(lengths = emptyList())
            Text(
                text = "(intentionally renders nothing — early return on empty list)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "EpisodeLength — dark", widthDp = 360)
@Composable
private fun EpisodeLengthDarkPreview() {
    StatPreviewSurface(isDark = true) {
        EpisodeLengthDistributionSection(lengths = previewLengths)
    }
}

// endregion
