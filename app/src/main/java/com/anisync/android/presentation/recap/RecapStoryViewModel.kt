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
import com.anisync.android.domain.recap.ThemeSongChoice
import com.anisync.android.domain.recap.pickThemeSong
import com.anisync.android.domain.recap.rankNote
import com.anisync.android.domain.recap.recapPages
import com.anisync.android.domain.recap.seed
import com.anisync.android.domain.recap.songCandidates
import com.anisync.android.presentation.navigation.RecapStory
import com.anisync.android.ui.theme.genreSeed
import com.anisync.android.ui.theme.resolveDarkTheme
import com.anisync.android.ui.theme.seededColorScheme
import com.anisync.android.worker.RecapNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    /** The player may be buffering before the story shows, it only plays once its song is on screen. */
    private fun syncPlayback() {
        val player = player ?: return
        val state = _uiState.value
        if (visible && state.song != null && !state.isStopped) player.play() else player.pause()
    }

    private fun load() {
        val period = period ?: run {
            _uiState.update { it.copy(loading = false, empty = true) }
            return
        }
        _uiState.update { it.copy(loading = true, failed = false, rateLimited = false, empty = false) }
        viewModelScope.launch {
            val loaded = if (route.preview) preview(period) else own(period) ?: return@launch
            val stored = loaded.recap
            if (stored == null) {
                _uiState.update { it.copy(loading = false, empty = true) }
                return@launch
            }
            val song = async { prepareSong(stored) }
            val story = withContext(Dispatchers.Default) { story(stored, loaded) }
            _uiState.update { it.copy(loading = false, story = story, page = 0) }
            if (!route.preview) {
                recapRepository.markSeen(period)
                accountStore.activeAccount.value?.id?.let { notifier.dismiss(it, period) }
            }
            song.await()?.let { showSong(story, it) }
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
                        _uiState.update { it.copy(loading = false, failed = true, rateLimited = countdown != null) }
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

    /** True when the story tries for a song: songs are on, the connection allows one and a title can lend it. */
    private fun songWanted(stored: StoredRecap): Boolean =
        appSettings.recapThemeSong.value &&
            (!ThemeSongPlayer.onMeteredNetwork(context) || appSettings.recapSongsOnMobileData.value) &&
            stored.recap.ranked.isNotEmpty()

    private class PreparedSong(val choice: ThemeSongChoice, val rank: Int, val muted: Boolean)

    /**
     * Picks the period's song and starts buffering it, while the pages are still being prepared.
     * Stored themes are reused, the rest of the top five go out in one AnimeThemes request.
     */
    private suspend fun prepareSong(stored: StoredRecap): PreparedSong? {
        if (!songWanted(stored)) return null
        val candidates = stored.songCandidates()
        val themes = themesRepository.themesFor(candidates.map { it.mediaId })
        val choice = withContext(Dispatchers.Default) { pickThemeSong(candidates) { themes[it] } } ?: return null
        val muted = ThemeSongPlayer.otherAudioPlaying(context)
        player = ThemeSongPlayer(context, choice.audio.url, muted)
        return PreparedSong(choice, candidates.indexOfFirst { it.mediaId == choice.mediaId }, muted)
    }

    private fun showSong(story: StoryModel, song: PreparedSong) {
        val theme = song.choice.theme
        _uiState.update {
            it.copy(
                song = StorySong(
                    title = theme.songTitle ?: theme.slug,
                    artist = theme.artists.firstOrNull(),
                    type = theme.type,
                    sourceRank = song.rank,
                    sourceTitle = story.title(song.choice.mediaId)?.title.orEmpty(),
                    muted = song.muted,
                )
            )
        }
        syncPlayback()
    }

    /** Built from the settings the pages read, so their first frame finds it cached. */
    private fun prepareScheme(seed: Color) {
        val dark = appSettings.themeMode.value.resolveDarkTheme(context.resources.configuration)
        seededColorScheme(seed, darkTheme = dark, amoled = appSettings.amoledEnabled.value, paletteStyle = appSettings.paletteStyle.value)
    }

    override fun onCleared() {
        player?.release()
        player = null
    }
}
