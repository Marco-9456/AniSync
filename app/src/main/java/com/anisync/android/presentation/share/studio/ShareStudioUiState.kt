package com.anisync.android.presentation.share.studio

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.anisync.android.data.CardSize
import com.anisync.android.presentation.share.cards.CardBackground
import com.anisync.android.presentation.share.cards.CardDetail
import com.anisync.android.presentation.share.cards.CardSpec
import com.anisync.android.presentation.share.cards.CardTone
import com.anisync.android.presentation.share.cards.ShareSubject
import com.anisync.android.presentation.share.cards.ShareTemplate
import com.anisync.android.presentation.share.cards.templatesFor

/** The studio's four panels, one at a time on a phone and stacked on a tablet. */
enum class StudioPanel { LAYOUT, COLOR, BACKGROUND, DETAILS }

/** Why the studio has no card to show. */
enum class StudioFailure {
    /** The route names nothing there is to share. */
    NOTHING,

    /** A request failed, trying again may help. */
    FAILED,

    /** AniList's rate limit outlasted every wait. */
    RATE_LIMITED,
}

/** A palette the card can wear. A null [seed] is the app's own palette, which only the screen can build. */
@Immutable
data class StudioSwatch(
    @StringRes val label: Int,
    /** The colour shown on the swatch's top half, as AniList or the cover has it. */
    val source: Color,
    val seed: Color?,
)

@Immutable
data class ShareStudioUiState(
    val subject: ShareSubject? = null,
    val failure: StudioFailure? = null,
    val templateIndex: Int = 0,
    val spec: CardSpec = CardSpec(size = CardSize.STORY),
    /** Until the viewer picks one, the card follows the app's light or dark tone. */
    val toneChosen: Boolean = false,
    val swatches: List<StudioSwatch> = emptyList(),
    val swatchIndex: Int = 0,
    val panel: StudioPanel = StudioPanel.LAYOUT,
) {
    val templates: List<ShareTemplate<*>> get() = subject?.let(::templatesFor).orEmpty()
    val template: ShareTemplate<*>? get() = templates.getOrNull(templateIndex)
    val swatch: StudioSwatch? get() = swatches.getOrNull(swatchIndex)

    /** Clear needs transparent corners, which only Card for chats has. */
    val clearAllowed: Boolean get() = spec.size == CardSize.CHAT

    /** Dimming only reaches the artwork and the gradient. */
    val dimAllowed: Boolean get() = spec.background == CardBackground.ARTWORK_BLUR || spec.background == CardBackground.GRADIENT
}

sealed interface ShareStudioAction {
    data class SelectTemplate(val index: Int) : ShareStudioAction
    data class SelectSize(val size: CardSize) : ShareStudioAction
    data class SelectSwatch(val index: Int) : ShareStudioAction
    data class SelectTone(val tone: CardTone) : ShareStudioAction
    data class SelectBackground(val background: CardBackground) : ShareStudioAction
    data class SetDim(val dim: Float) : ShareStudioAction
    data class ToggleDetail(val detail: CardDetail) : ShareStudioAction
    data class SetCaption(val caption: String) : ShareStudioAction
    data class SelectPanel(val panel: StudioPanel) : ShareStudioAction

    /** The tone the app is in right now, which the card starts in. */
    data class FollowAppTone(val tone: CardTone) : ShareStudioAction
    data object Reset : ShareStudioAction
    data object Retry : ShareStudioAction
}
