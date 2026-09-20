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
import com.anisync.android.data.media.CatboxUploader
import com.anisync.android.data.media.CustomMultipartUploader
import com.anisync.android.data.media.LitterboxUploader
import com.anisync.android.data.media.MediaUploadException
import com.anisync.android.data.media.UploadFailure
import com.anisync.android.domain.media.MediaHost
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
                        message = mapErrorMessage(err),
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

    /**
     * Says what actually went wrong. Every failure used to read "Catbox rejected this file",
     * because the only test was whether the message mentioned 412 — which it also does for a
     * stale user hash and for a request that arrived empty. Those are fixed in two different
     * places, and neither is the file.
     */
    private fun mapErrorMessage(error: Throwable): String {
        val failure = error as? MediaUploadException
            ?: return when (error) {
                is UnknownHostException, is ConnectException, is SocketTimeoutException ->
                    context.getString(R.string.media_attach_error_offline, currentHostName())
                else -> error.message ?: context.getString(R.string.media_attach_error_generic)
            }

        return when (failure.reason) {
            is UploadFailure.HostDown ->
                context.getString(R.string.media_attach_error_host_down, failure.host)
            UploadFailure.BadUserHash ->
                context.getString(R.string.media_attach_error_bad_userhash)
            UploadFailure.NoFileReceived ->
                context.getString(R.string.media_attach_error_no_file, failure.host)
            UploadFailure.FileRejected ->
                context.getString(R.string.media_attach_error_catbox_rejected)
            is UploadFailure.Unexpected -> failure.message
                ?: context.getString(R.string.media_attach_error_generic)
        }
    }

    private fun currentHostName(): String = when (uploaderFactory.currentHost()) {
        MediaHost.CATBOX -> CatboxUploader.HOST
        MediaHost.LITTERBOX -> LitterboxUploader.HOST
        MediaHost.CUSTOM -> CustomMultipartUploader.HOST
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
