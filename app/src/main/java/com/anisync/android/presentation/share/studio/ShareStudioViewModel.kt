package com.anisync.android.presentation.share.studio

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import com.anisync.android.data.CardSize
import com.anisync.android.domain.Result
import com.anisync.android.presentation.navigation.ShareStudio
import com.anisync.android.presentation.share.cards.CardBackground
import com.anisync.android.presentation.share.cards.CardDetail
import com.anisync.android.presentation.share.cards.CardSpec
import com.anisync.android.presentation.share.cards.CardTone
import com.anisync.android.presentation.share.cards.ShareSubject
import com.anisync.android.presentation.share.cards.ShareTemplate
import com.anisync.android.presentation.share.cards.templatesFor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ShareStudioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loader: ShareSubjectLoader,
    private val appSettings: AppSettings,
    private val imageLoader: ImageLoader,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val route: ShareStudio = savedStateHandle.toRoute()
    private var appTone = CardTone.DARK
    private var swatchChosen = false

    private val _uiState = MutableStateFlow(ShareStudioUiState(spec = CardSpec(size = appSettings.defaultCardSize.value)))
    val uiState: StateFlow<ShareStudioUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun onAction(action: ShareStudioAction) {
        when (action) {
            is ShareStudioAction.SelectTemplate -> _uiState.update { state ->
                if (action.index !in state.templates.indices) return@update state
                state.copy(templateIndex = action.index)
            }
            is ShareStudioAction.SelectSize -> _uiState.update { state ->
                state.copy(spec = state.spec.copy(size = action.size, background = state.spec.background.allowedAt(action.size, state.template)))
            }
            is ShareStudioAction.SelectSwatch -> {
                swatchChosen = true
                _uiState.update { it.copy(swatchIndex = action.index) }
            }
            is ShareStudioAction.SelectTone -> _uiState.update {
                it.copy(spec = it.spec.copy(tone = action.tone), toneChosen = true)
            }
            is ShareStudioAction.SelectBackground -> _uiState.update { state ->
                // Clear only exists at Card for chats, so picking it takes the card there.
                val size = if (action.background == CardBackground.CLEAR) CardSize.CHAT else state.spec.size
                state.copy(spec = state.spec.copy(size = size, background = action.background))
            }
            is ShareStudioAction.SetDim -> _uiState.update { it.copy(spec = it.spec.copy(dim = action.dim.coerceIn(0f, 1f))) }
            is ShareStudioAction.ToggleDetail -> _uiState.update { state ->
                val details = state.spec.details
                state.copy(spec = state.spec.copy(details = if (action.detail in details) details - action.detail else details + action.detail))
            }
            is ShareStudioAction.SetCaption -> _uiState.update {
                it.copy(spec = it.spec.copy(caption = action.caption.take(CardSpec.CAPTION_LIMIT)))
            }
            is ShareStudioAction.SelectPanel -> _uiState.update { it.copy(panel = action.panel) }
            is ShareStudioAction.FollowAppTone -> {
                appTone = action.tone
                _uiState.update { if (it.toneChosen) it else it.copy(spec = it.spec.copy(tone = action.tone)) }
            }
            ShareStudioAction.Reset -> {
                swatchChosen = false
                _uiState.update { state ->
                    val subject = state.subject ?: return@update state
                    state.copy(
                        templateIndex = 0,
                        spec = defaultSpec(subject, templatesFor(subject).first()),
                        toneChosen = false,
                        swatchIndex = 0,
                    )
                }
            }
        }
    }

    private suspend fun load() {
        val subject = (loader.load(route) as? Result.Success)?.data
        if (subject == null) {
            _uiState.update { it.copy(failed = true) }
            return
        }
        val aniList = subject.aniListColor?.let { listOf(StudioSwatch(aniListLabel(subject), it, it)) }.orEmpty()
        _uiState.update {
            it.copy(
                subject = subject,
                spec = defaultSpec(subject, templatesFor(subject).first()),
                swatches = aniList + AppSwatch,
            )
        }
        val cover = subject.artworkUrl?.let { runCatching { coverSwatchesOf(it) }.getOrNull() }.orEmpty()
        if (cover.isEmpty()) return
        _uiState.update { state ->
            val current = state.swatch
            val swatches = aniList + cover + AppSwatch
            val index = if (swatchChosen && current != null) swatches.indexOf(current).coerceAtLeast(0) else 0
            state.copy(swatches = swatches, swatchIndex = index)
        }
    }

    private fun defaultSpec(subject: ShareSubject, template: ShareTemplate<*>): CardSpec {
        val details = subject.details.toMutableSet()
        if (appSettings.hideScoresOnCards.value) details -= CardDetail.SCORE
        val size = appSettings.defaultCardSize.value
        return CardSpec(
            size = size,
            tone = appTone,
            background = template.defaultBackground.allowedAt(size, template),
            details = details,
        )
    }

    private fun CardBackground.allowedAt(size: CardSize, template: ShareTemplate<*>?): CardBackground =
        if (this == CardBackground.CLEAR && size != CardSize.CHAT) {
            template?.defaultBackground?.takeIf { it != CardBackground.CLEAR } ?: CardBackground.SOLID
        } else {
            this
        }

    private suspend fun coverSwatchesOf(url: String): List<StudioSwatch> {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(SwatchSampleSize)
            .allowHardware(false)
            .build()
        val bitmap = (imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap() ?: return emptyList()
        return withContext(Dispatchers.Default) {
            coverSwatches(bitmap)
                .map { argb -> StudioSwatch(swatchName(argb), Color(argb), Color(argb)) }
                .distinctBy { it.label }
        }
    }

    private fun aniListLabel(subject: ShareSubject): Int = when (subject) {
        is ShareSubject.Stats, is ShareSubject.Favourites -> R.string.share_swatch_profile
        else -> R.string.share_card_source
    }

    private companion object {
        const val SwatchSampleSize = 128
        val AppSwatch = StudioSwatch(R.string.share_swatch_app, Color.Unspecified, null)
    }
}
