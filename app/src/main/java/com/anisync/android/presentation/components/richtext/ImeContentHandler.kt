package com.anisync.android.presentation.components.richtext

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume

/**
 * Bridges Samsung / Gboard / any IME's `commitContent` flow into the upload
 * pipeline. Consumes URIs whose MIME type is image or video, hands them to the
 * VM, and returns the unconsumed remainder so other receivers (e.g. paste) can
 * still see plain text.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun handleImeContent(
    transferableContent: TransferableContent,
    viewModel: MediaAttachViewModel,
    onMarkdownReady: (String) -> Unit
): TransferableContent? {
    val description = transferableContent.clipEntry.clipData.description
    val mediaMime = (0 until description.mimeTypeCount)
        .map { description.getMimeType(it) }
        .firstOrNull { it.startsWith("image/") || it.startsWith("video/") }
        ?: return transferableContent
    return transferableContent.consume { item ->
        val uri = item.uri ?: return@consume false
        viewModel.ingestFromIme(uri, mediaMime, onMarkdownReady)
        true
    }
}
