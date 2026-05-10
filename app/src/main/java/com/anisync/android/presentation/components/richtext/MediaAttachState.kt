package com.anisync.android.presentation.components.richtext

import android.net.Uri
import com.anisync.android.domain.media.MediaKind
import com.anisync.android.domain.media.MediaSizeChoice

/**
 * State for the media-attach flow inside a single rich-text composer.
 *
 * Lifecycle:
 *   Idle ─pick─▶ Picked ─upload─▶ Uploading ─done─▶ Idle (markdown inserted)
 *                                            └fail─▶ Failed ─retry─▶ Uploading
 *                                                            └cancel─▶ Idle
 *
 * IME-committed content (Samsung / Gboard GIF) skips Picked and goes straight
 * to Uploading at default size; [Source] tags those so the composer can render
 * an inline progress strip while the attach sheet stays closed.
 */
sealed interface MediaAttachState {

    enum class Source { SheetPick, Ime }

    data object Idle : MediaAttachState

    data class Picked(
        val uri: Uri,
        val mime: String,
        val displayName: String,
        val kind: MediaKind,
        val size: MediaSizeChoice,
        val customSizeText: String = "",
        val source: Source = Source.SheetPick
    ) : MediaAttachState

    data class Uploading(
        val displayName: String,
        val uploaded: Long,
        val total: Long,
        val source: Source = Source.SheetPick
    ) : MediaAttachState

    data class Failed(
        val displayName: String,
        val message: String,
        val retry: Picked
    ) : MediaAttachState
}
