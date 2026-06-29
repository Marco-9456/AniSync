package com.anisync.android.presentation.library.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.util.LIBRARY_ALL_TAB_ID
import com.anisync.android.presentation.util.libraryTabLabel
import com.anisync.android.presentation.util.toIcon
import com.anisync.android.type.MediaType
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListManagementSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    tabOrder: List<String>,
    customLists: List<String>,
    hiddenLists: Set<String>,
    mediaType: MediaType,
    onVisibilityChanged: (String, Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
    onDeleteList: (String) -> Unit,
    onCreateList: (String, MediaType) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var isCreatingList by remember { mutableStateOf(false) }
    var newListTitle by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MediaType.ANIME) }
    val focusRequester = remember { FocusRequester() }

    if (visible) {
        AppModalBottomSheet(
            onDismissRequest = {
                // Reset state when sheet is closed
                isCreatingList = false
                newListTitle = ""
                onDismiss()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            val hapticFeedback = LocalHapticFeedback.current

            // Maintain a local mutable list for drag-to-reorder.
            // Syncs from the tabOrder whenever it changes externally.
            val localOrder = remember { tabOrder.toMutableStateList() }
            LaunchedEffect(tabOrder) {
                if (localOrder.toList() != tabOrder) {
                    localOrder.clear()
                    localOrder.addAll(tabOrder)
                }
            }

            val lazyListState = rememberLazyListState()
            val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                localOrder.apply {
                    add(to.index - NON_REORDERABLE_ITEM_COUNT, removeAt(from.index - NON_REORDERABLE_ITEM_COUNT))
                }
                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }

            // CRITICAL FIX: Everything is placed inside the LazyColumn.
            // This ensures that when the keyboard opens (.imePadding()), the content simply
            // becomes scrollable instead of shrinking the TextField to 0 height and dropping focus.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 16.dp,
                    bottom = 32.dp
                )
            ) {

                // Header
                item(key = "header") {
                    Text(
                        text = stringResource(R.string.manage_tabs),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Form / Button
                item(key = "create_form") {
                    if (isCreatingList) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newListTitle,
                                onValueChange = { newListTitle = it },
                                placeholder = { Text(stringResource(R.string.new_list_name)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newListTitle.isNotBlank()) {
                                            onCreateList(newListTitle.trim(), selectedType)
                                        }
                                        isCreatingList = false
                                        newListTitle = ""
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                            
                            // Media Type Selection for new list
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.FilterChip(
                                    selected = selectedType == MediaType.ANIME,
                                    onClick = { selectedType = MediaType.ANIME },
                                    label = { Text(stringResource(R.string.media_type_anime)) },
                                    leadingIcon = if (selectedType == MediaType.ANIME) {
                                        { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                                androidx.compose.material3.FilterChip(
                                    selected = selectedType == MediaType.MANGA,
                                    onClick = { selectedType = MediaType.MANGA },
                                    label = { Text(stringResource(R.string.media_type_manga)) },
                                    leadingIcon = if (selectedType == MediaType.MANGA) {
                                        { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                            
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
                                    Text(stringResource(R.string.cancel))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newListTitle.isNotBlank()) {
                                            onCreateList(newListTitle.trim(), selectedType)
                                        }
                                        isCreatingList = false
                                        newListTitle = ""
                                    },
                                    enabled = newListTitle.isNotBlank()
                                ) {
                                    Text(stringResource(R.string.save))
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
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_new_list))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.create_new_list))
                        }
                    }
                }

                // Divider
                item(key = "divider") {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Reorderable Tab Items
                items(localOrder, key = { it }) { tabId ->
                    ReorderableItem(reorderableLazyListState, key = tabId) { isDragging ->
                        // "All" isn't a custom list, so it must not offer the delete affordance.
                        val isCustom = !tabId.startsWith("status:") && tabId != LIBRARY_ALL_TAB_ID
                        val isHidden = hiddenLists.contains(tabId)

                        val elevation by animateDpAsState(
                            if (isDragging) 4.dp else 0.dp,
                            label = "drag_elevation"
                        )

                        Surface(
                            shadowElevation = elevation,
                            tonalElevation = if (isDragging) 2.dp else 0.dp,
                            shape = MaterialTheme.shapes.medium,
                            color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Drag handle
                                IconButton(
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                        },
                                        onDragStopped = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            // Persist the new order
                                            onReorder(localOrder.toList())
                                        },
                                    ),
                                    onClick = {},
                                ) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = stringResource(R.string.cd_reorder),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Tab icon
                                Icon(
                                    imageVector = getTabIcon(tabId, mediaType),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                                           else MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // Tab label
                                TabLabel(
                                    tabId = tabId,
                                    mediaType = mediaType,
                                    isHidden = isHidden,
                                    modifier = Modifier.weight(1f)
                                )

                                // Controls — visibility toggle for all, delete for custom lists only
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onVisibilityChanged(tabId, !isHidden) }) {
                                        Icon(
                                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isHidden) "Show Tab" else "Hide Tab",
                                            tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                                                   else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (isCustom) {
                                        IconButton(onClick = { onDeleteList(tabId) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.cd_delete_list),
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
        }
    }
}

/**
 * Number of non-reorderable items before the reorderable items in the LazyColumn.
 * (header, create_form, divider)
 */
private const val NON_REORDERABLE_ITEM_COUNT = 3

/**
 * Displays the localized label for a tab identifier.
 */
@Composable
private fun TabLabel(
    tabId: String,
    mediaType: MediaType,
    isHidden: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = libraryTabLabel(tabId, mediaType),
        style = MaterialTheme.typography.bodyLarge,
        color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/**
 * Returns the appropriate icon for a tab identifier.
 */
private fun getTabIcon(tabId: String, mediaType: MediaType): ImageVector = when {
    tabId == LIBRARY_ALL_TAB_ID -> Icons.Default.AllInclusive
    tabId == "status:FAVORITES" -> Icons.Default.Favorite
    tabId.startsWith("status:") -> {
        val statusName = tabId.removePrefix("status:")
        LibraryStatus.entries.find { it.name == statusName }?.toIcon(mediaType)
            ?: Icons.AutoMirrored.Filled.List
    }
    else -> Icons.AutoMirrored.Filled.List // Custom list
}
