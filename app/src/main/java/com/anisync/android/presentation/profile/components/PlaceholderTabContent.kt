package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.EmptyState

/** A list that did not load, with Retry. */
@Composable
internal fun LoadFailedState(title: String, description: String, onRetry: () -> Unit) {
    EmptyState(
        icon = Icons.Default.CloudOff,
        title = title,
        description = description,
        actionLabel = stringResource(R.string.retry),
        actionIcon = Icons.Default.Refresh,
        onAction = onRetry,
        modifier = Modifier.padding(top = 32.dp)
    )
}

@Composable
fun PlaceholderTabContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
