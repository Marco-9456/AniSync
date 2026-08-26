package com.anisync.android.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.R
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.displayValue
import com.anisync.android.domain.max
import com.anisync.android.domain.sliderSteps
import com.anisync.android.domain.snap
import com.anisync.android.domain.url
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.iconRes
import com.anisync.android.presentation.components.toIndicatorKind
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.listIndicatorColor
import com.anisync.android.util.getTitle
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(20.dp)
private val ControlShape = RoundedCornerShape(16.dp)

/**
 * The edit sheet for one library entry.
 *
 * Ordered by what the sheet is actually opened for: progress leads, Save is pinned to the top bar
 * so it never scrolls out of reach, and the fields that are rarely touched fold away behind More
 * options. Remove lives at the bottom of that fold rather than beside Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLibraryEntrySheet(
    entry: LibraryEntry,
    titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    scoreFormat: ScoreFormat = ScoreFormat.POINT_100,
    availableCustomLists: List<String> = emptyList(),
    advancedScoringCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (LibraryEntry) -> Unit,
    onDelete: () -> Unit
) {
    var status by rememberSaveable(entry.id) { mutableStateOf(entry.status) }
    var progress by rememberSaveable(entry.id) { mutableIntStateOf(entry.progress) }
    var progressVolumes by rememberSaveable(entry.id) { mutableIntStateOf(entry.progressVolumes ?: 0) }
    var score by rememberSaveable(entry.id) { mutableDoubleStateOf(entry.score ?: 0.0) }
    var notes by rememberSaveable(entry.id) { mutableStateOf(entry.notes.orEmpty()) }
    var startedAt by rememberSaveable(entry.id) { mutableStateOf(entry.startedAt) }
    var completedAt by rememberSaveable(entry.id) { mutableStateOf(entry.completedAt) }
    var rewatches by rememberSaveable(entry.id) { mutableIntStateOf(entry.rewatches) }
    var isPrivate by rememberSaveable(entry.id) { mutableStateOf(entry.isPrivate) }
    var hiddenFromStatusLists by rememberSaveable(entry.id) { mutableStateOf(entry.hiddenFromStatusLists) }
    var selectedCustomLists by remember(entry.id) { mutableStateOf(entry.customLists.toSet()) }
    var advancedScores by remember(entry.id) { mutableStateOf(entry.advancedScores) }

    var finishPromptDismissed by rememberSaveable(entry.id) { mutableStateOf(false) }
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showCompletedPicker by rememberSaveable { mutableStateOf(false) }
    var numberPrompt by remember { mutableStateOf<NumberPrompt?>(null) }

    val isAnime = entry.type != MediaType.MANGA
    val total = if (isAnime) entry.totalEpisodes else entry.totalChapters
    val haptics = rememberHapticFeedback()

    val hasChanges by remember(entry.id) {
        derivedStateOf {
            status != entry.status ||
                progress != entry.progress ||
                progressVolumes != (entry.progressVolumes ?: 0) ||
                score != (entry.score ?: 0.0) ||
                notes != entry.notes.orEmpty() ||
                startedAt != entry.startedAt ||
                completedAt != entry.completedAt ||
                rewatches != entry.rewatches ||
                isPrivate != entry.isPrivate ||
                hiddenFromStatusLists != entry.hiddenFromStatusLists ||
                selectedCustomLists != entry.customLists.toSet() ||
                advancedScores != entry.advancedScores
        }
    }

    // The gate reads a plain holder rather than the derived state: confirmValueChange is captured
    // once when the sheet state is created and would otherwise keep answering for a clean entry.
    val dirty = remember { mutableStateOf(false) }
    LaunchedEffect(hasChanges) { dirty.value = hasChanges }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && dirty.value) {
                showDiscardDialog = true
                false
            } else {
                true
            }
        }
    )

    fun edited() = entry.copy(
        status = status,
        progress = progress,
        progressVolumes = progressVolumes.takeIf { entry.type == MediaType.MANGA },
        score = score.takeIf { it > 0 },
        // The raw text goes out, empty string included: collapsing it to null makes the repository
        // send Optional.absent(), which leaves the old note on AniList instead of clearing it.
        notes = notes,
        startedAt = startedAt,
        completedAt = completedAt,
        rewatches = rewatches,
        customLists = selectedCustomLists.toList(),
        advancedScores = advancedScores,
        isPrivate = isPrivate,
        hiddenFromStatusLists = hiddenFromStatusLists
    )

    AppModalBottomSheet(
        onDismissRequest = { if (dirty.value) showDiscardDialog = true else onDismiss() },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        confirmDismiss = {
            if (dirty.value) {
                showDiscardDialog = true
                false
            } else {
                true
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            EditEntryTopBar(
                saveEnabled = hasChanges,
                onClose = { if (hasChanges) showDiscardDialog = true else onDismiss() },
                onSave = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSave(edited())
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaRow(entry = entry, titleLanguage = titleLanguage)

                ProgressCard(
                    progress = progress,
                    total = total,
                    isAnime = isAnime,
                    volumes = if (isAnime) null else progressVolumes,
                    totalVolumes = entry.totalVolumes,
                    onProgressChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        progress = it.coerceIn(0, total ?: Int.MAX_VALUE)
                    },
                    onVolumesChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        progressVolumes = it.coerceIn(0, entry.totalVolumes ?: Int.MAX_VALUE)
                    },
                    onTypeRequest = {
                        numberPrompt = NumberPrompt.Progress(progress, total)
                    }
                )

                val atTheEnd = total != null && total > 0 && progress >= total
                AnimatedVisibility(
                    visible = atTheEnd && status != LibraryStatus.COMPLETED && !finishPromptDismissed
                ) {
                    FinishPrompt(
                        isAnime = isAnime,
                        onDismiss = { finishPromptDismissed = true },
                        onConfirm = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            status = LibraryStatus.COMPLETED
                            if (completedAt == null) completedAt = todayUtcMillis()
                            finishPromptDismissed = true
                        }
                    )
                }

                StatusGrid(
                    status = status,
                    isAnime = isAnime,
                    onSelect = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        status = it
                    }
                )

                if (availableCustomLists.isNotEmpty()) {
                    CustomListsRow(
                        available = availableCustomLists,
                        selectedLists = selectedCustomLists,
                        onToggle = { name ->
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            selectedCustomLists = if (name in selectedCustomLists) {
                                selectedCustomLists - name
                            } else {
                                selectedCustomLists + name
                            }
                        }
                    )
                }

                ScoreCard(
                    score = score,
                    scoreFormat = scoreFormat,
                    categories = advancedScoringCategories,
                    advancedScores = advancedScores,
                    onScoreChange = { score = it },
                    onClear = {
                        score = 0.0
                        advancedScores = emptyMap()
                    },
                    onCategoryChange = { name, value ->
                        val next = advancedScores.toMutableMap()
                        if (value <= 0.0) next.remove(name) else next[name] = value
                        advancedScores = next
                        // AniList treats the overall score as the average of the rated categories.
                        val rated = next.values.filter { it > 0.0 }
                        if (rated.isNotEmpty()) score = scoreFormat.snap(rated.average())
                    },
                    onTypeRequest = { numberPrompt = NumberPrompt.Score(score, scoreFormat) }
                )

                DateChips(
                    startedAt = startedAt,
                    completedAt = completedAt,
                    onPickStart = { showStartPicker = true },
                    onPickCompleted = { showCompletedPicker = true },
                    onClearStart = { startedAt = null },
                    onClearCompleted = { completedAt = null }
                )

                MoreOptionsRow(
                    expanded = moreExpanded,
                    isAnime = isAnime,
                    onToggle = { moreExpanded = !moreExpanded }
                )

                AnimatedVisibility(visible = moreExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RewatchRow(
                            rewatches = rewatches,
                            isAnime = isAnime,
                            onChange = { rewatches = it.coerceAtLeast(0) }
                        )

                        NotesField(notes = notes, onNotesChange = { notes = it })

                        PrivacyToggles(
                            isPrivate = isPrivate,
                            hiddenFromStatusLists = hiddenFromStatusLists,
                            onPrivateChange = { isPrivate = it },
                            onHiddenChange = { hiddenFromStatusLists = it }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        RemoveRow(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    numberPrompt?.let { prompt ->
        NumberPromptDialog(
            prompt = prompt,
            onDismiss = { numberPrompt = null },
            onConfirm = { value ->
                when (prompt) {
                    is NumberPrompt.Progress -> progress = value.roundToInt().coerceIn(0, total ?: Int.MAX_VALUE)
                    is NumberPrompt.Score -> score = value.coerceIn(0.0, scoreFormat.max)
                }
                numberPrompt = null
            }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showDiscardDialog) {
        DiscardConfirmationDialog(
            onConfirm = {
                showDiscardDialog = false
                onDismiss()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    if (showStartPicker) {
        DatePickerSheet(
            initialDate = startedAt,
            title = stringResource(R.string.start_date),
            onDateSelected = { startedAt = it },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showCompletedPicker) {
        DatePickerSheet(
            initialDate = completedAt,
            title = stringResource(R.string.completed_date),
            onDateSelected = { completedAt = it },
            onDismiss = { showCompletedPicker = false }
        )
    }
}

// ================== TOP BAR ==================

@Composable
private fun EditEntryTopBar(
    saveEnabled: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cancel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = stringResource(R.string.edit_entry),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(20.dp),
            contentPadding = ButtonDefaults.ContentPadding
        ) {
            Text(
                text = stringResource(R.string.save),
                style = MaterialTheme.typography.labelLarge.emphasis()
            )
        }
    }
}

// ================== MEDIA ==================

@Composable
private fun MediaRow(entry: LibraryEntry, titleLanguage: TitleLanguage) {
    val title = entry.getTitle(titleLanguage)
    val isAnime = entry.type != MediaType.MANGA
    val total = if (isAnime) entry.totalEpisodes else entry.totalChapters
    val unit = stringResource(if (isAnime) R.string.stat_episodes else R.string.stat_chapters)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(entry.cover.url() ?: entry.coverUrl)
                .crossfade(200)
                .build(),
            contentDescription = stringResource(R.string.content_description_cover),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 40.dp, height = 56.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    if (isAnime) stringResource(R.string.media_type_anime) else stringResource(R.string.media_type_manga),
                    total?.let { "$it $unit" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ================== PROGRESS ==================

@Composable
private fun ProgressCard(
    progress: Int,
    total: Int?,
    isAnime: Boolean,
    volumes: Int?,
    totalVolumes: Int?,
    onProgressChange: (Int) -> Unit,
    onVolumesChange: (Int) -> Unit,
    onTypeRequest: () -> Unit
) {
    val remaining = total?.let { (it - progress).coerceAtLeast(0) }
    val progressDescription = stringResource(
        R.string.a11y_progress_of,
        progress.toString(),
        total?.toString() ?: stringResource(R.string.unknown)
    )

    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
                .semantics(mergeDescendants = false) { contentDescription = progressDescription },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(
                    text = stringResource(
                        if (isAnime) R.string.edit_entry_episodes_watched else R.string.edit_entry_chapters_read
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (remaining != null) {
                    Text(
                        text = if (remaining == 0) {
                            stringResource(if (isAnime) R.string.edit_entry_all_watched else R.string.edit_entry_all_read)
                        } else {
                            stringResource(R.string.edit_entry_remaining, remaining)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepperButton(
                    icon = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_progress),
                    enabled = progress > 0,
                    onClick = { onProgressChange(progress - 1) }
                )

                ValueField(
                    value = progress.toString(),
                    suffix = total?.let { stringResource(R.string.edit_entry_of_total, it) },
                    onClick = onTypeRequest,
                    modifier = Modifier.weight(1f)
                )

                StepperButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_increase_progress),
                    enabled = total == null || progress < total,
                    onClick = { onProgressChange(progress + 1) }
                )
            }

            if (volumes != null) {
                VolumesRow(
                    volumes = volumes,
                    totalVolumes = totalVolumes,
                    onChange = onVolumesChange
                )
            }

            if (total != null && total > 0) {
                LinearProgressIndicator(
                    progress = { progress.toFloat() / total.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    // The default track is a container tone, which at zero progress reads as a
                    // full bar. This one has to read as empty at a glance.
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/** Manga carry a second count AniList tracks separately, so volumes get their own row. */
@Composable
private fun VolumesRow(
    volumes: Int,
    totalVolumes: Int?,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionLabel(
            text = stringResource(R.string.edit_entry_volumes_read),
            modifier = Modifier.weight(1f)
        )
        FilledTonalIconButton(
            onClick = { onChange(volumes - 1) },
            enabled = volumes > 0,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.cd_decrease_progress),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = if (totalVolumes != null) {
                stringResource(R.string.edit_entry_score_of_max, volumes, totalVolumes)
            } else {
                volumes.toString()
            },
            style = MaterialTheme.typography.titleSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(64.dp)
        )
        FilledTonalIconButton(
            onClick = { onChange(volumes + 1) },
            enabled = totalVolumes == null || volumes < totalVolumes,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_increase_progress),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

