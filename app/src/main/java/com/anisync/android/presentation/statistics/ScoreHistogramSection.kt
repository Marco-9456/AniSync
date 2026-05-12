package com.anisync.android.presentation.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.profile.ScoreUiModel
import com.anisync.android.presentation.util.formatDecimal
import com.anisync.android.ui.theme.LocalExpressiveTypography

@Composable
fun ScoreHistogramSection(
    scores: List<ScoreUiModel>,
    meanScore: Double
) {
    val expressive = LocalExpressiveTypography.current
    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_score_distribution),
            level = HeaderLevel.Section,
            padding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            val onContainer = MaterialTheme.colorScheme.onTertiaryContainer
            Column(Modifier.padding(24.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.statistics_mean_score).uppercase(),
                            style = expressive.statLabel,
                            color = onContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatDecimal(meanScore),
                            style = expressive.statNumericMedium,
                            color = onContainer
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    scores.forEach { item ->
                        val animatedHeight = remember { Animatable(0f) }

                        LaunchedEffect(item.heightFraction) {
                            animatedHeight.animateTo(
                                item.heightFraction,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .weight(1f, fill = false)
                                    .fillMaxHeight(if (item.count > 0) animatedHeight.value else 0.02f)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(
                                        when {
                                            item.count == 0 -> onContainer.copy(alpha = 0.2f)
                                            item.normalizedScore >= 0.8f -> onContainer
                                            item.normalizedScore >= 0.5f -> onContainer.copy(alpha = 0.65f)
                                            else -> onContainer.copy(alpha = 0.4f)
                                        }
                                    )
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = item.label,
                                style = expressive.numericMono,
                                color = if (item.count > 0)
                                    onContainer
                                else
                                    onContainer.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "ScoreHistogram — typical", widthDp = 360)
@Composable
private fun ScoreHistogramTypicalPreview() {
    StatPreviewSurface(isDark = false) {
        ScoreHistogramSection(scores = previewScores, meanScore = 7.4)
    }
}

@Preview(showBackground = true, name = "ScoreHistogram — all zero", widthDp = 360)
@Composable
private fun ScoreHistogramAllZeroPreview() {
    StatPreviewSurface(isDark = false) {
        ScoreHistogramSection(scores = previewScoresAllZero, meanScore = 0.0)
    }
}

@Preview(showBackground = true, name = "ScoreHistogram — dark", widthDp = 360)
@Composable
private fun ScoreHistogramDarkPreview() {
    StatPreviewSurface(isDark = true) {
        ScoreHistogramSection(scores = previewScores, meanScore = 7.4)
    }
}

// endregion
