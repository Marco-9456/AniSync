package com.anisync.android.presentation.details.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.ui.theme.AppTheme
import com.anisync.android.ui.theme.emphasis

/** AniList's own write-up of how the weighted score is worked out. */
const val WEIGHTED_SCORE_THREAD_ID = 89398
const val WEIGHTED_SCORE_THREAD_TITLE = "Weighted Score Explanation"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreInfoSheet(
    title: String,
    weightedScore: String,
    meanScore: String?,
    onReadMore: () -> Unit,
    onDismiss: () -> Unit
) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        ScoreInfoContent(
            title = title,
            weightedScore = weightedScore,
            meanScore = meanScore,
            onReadMore = onReadMore
        )
    }
}

@Composable
private fun ScoreInfoContent(
    title: String,
    weightedScore: String,
    meanScore: String?,
    onReadMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.details_score_info_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        CopyableNamesCard {
            ScoreValueRow(stringResource(R.string.details_score_info_weighted), weightedScore)
            meanScore?.let { ScoreValueRow(stringResource(R.string.details_score_info_mean), it) }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.details_score_info_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.details_score_info_threshold),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onReadMore) {
            Text(stringResource(R.string.details_score_info_read_more))
        }
    }
}

@Composable
private fun ScoreValueRow(label: String, value: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.emphasis(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScoreInfoContentPreview() {
    AppTheme {
        ScoreInfoContent(
            title = "Sousou no Frieren",
            weightedScore = "9.1",
            meanScore = "9.2",
            onReadMore = {}
        )
    }
}
