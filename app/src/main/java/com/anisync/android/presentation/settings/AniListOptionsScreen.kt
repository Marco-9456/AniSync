package com.anisync.android.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.anisync.android.data.StaffNameLanguage
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.AniListListActivityStatus
import com.anisync.android.domain.AniListStaffNameLanguage
import com.anisync.android.domain.AniListTitleLanguage
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.settings.components.SettingsPickerSheet
import com.anisync.android.ui.theme.AniListProfileColors

/**
 * The AniList account-options sections (content & titles, social & activity, profile). Hosted by
 * [AniListSettingsScreen]; renders the cached account values and routes edits to the view model.
 * Everything here is the account's value and edits push to AniList via UpdateUser, except the two
 * device-override controls for adult content and title language.
 */
@Composable
internal fun AniListOptionsContent(
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
            subtitle = "Include 18+ anime and manga in search, the activity feed, and lists",
            checked = options?.displayAdultContent == true,
            onCheckedChange = { onAction(AniListOptionsAction.SetAdultContent(it)) },
        )

        var showTitleSheet by remember { mutableStateOf(false) }
        SelectionSettingsItem(
            title = "Title language",
            currentValue = options?.titleLanguage?.toBaseLanguage().label(),
            onClick = { showTitleSheet = true },
        )
        if (showTitleSheet) {
            OptionPickerSheet(
                title = "Title language",
                options = TITLE_LANGUAGE_OPTIONS,
                selected = options?.titleLanguage?.toBaseLanguage(),
                label = { it.label() },
                subtitle = { it.example() },
                onSelect = {
                    onAction(AniListOptionsAction.SetTitleLanguage(it))
                    showTitleSheet = false
                },
                onDismiss = { showTitleSheet = false },
            )
        }

        var showStaffSheet by remember { mutableStateOf(false) }
        SelectionSettingsItem(
            title = "Staff & character names",
            currentValue = options?.staffNameLanguage.label(),
            onClick = { showStaffSheet = true },
        )
        if (showStaffSheet) {
            OptionPickerSheet(
                title = "Staff & character names",
                options = AniListStaffNameLanguage.entries,
                selected = options?.staffNameLanguage,
                label = { it.label() },
                subtitle = { it.example() },
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
        )
        SwitchSettingsItem(
            title = "Only receive messages from people I follow",
            checked = options?.restrictMessagesToFollowing == true,
            onCheckedChange = { onAction(AniListOptionsAction.SetRestrictMessagesToFollowing(it)) },
        )

        var showMergeSheet by remember { mutableStateOf(false) }
        SelectionSettingsItem(
            title = "Merge consecutive activity",
            currentValue = activityMergeLabel(options?.activityMergeTime),
            onClick = { showMergeSheet = true },
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
internal fun SectionLabel(text: String) = SettingsSectionLabel(text)

// ── Pickers (bottom sheets) ─────────────────────────────────────────────────────────────────────

/**
 * Thin adapter onto the shared [SettingsPickerSheet] so this screen's plain-string pickers read the
 * same as every other picker in Settings. These are quick choices, so they stay as bottom sheets.
 */
@Composable
private fun <T> OptionPickerSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    subtitle: (T) -> String? = { null },
) {
    SettingsPickerSheet(
        title = title,
        items = options,
        selected = selected,
        itemLabel = { label(it) },
        itemSubtitle = { subtitle(it) },
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileColorSheet(
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppModalBottomSheet(
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AniListProfileColors.forEach { (name, color) ->
                    val isSelected = name == selected
                    Box(
                        modifier = Modifier
                            .size(48.dp)
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

internal fun AniListTitleLanguage?.label(): String = when (this) {
    AniListTitleLanguage.ROMAJI -> "Romaji"
    AniListTitleLanguage.ENGLISH -> "English"
    AniListTitleLanguage.NATIVE -> "Native"
    AniListTitleLanguage.ROMAJI_STYLISED -> "Romaji (stylised)"
    AniListTitleLanguage.ENGLISH_STYLISED -> "English (stylised)"
    AniListTitleLanguage.NATIVE_STYLISED -> "Native (stylised)"
    null -> "Romaji"
}

internal fun AniListStaffNameLanguage?.label(): String = when (this) {
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

// Worked examples shown under each language option so the choice is concrete.

private fun TitleLanguage.example(): String = when (this) {
    TitleLanguage.ROMAJI -> "Shingeki no Kyojin"
    TitleLanguage.ENGLISH -> "Attack on Titan"
    TitleLanguage.NATIVE -> "進撃の巨人"
}

private fun AniListTitleLanguage?.example(): String = when (this) {
    AniListTitleLanguage.ROMAJI, AniListTitleLanguage.ROMAJI_STYLISED -> "Shingeki no Kyojin"
    AniListTitleLanguage.ENGLISH, AniListTitleLanguage.ENGLISH_STYLISED -> "Attack on Titan"
    AniListTitleLanguage.NATIVE, AniListTitleLanguage.NATIVE_STYLISED -> "進撃の巨人"
    null -> "Shingeki no Kyojin"
}

private fun AniListStaffNameLanguage?.example(): String = when (this) {
    AniListStaffNameLanguage.ROMAJI_WESTERN -> "Hajime Isayama"
    AniListStaffNameLanguage.ROMAJI -> "Isayama Hajime"
    AniListStaffNameLanguage.NATIVE -> "諫山 創"
    null -> "Hajime Isayama"
}

internal fun ScoreFormat?.label(): String = when (this) {
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

/**
 * Title languages offered in the picker. The creator-stylised variants are intentionally excluded —
 * AniList's own site no longer surfaces them and they render identically to the base option here.
 */
private val TITLE_LANGUAGE_OPTIONS = listOf(
    AniListTitleLanguage.ROMAJI,
    AniListTitleLanguage.ENGLISH,
    AniListTitleLanguage.NATIVE,
)

/** Collapse a (possibly stylised) account title language onto its base for selection/display. */
internal fun AniListTitleLanguage.toBaseLanguage(): AniListTitleLanguage = when (this) {
    AniListTitleLanguage.ROMAJI, AniListTitleLanguage.ROMAJI_STYLISED -> AniListTitleLanguage.ROMAJI
    AniListTitleLanguage.ENGLISH, AniListTitleLanguage.ENGLISH_STYLISED -> AniListTitleLanguage.ENGLISH
    AniListTitleLanguage.NATIVE, AniListTitleLanguage.NATIVE_STYLISED -> AniListTitleLanguage.NATIVE
}

private val ACTIVITY_MERGE_PRESETS = listOf(0, 30, 60, 120, 360, 720, 1440, 20160)

internal fun activityMergeLabel(minutes: Int?): String = when {
    minutes == null -> "Default"
    minutes <= 0 -> "Never"
    minutes >= 20160 -> "Always"
    minutes < 60 -> "$minutes minutes"
    minutes < 1440 -> "${minutes / 60} hour${if (minutes >= 120) "s" else ""}"
    else -> "${minutes / 1440} day${if (minutes >= 2880) "s" else ""}"
}
