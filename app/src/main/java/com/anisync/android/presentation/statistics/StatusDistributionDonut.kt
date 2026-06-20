package com.anisync.android.presentation.statistics

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.StatusUiModel
import com.anisync.android.ui.theme.LocalExpressiveTypography

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun StatusDistributionDonut(
    statuses: List<StatusUiModel>,
    isManga: Boolean = false
) {
    val expressive = LocalExpressiveTypography.current
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.outline
    )
    val total = statuses.sumOf { it.count }

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_status_breakdown),
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
            // The ring (fixed) and its legend, arranged side by side when the card is wide enough,
            // or stacked (ring above a full-width legend) when narrow — so the legend labels never
            // get crushed into a sliver (which made "Completed" wrap one character per line).
            val ring = @Composable {
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        val stroke = 22.dp.toPx()
                        val pad = stroke / 2f
                        var sweepStart = -90f
                        statuses.forEachIndexed { idx, s ->
                            val sweep = s.fraction * 360f
                            val color = palette[idx % palette.size]
                            drawArc(
                                color = color,
                                startAngle = sweepStart,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(pad, pad),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width - stroke,
                                    size.height - stroke
                                ),
                                style = Stroke(width = stroke, cap = StrokeCap.Butt)
                            )
                            sweepStart += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = total.toString(),
                            style = expressive.statNumericMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "ENTRIES",
                            style = expressive.statLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val legend = @Composable { legendModifier: Modifier ->
                Column(
                    modifier = legendModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statuses.forEachIndexed { idx, s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(palette[idx % palette.size], CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = statusDisplayName(s.status, isManga),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = s.count.toString(),
                                style = expressive.numericMono,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 380.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ring()
                        legend(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        ring()
                        legend(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun statusDisplayName(status: String, isManga: Boolean): String = when (status) {
    "CURRENT" -> stringResource(if (isManga) R.string.status_reading else R.string.status_watching)
    "COMPLETED" -> stringResource(R.string.status_completed)
    "PLANNING" -> stringResource(R.string.status_planning)
    "PAUSED" -> stringResource(R.string.status_paused)
    "DROPPED" -> stringResource(R.string.status_dropped)
    "REPEATING" -> stringResource(if (isManga) R.string.status_rereading else R.string.status_rewatching)
    else -> status.lowercase().replaceFirstChar { it.uppercase() }
}

// region Previews

@Preview(showBackground = true, name = "StatusDonut — anime typical", widthDp = 360)
@Composable
private fun StatusDonutAnimePreview() {
    StatPreviewSurface(isDark = false) {
        StatusDistributionDonut(statuses = previewStatuses, isManga = false)
    }
}

@Preview(showBackground = true, name = "StatusDonut — manga labels", widthDp = 360)
@Composable
private fun StatusDonutMangaPreview() {
    StatPreviewSurface(isDark = false) {
        StatusDistributionDonut(statuses = previewStatuses, isManga = true)
    }
}

@Preview(showBackground = true, name = "StatusDonut — single status", widthDp = 360)
@Composable
private fun StatusDonutSinglePreview() {
    StatPreviewSurface(isDark = false) {
        StatusDistributionDonut(statuses = previewSingleStatus, isManga = false)
    }
}

@Preview(showBackground = true, name = "StatusDonut — dark", widthDp = 360)
@Composable
private fun StatusDonutDarkPreview() {
    StatPreviewSurface(isDark = true) {
        StatusDistributionDonut(statuses = previewStatuses, isManga = false)
    }
}

// endregion
