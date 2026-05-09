package com.anisync.android.presentation.components.richtext

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert

internal fun TextFieldState.wrapSelection(prefix: String, suffix: String) {
    edit {
        val start = selection.min
        val end = selection.max
        val selected = asCharSequence().substring(start, end)
        replace(start, end, "$prefix$selected$suffix")
    }
}

internal fun TextFieldState.getSelectedText(): String =
    text.substring(selection.min, selection.max)

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