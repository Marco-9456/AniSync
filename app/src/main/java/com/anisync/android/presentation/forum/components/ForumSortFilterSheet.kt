package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ThreadSortOption
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.util.bouncyClickable

/**
 * How the hub list is ordered, in the shape Library's "Sort & filter" set: an uppercase section
 * label with the direction toggle beside it, a two-column grid of columns to sort by, and a filled
 * confirm button.
 *
 * Narrowing is not here. The category is the rail's, and media and author belong to the search
 * overlay where a picker can actually run; a chip row that only ever cleared them was a control
 * pretending to be one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumSortFilterSheet(
    sort: ThreadSortOption,
    resultCount: Int,
    onSortChange: (ThreadSortOption) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 470.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.forum_sort_and_filter),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.forum_reset),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .bouncyClickable(onClick = onReset, clipShape = CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
            Spacer(Modifier.height(20.dp))

            ForumSortSection(sort = sort, onSortChange = onSortChange)

            Text(
                text = stringResource(R.string.forum_sort_best_match_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
            )
        }

        // Out of the scroll: a sheet whose only way to apply is below the fold reads as having none.
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onApply,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.forum_show_threads_count, resultCount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
