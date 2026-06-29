package com.anisync.android.presentation.components.richtext

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.media.MediaKind
import com.anisync.android.domain.media.MediaSizeChoice
import com.anisync.android.domain.media.toImageMarkdown
import com.anisync.android.domain.media.videoMarkdown
import com.anisync.android.presentation.components.AppModalBottomSheet
import kotlinx.coroutines.launch

/**
 * Bottom-sheet attach flow shared by every composer surface. Drives the
 * [MediaAttachViewModel] state machine; on success calls [onMarkdownReady] with
 * the AniList markdown to insert at the cursor, then dismisses itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaAttachSheet(
    viewModel: MediaAttachViewModel,
    onDismiss: () -> Unit,
    onMarkdownReady: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var urlInputMode by remember { mutableStateOf(UrlInputMode.None) }

    val close: () -> Unit = {
        coroutineScope.launch { sheetState.hide() }
        viewModel.reset()
        onDismiss()
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.pick(uri)
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.pick(uri)
    }

    AppModalBottomSheet(
        onDismissRequest = close,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.media_attach),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            when (val s = state) {
                MediaAttachState.Idle -> {
                    if (urlInputMode != UrlInputMode.None) {
                        UrlEntry(
                            initialMode = urlInputMode,
                            onCancel = { urlInputMode = UrlInputMode.None },
                            onInsert = { url, mode ->
                                val md = if (mode == UrlInputMode.Video) videoMarkdown(url)
                                else MediaSizeChoice.Default.toImageMarkdown(url)
                                onMarkdownReady(md)
                                urlInputMode = UrlInputMode.None
                                close()
                            }
                        )
                    } else {
                        AttachAction(
                            icon = Icons.Filled.Image,
                            title = stringResource(R.string.media_attach_pick_image),
                            subtitle = stringResource(R.string.media_attach_pick_image_desc),
                            onClick = {
                                imageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        AttachAction(
                            icon = Icons.Filled.Movie,
                            title = stringResource(R.string.media_attach_pick_video),
                            subtitle = stringResource(R.string.media_attach_pick_video_desc),
                            onClick = {
                                videoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        AttachAction(
                            icon = Icons.Outlined.Link,
                            title = stringResource(R.string.media_attach_paste_url),
                            subtitle = stringResource(R.string.media_attach_paste_url_desc),
                            onClick = { urlInputMode = UrlInputMode.Image }
                        )
                    }
                }

                is MediaAttachState.Picked -> PickedView(
                    state = s,
                    onSizeSelected = viewModel::setSize,
                    onCustomSizeChange = viewModel::setCustomSizeText,
                    onCancel = close,
                    onUpload = {
                        viewModel.upload { md ->
                            onMarkdownReady(md)
                            close()
                        }
                    }
                )

                is MediaAttachState.Uploading -> UploadingView(s, onCancel = {
                    viewModel.cancel()
                })

                is MediaAttachState.Failed -> FailedView(
                    state = s,
                    onCancel = close,
                    onRetry = {
                        viewModel.retry { md ->
                            onMarkdownReady(md)
                            close()
                        }
                    }
                )
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

private enum class UrlInputMode { None, Image, Video }

@Composable
private fun AttachAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PickedView(
    state: MediaAttachState.Picked,
    onSizeSelected: (MediaSizeChoice) -> Unit,
    onCustomSizeChange: (String) -> Unit,
    onCancel: () -> Unit,
    onUpload: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.kind != MediaKind.Video) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val fraction = sizeToWidthFraction(state.size)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .animateContentSize(tween(300))
                ) {
                    AsyncImage(
                        model = state.uri,
                        contentDescription = state.displayName,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (state.kind != MediaKind.Video) {
            Text(
                text = stringResource(R.string.media_attach_size_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SizeChip(stringResource(R.string.media_attach_size_small), state.size is MediaSizeChoice.Small) {
                    onSizeSelected(MediaSizeChoice.Small)
                }
                SizeChip(stringResource(R.string.media_attach_size_medium), state.size is MediaSizeChoice.Medium) {
                    onSizeSelected(MediaSizeChoice.Medium)
                }
                SizeChip(stringResource(R.string.media_attach_size_large), state.size is MediaSizeChoice.Large) {
                    onSizeSelected(MediaSizeChoice.Large)
                }
                SizeChip(stringResource(R.string.media_attach_size_original), state.size is MediaSizeChoice.Original) {
                    onSizeSelected(MediaSizeChoice.Original)
                }
                SizeChip(
                    stringResource(R.string.media_attach_size_custom),
                    state.size is MediaSizeChoice.CustomPx || state.size is MediaSizeChoice.CustomPercent
                ) {
                    onSizeSelected(MediaSizeChoice.CustomPx(300))
                }
            }
            Text(
                text = stringResource(R.string.media_attach_size_output, state.size.toImageMarkdown("…")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            if (state.size is MediaSizeChoice.CustomPx || state.size is MediaSizeChoice.CustomPercent) {
                OutlinedTextField(
                    value = state.customSizeText,
                    onValueChange = onCustomSizeChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.media_attach_size_custom_hint)) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.media_attach_cancel))
            }
            Button(onClick = onUpload, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.media_attach_upload))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UploadingView(state: MediaAttachState.Uploading, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.media_attach_uploading),
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = state.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (state.total > 0) {
            val targetProgress = (state.uploaded.toFloat() / state.total).coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "UploadProgressAnimation"
            )
            val percent = (targetProgress * 100).toInt()
            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.media_attach_cancel))
        }
    }
}

@Composable
private fun FailedView(
    state: MediaAttachState.Failed,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
        Text(
            text = state.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.media_attach_cancel))
            }
            Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.media_attach_retry))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun UrlEntry(
    initialMode: UrlInputMode,
    onCancel: () -> Unit,
    onInsert: (String, UrlInputMode) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(if (initialMode == UrlInputMode.None) UrlInputMode.Image else initialMode) }
    val invalid = text.isNotBlank() && !text.startsWith("http", ignoreCase = true)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.media_attach_url_input),
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == UrlInputMode.Image,
                onClick = { mode = UrlInputMode.Image },
                label = { Text(stringResource(R.string.media_attach_pick_image)) }
            )
            FilterChip(
                selected = mode == UrlInputMode.Video,
                onClick = { mode = UrlInputMode.Video },
                label = { Text(stringResource(R.string.media_attach_pick_video)) }
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = invalid,
            placeholder = { Text(stringResource(R.string.media_attach_url_input_hint)) }
        )
        if (invalid) {
            Text(
                text = stringResource(R.string.media_attach_error_invalid_url),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.media_attach_cancel))
            }
            Button(
                onClick = { onInsert(text.trim(), mode) },
                enabled = text.isNotBlank() && !invalid,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.media_attach_insert))
            }
        }
    }
}

private fun sizeToWidthFraction(size: MediaSizeChoice): Float = when (size) {
    is MediaSizeChoice.Small -> 0.25f
    is MediaSizeChoice.Medium -> 0.45f
    is MediaSizeChoice.Large -> 0.65f
    is MediaSizeChoice.Original -> 1f
    is MediaSizeChoice.CustomPx -> (size.pixels / 1200f).coerceIn(0.1f, 1f)
    is MediaSizeChoice.CustomPercent -> (size.percent / 100f).coerceIn(0.05f, 1f)
}
