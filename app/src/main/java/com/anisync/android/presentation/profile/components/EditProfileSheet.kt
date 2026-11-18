package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.richtext.RichTextInputSheet

/** Edits your bio. The profile colour, picture and banner stay in Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    about: String,
    isSaving: Boolean,
    onSave: (about: String) -> Unit,
    onDismiss: () -> Unit
) {
    RichTextInputSheet(
        title = stringResource(R.string.profile_edit),
        placeholder = stringResource(R.string.edit_profile_about_label),
        submitLabel = stringResource(R.string.save),
        isSubmitting = isSaving,
        onSubmit = onSave,
        onDismiss = { if (!isSaving) onDismiss() },
        prefillBody = about,
        minLength = 0,
        maxLength = 65_000,
        isSubmitEnabled = { body -> !isSaving && body != about },
        header = {
            Text(
                text = stringResource(R.string.edit_profile_about_section),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp)
            )
        }
    )
}
