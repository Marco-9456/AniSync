package com.anisync.android.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.TagStat
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.ui.theme.LocalExpressiveTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagCloudSection(tags: List<TagStat>) {
    if (tags.isEmpty()) return
    val expressive = LocalExpressiveTypography.current
    val maxCount = tags.maxOf { it.count }.coerceAtLeast(1)

    Column {
        SectionHeader(
            title = stringResource(R.string.statistics_top_tags),
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
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val frequency = tag.count.toFloat() / maxCount
                    val style = when {
                        frequency > 0.66f -> MaterialTheme.typography.headlineSmall
                        frequency > 0.33f -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.bodyMedium
                    }
                    val containerColor = when {
                        frequency > 0.66f -> MaterialTheme.colorScheme.primaryContainer
                        frequency > 0.33f -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val contentColor = when {
                        frequency > 0.66f -> MaterialTheme.colorScheme.onPrimaryContainer
                        frequency > 0.33f -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(containerColor)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag.name,
                            style = style,
                            color = contentColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tag.count.toString(),
                            style = expressive.numericMono,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "TagCloud — typical (mixed frequency)", widthDp = 360)
@Composable
private fun TagCloudTypicalPreview() {
    StatPreviewSurface(isDark = false) {
        TagCloudSection(tags = previewTags)
    }
}

@Preview(showBackground = true, name = "TagCloud — uniform frequency", widthDp = 360)
@Composable
private fun TagCloudUniformPreview() {
    StatPreviewSurface(isDark = false) {
        TagCloudSection(tags = previewTags.map { it.copy(count = 5) })
    }
}

@Preview(showBackground = true, name = "TagCloud — empty (renders nothing)", widthDp = 360, heightDp = 80)
@Composable
private fun TagCloudEmptyPreview() {
    StatPreviewSurface(isDark = false) {
        Column(Modifier.padding(16.dp)) {
            TagCloudSection(tags = emptyList())
            Text(
                text = "(intentionally renders nothing — early return on empty list)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "TagCloud — dark", widthDp = 360)
@Composable
private fun TagCloudDarkPreview() {
    StatPreviewSurface(isDark = true) {
        TagCloudSection(tags = previewTags)
    }
}

// endregion
