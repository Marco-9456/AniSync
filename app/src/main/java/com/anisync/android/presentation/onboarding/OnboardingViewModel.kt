package com.anisync.android.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anisync.android.data.AppSettings
import com.anisync.android.data.CoverQuality
import com.anisync.android.data.StartScreen
import com.anisync.android.data.ThemeMode
import com.anisync.android.data.TitleLanguage
import com.anisync.android.data.account.AccountManager
import com.anisync.android.domain.DiscoverRepository
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryRepository
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.domain.ProfileRepository
import com.anisync.android.domain.getOrNull
import com.anisync.android.type.MediaType
import com.anisync.android.util.AppLinksUtil
import com.anisync.android.util.BackgroundWorkUtil
import com.anisync.android.util.NotificationPermissionHelper
import com.anisync.android.widget.core.WidgetPin
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the first-run flow. Steps advance in one direction only, and the two that mutate anything
 * — the account import and the personalise choices — write through to the same repositories and
 * [AppSettings] the rest of the app reads, so nothing here is a parallel copy of app state.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
    private val accountManager: AccountManager,
    private val libraryRepository: LibraryRepository,
    private val discoverRepository: DiscoverRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            isLoggedIn = accountManager.activeAccount.value != null,
            username = accountManager.activeAccount.value?.name.orEmpty(),
            avatarUrl = accountManager.activeAccount.value?.avatarUrl,
            personalise = readPersonalise(),
            widgetPinSupported = WidgetPin.isSupported(context)
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null

    /**
     * Whether an account already existed when the flow opened. A developer replay starts signed in,
     * and must not skip the welcome step the way a fresh sign-in does.
     */
    private var wasLoggedIn = accountManager.activeAccount.value != null

    init {
        appSettings.markOnboardingStarted()
        observeReplayRequests()
        observeAccount()
        observeSettings()
        loadHeroCovers()
        refreshPermissions()
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.ContinueWithAniList ->
                if (_uiState.value.isLoggedIn) advance() else _uiState.update { copy(showSignInSheet = true) }

            OnboardingAction.DismissSignInSheet -> _uiState.update { copy(showSignInSheet = false) }
            OnboardingAction.Next -> advance()
            OnboardingAction.Back -> back()
            OnboardingAction.Skip -> advance()
            OnboardingAction.Finish -> appSettings.completeOnboarding()
            OnboardingAction.RefreshPermissions -> refreshPermissions()

            // The OS permission and AniSync's own master switch are separate gates; granting one
            // during onboarding without the other would leave the poll scheduled but silent.
            OnboardingAction.EnableNotifications -> {
                appSettings.setNotificationsEnabled(true)
                refreshPermissions()
            }

            is OnboardingAction.SetPalette -> appSettings.setSelectedPalette(action.paletteId)
            is OnboardingAction.SetThemeMode -> appSettings.setThemeMode(action.mode)
            is OnboardingAction.SetTitleLanguage -> appSettings.setTitleLanguage(action.language)
            is OnboardingAction.SetStartScreen -> appSettings.setStartScreen(action.screen)
        }
    }

    // =========================================================================
    // STEPS
    // =========================================================================

    private fun advance() {
        val next = when (_uiState.value.step) {
            OnboardingStep.WELCOME -> OnboardingStep.SYNCING
            OnboardingStep.SYNCING -> OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> OnboardingStep.PERSONALISE
            OnboardingStep.PERSONALISE -> OnboardingStep.DONE
            OnboardingStep.DONE -> OnboardingStep.DONE
        }
        _uiState.update { copy(step = next, showSignInSheet = false) }
        if (next == OnboardingStep.SYNCING) startSync()
        if (next == OnboardingStep.PERMISSIONS) refreshPermissions()
    }

    /**
     * A replay reuses this ViewModel: it is scoped to the activity, and the flow leaving composition
     * does not clear that store. So the flag turning true is what rewinds the steps — without this a
     * second run would resume on whatever screen the first one ended on.
     */
    private fun observeReplayRequests() {
        viewModelScope.launch {
            appSettings.onboardingReplay.collect { replaying ->
                if (!replaying) return@collect
                syncJob?.cancel()
                wasLoggedIn = accountManager.activeAccount.value != null
                _uiState.update {
                    copy(
                        step = OnboardingStep.WELCOME,
                        showSignInSheet = false,
                        sync = SyncProgress()
                    )
                }
            }
        }
    }

    private fun back() {
        val previous = _uiState.value.step.previous ?: return
        _uiState.update { copy(step = previous, showSignInSheet = false) }
    }

    // =========================================================================
    // ACCOUNT IMPORT
    // =========================================================================

    /**
     * Walks the import checklist in the order the step lists it. The pacing delays exist so a fast
     * import still reads as four discrete things happening rather than one instant jump.
     */
    private fun startSync() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            val account = accountManager.activeAccount.value ?: return@launch
            _uiState.update {
                copy(username = account.name, avatarUrl = account.avatarUrl)
            }

            _uiState.update { copy(sync = sync.copy(library = TaskState.Running)) }
            profileRepository.refreshProfile(account.name)
            profileRepository.observeProfile().first()?.let { profile ->
                _uiState.update { copy(bannerUrl = profile.bannerUrl, avatarUrl = profile.avatarUrl) }
            }

            libraryRepository.refreshLibrary(account.name, MediaType.ANIME)
            libraryRepository.refreshLibrary(account.name, MediaType.MANGA)

            val anime = libraryRepository.observeLibrary(account.name, MediaType.ANIME).first()
            val manga = libraryRepository.observeLibrary(account.name, MediaType.MANGA).first()
            _uiState.update {
                copy(
                    sync = sync.copy(library = TaskState.Done, libraryEntries = anime.size + manga.size),
                    previewEntry = pickPreview(anime) ?: previewEntry
                )
            }

            _uiState.update { copy(sync = sync.copy(airing = TaskState.Running)) }
            delay(STEP_PACING_MS)
            _uiState.update {
                copy(sync = sync.copy(airing = TaskState.Done, airingThisWeek = countAiringThisWeek(anime)))
            }

            _uiState.update { copy(sync = sync.copy(notifications = TaskState.Running)) }
            delay(STEP_PACING_MS)
            _uiState.update {
                copy(
                    sync = sync.copy(
                        notifications = TaskState.Done,
                        notificationsOn = NotificationPermissionHelper.hasNotificationPermission(context) &&
                            appSettings.notificationsEnabled.value
                    )
                )
            }

            _uiState.update { copy(sync = sync.copy(widgets = TaskState.Running)) }
            delay(STEP_PACING_MS)
            _uiState.update {
                copy(sync = sync.copy(widgets = TaskState.Done, widgetsPlaced = WidgetPin.placedCount(context)))
            }
        }
    }

    /** The card under the personalise choices: the viewer's own next episode, when they have one. */
    private fun pickPreview(anime: List<LibraryEntry>): LibraryEntry? =
        anime.firstOrNull { it.status == LibraryStatus.CURRENT && it.nextAiringEpisode != null }
            ?: anime.firstOrNull { it.status == LibraryStatus.CURRENT }
            ?: anime.firstOrNull()

    private fun countAiringThisWeek(anime: List<LibraryEntry>): Int =
        anime.count { entry ->
            val seconds = entry.dynamicTimeUntilAiring ?: return@count false
            seconds <= WEEK_SECONDS
        }

    // =========================================================================
    // OBSERVERS
    // =========================================================================

    private fun observeAccount() {
        viewModelScope.launch {
            accountManager.activeAccount.collect { account ->
                val loggedIn = account != null
                _uiState.update {
                    copy(
                        isLoggedIn = loggedIn,
                        username = account?.name ?: username,
                        avatarUrl = account?.avatarUrl ?: avatarUrl
                    )
                }
                // A sign-in that lands mid-flow carries the welcome step forward on its own; a
                // replay that merely started signed in must not.
                if (loggedIn && !wasLoggedIn && _uiState.value.step == OnboardingStep.WELCOME) {
                    advance()
                }
                wasLoggedIn = loggedIn
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            appSettings.selectedPaletteId.collect { id ->
                _uiState.update { copy(personalise = personalise.copy(paletteId = id)) }
            }
        }
        viewModelScope.launch {
            appSettings.themeMode.collect { mode ->
                _uiState.update { copy(personalise = personalise.copy(themeMode = mode)) }
            }
        }
        viewModelScope.launch {
            appSettings.titleLanguage.collect { language ->
                _uiState.update { copy(personalise = personalise.copy(titleLanguage = language)) }
            }
        }
        viewModelScope.launch {
            appSettings.startScreen.collect { screen ->
                _uiState.update { copy(personalise = personalise.copy(startScreen = screen)) }
            }
        }
        // AniSync's own notification switch is half of what the alerts row reports, so the row has
        // to move when it does — granting the OS permission alone leaves the app silent.
        viewModelScope.launch {
            appSettings.notificationsEnabled.collect { refreshPermissions() }
        }
    }

    private fun readPersonalise() = PersonaliseState(
        paletteId = appSettings.selectedPaletteId.value,
        themeMode = appSettings.themeMode.value,
        titleLanguage = appSettings.titleLanguage.value,
        startScreen = appSettings.startScreen.value
    )

    /**
     * Trending covers for the welcome marquee, and the fallback preview card for anyone whose list
     * is empty. Unauthenticated, so it runs before the sign-in handoff.
     */
    private fun loadHeroCovers() {
        viewModelScope.launch {
            val trending = discoverRepository.getTrending(MediaType.ANIME).getOrNull().orEmpty()
            if (trending.isEmpty()) return@launch
            _uiState.update {
                copy(
                    // Deliberately not the viewer's CoverQuality preference: the marquee blows
                    // each cover up well past card size, where anything below extraLarge is visibly
                    // soft.
                    heroCovers = trending.mapNotNull { entry ->
                        entry.cover?.preferred(CoverQuality.EXTRA_LARGE) ?: entry.coverUrl
                    },
                    previewEntry = previewEntry ?: trending.firstOrNull { it.nextAiringEpisode != null }
                    ?: trending.firstOrNull()
                )
            }
        }
    }

    /** Re-reads every system toggle. Called on each resume, since all four are granted off-screen. */
    fun refreshPermissions() {
        _uiState.update {
            copy(
                permissions = PermissionStates(
                    notifications = NotificationPermissionHelper.hasNotificationPermission(context) &&
                        appSettings.notificationsEnabled.value,
                    batteryExempt = BackgroundWorkUtil.isIgnoringBatteryOptimizations(context),
                    linksVerified = AppLinksUtil.opensLinksFor(context),
                    hibernationExempt = BackgroundWorkUtil.isHibernationExempt(context)
                )
            )
        }
    }

    private inline fun MutableStateFlow<OnboardingUiState>.update(
        block: OnboardingUiState.() -> OnboardingUiState
    ) {
        value = value.block()
    }

    private companion object {
        const val STEP_PACING_MS = 420L
        const val WEEK_SECONDS = 7 * 24 * 60 * 60
    }
}
