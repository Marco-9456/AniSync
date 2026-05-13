package com.anisync.android.presentation.components.richtext

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.R
import com.anisync.android.data.media.MediaUploaderFactory
import com.anisync.android.data.media.queryDisplayName
import com.anisync.android.domain.media.MediaKind
import com.anisync.android.domain.media.MediaSizeChoice
import com.anisync.android.domain.media.toImageMarkdown
import com.anisync.android.domain.media.videoMarkdown
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
        val name = context.queryDisplayName(uri)
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
                total = -1L,
                source = picked.source
            )
            // Progress fires from OkHttp's IO thread; hop to Main so StateFlow
            // consumers (Compose recomposers) see ordered, frame-aligned updates.
            // UriRequestBody already throttles to ~20 Hz, so the launch volume
            // is bounded.
            val result = uploaderFactory.current().upload(picked.uri, picked.mime) { up, total ->
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    val cur = _state.value
                    if (cur is MediaAttachState.Uploading) {
                        _state.value = cur.copy(uploaded = up, total = total)
                    }
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
                        message = mapErrorMessage(err.message),
                        retry = picked
                    )
                }
        }
    }

    /**
     * Skip-picker path used by the IME content receiver: ingests an already-known
     * URI + MIME and uploads at the default size immediately. Tags the upload as
     * [MediaAttachState.Source.Ime] so the composer renders an inline progress
     * strip instead of waiting for the attach sheet to be opened.
     */
    fun ingestFromIme(uri: Uri, mime: String, onMarkdownReady: (String) -> Unit) {
        val name = context.queryDisplayName(uri)
        val kind = mediaKindFromMime(mime)
        _state.value = MediaAttachState.Picked(
            uri = uri,
            mime = mime,
            displayName = name,
            kind = kind,
            size = MediaSizeChoice.Default,
            source = MediaAttachState.Source.Ime
        )
        upload(onMarkdownReady)
    }

    private fun mapErrorMessage(raw: String?): String {
        val msg = raw ?: return "Upload failed"
        // Catbox bot/abuse filter — surfaces as HTTP 412 with body "Invalid uploader".
        // Even with an identifying UA, certain content (often tenor-sourced GIFs) stays
        // blocked by hash. Tell the user what to do instead of echoing the raw message.
        if (msg.contains("412") || msg.contains("Invalid uploader", ignoreCase = true)) {
            return context.getString(R.string.media_attach_error_catbox_rejected)
        }
        return msg
    }

    private fun mediaKindFromMime(mime: String): MediaKind = when {
        mime.equals("image/gif", ignoreCase = true) -> MediaKind.Gif
        mime.startsWith("video/", ignoreCase = true) -> MediaKind.Video
        else -> MediaKind.Image
    }

    private fun resolveSize(picked: MediaAttachState.Picked): MediaSizeChoice {
        return parseCustomSize(picked.customSizeText) ?: picked.size
    }

    private fun parseCustomSize(text: String): MediaSizeChoice? {
        val trimmed = text.trim()
        return if (trimmed.endsWith("%")) {
            trimmed.dropLast(1).trim().toIntOrNull()?.let { MediaSizeChoice.CustomPercent(it) }
        } else {
            trimmed.toIntOrNull()?.let { MediaSizeChoice.CustomPx(it) }
        }
    }
}
