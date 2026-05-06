package com.anisync.android.presentation.components.richtext

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal fun TextFieldValue.wrapSelection(prefix: String, suffix: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selected = text.substring(start, end)
    val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
    val cursor = if (selected.isEmpty()) {
        start + prefix.length
    } else {
        start + prefix.length + selected.length + suffix.length
    }
    return copy(text = newText, selection = TextRange(cursor))
}

internal fun TextFieldValue.getSelectedText(): String =
    text.substring(selection.min, selection.max)

internal fun TextFieldValue.insertAtCursor(insertion: String): TextFieldValue {
    val pos = selection.min
    val newText = text.substring(0, pos) + insertion + text.substring(pos)
    return copy(text = newText, selection = TextRange(pos + insertion.length))
}

internal fun TextFieldValue.toggleLinkSyntax(): TextFieldValue {
    val selected = getSelectedText()
    return if (selected.isNotEmpty()) {
        wrapSelection("[", "](url)")
    } else {
        insertAtCursor("[text](url)")
    }
}
