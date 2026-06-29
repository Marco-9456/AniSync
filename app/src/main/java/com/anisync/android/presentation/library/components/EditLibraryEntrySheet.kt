package com.anisync.android.presentation.library.components

import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.domain.url

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatScore
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.AppTheme
import com.anisync.android.util.getTitle
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * A Material Design 3 Expressive bottom sheet for editing library entries.
 * Supports editing status, progress, score, dates, rewatches/rereads, and notes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditLibraryEntrySheet(
    entry: LibraryEntry,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    scoreFormat: ScoreFormat = ScoreFormat.POINT_100,
    availableCustomLists: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (LibraryEntry) -> Unit,
    onDelete: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )
) {
    // State persistence across configuration changes
    var status by rememberSaveable(entry.id) { mutableStateOf(entry.status) }
    var progress by rememberSaveable(entry.id) { mutableIntStateOf(entry.progress) }
    var score by rememberSaveable(entry.id) { mutableDoubleStateOf(entry.score ?: 0.0) }
    var notes by rememberSaveable(entry.id) { mutableStateOf(entry.notes.orEmpty()) }
    var startedAt by rememberSaveable(entry.id) { mutableStateOf(entry.startedAt) }
    var completedAt by rememberSaveable(entry.id) { mutableStateOf(entry.completedAt) }
    var rewatches by rememberSaveable(entry.id) { mutableIntStateOf(entry.rewatches) }
    var isPrivate by rememberSaveable(entry.id) { mutableStateOf(entry.isPrivate ?: false) }
    var hiddenFromStatusLists by rememberSaveable(entry.id) { mutableStateOf(entry.hiddenFromStatusLists ?: false) }
    
    // Custom lists handled via standard compose states because they are collections
    var selectedCustomLists by remember(entry.id) { mutableStateOf(entry.customLists.toSet()) }

    // Track unsaved changes using derived state for performance
    val hasChanges by remember(entry.id) {
        derivedStateOf {
            status != entry.status ||
            selectedCustomLists != entry.customLists.toSet() ||
                    progress != entry.progress ||
                    score != (entry.score ?: 0.0) ||
                    notes != (entry.notes.orEmpty()) ||
                    startedAt != entry.startedAt ||
                    completedAt != entry.completedAt ||
                    rewatches != entry.rewatches ||
                    isPrivate != entry.isPrivate ||
                    hiddenFromStatusLists != entry.hiddenFromStatusLists
        }
    }

    val haptics = rememberHapticFeedback()
    val isAnime = entry.type == MediaType.ANIME

    // Dialog states
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
    var showCompletedDatePicker by rememberSaveable { mutableStateOf(false) }

    // Sync state if entry changes externally
    LaunchedEffect(entry) {
        status = entry.status
        progress = entry.progress
        score = entry.score ?: 0.0
        notes = entry.notes.orEmpty()
        startedAt = entry.startedAt
        completedAt = entry.completedAt
        rewatches = entry.rewatches
        isPrivate = entry.isPrivate ?: false
        hiddenFromStatusLists = entry.hiddenFromStatusLists ?: false
    }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val scrollState = rememberScrollState()

        // Disable overscroll stretch effect specifically for this bottom sheet content
        CompositionLocalProvider(
            LocalOverscrollFactory provides null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding() // OPTIMIZATION: Fixes the focus issue where keyboard would hide the TextFields (like Notes).
                    .verticalScroll(state = scrollState)
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                HeaderSection(
                    entry = entry,
                    titleLanguage = titleLanguage,
                    hasChanges = hasChanges
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                StatusSection(
                    statusProvider = { status },
                    mediaType = entry.type,
                    onStatusChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        status = it
                    }
                )

                if (availableCustomLists.isNotEmpty()) {
                    CustomListsSection(
                        availableCustomLists = availableCustomLists,
                        selectedCustomLists = selectedCustomLists,
                        onCustomListToggle = { listName ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCustomLists = if (listName in selectedCustomLists) {
                                selectedCustomLists - listName
                            } else {
                                selectedCustomLists + listName
                            }
                        }
                    )
                }

                ProgressSection(
                    progressProvider = { progress },
                    total = entry.totalEpisodes ?: entry.totalChapters,
                    isAnime = isAnime,
                    onProgressChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        progress = it.coerceIn(0, entry.totalEpisodes ?: Int.MAX_VALUE)
                    }
                )

                ScoreSection(
                    scoreProvider = { score },
                    scoreFormat = scoreFormat,
                    onScoreChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        score = it
                    }
                )

                DateSection(
                    startedAtProvider = { startedAt },
                    completedAtProvider = { completedAt },
                    isAnime = isAnime,
                    onStartClick = { showStartDatePicker = true },
                    onCompletedClick = { showCompletedDatePicker = true }
                )

                RewatchSection(
                    rewatchesProvider = { rewatches },
                    isAnime = isAnime,
                    onRewatchChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        rewatches = it.coerceAtLeast(0)
                    }
                )

                NotesSection(
                    notesProvider = { notes },
                    onNotesChange = { notes = it },
                    modifier = Modifier.heightIn(min = 120.dp, max = 200.dp)
                )

                PrivacySection(
                    isPrivateProvider = { isPrivate },
                    hiddenFromStatusListsProvider = { hiddenFromStatusLists },
                    onPrivateChange = { isPrivate = it },
                    onHiddenChange = { hiddenFromStatusLists = it }
                )

                ActionButtons(
                    onDelete = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteDialog = true
                    },
                    onSave = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSave(
                            entry.copy(
                                status = status,
                                progress = progress,
                                score = score.takeIf { it > 0 },
                                // Send the raw text (empty string included) so clearing a note
                                // reaches AniList as notes:"" — which clears it. Collapsing to null
                                // here makes the repo send Optional.absent(), so the old note stays.
                                notes = notes,
                                startedAt = startedAt,
                                completedAt = completedAt,
                                rewatches = rewatches,
                                customLists = selectedCustomLists.toList(),
                                isPrivate = isPrivate,
                                hiddenFromStatusLists = hiddenFromStatusLists
                            )
                        )
                    },
                    saveEnabled = hasChanges,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false },
            isAnime = isAnime
        )
    }

    if (showStartDatePicker) {
        DatePickerSheet(
            initialDate = startedAt,
            onDateSelected = { startedAt = it },
            onDismiss = { showStartDatePicker = false },
            title = stringResource(
                if (isAnime) R.string.start_date else R.string.start_date
            )
        )
    }

    if (showCompletedDatePicker) {
        DatePickerSheet(
            initialDate = completedAt,
            onDateSelected = { completedAt = it },
            onDismiss = { showCompletedDatePicker = false },
            title = stringResource(
                if (isAnime) R.string.completed_date else R.string.completed_date
            )
        )
    }
}

