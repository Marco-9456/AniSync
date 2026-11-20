package com.anisync.android.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.domain.AiringEpisode
import com.anisync.android.domain.CalendarRepository
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.DetailsRepository
import com.anisync.android.domain.DiscoverSection
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.MediaReview
import com.anisync.android.domain.Result
import com.anisync.android.type.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import com.anisync.android.domain.MainTab
import com.anisync.android.domain.observeTab

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val calendarRepository: CalendarRepository,
    private val detailsRepository: DetailsRepository,
    private val appSettings: com.anisync.android.data.AppSettings,
    private val tabReselectBus: com.anisync.android.domain.TabReselectBus
) : ViewModel() {

    val titleLanguage = appSettings.titleLanguage

    private val _uiState = MutableStateFlow(
        DiscoverUiState(mediaType = appSettings.discoverMediaType.value)
    )
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        observeSectionOrder()
        loadDiscoveryData()
        observeTabReselect()
    }

    /** The bottom bar's reselect gestures: scroll back to the top, or open the search overlay. */
    private fun observeTabReselect() {
        tabReselectBus.observeTab(
            tab = MainTab.DISCOVER,
            scope = viewModelScope,
            onScrollToTop = { _uiState.update { s -> s.copy(scrollToTopRequest = s.scrollToTopRequest + 1) } },
            onSearch = { _uiState.update { s -> s.copy(searchOverlayRequest = s.searchOverlayRequest + 1) } }
        )
    }

    fun onAction(action: DiscoverAction) {
        when (action) {
            is DiscoverAction.OnMediaTypeChange -> onMediaTypeChange(action.type)
            is DiscoverAction.Refresh -> refresh()
            is DiscoverAction.ReorderSections -> reorderSections(action.order)
            is DiscoverAction.SetSectionVisible -> setSectionVisible(action.section, action.visible)
            is DiscoverAction.ResetSectionOrder -> resetSectionOrder()
            is DiscoverAction.AddToPlanning -> addToPlanning(action.mediaId)
            is DiscoverAction.RetrySection -> retrySection(action.section)
        }
    }

    private fun onMediaTypeChange(type: MediaType) {
        val currentState = _uiState.value
        if (currentState.mediaType == type) return
        appSettings.setDiscoverMediaType(type)
        // The tabs do not share a rail set, so the order and the feeds both start over.
        _uiState.update {
            currentState.copy(
                mediaType = type,
                feeds = DiscoverFeeds(),
                sectionOrder = visibleSections(type),
                hiddenSections = hiddenSections(type)
            )
        }

        loadDiscoveryData()
    }

    private fun refresh() {
        loadDiscoveryData(isRefresh = true)
    }

    /**
     * Fans every rail out and lets each land on its own.
     *
     * The old version awaited all five media requests and, if any one of them failed, replaced the
     * whole screen with a single error. Each rail now writes only its own slice of the state, so a
     * failed request costs that rail and nothing else. Only rails the current tab actually shows
     * are requested: the airing timeline is anime only and Releasing now is manga only.
     */
    private fun loadDiscoveryData(isRefresh: Boolean = false) {
        val mediaType = _uiState.value.mediaType
        val supported = DiscoverSection.defaultOrder(mediaType).toSet()

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.update { state ->
                state.copy(
                    isRefreshing = isRefresh,
                    feeds = state.feeds.let { f ->
                        f.copy(
                            trending = f.trending.copy(isLoading = true, error = null),
                            airingToday = f.airingToday.copy(isLoading = true, error = null),
                            releasing = f.releasing.copy(isLoading = true, error = null),
                            popular = f.popular.copy(isLoading = true, error = null),
                            notYetReleased = f.notYetReleased.copy(isLoading = true, error = null),
                            newlyAdded = f.newlyAdded.copy(isLoading = true, error = null),
                            reviews = f.reviews.copy(isLoading = true, error = null)
                        )
                    }
                )
            }

            val jobs = buildList {
                add(launch {
                    loadEntries(mediaType, { discoverRepository.getTrending(it) }) { f, v -> f.copy(trending = v) }
                })
                add(launch {
                    loadEntries(mediaType, { discoverRepository.getPopular(it) }) { f, v -> f.copy(popular = v) }
                })
                add(launch {
                    loadEntries(mediaType, { discoverRepository.getNotYetReleased(it) }) { f, v -> f.copy(notYetReleased = v) }
                })
                add(launch {
                    loadEntries(mediaType, { discoverRepository.getNewlyAdded(it) }) { f, v -> f.copy(newlyAdded = v) }
                })
                add(launch { loadReviews(mediaType) })
                if (DiscoverSection.AIRING_TODAY in supported) {
                    add(launch { loadAiringToday(mediaType) })
                }
                if (DiscoverSection.RELEASING_NOW in supported) {
                    add(launch {
                        loadEntries(mediaType, { discoverRepository.getReleasing(it) }) { f, v -> f.copy(releasing = v) }
                    })
                }
            }
            jobs.forEach { it.join() }

            if (isRefresh) {
                // The pull-to-refresh spinner reads as a glitch if it vanishes instantly.
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < MIN_REFRESH_SPINNER_MS) delay(MIN_REFRESH_SPINNER_MS - elapsed)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** Run one media rail's request and fold the outcome into its own slot. */
    private suspend fun loadEntries(
        mediaType: MediaType,
        request: suspend (MediaType) -> Result<List<LibraryEntry>>,
        assign: (DiscoverFeeds, SectionFeed<LibraryEntry>) -> DiscoverFeeds
    ) {
        val feed = when (val result = request(mediaType)) {
            is Result.Success -> SectionFeed(items = result.data, isLoading = false)
            is Result.Error -> SectionFeed<LibraryEntry>(isLoading = false, error = result.message)
        }
        // A tab switch mid-flight must not write the old tab's rail over the new one.
        _uiState.update {
            if (it.mediaType != mediaType) it else it.copy(feeds = assign(it.feeds, feed))
        }
    }

    private suspend fun loadReviews(mediaType: MediaType) {
        val result = discoverRepository.getRecentReviews(mediaType = mediaType, page = 1)
        val feed = when (result) {
            is Result.Success -> SectionFeed(items = result.data.reviews, isLoading = false)
            is Result.Error -> SectionFeed<MediaReview>(isLoading = false, error = result.message)
        }
        _uiState.update {
            if (it.mediaType != mediaType) it else it.copy(feeds = it.feeds.copy(reviews = feed))
        }
    }

    /**
     * Today's airing schedule, in the device's own day. `airingSchedules` is sorted by TIME, so the
     * list arrives in the order the timeline draws it. Manga has no counterpart, which is why this
     * only runs on the Anime tab.
     */
    private suspend fun loadAiringToday(mediaType: MediaType) {
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000
        val result = calendarRepository.getWeekSchedule(dayStart, dayStart + DAY_SECONDS)
        val feed = when (result) {
            is Result.Success -> SectionFeed(items = result.data, isLoading = false)
            is Result.Error -> SectionFeed<AiringEpisode>(isLoading = false, error = result.message)
        }
        _uiState.update {
            if (it.mediaType != mediaType) it else it.copy(feeds = it.feeds.copy(airingToday = feed))
        }
    }

    /**
     * Add the spotlight title to Planning.
     *
     * No result handling beyond the request itself: the mutation writes through to Room, and the
     * card's own list indicator is fed by [com.anisync.android.presentation.util.LocalLibraryStatuses],
     * so a success shows up as the status chip replacing the button.
     */
    private fun addToPlanning(mediaId: Int) {
        viewModelScope.launch {
            detailsRepository.updateMediaListEntry(mediaId, LibraryStatus.PLANNING)
        }
    }

    /**
     * Re-run one rail's request.
     *
     * Reloading the whole screen to recover a single failed section would throw away six
     * responses that already arrived, and spend six requests against the rate limit to do it.
     */
    private fun retrySection(section: DiscoverSection) {
        val mediaType = _uiState.value.mediaType
        if (!section.supports(mediaType)) return
        _uiState.update { it.copy(feeds = it.feeds.markLoading(section)) }
        viewModelScope.launch {
            when (section) {
                DiscoverSection.TRENDING ->
                    loadEntries(mediaType, { discoverRepository.getTrending(it) }) { f, v -> f.copy(trending = v) }
                DiscoverSection.POPULAR ->
                    loadEntries(mediaType, { discoverRepository.getPopular(it) }) { f, v -> f.copy(popular = v) }
                DiscoverSection.NOT_YET_RELEASED ->
                    loadEntries(mediaType, { discoverRepository.getNotYetReleased(it) }) { f, v -> f.copy(notYetReleased = v) }
                DiscoverSection.NEWLY_ADDED ->
                    loadEntries(mediaType, { discoverRepository.getNewlyAdded(it) }) { f, v -> f.copy(newlyAdded = v) }
                DiscoverSection.RELEASING_NOW ->
                    loadEntries(mediaType, { discoverRepository.getReleasing(it) }) { f, v -> f.copy(releasing = v) }
                DiscoverSection.AIRING_TODAY -> loadAiringToday(mediaType)
                DiscoverSection.REVIEWS -> loadReviews(mediaType)
            }
        }
    }

    /** Mirror the persisted rail order and hidden set into the state for the active tab. */
    private fun observeSectionOrder() {
        viewModelScope.launch {
            combine(
                appSettings.discoverAnimeSectionOrder,
                appSettings.discoverMangaSectionOrder,
                appSettings.hiddenDiscoverAnimeSections,
                appSettings.hiddenDiscoverMangaSections
            ) { _, _, _, _ -> Unit }.collect {
                _uiState.update { state ->
                    state.copy(
                        sectionOrder = visibleSections(state.mediaType),
                        hiddenSections = hiddenSections(state.mediaType)
                    )
                }
            }
        }
    }

    /** The stored order for [type], hidden rails removed. */
    private fun visibleSections(type: MediaType): List<DiscoverSection> {
        val hidden = hiddenSections(type)
        return orderedSections(type).filterNot { it in hidden }
    }

    /** The stored order for [type], hidden rails included, which is what the reorder sheet lists. */
    private fun orderedSections(type: MediaType): List<DiscoverSection> = DiscoverSection.resolveOrder(
        type,
        if (type == MediaType.ANIME) appSettings.discoverAnimeSectionOrder.value
        else appSettings.discoverMangaSectionOrder.value
    )

    private fun hiddenSections(type: MediaType): Set<DiscoverSection> =
        (
            if (type == MediaType.ANIME) appSettings.hiddenDiscoverAnimeSections.value
            else appSettings.hiddenDiscoverMangaSections.value
            )
            .mapNotNull(DiscoverSection::fromId)
            .toSet()

    /** Full order for the reorder sheet, hidden rails still in place. */
    fun sectionsForReorder(): List<DiscoverSection> = orderedSections(_uiState.value.mediaType)

    private fun reorderSections(order: List<DiscoverSection>) {
        appSettings.setDiscoverSectionOrder(_uiState.value.mediaType, order.map { it.id })
    }

    private fun setSectionVisible(section: DiscoverSection, visible: Boolean) {
        val type = _uiState.value.mediaType
        val hidden = hiddenSections(type)
        val next = if (visible) hidden - section else hidden + section
        appSettings.setHiddenDiscoverSections(type, next.map { it.id }.toSet())
    }

    private fun resetSectionOrder() {
        val type = _uiState.value.mediaType
        appSettings.setDiscoverSectionOrder(type, emptyList())
        appSettings.setHiddenDiscoverSections(type, emptySet())
    }
}

/** How long the refresh spinner stays up even when the network beats it. */
private const val MIN_REFRESH_SPINNER_MS = 800L

private const val DAY_SECONDS = 24L * 60 * 60
