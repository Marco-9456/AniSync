package com.anisync.android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import androidx.compose.ui.res.stringResource
import com.anisync.android.domain.AniListUserOptions
import com.anisync.android.domain.ConflictState
import com.anisync.android.domain.UserOptionField
import com.anisync.android.domain.UserOptionsRepository

/**
 * App-wide host for the options sync-conflict prompt. Hosted once at the app root (see MainActivity)
 * so a conflict detected by the cold-start pull surfaces right after launch, not only when the user
 * happens to open AniList Settings.
 */
@Composable
fun UserOptionsConflictHandler(repository: UserOptionsRepository) {
    val conflict by repository.conflict.collectAsStateWithLifecycle()
    val local by repository.cachedOptions.collectAsStateWithLifecycle()
    conflict?.let {
        UserOptionsConflictDialog(
            conflict = it,
            local = local,
            onResolve = repository::resolveConflict,
        )
    }
}

@Composable
private fun UserOptionsConflictDialog(
    conflict: ConflictState,
    local: AniListUserOptions?,
    onResolve: (keepLocal: Boolean) -> Unit,
) {
    AlertDialog(
        // No outside-dismiss: the user must pick a direction to clear the conflict.
        onDismissRequest = {},
        title = { Text(stringResource(R.string.options_conflict_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.options_conflict_intro))
                conflict.fields.forEach { field ->
                    Column {
                        Text(
                            text = field.displayName(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(
                                R.string.options_conflict_field_values,
                                local?.conflictValueLabel(field) ?: "—",
                                conflict.serverValues.conflictValueLabel(field),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(stringResource(R.string.options_conflict_question))
            }
        },
        confirmButton = {
            TextButton(onClick = { onResolve(true) }) {
                Text(stringResource(R.string.options_conflict_keep_local))
            }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(false) }) {
                Text(stringResource(R.string.options_conflict_use_website))
            }
        },
    )
}

/** Human-readable label for a conflicting option field. */
private fun UserOptionField.displayName(): String = when (this) {
    UserOptionField.DISPLAY_ADULT_CONTENT -> "Adult content"
    UserOptionField.TITLE_LANGUAGE -> "Title language"
    UserOptionField.STAFF_NAME_LANGUAGE -> "Staff & character names"
    UserOptionField.SCORE_FORMAT -> "Scoring system"
    UserOptionField.AIRING_NOTIFICATIONS -> "Airing notifications"
    UserOptionField.RESTRICT_MESSAGES_TO_FOLLOWING -> "Message restriction"
    UserOptionField.ACTIVITY_MERGE_TIME -> "Activity merge time"
    UserOptionField.PROFILE_COLOR -> "Profile color"
    UserOptionField.DISABLED_LIST_ACTIVITY -> "List activity"
}

/** Human-readable value of a single field, for spelling out each side of a conflict. */
private fun AniListUserOptions.conflictValueLabel(field: UserOptionField): String = when (field) {
    UserOptionField.DISPLAY_ADULT_CONTENT -> if (displayAdultContent) "On" else "Off"
    UserOptionField.TITLE_LANGUAGE -> titleLanguage?.toBaseLanguage().label()
    UserOptionField.STAFF_NAME_LANGUAGE -> staffNameLanguage.label()
    UserOptionField.SCORE_FORMAT -> scoreFormat.label()
    UserOptionField.AIRING_NOTIFICATIONS -> if (airingNotifications) "On" else "Off"
    UserOptionField.RESTRICT_MESSAGES_TO_FOLLOWING -> if (restrictMessagesToFollowing) "On" else "Off"
    UserOptionField.ACTIVITY_MERGE_TIME -> activityMergeLabel(activityMergeTime)
    UserOptionField.PROFILE_COLOR -> profileColor?.replaceFirstChar { it.uppercase() } ?: "Default"
    UserOptionField.DISABLED_LIST_ACTIVITY -> "Custom"
}
