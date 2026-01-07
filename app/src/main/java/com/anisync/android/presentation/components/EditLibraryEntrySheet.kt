package com.anisync.android.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A beautiful Material Design 3 Expressive bottom sheet for editing library entries.
 * Supports editing status, progress, score, dates, rewatches/rereads, and notes.
 *
 * @param entry The library entry to edit
 * @param onDismiss Callback when the sheet is dismissed
 * @param onSave Callback when changes are saved, provides the updated entry
 * @param onDelete Callback when the delete button is clicked
 * @param sheetState Optional sheet state for controlling the bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditLibraryEntrySheet(
    entry: LibraryEntry,
    onDismiss: () -> Unit,
    onSave: (LibraryEntry) -> Unit,
    onDelete: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    // Mutable state for all editable fields
    var status by remember(entry) { mutableStateOf(entry.status) }
    var progress by remember(entry) { mutableIntStateOf(entry.progress) }
    var score by remember(entry) { mutableDoubleStateOf(entry.score ?: 0.0) }
    var notes by remember(entry) { mutableStateOf(entry.notes ?: "") }
    var startedAt by remember(entry) { mutableStateOf(entry.startedAt) }
    var completedAt by remember(entry) { mutableStateOf(entry.completedAt) }
    var rewatches by remember(entry) { mutableIntStateOf(entry.rewatches) }

    // Track if any changes were made
    val hasChanges by remember {
        derivedStateOf {
            status != entry.status ||
                    progress != entry.progress ||
                    score != (entry.score ?: 0.0) ||
                    notes != (entry.notes ?: "") ||
                    startedAt != entry.startedAt ||
                    completedAt != entry.completedAt ||
                    rewatches != entry.rewatches
        }
    }

    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Date picker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showCompletedDatePicker by remember { mutableStateOf(false) }

    val isAnime = entry.type == MediaType.ANIME

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            HeaderSection(entry = entry)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Status Section
            StatusSection(
                status = status,
                mediaType = entry.type,
                onStatusChange = { status = it }
            )

            // Progress Section
            ProgressSection(
                progress = progress,
                total = entry.totalEpisodes ?: entry.totalChapters,
                isAnime = isAnime,
                onProgressChange = { progress = it }
            )

            // Score Section
            ScoreSection(
                score = score,
                onScoreChange = { score = it }
            )

            // Date Section
            DateSection(
                startedAt = startedAt,
                completedAt = completedAt,
                onStartClick = { showStartDatePicker = true },
                onCompletedClick = { showCompletedDatePicker = true }
            )

            // Rewatches/Rereads Section
            RewatchSection(
                rewatches = rewatches,
                isAnime = isAnime,
                onRewatchChange = { rewatches = it }
            )

            // Notes Section
            NotesSection(
                notes = notes,
                onNotesChange = { notes = it }
            )

            // Action Buttons
            ActionButtons(
                onDelete = { showDeleteDialog = true },
                onSave = {
                    onSave(
                        entry.copy(
                            status = status,
                            progress = progress,
                            score = score.takeIf { it > 0 },
                            notes = notes.ifBlank { null },
                            startedAt = startedAt,
                            completedAt = completedAt,
                            rewatches = rewatches
                        )
                    )
                },
                saveEnabled = hasChanges
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Start Date Picker
    if (showStartDatePicker) {
        DatePickerSheet(
            initialDate = startedAt,
            onDateSelected = { startedAt = it },
            onDismiss = { showStartDatePicker = false }
        )
    }

    // Completed Date Picker
    if (showCompletedDatePicker) {
        DatePickerSheet(
            initialDate = completedAt,
            onDateSelected = { completedAt = it },
            onDismiss = { showCompletedDatePicker = false }
        )
    }
}

// -------------------------
// HEADER SECTION
// -------------------------

@Composable
private fun HeaderSection(entry: LibraryEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(entry.coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.content_description_cover),
            modifier = Modifier
                .size(width = 70.dp, height = 100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_entry),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // Media type badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = if (entry.type == MediaType.ANIME) 
                        stringResource(R.string.media_type_anime) 
                    else 
                        stringResource(R.string.media_type_manga),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// -------------------------
// STATUS SECTION
// -------------------------

private data class StatusOption(
    val status: LibraryStatus,
    val icon: ImageVector,
    val labelResId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSection(
    status: LibraryStatus,
    mediaType: MediaType?,
    onStatusChange: (LibraryStatus) -> Unit
) {
    val isAnime = mediaType == MediaType.ANIME

    val statusOptions = remember(isAnime) {
        listOf(
            StatusOption(
                LibraryStatus.CURRENT,
                Icons.Outlined.PlayArrow,
                if (isAnime) R.string.status_watching else R.string.status_reading
            ),
            StatusOption(LibraryStatus.COMPLETED, Icons.Outlined.Check, R.string.status_completed),
            StatusOption(LibraryStatus.PLANNING, Icons.Outlined.Schedule, R.string.status_planning),
            StatusOption(LibraryStatus.PAUSED, Icons.Outlined.Pause, R.string.status_paused),
            StatusOption(LibraryStatus.DROPPED, Icons.Outlined.Close, R.string.status_dropped)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.filter_status),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Use horizontally scrollable row with chips for better space efficiency
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusOptions.forEach { option ->
                val isSelected = status == option.status
                FilterChip(
                    selected = isSelected,
                    onClick = { onStatusChange(option.status) },
                    label = { Text(stringResource(option.labelResId)) },
                    leadingIcon = {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

// -------------------------
// PROGRESS SECTION
// -------------------------

@Composable
private fun ProgressSection(
    progress: Int,
    total: Int?,
    isAnime: Boolean,
    onProgressChange: (Int) -> Unit
) {
    val maxProgress = total ?: 999
    val sliderValue by animateFloatAsState(
        targetValue = progress.toFloat(),
        label = "progress_slider"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.sort_progress),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (total != null) "$progress / $total" else "$progress / ?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Decrement button
            FilledTonalIconButton(
                onClick = { if (progress > 0) onProgressChange(progress - 1) },
                enabled = progress > 0,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.descending)
                )
            }

            // Slider
            Slider(
                value = sliderValue,
                onValueChange = { onProgressChange(it.toInt()) },
                valueRange = 0f..maxProgress.toFloat(),
                steps = if (maxProgress > 1) maxProgress - 1 else 0,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )

            // Increment button
            FilledTonalIconButton(
                onClick = { if (total == null || progress < total) onProgressChange(progress + 1) },
                enabled = total == null || progress < total,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.ascending)
                )
            }
        }

        // Episodes/Chapters label
        Text(
            text = if (isAnime) stringResource(R.string.stat_episodes) else stringResource(R.string.stat_chapters),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// -------------------------
// SCORE SECTION
// -------------------------

@Composable
private fun ScoreSection(
    score: Double,
    onScoreChange: (Double) -> Unit
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        label = "score_animation"
    )

    val scoreColor by animateColorAsState(
        targetValue = when {
            score >= 8.0 -> MaterialTheme.colorScheme.primary
            score >= 6.0 -> MaterialTheme.colorScheme.tertiary
            score >= 4.0 -> MaterialTheme.colorScheme.secondary
            score > 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "score_color"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stat_score),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = scoreColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = if (score > 0) String.format(Locale.US, "%.1f", score) else stringResource(R.string.no_score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor
            )
        }

        Slider(
            value = animatedScore,
            onValueChange = { onScoreChange((it * 2).toInt() / 2.0) }, // Round to 0.5
            valueRange = 0f..10f,
            steps = 19, // 0, 0.5, 1, 1.5, ... 10
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = scoreColor,
                activeTrackColor = scoreColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )

        // Score scale labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "10",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// -------------------------
// DATE SECTION
// -------------------------

@Composable
private fun DateSection(
    startedAt: Long?,
    completedAt: Long?,
    onStartClick: () -> Unit,
    onCompletedClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.dates),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Date
            DateChip(
                label = stringResource(R.string.start_date),
                date = startedAt?.let { dateFormat.format(Date(it)) },
                onClick = onStartClick,
                modifier = Modifier.weight(1f)
            )

            // Completed Date
            DateChip(
                label = stringResource(R.string.completed_date),
                date = completedAt?.let { dateFormat.format(Date(it)) },
                onClick = onCompletedClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DateChip(
    label: String,
    date: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = date ?: stringResource(R.string.no_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (date != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// -------------------------
// REWATCH SECTION
// -------------------------

@Composable
private fun RewatchSection(
    rewatches: Int,
    isAnime: Boolean,
    onRewatchChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isAnime) stringResource(R.string.times_rewatched) else stringResource(R.string.times_reread),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledTonalIconButton(
                onClick = { if (rewatches > 0) onRewatchChange(rewatches - 1) },
                enabled = rewatches > 0,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.descending)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = rewatches.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            FilledTonalIconButton(
                onClick = { onRewatchChange(rewatches + 1) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.ascending)
                )
            }
        }
    }
}

// -------------------------
// NOTES SECTION
// -------------------------

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.notes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.notes_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// -------------------------
// ACTION BUTTONS
// -------------------------

@Composable
private fun ActionButtons(
    onDelete: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Delete Button
        FilledTonalButton(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.action_remove))
        }

        // Save Button - using Button for consistency
        Button(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.save),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// -------------------------
// DELETE CONFIRMATION DIALOG
// -------------------------

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.action_remove))
        },
        text = {
            Text(text = stringResource(R.string.delete_entry_confirm))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(R.string.action_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

// -------------------------
// DATE PICKER DIALOG
// -------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initialDate: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// -------------------------
// PREVIEWS
// -------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PreviewEditLibraryEntrySheet() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val mockEntry = LibraryEntry(
                    id = 1,
                    mediaId = 101,
                    title = "Frieren: Beyond Journey's End",
                    coverUrl = null,
                    progress = 12,
                    totalEpisodes = 28,
                    totalChapters = null,
                    totalVolumes = null,
                    type = MediaType.ANIME,
                    status = LibraryStatus.CURRENT,
                    score = 9.5,
                    startedAt = System.currentTimeMillis() - 86400000L * 30,
                    completedAt = null,
                    rewatches = 0,
                    notes = "Great anime!"
                )

                HeaderSection(entry = mockEntry)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatusSection(
                    status = mockEntry.status,
                    mediaType = mockEntry.type,
                    onStatusChange = {}
                )
                ProgressSection(
                    progress = mockEntry.progress,
                    total = mockEntry.totalEpisodes,
                    isAnime = true,
                    onProgressChange = {}
                )
                ScoreSection(score = mockEntry.score ?: 0.0, onScoreChange = {})
                DateSection(
                    startedAt = mockEntry.startedAt,
                    completedAt = mockEntry.completedAt,
                    onStartClick = {},
                    onCompletedClick = {}
                )
                RewatchSection(rewatches = 0, isAnime = true, onRewatchChange = {})
                NotesSection(notes = "", onNotesChange = {})
                ActionButtons(onDelete = {}, onSave = {}, saveEnabled = true)
            }
        }
    }
}
