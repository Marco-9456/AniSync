package com.anisync.android.presentation.library.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatScore
import com.anisync.android.presentation.components.filtersheet.FilterOptionRow
import com.anisync.android.presentation.components.filtersheet.FilterSheetScaffold
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.library.BulkKind
import com.anisync.android.presentation.library.BulkOperation
import com.anisync.android.presentation.util.bouncyClickable
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.presentation.util.toListIcon
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.listIndicatorColor
import kotlin.math.roundToInt

/**
 * Contextual bar shown while a selection is live.
 *
 * The scope line is not decoration: a selection belongs to the list it was started in, and
 * `Select all` on a 200-title list is only safe to offer if you can see which list that is.
 */
@Composable
fun LibrarySelectionTopBar(
    count: Int,
    listLabel: String,
    mediaType: MediaType,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.library_exit_selection)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = stringResource(R.string.library_selected_count, count),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    R.string.library_selection_scope,
                    listLabel,
                    stringResource(
                        if (mediaType == MediaType.MANGA) {
                            R.string.media_type_manga
                        } else {
                            R.string.media_type_anime
                        }
                    )
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onSelectAll) {
            Text(stringResource(R.string.select_all))
        }
    }
}

/**
 * Bulk actions, in the floating bar's place.
 *
 * Every action here maps to a mutation AniList actually exposes. Status and score are one
 * `UpdateMediaListEntries` call whatever the selection size; adding to a custom list is one request
 * per entry, because that mutation has no `customLists` argument, so it is marked as the slow path.
 * Removing is also per-entry and sits under More, where destructive actions belong.
 */
@Composable
fun LibraryBulkActionBar(
    onStatus: () -> Unit,
    onScore: () -> Unit,
    onAddToList: () -> Unit,
    moreMenu: @Composable (expanded: Boolean, onDismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var moreOpen by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BulkAction(
                icon = Icons.Default.VideoLibrary,
                label = stringResource(R.string.library_bulk_status),
                onClick = onStatus,
                modifier = Modifier.weight(1f)
            )
            BulkAction(
                icon = Icons.Default.Star,
                label = stringResource(R.string.library_bulk_score),
                onClick = onScore,
                modifier = Modifier.weight(1f)
            )
            BulkAction(
                icon = Icons.AutoMirrored.Filled.List,
                label = stringResource(R.string.library_bulk_add_to_list),
                onClick = onAddToList,
                slow = true,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.weight(1f)) {
                BulkAction(
                    icon = Icons.Default.MoreVert,
                    label = stringResource(R.string.more),
                    onClick = { moreOpen = true },
                    modifier = Modifier.fillMaxWidth()
                )
                moreMenu(moreOpen) { moreOpen = false }
            }
        }
    }
}

@Composable
private fun BulkAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    slow: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = label,
                clipShape = RoundedCornerShape(20.dp)
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            // Marks the actions that cost one request per entry rather than one for the batch.
            if (slow) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** The remaining bulk actions, including the destructive one. */
@Composable
fun LibraryBulkMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canEditSingle: Boolean,
    onEditSingle: () -> Unit,
    onSetPrivate: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    Menu(expanded = expanded, onDismissRequest = onDismiss) {
        if (canEditSingle) {
            item(
                text = stringResource(R.string.library_edit_entry),
                leadingIcon = Icons.Default.Edit,
                onClick = {
                    onDismiss()
                    onEditSingle()
                }
            )
        }
        item(
            text = stringResource(R.string.library_make_private),
            leadingIcon = Icons.Outlined.Lock,
            onClick = {
                onDismiss()
                onSetPrivate(true)
            }
        )
        item(
            text = stringResource(R.string.library_make_public),
            leadingIcon = Icons.Outlined.LockOpen,
            onClick = {
                onDismiss()
                onSetPrivate(false)
            }
        )
        gap()
        item(
            text = stringResource(R.string.library_remove_from_library),
            leadingIcon = Icons.Default.Delete,
            destructive = true,
            onClick = {
                onDismiss()
                onRemove()
            }
        )
    }
}

/**
 * Status for the whole selection. One request, so the footer says so rather than leaving the user
 * to wonder whether a 200-entry move is going to take a minute.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkStatusSheet(
    visible: Boolean,
    count: Int,
    mediaType: MediaType,
    onPick: (LibraryStatus) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    var picked by remember { mutableStateOf<LibraryStatus?>(null) }
    val statuses = remember {
        listOf(
            LibraryStatus.CURRENT,
            LibraryStatus.REPEATING,
            LibraryStatus.PLANNING,
            LibraryStatus.COMPLETED,
            LibraryStatus.PAUSED,
            LibraryStatus.DROPPED
        )
    }

    FilterSheetScaffold(
        title = stringResource(R.string.library_set_status),
        onDismiss = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        statuses.forEach { status ->
            FilterOptionRow(
                label = status.toLabel(mediaType),
                selected = picked == status,
                leading = { StatusBadge(status) },
                onClick = { picked = status }
            )
        }
        Text(
            text = stringResource(R.string.library_bulk_one_request, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
        androidx.compose.material3.Button(
            onClick = { picked?.let(onPick) },
            enabled = picked != null,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp)
        ) {
            Text(
                text = picked?.let {
                    stringResource(R.string.library_move_count_to, count, it.toLabel(mediaType))
                } ?: stringResource(R.string.library_set_status)
            )
        }
    }
}

/**
 * The list badge, shaped by family: a circle while a title is in motion, a rounded square while it
 * is parked, a square once it is finished with — the same grammar `ListIndicator` documents.
 */
