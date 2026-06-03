package com.anisync.android.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.media.MediaHost

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaUploadSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaUploadSettingsViewModel = hiltViewModel()
) {
    val host by viewModel.mediaHost.collectAsStateWithLifecycle()
    val litterboxDuration by viewModel.litterboxDuration.collectAsStateWithLifecycle()
    val customUrl by viewModel.customHostUrl.collectAsStateWithLifecycle()
    val customField by viewModel.customHostFileField.collectAsStateWithLifecycle()
    val customAuth by viewModel.customHostAuthHeader.collectAsStateWithLifecycle()
    val customJsonPath by viewModel.customHostResponseJsonPath.collectAsStateWithLifecycle()
    val catboxUserHash by viewModel.catboxUserHash.collectAsStateWithLifecycle()
    var showHashDialog by rememberSaveable { mutableStateOf(false) }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_media_upload),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        IntroCard()

        Spacer(Modifier.height(8.dp))

        SettingsGroup {
            Column {
                HostRow(
                    current = host,
                    value = MediaHost.CATBOX,
                    titleRes = R.string.media_upload_host_catbox,
                    subtitleRes = R.string.media_upload_host_catbox_desc,
                    icon = Icons.Outlined.Cloud,
                    onSelect = viewModel::setMediaHost
                )
                AnimatedVisibility(
                    visible = host == MediaHost.CATBOX,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        onClick = { showHashDialog = true },
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        CatboxAccountRow(userHash = catboxUserHash)
                    }
                }
            }
            SettingsDivider()
            Column {
                HostRow(
                    current = host,
                    value = MediaHost.LITTERBOX,
                    titleRes = R.string.media_upload_host_litterbox,
                    subtitleRes = R.string.media_upload_host_litterbox_desc,
                    icon = Icons.Outlined.Schedule,
                    onSelect = viewModel::setMediaHost
                )
                AnimatedVisibility(
                    visible = host == MediaHost.LITTERBOX,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        LitterboxDurationRow(
                            selected = litterboxDuration,
                            onSelect = viewModel::setLitterboxDuration
                        )
                    }
                }
            }
            SettingsDivider()
            Column {
                HostRow(
                    current = host,
                    value = MediaHost.CUSTOM,
                    titleRes = R.string.media_upload_host_custom,
                    subtitleRes = R.string.media_upload_host_custom_desc,
                    icon = Icons.Outlined.Tune,
                    onSelect = viewModel::setMediaHost
                )
                AnimatedVisibility(
                    visible = host == MediaHost.CUSTOM,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        CustomHostCard(
                            url = customUrl,
                            field = customField,
                            auth = customAuth,
                            jsonPath = customJsonPath,
                            onUrlChange = viewModel::setCustomHostUrl,
                            onFieldChange = viewModel::setCustomHostFileField,
                            onAuthChange = viewModel::setCustomHostAuthHeader,
                            onJsonPathChange = viewModel::setCustomHostResponseJsonPath
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    if (showHashDialog) {
        CatboxUserHashDialog(
            initial = catboxUserHash,
            onSave = {
                viewModel.setCatboxUserHash(it)
                showHashDialog = false
            },
            onDismiss = { showHashDialog = false }
        )
    }
}

@Composable
private fun IntroCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.media_upload_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HostRow(
    current: MediaHost,
    value: MediaHost,
    titleRes: Int,
    subtitleRes: Int,
    onSelect: (MediaHost) -> Unit,
    icon: ImageVector? = null
) {
    RadioSettingsItem(
        title = stringResource(titleRes),
        subtitle = stringResource(subtitleRes),
        selected = current == value,
        onClick = { onSelect(value) },
        icon = icon
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LitterboxDurationRow(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 20.dp, bottom = 16.dp, top = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.media_upload_litterbox_expiry),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DurationChip("1h", R.string.media_upload_litterbox_1h, selected, onSelect)
            DurationChip("24h", R.string.media_upload_litterbox_24h, selected, onSelect)
            DurationChip("72h", R.string.media_upload_litterbox_72h, selected, onSelect)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationChip(
    value: String,
    labelRes: Int,
    selected: String,
    onSelect: (String) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(stringResource(labelRes)) }
    )
}

@Composable
private fun CustomHostCard(
    url: String,
    field: String,
    auth: String,
    jsonPath: String,
    onUrlChange: (String) -> Unit,
    onFieldChange: (String) -> Unit,
    onAuthChange: (String) -> Unit,
    onJsonPathChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LabeledField(
            icon = Icons.Outlined.Link,
            label = stringResource(R.string.media_upload_custom_url),
            value = url,
            onValueChange = onUrlChange,
            placeholder = stringResource(R.string.media_upload_custom_url_hint)
        )
        LabeledField(
            icon = Icons.Outlined.UploadFile,
            label = stringResource(R.string.media_upload_custom_field),
            value = field,
            onValueChange = onFieldChange,
            placeholder = "fileToUpload"
        )
        LabeledField(
            icon = Icons.Outlined.Lock,
            label = stringResource(R.string.media_upload_custom_auth),
            value = auth,
            onValueChange = onAuthChange,
            placeholder = stringResource(R.string.media_upload_custom_auth_hint)
        )
        LabeledField(
            icon = Icons.Outlined.Code,
            label = stringResource(R.string.media_upload_custom_json_path),
            value = jsonPath,
            onValueChange = onJsonPathChange,
            placeholder = stringResource(R.string.media_upload_custom_json_path_hint)
        )
        Text(
            text = stringResource(R.string.media_upload_custom_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Status row revealed under the Catbox host. Shows whether uploads are bound to an account and,
 * when they are, a masked preview of the hash. Tapping the row (handled by the parent [Surface])
 * opens [CatboxUserHashDialog] to edit or clear it.
 */
@Composable
private fun CatboxAccountRow(userHash: String) {
    val connected = userHash.isNotBlank()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            imageVector = if (connected) Icons.Outlined.CloudDone else Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.media_upload_catbox_account),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (connected) {
                    stringResource(R.string.media_upload_catbox_account_connected, maskUserHash(userHash))
                } else {
                    stringResource(R.string.media_upload_catbox_account_anonymous)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Show only the head and tail so the full credential is never rendered in the settings list. */
private fun maskUserHash(hash: String): String =
    if (hash.length <= 8) hash else "${hash.take(4)}…${hash.takeLast(4)}"

@Composable
private fun CatboxUserHashDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.media_upload_catbox_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.media_upload_catbox_userhash)) },
                    placeholder = { Text(stringResource(R.string.media_upload_catbox_userhash_hint)) },
                    trailingIcon = {
                        if (draft.isNotEmpty()) {
                            IconButton(onClick = { draft = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.media_upload_catbox_userhash_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft.trim()) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun LabeledField(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder) }
        )
    }
}