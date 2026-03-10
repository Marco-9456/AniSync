package com.anisync.android.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListManagementSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    customLists: List<String>,
    hiddenLists: Set<String>,
    listOrder: List<String>,
    onVisibilityChanged: (String, Boolean) -> Unit,
    onOrderMoveUp: (String) -> Unit,
    onOrderMoveDown: (String) -> Unit,
    onDeleteList: (String) -> Unit,
    onCreateList: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var isCreatingList by remember { mutableStateOf(false) }
    var newListTitle by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = {
                // Reset state when sheet is closed
                isCreatingList = false
                newListTitle = ""
                onDismiss()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            val combinedList = remember(listOrder, customLists) {
                val orderSet = listOrder.toSet()
                listOrder.filter { it in customLists } + customLists.filter { it !in orderSet }
            }

            // CRITICAL FIX: Everything is placed inside the LazyColumn.
            // This ensures that when the keyboard opens (.imePadding()), the content simply
            // becomes scrollable instead of shrinking the TextField to 0 height and dropping focus.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 16.dp,
                    bottom = 32.dp
                )
            ) {

                // Header
                item {
                    Text(
                        text = "Manage Custom Lists",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Form / Button
                item {
                    if (isCreatingList) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newListTitle,
                                onValueChange = { newListTitle = it },
                                placeholder = { Text("New List Name") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newListTitle.isNotBlank()) {
                                            onCreateList(newListTitle.trim())
                                        }
                                        isCreatingList = false
                                        newListTitle = ""
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        isCreatingList = false
                                        newListTitle = ""
                                    }
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newListTitle.isNotBlank()) {
                                            onCreateList(newListTitle.trim())
                                        }
                                        isCreatingList = false
                                        newListTitle = ""
                                    },
                                    enabled = newListTitle.isNotBlank()
                                ) {
                                    Text("Save")
                                }
                            }

                            // Request focus after the layout is successfully mounted in the LazyColumn
                            LaunchedEffect(Unit) {
                                delay(100)
                                focusRequester.requestFocus()
                            }
                        }
                    } else {
                        Button(
                            onClick = { isCreatingList = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create New List")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create New List")
                        }
                    }
                }

                // Divider
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // List Items
                itemsIndexed(combinedList, key = { _, item -> item }) { index, listName ->
                    val isHidden = hiddenLists.contains(listName)
                    val isFirst = index == 0
                    val isLast = index == combinedList.lastIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = { onVisibilityChanged(listName, !isHidden) }) {
                                Icon(
                                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isHidden) "Show List" else "Hide List",
                                    tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = listName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onOrderMoveUp(listName) }, enabled = !isFirst) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                            }
                            IconButton(onClick = { onOrderMoveDown(listName) }, enabled = !isLast) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                            }
                            IconButton(onClick = { onDeleteList(listName) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete List",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}