// ================== HEADER SECTION ==================

@Composable
private fun HeaderSection(
    entry: LibraryEntry,
    titleLanguage: TitleLanguage,
    hasChanges: Boolean,
    modifier: Modifier = Modifier
) {
    val title = entry.getTitle(titleLanguage)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                stateDescription = if (hasChanges) "Unsaved changes" else "No changes"
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(entry.cover.url() ?: entry.coverUrl)
                .crossfade(200)
                .build(),
            contentDescription = stringResource(R.string.content_description_cover),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 80.dp, height = 113.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {}
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.edit_entry).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 28.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics {
                    contentDescription = "Editing $title"
                }
            )

            MediaTypeChip(
                type = entry.type,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun MediaTypeChip(
    type: MediaType?,
    modifier: Modifier = Modifier
) {
    val label = if (type == MediaType.ANIME) {
        stringResource(R.string.media_type_anime)
    } else {
        stringResource(R.string.media_type_manga)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

// ================== STATUS SECTION ==================

@Stable
private data class StatusOption(
    val status: LibraryStatus,
    val icon: ImageVector,
    val labelResId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSection(
    statusProvider: () -> LibraryStatus,
    mediaType: MediaType?,
    onStatusChange: (LibraryStatus) -> Unit
) {
    val status = statusProvider()
    val isAnime = mediaType == MediaType.ANIME

    val statusOptions = remember(isAnime) {
        listOf(
            StatusOption(
                LibraryStatus.CURRENT,
                Icons.Default.PlayArrow,
                if (isAnime) R.string.status_watching else R.string.status_reading
            ),
            StatusOption(
                LibraryStatus.REPEATING,
                Icons.Default.Refresh,
                if (isAnime) R.string.status_rewatching else R.string.status_rereading
            ),
            StatusOption(LibraryStatus.COMPLETED, Icons.Default.Check, R.string.status_completed),
            StatusOption(LibraryStatus.PLANNING, Icons.Default.Schedule, R.string.status_planning),
            StatusOption(LibraryStatus.PAUSED, Icons.Default.Close, R.string.status_paused),
            StatusOption(LibraryStatus.DROPPED, Icons.Default.Close, R.string.status_dropped)
        )
    }

    val currentStatusLabel = stringResource(
        statusOptions.firstOrNull { it.status == status }?.labelResId ?: R.string.filter_status
    )

    Column(
        modifier = Modifier.semantics {
            stateDescription = "Current status: $currentStatusLabel"
        },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(text = stringResource(R.string.filter_status))

        val statusScrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(state = statusScrollState),
            verticalAlignment = Alignment.CenterVertically,
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
                    modifier = Modifier.semantics {
                        selected = isSelected
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = if (isSelected) null else {
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                )
            }
        }
    }
}

// ================== CUSTOM LISTS SECTION ==================

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CustomListsSection(
    availableCustomLists: List<String>,
    selectedCustomLists: Set<String>,
    onCustomListToggle: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(text = stringResource(R.string.custom_lists))

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableCustomLists.forEach { customListName ->
                val isSelected = customListName in selectedCustomLists
                FilterChip(
                    selected = isSelected,
                    onClick = { onCustomListToggle(customListName) },
                    label = { Text(customListName) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.semantics {
                        selected = isSelected
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = if (isSelected) null else {
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                )
            }
        }
    }
}

// ================== PROGRESS SECTION ==================

@Composable
private fun ProgressSection(
    progressProvider: () -> Int,
    total: Int?,
    isAnime: Boolean,
    onProgressChange: (Int) -> Unit
) {
    val progress = progressProvider()
    val maxProgress = total ?: 999

    val unitString = if (isAnime) {
        stringResource(R.string.stat_episodes)
    } else {
        stringResource(R.string.stat_chapters)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Progress $progress of ${total ?: "unknown"}"
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(text = stringResource(R.string.sort_progress))

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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val minusInteraction = remember { MutableInteractionSource() }
            val minusPressed by minusInteraction.collectIsPressedAsState()
            val minusScale by animateDpAsState(
                targetValue = if (minusPressed) 32.dp else 40.dp,
                label = "minus_scale"
            )

            FilledTonalIconButton(
                onClick = { onProgressChange(progress - 1) },
                enabled = progress > 0,
                modifier = Modifier.size(minusScale),
                interactionSource = minusInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.descending)
                )
            }

            Slider(
                value = progress.toFloat(),
                onValueChange = { value ->
                    onProgressChange(value.roundToInt().coerceIn(0, maxProgress))
                },
                valueRange = 0f..maxProgress.toFloat(),
                steps = (maxProgress - 1).coerceAtLeast(0),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )

            val plusInteraction = remember { MutableInteractionSource() }
            val plusPressed by plusInteraction.collectIsPressedAsState()
            val plusScale by animateDpAsState(
                targetValue = if (plusPressed) 32.dp else 40.dp,
                label = "plus_scale"
            )

            FilledTonalIconButton(
                onClick = { onProgressChange(progress + 1) },
                enabled = total == null || progress < total,
                modifier = Modifier.size(plusScale),
                interactionSource = plusInteraction
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.ascending)
                )
            }
        }

        Text(
            text = unitString,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

// ================== SCORE SECTION ==================

@Composable
private fun ScoreSection(
    scoreProvider: () -> Double,
    scoreFormat: ScoreFormat,
    onScoreChange: (Double) -> Unit
) {
    val score = scoreProvider()
    val maxScore = when (scoreFormat) {
        ScoreFormat.POINT_100 -> 100f
        ScoreFormat.POINT_10_DECIMAL, ScoreFormat.POINT_10 -> 10f
        ScoreFormat.POINT_5 -> 5f
        ScoreFormat.POINT_3 -> 3f
    }
    val normalizedScore = if (score > 0) (score / maxScore.toDouble()).coerceIn(0.0, 1.0) else 0.0

    val scoreColor by animateColorAsState(
        targetValue = when {
            normalizedScore >= 0.8 -> MaterialTheme.colorScheme.primary
            normalizedScore >= 0.6 -> MaterialTheme.colorScheme.tertiary
            normalizedScore >= 0.4 -> MaterialTheme.colorScheme.secondary
            normalizedScore > 0.0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "score_color"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics {
            stateDescription = if (score > 0) "Score $score out of 10" else "No score set"
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(text = stringResource(R.string.stat_score))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = scoreColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            val displayScore = formatScore(if (score > 0) score else null, scoreFormat)

            Text(
                text = displayScore,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor
            )
        }

        val steps = when (scoreFormat) {
            ScoreFormat.POINT_100 -> 99
            ScoreFormat.POINT_10_DECIMAL -> 99 // 0.1 increments
            ScoreFormat.POINT_10 -> 9
            ScoreFormat.POINT_5 -> 4
            ScoreFormat.POINT_3 -> 2
        }

        Slider(
            value = score.toFloat().coerceIn(0f, maxScore),
            onValueChange = {
                val rounded = if (scoreFormat == ScoreFormat.POINT_10_DECIMAL) {
                    (it * 10).roundToInt() / 10.0
                } else {
                    it.roundToInt().toDouble()
                }
                onScoreChange(rounded.coerceIn(0.0, maxScore.toDouble()))
            },
            valueRange = 0f..maxScore,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = scoreColor,
                activeTrackColor = scoreColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = maxScore.toInt().toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ================== PRIVACY SECTION ==================

@Composable
private fun PrivacySection(
    isPrivateProvider: () -> Boolean,
    hiddenFromStatusListsProvider: () -> Boolean,
    onPrivateChange: (Boolean) -> Unit,
    onHiddenChange: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(text = stringResource(R.string.privacy))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPrivateChange(!isPrivateProvider()) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.private_entry),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.private_entry_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isPrivateProvider(),
                onCheckedChange = { onPrivateChange(it) }
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHiddenChange(!hiddenFromStatusListsProvider()) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.hide_from_status_lists),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.hide_from_status_lists_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = hiddenFromStatusListsProvider(),
                onCheckedChange = { onHiddenChange(it) }
            )
        }
    }
}

// ================== DATE SECTION ==================

@Composable
private fun DateSection(
    startedAtProvider: () -> Long?,
    completedAtProvider: () -> Long?,
    isAnime: Boolean,
    onStartClick: () -> Unit,
    onCompletedClick: () -> Unit
) {
    val startedAt = startedAtProvider()
    val completedAt = completedAtProvider()
    // Fuzzy-date millis are UTC-anchored (see DataMappers / issue #85), so the
    // formatter must read them in UTC or it renders the wrong day off the offset.
    val dateFormatter = remember {
        DateFormat.getDateInstance(DateFormat.MEDIUM).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(text = stringResource(R.string.dates))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val startDateStr = startedAt?.let { dateFormatter.format(Date(it)) }
            val completedDateStr = completedAt?.let { dateFormatter.format(Date(it)) }

            DateChip(
                label = stringResource(R.string.start_date),
                date = startDateStr,
                onClick = onStartClick,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = startDateStr?.let {
                            "Start date set to $it"
                        } ?: "Start date not set, tap to set"
                    }
            )

            DateChip(
                label = stringResource(R.string.completed_date),
                date = completedDateStr,
                onClick = onCompletedClick,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = completedDateStr?.let {
                            "Completed date set to $it"
                        } ?: "Completed date not set, tap to set"
                    }
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
        enabled = true,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (date != null) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = if (date == null) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = if (date != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = date ?: stringResource(R.string.no_date),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (date != null) FontWeight.Medium else FontWeight.Normal,
                    color = if (date != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ================== REWATCH SECTION ==================

@Composable
private fun RewatchSection(
    rewatchesProvider: () -> Int,
    isAnime: Boolean,
    onRewatchChange: (Int) -> Unit
) {
    val rewatches = rewatchesProvider()
    val label = stringResource(
        if (isAnime) R.string.times_rewatched else R.string.times_reread
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $rewatches"
        }
    ) {
        SectionTitle(text = label)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledTonalIconButton(
                onClick = { onRewatchChange(rewatches - 1) },
                enabled = rewatches > 0,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.descending)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = rewatches.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            FilledTonalIconButton(
                onClick = { onRewatchChange(rewatches + 1) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.ascending)
                )
            }
        }
    }
}

// ================== NOTES SECTION ==================

@Composable
private fun NotesSection(
    notesProvider: () -> String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes = notesProvider()
    var isFocused by remember { mutableStateOf(false) }
    val haptics = rememberHapticFeedback()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        SectionTitle(text = stringResource(R.string.notes))

        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 180.dp)
                .onFocusChanged {
                    if (it.isFocused && !isFocused) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
                    isFocused = it.isFocused
                }
                .semantics {
                    if (notes.isNotEmpty()) {
                        stateDescription = "${notes.length} characters entered"
                    }
                },
            placeholder = {
                Text(
                    text = stringResource(R.string.notes_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { /* Handle keyboard done if needed */ }
            ),
            maxLines = 5,
            minLines = 3
        )

        AnimatedVisibility(
            visible = notes.isNotEmpty(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = stringResource(R.string.char_count, notes.length),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ================== ACTION BUTTONS ==================

@Composable
private fun ActionButtons(
    onDelete: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val deleteInteraction = remember { MutableInteractionSource() }
    val saveInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(
            onClick = onDelete,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            interactionSource = deleteInteraction
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_remove),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Button(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier
                .weight(1.5f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ),
            interactionSource = saveInteraction,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.save),
                style = MaterialTheme.typography.labelLarge.emphasis()
            )
        }
    }
}

// ================== DIALOGS ==================

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isAnime: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
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
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initialDate: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    title: String
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate,
        initialDisplayedMonthMillis = initialDate
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
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            headline = null
        )
    }
}

// ================== UTILITIES ==================

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() }
    )
}

// ================== PREVIEWS ==================

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7_pro")
@Composable
private fun EditLibraryEntrySheetPreview() {
    AppTheme {
        Surface {
            val mockEntry = LibraryEntry(
                id = 1,
                mediaId = 101,
                titleRomaji = "Sousou no Frieren",
                titleEnglish = "Frieren: Beyond Journey's End",
                titleNative = "葬送のフリーレン",
                titleUserPreferred = "Frieren: Beyond Journey's End",
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
                notes = "Great anime! The pacing is perfect."
            )

            EditLibraryEntrySheet(
                entry = mockEntry,
                onDismiss = {},
                onSave = {},
                onDelete = {}
            )
        }
    }
}

@Preview
@Composable
private fun DarkModePreview() {
    AppTheme(darkTheme = true) {
        EditLibraryEntrySheetPreview()
    }
}