@Composable
private fun StatusBadge(status: LibraryStatus) {
    val kind = when (status) {
        LibraryStatus.CURRENT -> ListIndicatorKind.WATCHING
        LibraryStatus.REPEATING -> ListIndicatorKind.REPEATING
        LibraryStatus.PLANNING -> ListIndicatorKind.PLANNING
        LibraryStatus.PAUSED -> ListIndicatorKind.PAUSED
        LibraryStatus.COMPLETED -> ListIndicatorKind.COMPLETED
        LibraryStatus.DROPPED -> ListIndicatorKind.DROPPED
        LibraryStatus.UNKNOWN -> ListIndicatorKind.CUSTOM
    }
    val colors = listIndicatorColor(kind)
    val shape = when (kind) {
        ListIndicatorKind.WATCHING, ListIndicatorKind.REPEATING -> CircleShape
        ListIndicatorKind.COMPLETED, ListIndicatorKind.DROPPED -> RoundedCornerShape(4.dp)
        else -> RoundedCornerShape(9.dp)
    }
    Box(
        modifier = Modifier.size(28.dp).clip(shape).background(colors.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = status.toListIcon(),
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** Score for the whole selection, in the viewer's own scoring format. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkScoreSheet(
    visible: Boolean,
    count: Int,
    format: ScoreFormat,
    onPick: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val max = when (format) {
        ScoreFormat.POINT_100 -> 100f
        ScoreFormat.POINT_10, ScoreFormat.POINT_10_DECIMAL -> 10f
        ScoreFormat.POINT_5 -> 5f
        ScoreFormat.POINT_3 -> 3f
        else -> 10f
    }
    val steps = when (format) {
        ScoreFormat.POINT_100 -> 99
        ScoreFormat.POINT_10_DECIMAL -> 19
        ScoreFormat.POINT_10 -> 9
        ScoreFormat.POINT_5 -> 4
        ScoreFormat.POINT_3 -> 2
        else -> 9
    }
    var value by remember { mutableFloatStateOf(max / 2f) }

    FilterSheetScaffold(
        title = stringResource(R.string.library_set_score),
        onDismiss = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = formatScore(value.toDouble(), format),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Slider(
                value = value,
                onValueChange = { value = it },
                valueRange = 0f..max,
                steps = steps
            )
            Text(
                text = stringResource(R.string.library_bulk_one_request, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            androidx.compose.material3.Button(
                onClick = {
                    val rounded = if (format == ScoreFormat.POINT_10_DECIMAL) {
                        (value * 2).roundToInt() / 2.0
                    } else {
                        value.roundToInt().toDouble()
                    }
                    onPick(rounded)
                },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.library_score_count, count))
            }
        }
    }
}

/** Which custom list to fill. Empty state points at the list manager rather than dead-ending. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAddToListSheet(
    visible: Boolean,
    lists: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    FilterSheetScaffold(
        title = stringResource(R.string.library_add_to_list),
        onDismiss = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        if (lists.isEmpty()) {
            Text(
                text = stringResource(R.string.library_no_custom_lists),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        } else {
            lists.forEach { name ->
                FilterOptionRow(
                    label = name,
                    selected = false,
                    leading = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { onPick(name) }
                )
            }
        }
    }
}

/**
 * Progress for the two operations that cost one request per entry.
 *
 * The explanation is deliberately plain: AniList exposes no bulk delete and no bulk custom-list
 * write, and the client paces itself at 25 requests a minute, so a large selection genuinely takes
 * minutes. Saying so is better than a spinner that looks stuck.
 */
@Composable
fun BulkProgressDialog(operation: BulkOperation, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = when (operation.kind) {
                    BulkKind.REMOVE -> stringResource(R.string.library_removing_title, operation.total)
                    BulkKind.ADD_TO_LIST -> stringResource(
                        R.string.library_adding_title,
                        operation.total,
                        operation.listName.orEmpty()
                    )
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.library_bulk_per_entry_explainer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = {
                        if (operation.total == 0) 0f else operation.done.toFloat() / operation.total
                    },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.library_bulk_progress,
                        operation.done,
                        operation.total
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
