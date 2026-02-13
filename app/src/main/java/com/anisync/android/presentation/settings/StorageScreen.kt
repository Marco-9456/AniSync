package com.anisync.android.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R

/**
 * Storage settings screen.
 * Displays cache usage and allows clearing cache.
 */
@Composable
fun StorageScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val cacheSize by viewModel.cacheSize.collectAsStateWithLifecycle()
    val isCacheCleared by viewModel.isCacheCleared.collectAsStateWithLifecycle()
    val isCacheLoading by viewModel.isCacheLoading.collectAsStateWithLifecycle()
    val isCacheClearing by viewModel.isCacheClearing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val cacheClearedMessage = stringResource(R.string.settings_cache_cleared)

    // Show snackbar when cache is cleared
    LaunchedEffect(isCacheCleared) {
        if (isCacheCleared) {
            snackbarHostState.showSnackbar(cacheClearedMessage)
            viewModel.resetCacheCleared()
        }
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_storage),
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    ) {
        // Cache size display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCacheLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = cacheSize,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                stringResource(R.string.settings_cache_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Clear cache option
        SettingsGroup {
            SettingsItem(
                icon = Icons.Outlined.Delete,
                title = stringResource(R.string.settings_clear_cache),
                subtitle = stringResource(R.string.settings_cache_description, cacheSize),
                onClick = { viewModel.clearCache() },
                enabled = !isCacheClearing
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info text
        Text(
            text = stringResource(R.string.settings_cache_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
