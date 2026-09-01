package com.anisync.android.presentation.discover.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.EmptyState
import com.anisync.android.presentation.components.EmptyStateAction

/**
 * Nothing came back at all.
 *
 * Every rail owns its own request, so this only shows when they all failed, which in practice
 * means the network. A single rail failing gets [SectionErrorCard] instead and leaves the rest of
 * the screen alone.
 */
@Composable
fun DiscoverOfflineState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.Public,
        title = stringResource(R.string.discover_empty_offline_title),
        description = stringResource(R.string.discover_empty_offline_desc),
        actionLabel = stringResource(R.string.discover_empty_action_retry),
        actionIcon = Icons.Default.Refresh,
        onAction = onRetry,
        emblemShape = RoundedCornerShape(22.dp),
        emblemContainer = MaterialTheme.colorScheme.secondaryContainer,
        emblemContent = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
    )
}

/**
 * Every rail switched off in the reorder sheet.
 *
 * Emphasised, because unlike the offline state the viewer caused this one and a single tap undoes
 * it. That is the same rule the library applies to a list filtered down to nothing.
 */
@Composable
fun DiscoverAllHiddenState(
    onReorder: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.VisibilityOff,
        title = stringResource(R.string.discover_empty_hidden_title),
        description = stringResource(R.string.discover_empty_hidden_desc),
        actionLabel = stringResource(R.string.discover_reorder_title),
        actionIcon = Icons.AutoMirrored.Filled.Sort,
        onAction = onReorder,
        emblemShape = RoundedCornerShape(22.dp),
        emblemContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
        emblemContent = MaterialTheme.colorScheme.onSurfaceVariant,
        actionEmphasised = true,
        modifier = modifier
    )
}

/**
 * A search that matched nothing.
 *
 * Filters are the usual cause, so when any are on the action clears them and takes the emphasis;
 * with none on there is nothing to undo and the state just reports the result.
 */
@Composable
fun DiscoverNoResultsState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.Search,
        title = stringResource(R.string.discover_empty_search_title),
        description = stringResource(
            if (hasFilters) R.string.discover_empty_search_desc_filtered
            else R.string.discover_empty_search_desc
        ),
        // With no filters on there is nothing to undo, and a Retry on a search that genuinely
        // matched nothing would just run it again.
        actionLabel = if (hasFilters) stringResource(R.string.discover_empty_action_clear_filters) else null,
        actionIcon = if (hasFilters) Icons.Default.Close else null,
        onAction = if (hasFilters) onClearFilters else null,
        emblemShape = RoundedCornerShape(22.dp),
        emblemContainer = MaterialTheme.colorScheme.secondaryContainer,
        emblemContent = MaterialTheme.colorScheme.onSecondaryContainer,
        actionEmphasised = hasFilters,
        animationKey = hasFilters,
        modifier = modifier
    )
}

/**
 * One rail's request failed.
 *
 * Sits where the rail would be, under that rail's own header, so the failure is attributed to a
 * section rather than to the screen. Without it the section simply vanished, which reads as
 * Discover having decided there is nothing to show.
 */
@Composable
fun SectionErrorCard(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.discover_section_error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.discover_section_error_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        EmptyStateAction(
            label = stringResource(R.string.discover_empty_action_retry),
            icon = Icons.Default.Refresh,
            emphasised = false,
            onClick = onRetry,
            modifier = Modifier.height(36.dp)
        )
    }
}
