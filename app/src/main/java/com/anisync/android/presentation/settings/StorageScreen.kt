package com.anisync.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.RadioButton
import com.anisync.android.data.CoverQuality
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.presentation.components.LocalWhatsNew
import com.anisync.android.presentation.components.WhatsNewPill
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val whatsNewStore = LocalWhatsNew.current
    // Read once on entry so the pill stays up for the visit that clears it.
    val showCoverQualityMoved = remember {
        WhatsNewItem.STORAGE_COVER_QUALITY in whatsNewStore.active.value
    }
    LaunchedEffect(Unit) { whatsNewStore.markSeen(WhatsNewItem.STORAGE_COVER_QUALITY) }
    val cacheSize = uiState.cacheSize
    val isCacheCleared = uiState.isCacheCleared
    val isCacheLoading = uiState.isCacheLoading
    val isCacheClearing = uiState.isCacheClearing

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_storage),
        onBackClick = onBackClick,
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
                    AppCircularProgressIndicator(
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
                onClick = { viewModel.onAction(SettingsAction.ClearCache) },
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

        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionLabel(stringResource(R.string.settings_section_covers))
        SettingsGroup {
            SettingsBlock(modifier = Modifier.selectableGroup()) {
                Row(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_cover_quality),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (showCoverQualityMoved) WhatsNewPill(WhatsNewItem.Kind.MOVED)
                        }
                        Text(
                            text = stringResource(R.string.settings_cover_quality_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                CoverQuality.entries.forEach { quality ->
                    CoverQualityOption(
                        quality = quality,
                        selected = quality == uiState.coverQuality,
                        onClick = { viewModel.onAction(SettingsAction.SetCoverQuality(quality)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverQualityOption(
    quality: CoverQuality,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val (label, description) = when (quality) {
        CoverQuality.EXTRA_LARGE -> R.string.cover_quality_extra_large to R.string.cover_quality_extra_large_desc
        CoverQuality.LARGE -> R.string.cover_quality_large to R.string.cover_quality_large_desc
        CoverQuality.MEDIUM -> R.string.cover_quality_medium to R.string.cover_quality_medium_desc
    }
    SettingsChoiceTile(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}