/** The value doubles as the tap target for typing one in, which is the only sane way past 100. */
@Composable
private fun ValueField(
    value: String,
    suffix: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    stretch: Boolean = true,
    emphasised: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = ControlShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = if (stretch) Modifier.fillMaxWidth() else Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = if (emphasised) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (suffix != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Reaching the last episode is the moment the entry is finished, so the sheet offers the two edits
 * that always follow rather than leaving them to be found separately.
 */
@Composable
private fun FinishPrompt(
    isAnime: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ControlShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isAnime) R.string.edit_entry_finish_title_anime else R.string.edit_entry_finish_title_manga
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = stringResource(R.string.edit_entry_finish_body),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.edit_entry_finish_dismiss))
                }
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(R.string.edit_entry_finish_apply),
                        style = MaterialTheme.typography.labelLarge.emphasis()
                    )
                }
            }
        }
    }
}

// ================== STATUS ==================

@Composable
private fun StatusGrid(
    status: LibraryStatus,
    isAnime: Boolean,
    onSelect: (LibraryStatus) -> Unit
) {
    val rows = listOf(
        listOf(LibraryStatus.CURRENT, LibraryStatus.REPEATING, LibraryStatus.COMPLETED),
        listOf(LibraryStatus.PLANNING, LibraryStatus.PAUSED, LibraryStatus.DROPPED)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = stringResource(R.string.filter_status))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { option ->
                        StatusCell(
                            status = option,
                            isAnime = isAnime,
                            isSelected = option == status,
                            onClick = { onSelect(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCell(
    status: LibraryStatus,
    isAnime: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kind = status.toIndicatorKind()
    val listColors = listIndicatorColor(kind)
    val label = stringResource(status.labelRes(isAnime))

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .semantics { selected = isSelected },
        shape = ControlShape,
        color = if (isSelected) listColors.container else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (isSelected) listColors.content else MaterialTheme.colorScheme.onSurfaceVariant,
        // Colour alone would carry the selection, so the ring gives it a second cue.
        border = if (isSelected) BorderStroke(1.5.dp, listColors.content) else null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(kind.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun LibraryStatus.labelRes(isAnime: Boolean): Int = when (this) {
    LibraryStatus.CURRENT -> if (isAnime) R.string.status_watching else R.string.status_reading
    LibraryStatus.REPEATING -> if (isAnime) R.string.status_rewatching else R.string.status_rereading
    LibraryStatus.COMPLETED -> R.string.status_completed
    LibraryStatus.PLANNING -> R.string.status_planning
    LibraryStatus.PAUSED -> R.string.status_paused
    LibraryStatus.DROPPED -> R.string.status_dropped
    LibraryStatus.UNKNOWN -> R.string.filter_status
}

// ================== CUSTOM LISTS ==================

@Composable
private fun CustomListsRow(
    available: List<String>,
    selectedLists: Set<String>,
    onToggle: (String) -> Unit
) {
    val listColors = listIndicatorColor(ListIndicatorKind.CUSTOM)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = stringResource(R.string.custom_lists))
        // Custom lists are user defined and unbounded, so they scroll rather than wrap the sheet
        // taller with every list the viewer owns.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            available.forEach { name ->
                val isSelected = name in selectedLists
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(name) },
                    label = { Text(name) },
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        selectedContainerColor = listColors.container,
                        selectedLabelColor = listColors.content,
                        selectedLeadingIconColor = listColors.content
                    ),
                    border = if (isSelected) null else {
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    },
                    modifier = Modifier.semantics { selected = isSelected }
                )
            }
        }
    }
}

