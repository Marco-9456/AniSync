package com.anisync.android.presentation.recap

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.anisync.android.data.AppSettings
import com.anisync.android.data.account.AccountStore
import com.anisync.android.domain.MediaThemesRepository
import com.anisync.android.domain.ProfileStatsLauncher
import com.anisync.android.domain.RecapRepository
import com.anisync.android.domain.Result
import com.anisync.android.domain.StoredRecap
import com.anisync.android.domain.recap.PageSeed
import com.anisync.android.domain.recap.RecapPeriod
import com.anisync.android.domain.recap.ThemeSongCandidate
import com.anisync.android.domain.recap.pickThemeSong
import com.anisync.android.domain.recap.rankNote
import com.anisync.android.domain.recap.recapPages
import com.anisync.android.domain.recap.seed
import com.anisync.android.presentation.navigation.RecapStory
import com.anisync.android.ui.theme.genreSeed
import com.anisync.android.ui.theme.seededColorScheme
import com.anisync.android.worker.RecapNotifier
import com.materialkolor.PaletteStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** How many rate limit windows a story waits out before it gives up. */
private const val MAX_RATE_LIMIT_WAITS = 3

/** The candidates the song is picked from, the top five. */
private const val SONG_CANDIDATES = 5

@HiltViewModel
class RecapStoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recapRepository: RecapRepository,
    private val themesRepository: MediaThemesRepository,
    private val previewStore: RecapPreviewStore,
    private val accountStore: AccountStore,
    private val appSettings: AppSettings,
    private val cardFactory: RecapCardFactory,
    private val statsLauncher: ProfileStatsLauncher,
    private val notifier: RecapNotifier,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val route: RecapStory = savedStateHandle.toRoute()
    private val period: RecapPeriod? = RecapPeriod.fromKey(route.periodKey)

    private val _uiState = MutableStateFlow(RecapStoryUiState())
    val uiState: StateFlow<RecapStoryUiState> = _uiState.asStateFlow()

    private var player: ThemeSongPlayer? = null
    private var visible = true

    init {
        load()
    }

    fun onAction(action: RecapStoryAction) {
        when (action) {
            RecapStoryAction.Next -> movePage(_uiState.value.page + 1)
            RecapStoryAction.Previous -> movePage(_uiState.value.page - 1)
            is RecapStoryAction.GoTo -> movePage(action.page)
            RecapStoryAction.TogglePause -> {
                _uiState.update { it.copy(paused = !it.paused) }
                syncPlayback()
            }
            is RecapStoryAction.Hold -> {
                _uiState.update { it.copy(held = action.held) }
                syncPlayback()
            }
            RecapStoryAction.ToggleMute -> {
                val song = _uiState.value.song ?: return
                player?.setMuted(!song.muted)
                _uiState.update { it.copy(song = song.copy(muted = !song.muted)) }
            }
            RecapStoryAction.Replay -> {
                player?.restart()
                _uiState.update { it.copy(page = 0, paused = false, held = false, replays = it.replays + 1) }
                syncPlayback()
            }
            RecapStoryAction.Retry -> load()
            RecapStoryAction.SeeStatistics -> statsLauncher.open()
            is RecapStoryAction.Visible -> {
                visible = action.visible
                syncPlayback()
            }
        }
    }

    private fun movePage(page: Int) {
        val pages = _uiState.value.story?.pages ?: return
        _uiState.update { it.copy(page = page.coerceIn(0, pages.lastIndex), held = false) }
    }

    private fun syncPlayback() {
        val player = player ?: return
        if (visible && !_uiState.value.isStopped) player.play() else player.pause()
    }

    private fun load() {
        val period = period ?: run {
            _uiState.update { it.copy(loading = false, empty = true) }
            return
        }
        _uiState.update { it.copy(loading = true, failed = false, empty = false) }
        viewModelScope.launch {
            val loaded = if (route.preview) preview(period) else own(period) ?: return@launch
            val stored = loaded.recap
            if (stored == null) {
                _uiState.update { it.copy(loading = false, empty = true) }
                return@launch
            }
            val story = withContext(Dispatchers.Default) { story(stored, loaded) }
            _uiState.update { it.copy(loading = false, story = story, page = 0) }
            if (!route.preview) {
                recapRepository.markSeen(period)
                accountStore.activeAccount.value?.id?.let { notifier.dismiss(it, period) }
            }
            startSong(story, stored)
        }
    }

    private class Loaded(
        val recap: StoredRecap?,
        val previous: StoredRecap?,
        val userName: String,
        val avatarUrl: String?,
    )

    private fun preview(period: RecapPeriod): Loaded {
        val preview = previewStore.preview?.takeIf { it.recap.period == period }
        return Loaded(preview?.recap, preview?.previous, preview?.userName.orEmpty(), preview?.avatarUrl)
    }

    /** The active account's recap. A rate limit keeps the story loading and tries again once it ends. */
    private suspend fun own(period: RecapPeriod): Loaded? {
        var waits = 0
        while (true) {
            when (val result = recapRepository.getRecap(period)) {
                is Result.Success -> {
                    val account = accountStore.activeAccount.value
                    val previous = (period as? RecapPeriod.Month)?.let { recapRepository.storedRecap(it.previous) }
                    return Loaded(result.data, previous, account?.name.orEmpty(), account?.avatarUrl)
                }
                is Result.Error -> {
                    val countdown = result.countdownSeconds
                    if (countdown != null && waits < MAX_RATE_LIMIT_WAITS) {
                        waits++
                        delay((countdown + 1) * 1000L)
                    } else {
                        _uiState.update { it.copy(loading = false, failed = true) }
                        return null
                    }
                }
            }
        }
    }

    /** The story's model, with every page's scheme built here so no page waits on one. */
    private fun story(stored: StoredRecap, loaded: Loaded): StoryModel {
        val recap = stored.recap
        val titles = cardFactory.titles(stored)
        val pages = recapPages(recap)
        val topColor = recap.ranked.firstOrNull()?.let { titles[it.mediaId]?.color }
        val seeds = pages.map { page ->
            when (val seed = page.seed) {
                is PageSeed.Title -> recap.ranked.getOrNull(seed.rank)?.let { titles[it.mediaId]?.color } ?: topColor
                PageSeed.TopGenre -> recap.genres.firstOrNull()?.let { genreSeed(it.name) } ?: topColor
                PageSeed.App -> null
            }
        }
        seeds.filterNotNull().distinct().forEach { prepareScheme(it) }
        return StoryModel(
            period = stored.period,
            recap = recap,
            userName = loaded.userName,
            avatarUrl = loaded.avatarUrl,
            titles = titles,
            pages = pages,
            seeds = seeds,
            previousEpisodes = loaded.previous?.recap?.episodes?.takeIf { it > 0 },
            rankNote = rankNote(recap, stored.media),
            card = cardFactory.card(stored, loaded.userName, titles),
            coverColor = cardFactory.coverColorHex(stored),
            isPreview = route.preview,
        )
    }

    /**
     * Plays the song the period picks, when songs are on and the connection allows one. Stored
     * themes are reused, the rest of the top five go out in one AnimeThemes request.
     */
    private suspend fun startSong(story: StoryModel, stored: StoredRecap) {
        if (!appSettings.recapThemeSong.value) return
        if (ThemeSongPlayer.onMeteredNetwork(context) && !appSettings.recapSongsOnMobileData.value) return
        val candidates = stored.recap.ranked.take(SONG_CANDIDATES).map { ranked ->
            ThemeSongCandidate(ranked.mediaId, stored.media[ranked.mediaId]?.isFilm == true, ranked.watched)
        }
        if (candidates.isEmpty()) return
        val themes = themesRepository.themesFor(candidates.map { it.mediaId })
        val choice = withContext(Dispatchers.Default) { pickThemeSong(candidates) { themes[it] } } ?: return
        val muted = ThemeSongPlayer.otherAudioPlaying(context)
        val rank = candidates.indexOfFirst { it.mediaId == choice.mediaId }
        player = ThemeSongPlayer(context, choice.audio.url, muted)
        _uiState.update {
            it.copy(
                song = StorySong(
                    title = choice.theme.songTitle ?: choice.theme.slug,
                    artist = choice.theme.artists.firstOrNull(),
                    type = choice.theme.type,
                    sourceRank = rank,
                    sourceTitle = story.title(choice.mediaId)?.title.orEmpty(),
                    muted = muted,
                )
            )
        }
        syncPlayback()
    }

    private fun prepareScheme(seed: Color) {
        seededColorScheme(seed, darkTheme = true, amoled = false, paletteStyle = PaletteStyle.TonalSpot)
    }

    override fun onCleared() {
        player?.release()
        player = null
    }
}
