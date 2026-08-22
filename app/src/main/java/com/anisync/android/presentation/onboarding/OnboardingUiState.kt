package com.anisync.android.presentation.onboarding

import androidx.compose.runtime.Immutable
import com.anisync.android.data.StartScreen
import com.anisync.android.data.ThemeMode
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry

/**
 * The first-run flow, in order. [WELCOME] is the only step reachable while signed out; everything
 * after it needs the token the AniList handoff returns.
 */
enum class OnboardingStep {
    WELCOME,
    SYNCING,
    PERMISSIONS,
    PERSONALISE,
    DONE;

    /**
     * The step a back press returns to, or null where there is nothing to go back to. Welcome has
     * nothing behind it, and syncing cannot un-attach the account it just imported.
     */
    val previous: OnboardingStep?
        get() = when (this) {
            WELCOME, SYNCING -> null
            PERMISSIONS -> SYNCING
            PERSONALISE -> PERMISSIONS
            DONE -> PERSONALISE
        }

    /** Position within the two numbered "STEP n OF 2" screens, or 0 for the unnumbered ones. */
    val numberedIndex: Int
        get() = when (this) {
            PERMISSIONS -> 1
            PERSONALISE -> 2
            else -> 0
        }
}

/** One row of the account-import checklist on the syncing step. */
enum class TaskState { Pending, Running, Done }

@Immutable
data class SyncProgress(
    val library: TaskState = TaskState.Pending,
    val libraryEntries: Int = 0,
    val airing: TaskState = TaskState.Pending,
    val airingThisWeek: Int = 0,
    val notifications: TaskState = TaskState.Pending,
    val notificationsOn: Boolean = false,
    val widgets: TaskState = TaskState.Pending,
    val widgetsPlaced: Int = 0
) {
    private val states get() = listOf(library, airing, notifications, widgets)

    val fraction: Float get() = states.count { it == TaskState.Done } / states.size.toFloat()

    val isComplete: Boolean get() = states.all { it == TaskState.Done }
}

/**
 * The four background-work escapes the set-up step offers. Every one is a system decision AniSync
 * can only ask for, so the row reflects what the OS reports rather than what the user tapped.
 */
@Immutable
data class PermissionStates(
    val notifications: Boolean = false,
    val batteryExempt: Boolean = false,
    val linksVerified: Boolean = false,
    val hibernationExempt: Boolean = false
) {
    val grantedCount: Int
        get() = listOf(notifications, batteryExempt, linksVerified, hibernationExempt).count { it }

    val total: Int get() = 4
}

@Immutable
data class PersonaliseState(
    val paletteId: String = "dynamic",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val titleLanguage: TitleLanguage = TitleLanguage.ROMAJI,
    val startScreen: StartScreen = StartScreen.LAST_VISITED
)

@Immutable
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val showSignInSheet: Boolean = false,
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    /** Cover art for the welcome marquee. Empty until trending resolves, or on a cold offline start. */
    val heroCovers: List<String> = emptyList(),
    val sync: SyncProgress = SyncProgress(),
    val permissions: PermissionStates = PermissionStates(),
    val personalise: PersonaliseState = PersonaliseState(),
    /** The card shown under the personalise choices — the viewer's own next episode when there is one. */
    val previewEntry: LibraryEntry? = null,
    val widgetPinSupported: Boolean = true
)

sealed interface OnboardingAction {
    data object ContinueWithAniList : OnboardingAction
    data object DismissSignInSheet : OnboardingAction
    data object Next : OnboardingAction
    data object Back : OnboardingAction
    data object Skip : OnboardingAction
    data object Finish : OnboardingAction
    data object RefreshPermissions : OnboardingAction
    data object EnableNotifications : OnboardingAction
    data class SetPalette(val paletteId: String) : OnboardingAction
    data class SetThemeMode(val mode: ThemeMode) : OnboardingAction
    data class SetTitleLanguage(val language: TitleLanguage) : OnboardingAction
    data class SetStartScreen(val screen: StartScreen) : OnboardingAction
}
