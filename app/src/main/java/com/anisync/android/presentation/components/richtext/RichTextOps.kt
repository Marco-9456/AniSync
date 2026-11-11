package com.anisync.android.presentation.components.richtext

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import com.anisync.android.domain.ContentLimits

/**
 * Caps the field at [maxLength] characters. Excess input is silently dropped
 * (typing keeps the prefix; paste truncates), so the field's character counter
 * tops out cleanly instead of letting the user run past the AniList-enforced
 * limit and discovering the rejection only on submit.
 */
internal class MaxLengthInputTransformation(private val maxLength: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val text = asCharSequence()
        if (ContentLimits.characterCount(text) <= maxLength) return
        delete(Character.offsetByCodePoints(text, 0, maxLength), length)
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

/**
 * Wraps the selection in spoiler markers. A selection over several lines gets them on lines of their
 * own, the block form AniList draws as one box, with room for a `{label}` after the opener.
 */
internal fun TextFieldState.wrapSpoiler() {
    edit {
        val text = asCharSequence()
        val start = selection.min
        val end = selection.max
        val selected = text.substring(start, end)
        if (!selected.contains('\n')) {
            replace(start, end, "~!$selected!~")
            return@edit
        }
        val before = if (start > 0 && text[start - 1] != '\n') "\n" else ""
        val after = if (end < text.length && text[end] != '\n') "\n" else ""
        replace(start, end, "$before~!\n${selected.trim('\n')}\n!~$after")
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
