package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.forum.ForumFeed
import com.anisync.android.presentation.util.bouncyClickable

/**
 * The five feeds. A connected toggle is the app's two-way switch, so five options get a sheet off
 * the pinned rail button instead of a third strip of chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumFeedSheet(
    selected: ForumFeed,
    onSelect: (ForumFeed) -> Unit,
    onDismiss: () -> Unit
) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.forum_choose_feed),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
        Spacer(Modifier.height(8.dp))

        ForumFeed.entries.forEach { feed ->
            val isSelected = feed == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable(
                        onClick = { onSelect(feed) },
                        clipShape = RoundedCornerShape(0.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = feed.icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(feed.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
