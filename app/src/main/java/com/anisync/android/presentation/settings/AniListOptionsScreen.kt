package com.anisync.android.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.data.StaffNameLanguage
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.AniListListActivityStatus
import com.anisync.android.domain.AniListStaffNameLanguage
import com.anisync.android.domain.AniListTitleLanguage
import com.anisync.android.domain.ScoreFormat

/**
 * AniList Account options — the settings that live on the user's AniList account (UserOptions +
 * score format). Everything here is the account's value and edits push to AniList via UpdateUser,
 * except the two device-override controls for adult content and title language.
 */
@Composable
fun AniListOptionsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AniListOptionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreenScaffold(
        title = "AniList Account",
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        when {
            !uiState.isSignedIn -> NoticeCard(
                "Sign in to an AniList account to view and edit these options."
            )

            uiState.isLoading && uiState.options == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            else -> AniListOptionsContent(uiState, viewModel::onAction)
        }
    }
}

@Composable
private fun AniListOptionsContent(
    uiState: AniListOptionsUiState,
    onAction: (AniListOptionsAction) -> Unit,
) {
    val options = uiState.options

    SectionHeader("These settings are stored on your AniList account and sync with the website.")

    // ── Content & titles ─────────────────────────────────────────────────────────────────────────
    SectionLabel("Content & titles")
    SettingsGroup {
        SwitchSettingsItem(
            title = "Show adult content (18+)",
            subtitle = if (uiState.adultOverrideEnabled) "Overridden on this device"
            else "Synced with your AniList account",
            checked = uiState.effectiveShowAdult,
            onCheckedChange = { onAction(AniListOptionsAction.SetAdultContent(it)) },
            enabled = !uiState.isSaving,
        )
        SwitchSettingsItem(
            title = "Override on this device",
            subtitle = "Ignore the account setting and use the value above only on this device",
            checked = uiState.adultOverrideEnabled,
            onCheckedChange = { onAction(AniListOptionsAction.SetAdultOverrideEnabled(it)) },
        )

        var showTitleSheet by remember { mutableStateOf(false) }
        SelectionSettingsItem(
            title = "Title language",
            currentValue = if (uiState.titleLanguageOverrideEnabled) {
                uiState.localTitleLanguage.label()
            } else {
                options?.titleLanguage.label()
            },
            onClick = { showTitleSheet = true },
            enabled = !uiState.isSaving,
        )
        if (showTitleSheet) {
            if (uiState.titleLanguageOverrideEnabled) {
                OptionPickerSheet(
                    title = "Title language",
                    options = TitleLanguage.entries,
                    selected = uiState.localTitleLanguage,
                    label = { it.label() },
                    onSelect = {
                        onAction(AniListOptionsAction.SetLocalTitleLanguage(it))
                        showTitleSheet = false
                    },
                    onDismiss = { showTitleSheet = false },
                )
            } else {
                OptionPickerSheet(
                    title = "Title language",
                    options = AniListTitleLanguage.entries,
                    selected = options?.titleLanguage,
                    label = { it.label() },
                    onSelect = {
                        onAction(AniListOptionsAction.SetTitleLanguageAccount(it))
                        showTitleSheet = false
                    },
                    onDismiss = { showTitleSheet = false },
                )
            }
        }
        SwitchSettingsItem(
            title = "Override title language on this device",
            checked = uiState.titleLanguageOverrideEnabled,
            onCheckedChange = { onAction(AniListOptionsAction.SetTitleLanguageOverrideEnabled(it)) },
        )

        var showStaffSheet by remember { mutableStateOf(false) }
        SelectionSettingsItem(
            title = "Staff & character names",
            currentValue = options?.staffNameLanguage.label(),
            onClick = { showStaffSheet = true },
            enabled = !uiState.isSaving,
        )
        if (showStaffSheet) {
            OptionPickerSheet(
                title = "Staff & character names",
                options = AniListStaffNameLanguage.entries,
                selected = options?.staffNameLanguage,
                label = { it.label() },
                onSelect = {
                    onAction(AniListOptionsAction.SetStaffNameLanguage(it))
                    showStaffSheet = false
                },
                onDismiss = { showStaffSheet = false },
            )
        }

        var showScoreSheet by remember { mutableStateOf(false) }
        SelectionSettingsItem(
            title = "Scoring system",
            currentValue = options?.scoreFormat.label(),
            onClick = { showScoreSheet = true },
            enabled = !uiState.isSaving,
        )
        if (showScoreSheet) {
            OptionPickerSheet(
                title = "Scoring system",
                options = ScoreFormat.entries,
                selected = options?.scoreFormat,
                label = { it.label() },
                onSelect = {
                    onAction(AniListOptionsAction.SetScoreFormat(it))
                    showScoreSheet = false
                },
                onDismiss = { showScoreSheet = false },
            )
        }
    }

    // ── Social & activity ────────────────────────────────────────────────────────────────────────
    SectionLabel("Social & activity")
    SettingsGroup {
        SwitchSettingsItem(
            title = "Airing notifications",
            subtitle = "Get notified when shows you're watching air",
            checked = options?.airingNotifications == true,
            onCheckedChange = { onAction(AniListOptionsAction.SetAiringNotifications(it)) },
            enabled = !uiState.isSaving,
        )
        SwitchSettingsItem(
            title = "Only receive messages from people I follow",
            checked = options?.restrictMessagesToFollowing == true,
            onCheckedChange = { onAction(AniListOptionsAction.SetRestrictMessagesToFollowing(it)) },
            enabled = !uiState.isSaving,
        )

        var showMergeSheet by remember { mutableStateOf(false) }
        SelectionSettingsItem(
            title = "Merge consecutive activity",
            currentValue = activityMergeLabel(options?.activityMergeTime),
            onClick = { showMergeSheet = true },
            enabled = !uiState.isSaving,
        )
        if (showMergeSheet) {
            OptionPickerSheet(
                title = "Merge consecutive activity",
                options = ACTIVITY_MERGE_PRESETS,
                selected = ACTIVITY_MERGE_PRESETS.minByOrNull {
                    kotlin.math.abs(it - (options?.activityMergeTime ?: 0))
                },
                label = { activityMergeLabel(it) },
                onSelect = {
                    onAction(AniListOptionsAction.SetActivityMergeTime(it))
                    showMergeSheet = false
                },
                onDismiss = { showMergeSheet = false },
            )
        }
    }

    SectionLabel("Create list activity for")
    SettingsGroup {
        AniListListActivityStatus.entries.forEach { status ->
            val disabled = options?.disabledListActivity?.get(status) == true
            SwitchSettingsItem(
                title = status.label(),
                checked = !disabled, // "create activity" = NOT disabled
                onCheckedChange = { create ->
                    onAction(AniListOptionsAction.SetListActivityDisabled(status, disabled = !create))
                },
                enabled = !uiState.isSaving,
            )
        }
    }

    // ── Profile ──────────────────────────────────────────────────────────────────────────────────
    SectionLabel("Profile")
    SettingsGroup {
        var showColorSheet by remember { mutableStateOf(false) }
        SelectionSettingsItem(
            title = "Profile color",
            currentValue = options?.profileColor?.replaceFirstChar { it.uppercase() } ?: "Default",
            onClick = { showColorSheet = true },
            enabled = !uiState.isSaving,
        )
        if (showColorSheet) {
            ProfileColorSheet(
                selected = options?.profileColor,
                onSelect = {
                    onAction(AniListOptionsAction.SetProfileColor(it))
                    showColorSheet = false
                },
                onDismiss = { showColorSheet = false },
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    SettingsGroup {
        SettingsItem(
            title = "Refresh from AniList",
            icon = Icons.Outlined.Refresh,
            onClick = { onAction(AniListOptionsAction.Refresh) },
            enabled = !uiState.isLoading && !uiState.isSaving,
        )
    }
}

// ── Small building blocks ────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun NoticeCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionPickerSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(options) { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(option) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileColorSheet(
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Text(
                text = "Profile color",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PROFILE_COLORS.forEach { (name, color) ->
                    val isSelected = name == selected
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { onSelect(name) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ── Labels ───────────────────────────────────────────────────────────────────────────────────────

private fun TitleLanguage.label(): String = when (this) {
    TitleLanguage.ROMAJI -> "Romaji"
    TitleLanguage.ENGLISH -> "English"
    TitleLanguage.NATIVE -> "Native"
}

private fun AniListTitleLanguage?.label(): String = when (this) {
    AniListTitleLanguage.ROMAJI -> "Romaji"
    AniListTitleLanguage.ENGLISH -> "English"
    AniListTitleLanguage.NATIVE -> "Native"
    AniListTitleLanguage.ROMAJI_STYLISED -> "Romaji (stylised)"
    AniListTitleLanguage.ENGLISH_STYLISED -> "English (stylised)"
    AniListTitleLanguage.NATIVE_STYLISED -> "Native (stylised)"
    null -> "Romaji"
}

private fun AniListStaffNameLanguage?.label(): String = when (this) {
    AniListStaffNameLanguage.ROMAJI_WESTERN -> "Romaji, Western order"
    AniListStaffNameLanguage.ROMAJI -> "Romaji"
    AniListStaffNameLanguage.NATIVE -> "Native"
    null -> "Romaji, Western order"
}

private fun StaffNameLanguage.label(): String = when (this) {
    StaffNameLanguage.ROMAJI_WESTERN -> "Romaji, Western order"
    StaffNameLanguage.ROMAJI -> "Romaji"
    StaffNameLanguage.NATIVE -> "Native"
}

private fun ScoreFormat?.label(): String = when (this) {
    ScoreFormat.POINT_100 -> "100 Point (5/100)"
    ScoreFormat.POINT_10_DECIMAL -> "10 Point Decimal (5.5/10)"
    ScoreFormat.POINT_10 -> "10 Point (5/10)"
    ScoreFormat.POINT_5 -> "5 Star (3/5)"
    ScoreFormat.POINT_3 -> "3 Point Smiley :)"
    null -> "10 Point Decimal (5.5/10)"
}

private fun AniListListActivityStatus.label(): String = when (this) {
    AniListListActivityStatus.CURRENT -> "Watching / Reading"
    AniListListActivityStatus.PLANNING -> "Planning"
    AniListListActivityStatus.COMPLETED -> "Completed"
    AniListListActivityStatus.DROPPED -> "Dropped"
    AniListListActivityStatus.PAUSED -> "Paused"
    AniListListActivityStatus.REPEATING -> "Rewatching / Rereading"
}

private val ACTIVITY_MERGE_PRESETS = listOf(0, 30, 60, 120, 360, 720, 1440, 20160)

private fun activityMergeLabel(minutes: Int?): String = when {
    minutes == null -> "Default"
    minutes <= 0 -> "Never"
    minutes >= 20160 -> "Always"
    minutes < 60 -> "$minutes minutes"
    minutes < 1440 -> "${minutes / 60} hour${if (minutes >= 120) "s" else ""}"
    else -> "${minutes / 1440} day${if (minutes >= 2880) "s" else ""}"
}

private val PROFILE_COLORS: List<Pair<String, Color>> = listOf(
    "blue" to Color(0xFF3DB4F2),
    "purple" to Color(0xFFC063FF),
    "pink" to Color(0xFFFC9DD6),
    "orange" to Color(0xFFEF881A),
    "red" to Color(0xFFE13333),
    "green" to Color(0xFF4CCB48),
    "gray" to Color(0xFF677B94),
)
