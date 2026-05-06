package com.anisync.android.presentation.components.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Lets a parent inject text at the cursor of a [RichTextScaffold] body field
 * without owning the underlying [androidx.compose.ui.text.input.TextFieldValue].
 *
 * Pass an instance via `rememberRichTextInsertController()` to the scaffold;
 * the scaffold binds [insertText] to its body state. Calls before binding are
 * no-ops.
 */
class RichTextInsertController internal constructor() {
    private var insert: ((String) -> Unit)? = null

    internal fun bind(insertFn: (String) -> Unit) {
        insert = insertFn
    }

    fun insertText(text: String) {
        insert?.invoke(text)
    }
}

@Composable
fun rememberRichTextInsertController(): RichTextInsertController =
    remember { RichTextInsertController() }