// ================== SCORE ==================

@Composable
private fun ScoreCard(
    score: Double,
    scoreFormat: ScoreFormat,
    categories: List<String>,
    advancedScores: Map<String, Double>,
    onScoreChange: (Double) -> Unit,
    onClear: () -> Unit,
    onCategoryChange: (String, Double) -> Unit,
    onTypeRequest: () -> Unit
) {
    Surface(shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // POINT_5 and POINT_3 are glyph scales on AniList, so they get their glyphs as targets
            // rather than a track cut into five or three.
            val glyphFormat = scoreFormat == ScoreFormat.POINT_5 || scoreFormat == ScoreFormat.POINT_3

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionLabel(text = stringResource(R.string.stat_score), modifier = Modifier.weight(1f))

                if (glyphFormat) {
                    Text(
                        text = if (score > 0) {
                            stringResource(R.string.edit_entry_score_of_max, score.toInt(), scoreFormat.max.toInt())
                        } else {
                            stringResource(R.string.no_score)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ValueField(
                        value = if (score > 0) scoreFormat.displayValue(score) else stringResource(R.string.no_score),
                        suffix = if (score > 0) stringResource(R.string.edit_entry_of_total, scoreFormat.max.toInt()) else null,
                        onClick = onTypeRequest,
                        stretch = false,
                        emphasised = score > 0
                    )
                }

                if (score > 0) {
                    TextButton(onClick = onClear) {
                        Text(text = stringResource(R.string.edit_entry_clear_score))
                    }
                }
            }

            when (scoreFormat) {
                ScoreFormat.POINT_5 -> StarRow(score = score.toInt(), onScoreChange = { onScoreChange(it.toDouble()) })
                ScoreFormat.POINT_3 -> SmileyRow(score = score.toInt(), onScoreChange = { onScoreChange(it.toDouble()) })
                else -> Slider(
                    value = score.toFloat().coerceIn(0f, scoreFormat.max.toFloat()),
                    onValueChange = { onScoreChange(scoreFormat.snap(it.toDouble())) },
                    valueRange = 0f..scoreFormat.max.toFloat(),
                    steps = scoreFormat.sliderSteps,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (categories.isNotEmpty()) {
                CategoryScores(
                    categories = categories,
                    scores = advancedScores,
                    scoreFormat = scoreFormat,
                    onCategoryChange = onCategoryChange
                )
            }
        }
    }
}

/**
 * AniList's advanced scoring: the viewer names the categories, rates any of them, and the overall
 * score follows their average. A category left at zero is unrated and stays out of that average.
 */
@Composable
private fun CategoryScores(
    categories: List<String>,
    scores: Map<String, Double>,
    scoreFormat: ScoreFormat,
    onCategoryChange: (String, Double) -> Unit
) {
    Surface(shape = ControlShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionLabel(text = stringResource(R.string.edit_entry_by_category))
            Text(
                text = stringResource(R.string.edit_entry_by_category_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            categories.forEach { category ->
                val value = scores[category] ?: 0.0
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (value > 0) scoreFormat.displayValue(value) else "–",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (value > 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Slider(
                        value = value.toFloat().coerceIn(0f, scoreFormat.max.toFloat()),
                        onValueChange = { onCategoryChange(category, scoreFormat.snap(it.toDouble())) },
                        valueRange = 0f..scoreFormat.max.toFloat(),
                        steps = scoreFormat.sliderSteps,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Tapping the star that is already the score clears it, which is the only way back to unrated. */
@Composable
private fun StarRow(score: Int, onScoreChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { star ->
            IconButton(onClick = { onScoreChange(if (score == star) 0 else star) }) {
                Icon(
                    imageVector = if (star <= score) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = stringResource(R.string.edit_entry_score_of_max, star, 5),
                    modifier = Modifier.size(32.dp),
                    tint = if (star <= score) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/** The same three faces [formatScore] prints, sized as targets instead of thirds of a track. */
@Composable
private fun SmileyRow(score: Int, onScoreChange: (Int) -> Unit) {
    val faces = listOf(1 to ":(", 2 to ":|", 3 to ":)")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        faces.forEach { (value, glyph) ->
            val selected = score == value
            Surface(
                onClick = { onScoreChange(if (selected) 0 else value) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = ControlShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = glyph,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ================== DATES ==================

@Composable
private fun DateChips(
    startedAt: Long?,
    completedAt: Long?,
    onPickStart: () -> Unit,
    onPickCompleted: () -> Unit,
    onClearStart: () -> Unit,
    onClearCompleted: () -> Unit
) {
    // Fuzzy-date millis are UTC anchored (see DataMappers, issue #85), so the formatter has to read
    // them in UTC or it renders the day before for anyone behind the line.
    val formatter = remember {
        DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DateChip(
            label = stringResource(R.string.edit_entry_started),
            date = startedAt?.let { formatter.format(Date(it)) },
            onClick = onPickStart,
            onClear = onClearStart,
            modifier = Modifier.weight(1f)
        )
        DateChip(
            label = stringResource(R.string.edit_entry_finished),
            date = completedAt?.let { formatter.format(Date(it)) },
            onClick = onPickCompleted,
            onClear = onClearCompleted,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateChip(
    label: String,
    date: String?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = ControlShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (date == null) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (date != null) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onClear, modifier = Modifier.size(20.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear_date),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Text(
                text = date ?: stringResource(R.string.edit_entry_add_date),
                style = MaterialTheme.typography.bodyMedium,
                color = if (date != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ================== MORE OPTIONS ==================

@Composable
private fun MoreOptionsRow(
    expanded: Boolean,
    isAnime: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = ControlShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.more_options),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            if (!expanded) {
                Text(
                    text = stringResource(
                        if (isAnime) R.string.edit_entry_more_summary_anime else R.string.edit_entry_more_summary_manga
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
    }
}

@Composable
private fun RewatchRow(
    rewatches: Int,
    isAnime: Boolean,
    onChange: (Int) -> Unit
) {
    val label = stringResource(if (isAnime) R.string.times_rewatched else R.string.times_reread)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = ControlShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 10.dp)
                .semantics(mergeDescendants = true) { contentDescription = "$label: $rewatches" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(
                onClick = { onChange(rewatches - 1) },
                enabled = rewatches > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_progress),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = rewatches.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(26.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            FilledTonalIconButton(
                onClick = { onChange(rewatches + 1) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_increase_progress),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NotesField(notes: String, onNotesChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = stringResource(R.string.notes))
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp),
            placeholder = { Text(text = stringResource(R.string.notes_placeholder)) },
            shape = ControlShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default
            ),
            minLines = 3,
            maxLines = 6
        )
    }
}

@Composable
private fun PrivacyToggles(
    isPrivate: Boolean,
    hiddenFromStatusLists: Boolean,
    onPrivateChange: (Boolean) -> Unit,
    onHiddenChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(text = stringResource(R.string.privacy))
        ToggleRow(
            title = stringResource(R.string.private_entry),
            subtitle = stringResource(R.string.private_entry_desc),
            checked = isPrivate,
            onCheckedChange = onPrivateChange
        )
        ToggleRow(
            title = stringResource(R.string.hide_from_status_lists),
            subtitle = stringResource(R.string.hide_from_status_lists_desc),
            checked = hiddenFromStatusLists,
            onCheckedChange = onHiddenChange
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RemoveRow(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = ControlShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.error
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.edit_entry_remove),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ================== DIALOGS ==================

/** The two values that are worth typing rather than dragging. */
private sealed interface NumberPrompt {
    data class Progress(val current: Int, val total: Int?) : NumberPrompt
    data class Score(val current: Double, val format: ScoreFormat) : NumberPrompt
}

@Composable
private fun NumberPromptDialog(
    prompt: NumberPrompt,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val decimals = prompt is NumberPrompt.Score && prompt.format == ScoreFormat.POINT_10_DECIMAL
    val initial = when (prompt) {
        is NumberPrompt.Progress -> prompt.current.toString()
        is NumberPrompt.Score -> if (prompt.current <= 0.0) "" else prompt.format.displayValue(prompt.current)
    }
    var text by rememberSaveable { mutableStateOf(initial) }
    val parsed = text.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    when (prompt) {
                        is NumberPrompt.Progress -> R.string.edit_entry_set_progress
                        is NumberPrompt.Score -> R.string.edit_entry_set_score
                    }
                )
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.filter { it.isDigit() || (decimals && it == '.') }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                suffix = when (prompt) {
                    is NumberPrompt.Progress -> prompt.total?.let { { Text(stringResource(R.string.edit_entry_of_total, it)) } }
                    is NumberPrompt.Score -> {
                        { Text(stringResource(R.string.edit_entry_of_total, prompt.format.max.toInt())) }
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null
            ) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(text = stringResource(R.string.action_remove)) },
        text = { Text(text = stringResource(R.string.delete_entry_confirm)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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

@Composable
private fun DiscardConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.edit_entry_discard_title)) },
        text = { Text(text = stringResource(R.string.edit_entry_discard_body)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(text = stringResource(R.string.edit_entry_discard_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.edit_entry_keep_editing))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initialDate: Long?,
    title: String,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
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

// ================== SHARED ==================

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() }
    )
}

/** Fuzzy dates are UTC anchored, so "today" has to be UTC midnight and not the local one. */
private fun todayUtcMillis(): Long = LocalDate.now(ZoneOffset.UTC).toEpochDay() * 86_400_000L
