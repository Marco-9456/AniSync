package com.anisync.android.presentation.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.StartScreen
import com.anisync.android.data.ThemeMode
import com.anisync.android.data.TitleLanguage
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.presentation.components.WatchingCardConfig
import com.anisync.android.presentation.library.components.LibraryListCard
import com.anisync.android.presentation.onboarding.PersonaliseState
import com.anisync.android.presentation.settings.components.ColorSchemeSelector
import com.anisync.android.type.MediaType
import com.anisync.android.ui.theme.PresetPalettes
import com.materialkolor.PaletteStyle

/** Widest the step runs: enough for the choices and the live preview side by side. */
private val StepMaxWidth = 980.dp

/**
 * Step 2 of 2: the four choices worth making before the first screen loads. Each writes straight
 * through to the same preference Look and Feel edits, and the card underneath is a live library
 * card — the real component, not a mock-up — so the accent is shown doing its actual job.
 *
 * On a wide window the preview moves alongside the choices instead of below them, so changing an
 * accent shows its effect without scrolling.
 */
@Composable
fun PersonaliseStep(
    personalise: PersonaliseState,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    previewEntry: LibraryEntry?,
    onPaletteSelected: (String) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onTitleLanguageSelected: (TitleLanguage) -> Unit,
    onStartScreenSelected: (StartScreen) -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val wide = isWideOnboarding()
    val margin = onboardingMargin()

    Column(modifier = modifier.fillMaxSize()) {
        OnboardingStepHeader(
            step = 2,
            total = 2,
            onSkip = onSkip,
            onBack = onBack,
            modifier = if (wide) {
                Modifier
                    .padding(horizontal = OnboardingWidePadding)
                    .padding(top = 12.dp)
            } else {
                Modifier
                    .onboardingColumn(StepMaxWidth)
                    .padding(horizontal = margin)
                    .padding(top = 12.dp)
            }
        )

        if (wide) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = OnboardingWidePadding)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(OnboardingPaneGap)
            ) {
                OnboardingCentredScroll(
                    modifier = Modifier
                        .weight(OnboardingContextPaneWeight)
                        .fillMaxHeight()
                ) {
                    OnboardingHeadline(stringResource(R.string.onboarding_personalise_headline))
                    Spacer(modifier = Modifier.height(12.dp))
                    OnboardingBody(stringResource(R.string.onboarding_personalise_body))
                    Spacer(modifier = Modifier.height(28.dp))
                    OnboardingNote(text = stringResource(R.string.onboarding_personalise_note))
                    Spacer(modifier = Modifier.height(16.dp))
                    OnboardingPrimaryButton(
                        text = stringResource(R.string.onboarding_continue),
                        onClick = onContinue
                    )
                }

                OnboardingCentredScroll(
                    modifier = Modifier
                        .weight(OnboardingContentPaneWeight)
                        .fillMaxHeight()
                ) {
                    // The controls carry a 16.dp inset of their own, so the pane adds nothing more
                    // and the labels line up with the pills beneath them.
                    Choices(
                        personalise = personalise,
                        isDarkMode = isDarkMode,
                        paletteStyle = paletteStyle,
                        labelPadding = 16.dp,
                        controlPadding = 0.dp,
                        onPaletteSelected = onPaletteSelected,
                        onThemeModeSelected = onThemeModeSelected,
                        onTitleLanguageSelected = onTitleLanguageSelected,
                        onStartScreenSelected = onStartScreenSelected
                    )

                    if (previewEntry != null) {
                        Spacer(modifier = Modifier.height(26.dp))
                        LibraryListCard(
                            entry = previewEntry,
                            mediaType = MediaType.ANIME,
                            titleLanguage = personalise.titleLanguage,
                            onClick = {},
                            config = WatchingCardConfig,
                            onIncrement = {},
                            onDecrement = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .onboardingColumn(StepMaxWidth)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            OnboardingHeadline(
                text = stringResource(R.string.onboarding_personalise_headline),
                modifier = Modifier.padding(horizontal = margin)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OnboardingBody(
                text = stringResource(R.string.onboarding_personalise_body),
                modifier = Modifier.padding(horizontal = margin)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Choices(
                personalise = personalise,
                isDarkMode = isDarkMode,
                paletteStyle = paletteStyle,
                labelPadding = OnboardingMargin,
                controlPadding = 4.dp,
                onPaletteSelected = onPaletteSelected,
                onThemeModeSelected = onThemeModeSelected,
                onTitleLanguageSelected = onTitleLanguageSelected,
                onStartScreenSelected = onStartScreenSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (previewEntry != null) {
                LibraryListCard(
                    entry = previewEntry,
                    mediaType = MediaType.ANIME,
                    titleLanguage = personalise.titleLanguage,
                    onClick = {},
                    config = WatchingCardConfig,
                    onIncrement = {},
                    onDecrement = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OnboardingMargin)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        OnboardingNote(
            text = stringResource(R.string.onboarding_personalise_note),
            modifier = Modifier
                .onboardingColumn(StepMaxWidth)
                .padding(horizontal = margin)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingPrimaryButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            modifier = Modifier
                .onboardingColumn()
                .padding(horizontal = margin)
                .padding(bottom = 24.dp)
        )
    }
}

/**
 * The four labelled choices. [labelPadding] positions the section labels; [controlPadding] tops up
 * the 16.dp inset the selector and the segmented groups already apply themselves, so both land on
 * the same left edge whichever layout is hosting them.
 */
@Composable
private fun ColumnScope.Choices(
    personalise: PersonaliseState,
    isDarkMode: Boolean,
    paletteStyle: PaletteStyle,
    labelPadding: Dp,
    controlPadding: Dp,
    onPaletteSelected: (String) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onTitleLanguageSelected: (TitleLanguage) -> Unit,
    onStartScreenSelected: (StartScreen) -> Unit
) {
    OnboardingSectionLabel(
        text = stringResource(R.string.onboarding_personalise_accent),
        modifier = Modifier.padding(horizontal = labelPadding)
    )
    Spacer(modifier = Modifier.height(10.dp))
    ColorSchemeSelector(
        palettes = PresetPalettes.all,
        selectedPaletteId = personalise.paletteId,
        isDarkMode = isDarkMode,
        paletteStyle = paletteStyle,
        onPaletteSelected = { onPaletteSelected(it.id) },
        modifier = Modifier.padding(horizontal = controlPadding)
    )

    Spacer(modifier = Modifier.height(22.dp))

    OnboardingSectionLabel(
        text = stringResource(R.string.onboarding_personalise_theme),
        modifier = Modifier.padding(horizontal = labelPadding)
    )
    Spacer(modifier = Modifier.height(10.dp))
    SegmentedTabGroup(
        options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK),
        selected = personalise.themeMode,
        onSelect = onThemeModeSelected,
        label = { mode ->
            stringResource(
                when (mode) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                }
            )
        },
        fillEqually = true,
        modifier = Modifier.padding(horizontal = labelPadding)
    )

    Spacer(modifier = Modifier.height(22.dp))

    OnboardingSectionLabel(
        text = stringResource(R.string.onboarding_personalise_titles),
        modifier = Modifier.padding(horizontal = labelPadding)
    )
    Spacer(modifier = Modifier.height(10.dp))
    SegmentedTabGroup(
        options = listOf(TitleLanguage.ROMAJI, TitleLanguage.ENGLISH, TitleLanguage.NATIVE),
        selected = personalise.titleLanguage,
        onSelect = onTitleLanguageSelected,
        label = { language ->
            stringResource(
                when (language) {
                    TitleLanguage.ROMAJI -> R.string.title_language_romaji
                    TitleLanguage.ENGLISH -> R.string.title_language_english
                    TitleLanguage.NATIVE -> R.string.title_language_native
                }
            )
        },
        icon = { if (it == personalise.titleLanguage) Icons.Filled.Check else null },
        modifier = Modifier.padding(horizontal = controlPadding)
    )

    Spacer(modifier = Modifier.height(22.dp))

    OnboardingSectionLabel(
        text = stringResource(R.string.onboarding_personalise_open_on),
        modifier = Modifier.padding(horizontal = labelPadding)
    )
    Spacer(modifier = Modifier.height(10.dp))
    // "Last visited" leads because it is the app's own default; picking any of the others pins cold
    // launches to that tab, exactly as Look and Feel ▸ Open on does.
    SegmentedTabGroup(
        options = StartScreen.entries.toList(),
        selected = personalise.startScreen,
        onSelect = onStartScreenSelected,
        label = { screen ->
            stringResource(
                when (screen) {
                    StartScreen.LAST_VISITED -> R.string.start_screen_last_visited
                    StartScreen.LIBRARY -> R.string.nav_library
                    StartScreen.DISCOVER -> R.string.nav_discover
                    StartScreen.FEED -> R.string.nav_feed
                    StartScreen.FORUM -> R.string.nav_forum
                }
            )
        },
        icon = { if (it == personalise.startScreen) Icons.Filled.Check else null },
        modifier = Modifier.padding(horizontal = controlPadding)
    )
}
