package com.anisync.android.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.domain.media.MediaHost

@Composable
fun MediaUploadSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaUploadSettingsViewModel = hiltViewModel()
) {
    val host by viewModel.mediaHost.collectAsStateWithLifecycle()
    val imgurClientId by viewModel.imgurClientId.collectAsStateWithLifecycle()
    val customUrl by viewModel.customHostUrl.collectAsStateWithLifecycle()
    val customField by viewModel.customHostFileField.collectAsStateWithLifecycle()
    val customAuth by viewModel.customHostAuthHeader.collectAsStateWithLifecycle()
    val customJsonPath by viewModel.customHostResponseJsonPath.collectAsStateWithLifecycle()

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_media_upload),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SectionHeader(stringResource(R.string.media_upload_intro))

        SettingsGroup {
            HostRow(host, MediaHost.CATBOX, R.string.media_upload_host_catbox, R.string.media_upload_host_catbox_desc, viewModel)
            SettingsDivider()
            HostRow(host, MediaHost.LITTERBOX_1H, R.string.media_upload_host_litterbox_1h, R.string.media_upload_host_litterbox_desc, viewModel)
            SettingsDivider()
            HostRow(host, MediaHost.LITTERBOX_24H, R.string.media_upload_host_litterbox_24h, R.string.media_upload_host_litterbox_desc, viewModel)
            SettingsDivider()
            HostRow(host, MediaHost.LITTERBOX_72H, R.string.media_upload_host_litterbox_72h, R.string.media_upload_host_litterbox_desc, viewModel)
            SettingsDivider()
            HostRow(host, MediaHost.IMGUR, R.string.media_upload_host_imgur, R.string.media_upload_host_imgur_desc, viewModel)
            SettingsDivider()
            HostRow(host, MediaHost.CUSTOM, R.string.media_upload_host_custom, R.string.media_upload_host_custom_desc, viewModel)
        }

        AnimatedVisibility(visible = host == MediaHost.IMGUR) {
            SettingsGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.media_upload_imgur_client_id),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = imgurClientId,
                        onValueChange = viewModel::setImgurClientId,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.media_upload_imgur_client_id_hint)) }
                    )
                    Text(
                        text = stringResource(R.string.media_upload_imgur_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(visible = host == MediaHost.CUSTOM) {
            SettingsGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LabeledField(
                        label = stringResource(R.string.media_upload_custom_url),
                        value = customUrl,
                        onValueChange = viewModel::setCustomHostUrl,
                        placeholder = stringResource(R.string.media_upload_custom_url_hint)
                    )
                    LabeledField(
                        label = stringResource(R.string.media_upload_custom_field),
                        value = customField,
                        onValueChange = viewModel::setCustomHostFileField,
                        placeholder = "fileToUpload"
                    )
                    LabeledField(
                        label = stringResource(R.string.media_upload_custom_auth),
                        value = customAuth,
                        onValueChange = viewModel::setCustomHostAuthHeader,
                        placeholder = stringResource(R.string.media_upload_custom_auth_hint)
                    )
                    LabeledField(
                        label = stringResource(R.string.media_upload_custom_json_path),
                        value = customJsonPath,
                        onValueChange = viewModel::setCustomHostResponseJsonPath,
                        placeholder = stringResource(R.string.media_upload_custom_json_path_hint)
                    )
                    Text(
                        text = stringResource(R.string.media_upload_custom_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HostRow(
    current: MediaHost,
    host: MediaHost,
    titleRes: Int,
    subtitleRes: Int,
    viewModel: MediaUploadSettingsViewModel
) {
    RadioSettingsItem(
        title = stringResource(titleRes),
        subtitle = stringResource(subtitleRes),
        selected = current == host,
        onClick = { viewModel.setMediaHost(host) }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder) }
        )
    }
}
