package com.anisync.android.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * A redesign of the EditLibraryEntryDialog.
 *
 * It uses State Hoisting:
 * - [EditLibraryEntryDialog] handles the domain model (LibraryEntry) and saving logic.
 * - [EditLibraryEntryContent] handles the pure UI and interactions, making it easily previewable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLibraryEntryDialog(
    entry: LibraryEntry,
    onDismiss: () -> Unit,
    onSave: (LibraryEntry) -> Unit
) {
    // Deconstruct entry into local state - key by entry to ensure updates
    var status by remember(entry) { mutableStateOf(entry.status) }
    var score by remember(entry) { mutableStateOf(entry.score?.toFloat() ?: 0f) }
    var rewatches by remember(entry) { mutableStateOf(entry.rewatches) }
    var notes by remember(entry) { mutableStateOf(entry.notes ?: "") }
    var startedAt by remember(entry) { mutableStateOf(entry.startedAt) }
    var completedAt by remember(entry) { mutableStateOf(entry.completedAt) }

    // Date Picker Logic
    var showDatePickerFor by remember { mutableStateOf<String?>(null) }
    val datePickerState = rememberDatePickerState()

    if (showDatePickerFor != null) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            if (showDatePickerFor == "start") {
                                startedAt = selectedDate
                            } else {
                                completedAt = selectedDate
                            }
                        }
                        showDatePickerFor = null
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        EditLibraryEntryContent(
            status = status,
            score = score,
            rewatches = rewatches,
            notes = notes,
            startedAt = startedAt,
            completedAt = completedAt,
            statusOptions = LibraryStatus.entries,
            onStatusChange = { status = it },
            onScoreChange = { score = it },
            onRewatchesChange = { rewatches = it },
            onNotesChange = { notes = it },
            onDateClick = { type, currentMillis ->
                datePickerState.selectedDateMillis = currentMillis
                showDatePickerFor = type
            },
            onDismiss = onDismiss,
            onSave = {
                onSave(
                    entry.copy(
                        status = status,
                        score = score.toDouble(),
                        rewatches = rewatches,
                        startedAt = startedAt,
                        completedAt = completedAt,
                        notes = notes
                    )
                )
            }
        )
    }
}

/**
 * Pure UI Component for editing a library entry.
 * Decoupled from LibraryEntry to allow for easy Previewing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLibraryEntryContent(
    status: LibraryStatus,
    score: Float,
    rewatches: Int,
    notes: String,
    startedAt: Long?,
    completedAt: Long?,
    statusOptions: List<LibraryStatus>,
    onStatusChange: (LibraryStatus) -> Unit,
    onScoreChange: (Float) -> Unit,
    onRewatchesChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onDateClick: (String, Long?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Update Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // --- Status ---
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = formatEnumName(status.name),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    statusOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(formatEnumName(option.name)) },
                            onClick = {
                                onStatusChange(option)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // --- Score ---
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
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
                    onValueChange = onScoreChange,
                    valueRange = 0f..10f,
                    steps = 19, // 0.5 increments
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- Rewatches ---
            OutlinedTextField(
                value = if (rewatches == 0) "" else rewatches.toString(),
                onValueChange = {
                    if (it.isEmpty()) onRewatchesChange(0)
                    else it.toIntOrNull()?.let(onRewatchesChange)
                },
                label = { Text("Rewatches") },
                placeholder = { Text("0") },
                leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // --- Dates ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start Date
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = formatDate(startedAt),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Started") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp)) },
                        enabled = false, // We handle click on the box
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = Color.Transparent
                        )
                    )
                    // Overlay for click
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { onDateClick("start", startedAt) }
                    )
                }

                // End Date
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = formatDate(completedAt),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Finished") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp)) },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = Color.Transparent
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { onDateClick("end", completedAt) }
                    )
                }
            }

            // --- Notes ---
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 5
            )

            // --- Actions ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSave) {
                    Text("Save")
                }
            }
        }
    }
}

// --- Helpers ---

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return ""
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatEnumName(name: String): String {
    return name.lowercase().replaceFirstChar { it.uppercase() }
        .replace("_", " ")
}


// --- Preview ---

/**
 * Preview for the Edit Dialog Content.
 * NOTE: This assumes LibraryStatus.entries is available.
 * If your project doesn't have it, adjust the statusOptions parameter.
 */
@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun EditLibraryEntryContentPreview() {
    // Mock data for preview purposes
    val mockStatuses = try {
        LibraryStatus.entries
    } catch (e: Exception) {
        // Fallback if enum entries aren't accessible in preview context (though they should be)
        emptyList()
    }

    // If enum is empty (e.g. IDE preview issue), we construct a dummy fallback
    val safeStatuses = if (mockStatuses.isNotEmpty()) mockStatuses else listOf()
    val initialStatus = if (safeStatuses.isNotEmpty()) safeStatuses.first() else null

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            // We use a Box to simulate the dialog container background in preview
            if (initialStatus != null) {
                EditLibraryEntryContent(
                    status = initialStatus,
                    score = 7.5f,
                    rewatches = 2,
                    notes = "This was a really great series! Highly recommended.",
                    startedAt = System.currentTimeMillis() - 86400000L * 30, // 30 days ago
                    completedAt = System.currentTimeMillis(),
                    statusOptions = safeStatuses,
                    onStatusChange = {},
                    onScoreChange = {},
                    onRewatchesChange = {},
                    onNotesChange = {},
                    onDateClick = { _, _ -> },
                    onDismiss = {},
                    onSave = {}
                )
            } else {
                Text("LibraryStatus enum not found for preview.")
            }
        }
    }
}