package com.anisync.android.presentation.components.richtext

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.media.MediaUploaderFactory
import com.anisync.android.domain.media.MediaKind
import com.anisync.android.domain.media.MediaSizeChoice
import com.anisync.android.domain.media.toImageMarkdown
import com.anisync.android.domain.media.videoMarkdown
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the upload lifecycle for a single composer instance. Multiple composers
 * never share a VM (each calls `hiltViewModel()` inside its own composition), so
 * concurrent attaches on different surfaces don't collide.
 */
@HiltViewModel
class MediaAttachViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploaderFactory: MediaUploaderFactory
) : ViewModel() {

    private val _state = MutableStateFlow<MediaAttachState>(MediaAttachState.Idle)
    val state: StateFlow<MediaAttachState> = _state.asStateFlow()

    private var uploadJob: Job? = null

    fun pick(uri: Uri) {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val name = queryDisplayName(uri)
        val kind = mediaKindFromMime(mime)
        _state.value = MediaAttachState.Picked(
            uri = uri,
            mime = mime,
            displayName = name,
            kind = kind,
            size = MediaSizeChoice.Default
        )
    }

    fun setSize(size: MediaSizeChoice) {
        val current = _state.value as? MediaAttachState.Picked ?: return
        _state.value = current.copy(size = size)
    }

    fun setCustomSizeText(text: String) {
        val current = _state.value as? MediaAttachState.Picked ?: return
        _state.value = current.copy(customSizeText = text)
    }

    fun cancel() {
        uploadJob?.cancel()
        uploadJob = null
        _state.value = MediaAttachState.Idle
    }

    /** Drops any pending pick or failure without uploading. */
    fun reset() {
        cancel()
    }

    fun retry(onMarkdownReady: (String) -> Unit) {
        val failed = _state.value as? MediaAttachState.Failed ?: return
        _state.value = failed.retry
        upload(onMarkdownReady)
    }

    /**
     * Uploads the currently-picked media via the user's selected host. On success,
     * emits AniList markdown via [onMarkdownReady] and resets to Idle. On failure,
     * transitions to [MediaAttachState.Failed] with the error message preserved.
     */
    fun upload(onMarkdownReady: (String) -> Unit) {
        val picked = _state.value as? MediaAttachState.Picked ?: return
        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            _state.value = MediaAttachState.Uploading(
                displayName = picked.displayName,
                uploaded = 0L,
                total = -1L
            )
            val result = uploaderFactory.current().upload(picked.uri, picked.mime) { up, total ->
                val current = _state.value
                if (current is MediaAttachState.Uploading) {
                    _state.value = current.copy(uploaded = up, total = total)
                }
            }
            result
                .onSuccess { uploaded ->
                    val markdown = when (uploaded.kind) {
                        MediaKind.Video -> videoMarkdown(uploaded.url)
                        else -> resolveSize(picked).toImageMarkdown(uploaded.url)
                    }
                    onMarkdownReady(markdown)
                    _state.value = MediaAttachState.Idle
                }
                .onFailure { err ->
                    _state.value = MediaAttachState.Failed(
                        displayName = picked.displayName,
                        message = err.message ?: "Upload failed",
                        retry = picked
                    )
                }
        }
    }

    /**
     * Skip-picker path used by the IME content receiver: ingests an already-known
     * URI + MIME and uploads at the default size immediately.
     */
    fun ingestFromIme(uri: Uri, mime: String, onMarkdownReady: (String) -> Unit) {
        val name = queryDisplayName(uri)
        val kind = mediaKindFromMime(mime)
        _state.value = MediaAttachState.Picked(
            uri = uri,
            mime = mime,
            displayName = name,
            kind = kind,
            size = MediaSizeChoice.Default
        )
        upload(onMarkdownReady)
    }

    private fun queryDisplayName(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null }
        }.getOrNull() ?: "upload.bin"
    }

    private fun mediaKindFromMime(mime: String): MediaKind = when {
        mime.equals("image/gif", ignoreCase = true) -> MediaKind.Gif
        mime.startsWith("video/", ignoreCase = true) -> MediaKind.Video
        else -> MediaKind.Image
    }

    private fun resolveSize(picked: MediaAttachState.Picked): MediaSizeChoice {
        val raw = picked.customSizeText.trim()
        return when (val s = picked.size) {
            is MediaSizeChoice.CustomPx, is MediaSizeChoice.CustomPercent -> s
            else -> if (raw.isBlank()) s else parseCustomSize(raw) ?: s
        }
    }

    private fun parseCustomSize(text: String): MediaSizeChoice? {
        val trimmed = text.trim().removeSuffix(" ")
        return if (trimmed.endsWith("%")) {
            trimmed.dropLast(1).trim().toIntOrNull()?.let { MediaSizeChoice.CustomPercent(it) }
        } else {
            trimmed.toIntOrNull()?.let { MediaSizeChoice.CustomPx(it) }
        }
    }
}
