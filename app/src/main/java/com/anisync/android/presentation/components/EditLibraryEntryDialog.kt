package com.anisync.android.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Material Design 3 Full-Screen Dialog for editing a library entry.
 *
 * Following M3 specifications:
 * - Full-screen container occupying entire screen
 * - TopAppBar with close icon (navigation), title, and Save action
 * - HorizontalDivider between header and content
 * - Surface with surface color background
 *
 * @see <a href="https://m3.material.io/components/dialogs/overview">M3 Dialog Overview</a>
 * @see <a href="https://m3.material.io/components/dialogs/specs">M3 Dialog Specs</a>
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLibraryEntryDialog(
    entry: LibraryEntry,
    onDismiss: () -> Unit,
    onSave: (LibraryEntry) -> Unit
) {
    // Local mutable state for form fields
    var status by remember(entry) { mutableStateOf(entry.status) }
    var score by remember(entry) { mutableStateOf(entry.score?.toFloat() ?: 0f) }
    var rewatches by remember(entry) { mutableStateOf(entry.rewatches) }
    var notes by remember(entry) { mutableStateOf(entry.notes ?: "") }
    var startedAt by remember(entry) { mutableStateOf(entry.startedAt) }
    var completedAt by remember(entry) { mutableStateOf(entry.completedAt) }

    // Date picker state
    var showDatePickerFor by remember { mutableStateOf<String?>(null) }
    val datePickerState = rememberDatePickerState()

    // Modal DatePickerDialog
    if (showDatePickerFor != null) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            when (showDatePickerFor) {
                                "start" -> startedAt = selectedDate
                                "end" -> completedAt = selectedDate
                            }
                        }
                        showDatePickerFor = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Full-Screen Dialog
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Scaffold(
                topBar = {
                    FullScreenDialogTopBar(
                        title = "Update Progress",
                        onCloseClick = onDismiss,
                        onSaveClick = {
                            onSave(
                                entry.copy(
                                    status = status,
                                    score = score.toDouble(),
                                    rewatches = rewatches,
                                    notes = notes,
                                    startedAt = startedAt,
                                    completedAt = completedAt
                                )
                            )
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Status Dropdown
                        StatusDropdown(
                            selectedStatus = status,
                            onStatusSelected = { status = it }
                        )

                        // Score Slider
                        ScoreSlider(
                            score = score,
                            onScoreChange = { score = it }
                        )

                        // Rewatches Input
                        RewatchesInput(
                            rewatches = rewatches,
                            onRewatchesChange = { rewatches = it }
                        )

                        // Date Fields
                        DateFieldsRow(
                            startedAt = startedAt,
                            completedAt = completedAt,
                            onStartDateClick = {
                                datePickerState.selectedDateMillis = startedAt
                                showDatePickerFor = "start"
                            },
                            onEndDateClick = {
                                datePickerState.selectedDateMillis = completedAt
                                showDatePickerFor = "end"
                            }
                        )

                        // Notes Input
                        NotesInput(
                            notes = notes,
                            onNotesChange = { notes = it }
                        )
                    }
                }
            }
        }
    }
}

/**
 * TopAppBar for full-screen dialog following M3 specifications.
 * - Navigation icon: Close (X) icon
 * - Title: Centered headline
 * - Action: Save text button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenDialogTopBar(
    title: String,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        },
        actions = {
            TextButton(onClick = onSaveClick) {
                Text("Save")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Status dropdown using ExposedDropdownMenuBox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    selectedStatus: LibraryStatus,
    onStatusSelected: (LibraryStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = formatStatusName(selectedStatus.name),
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Filter out UNKNOWN status as it's not a valid user selection
            LibraryStatus.entries
                .filter { it != LibraryStatus.UNKNOWN }
                .forEach { status ->
                    DropdownMenuItem(
                        text = { Text(formatStatusName(status.name)) },
                        onClick = {
                            onStatusSelected(status)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
        }
    }
}

/**
 * Score slider with label and current value display.
 */
@Composable
private fun ScoreSlider(
    score: Float,
    onScoreChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Score",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Score: ${String.format(Locale.US, "%.1f", score)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = score,
            onValueChange = { newValue ->
                // Round to nearest 0.5 for cleaner values
                val rounded = (newValue * 2).toInt() / 2f
                onScoreChange(rounded)
            },
            valueRange = 0f..10f,
            steps = 19, // 0.5 increments
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Rewatches input field.
 */
@Composable
private fun RewatchesInput(
    rewatches: Int,
    onRewatchesChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = if (rewatches == 0) "" else rewatches.toString(),
        onValueChange = { input ->
            if (input.isEmpty()) {
                onRewatchesChange(0)
            } else {
                // Only accept positive integers
                input.toIntOrNull()?.let { value ->
                    if (value >= 0) onRewatchesChange(value)
                }
            }
        },
        label = { Text("Rewatches") },
        placeholder = { Text("0") },
        leadingIcon = {
            Icon(Icons.Outlined.Refresh, contentDescription = "Rewatches")
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

/**
 * Row containing start and end date fields.
 */
@Composable
private fun DateFieldsRow(
    startedAt: Long?,
    completedAt: Long?,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DateField(
            label = "Started",
            timestamp = startedAt,
            onClick = onStartDateClick,
            modifier = Modifier.weight(1f)
        )
        DateField(
            label = "Finished",
            timestamp = completedAt,
            onClick = onEndDateClick,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual date field with click handling.
 * Uses Box wrapper for reliable click handling.
 */
@Composable
private fun DateField(
    label: String,
    timestamp: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clickable(
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        )
    ) {
        OutlinedTextField(
            value = formatTimestamp(timestamp),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text("Select date") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select $label date",
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = Color.Transparent
            )
        )
    }
}

/**
 * Notes text input field.
 */
@Composable
private fun NotesInput(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Notes") },
        placeholder = { Text("Add notes about this entry...") },
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        minLines = 3,
        maxLines = 8
    )
}

// --- Utility Functions ---

/**
 * Formats a timestamp to a human-readable date string.
 * Uses short format (MM/dd/yy) to prevent text wrapping in narrow fields.
 */
private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return ""
    val formatter = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

/**
 * Formats an enum name to a human-readable string.
 * Example: "CURRENT" -> "Current", "PLANNING" -> "Planning"
 */
private fun formatStatusName(name: String): String {
    return name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
}