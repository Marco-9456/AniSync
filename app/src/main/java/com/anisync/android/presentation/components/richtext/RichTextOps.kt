package com.anisync.android.presentation.components.richtext

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete

/**
 * Caps the field at [maxLength] characters. Excess input is silently dropped
 * (typing keeps the prefix; paste truncates), so the field's character counter
 * tops out cleanly instead of letting the user run past the AniList-enforced
 * limit and discovering the rejection only on submit.
 */
internal class MaxLengthInputTransformation(private val maxLength: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (length > maxLength) delete(maxLength, length)
    }
}

internal fun TextFieldState.wrapSelection(prefix: String, suffix: String) {
    edit {
        val start = selection.min
        val end = selection.max
        val selected = asCharSequence().substring(start, end)
        replace(start, end, "$prefix$selected$suffix")
    }
}

internal fun TextFieldState.insertAtCursor(insertion: String) {
    edit {
        replace(selection.min, selection.max, insertion)
    }
}

internal fun TextFieldState.toggleLinkSyntax() {
    edit {
        val start = selection.min
        val end = selection.max
        val selected = asCharSequence().substring(start, end)
        if (selected.isNotEmpty()) {
            replace(start, end, "[$selected](url)")
        } else {
            replace(start, end, "[text](url)")
        }
    }
}
