package com.anisync.android.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anisync.android.R
import com.anisync.android.presentation.navigation.DetailPanePlaceholder
import com.anisync.android.presentation.navigation.Login
import com.anisync.android.presentation.navigation.SettingsAbout
import com.anisync.android.presentation.navigation.SettingsAcknowledgments
import com.anisync.android.presentation.navigation.SettingsAniList
import com.anisync.android.presentation.navigation.SettingsDeveloperTools
import com.anisync.android.presentation.navigation.SettingsFontPlayground
import com.anisync.android.presentation.navigation.SettingsLanguage
import com.anisync.android.presentation.navigation.SettingsLinks
import com.anisync.android.presentation.navigation.SettingsLookAndFeel
import com.anisync.android.presentation.navigation.SettingsMediaUpload
import com.anisync.android.presentation.navigation.SettingsNotifications
import com.anisync.android.presentation.navigation.SettingsOpenSourceLicenses
import com.anisync.android.presentation.navigation.SettingsSponsors
import com.anisync.android.presentation.navigation.SettingsStorage
import com.anisync.android.presentation.navigation.SettingsTheme
import com.anisync.android.presentation.navigation.SettingsUpdates
import com.anisync.android.presentation.navigation.TwoPaneListDetailScaffold
import com.anisync.android.presentation.util.LocalAdaptiveInfo

/**
 * The Settings hub. Compact/medium widths show the plain [SettingsScreen] and push each chosen
 * category's subscreen full screen (unchanged behaviour). Expanded widths use the shared two-pane
 * [TwoPaneListDetailScaffold] — the category list as the permanent list pane, the chosen category's
 * subscreen in the on-demand resizable detail pane (closable ✕ / back).
 *
 * Settings is the textbook Material 3 list-detail / supporting-pane surface. A category has no detail
 * of its own beyond its existing subscreen, so the detail pane is a self-contained [NavHost] keyed by
 * a [SettingsCategory]; drilling deeper within a category (Look & Feel → Theme/Language, About →
 * licenses/acknowledgements/links, Developer Tools → font playground) stays in-pane, while logging out
 * escalates to the app [navController].
 *
 * Settings is a pushed route with no navigation rail, so the two-pane gutter is symmetric (not the
 * rail-flush default).
 */
@Composable
fun SettingsListDetail(
    navController: NavHostController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalAdaptiveInfo.current.supportsTwoPane) {
        SettingsScreen(
            onCategorySelected = { navController.navigate(it.toPaneRoute()) },
            onBackClick = onBackClick,
            modifier = modifier,
        )
        return
    }

    TwoPaneListDetailScaffold(
        modifier = modifier,
        selectionSaver = SettingsCategorySaver,
        gutterPadding = PaddingValues(16.dp),
        placeholderPane = {
            DetailPanePlaceholder(
                icon = Icons.Outlined.Settings,
                text = stringResource(R.string.pane_placeholder_settings),
            )
        },
        listPane = { onSelect ->
            SettingsScreen(
                onCategorySelected = onSelect,
                onBackClick = onBackClick,
            )
        },
        detailPane = { category, onClose ->
            SettingsDetailPane(category = category, navController = navController, onClose = onClose)
        },
    )
}

/** A Settings category that opens its own subscreen in the detail pane (App Links stays a dialog). */
enum class SettingsCategory {
    LookAndFeel,
    AniList,
    Notifications,
    Storage,
    MediaUpload,
    Updates,
    Sponsors,
    About,
    DeveloperTools,
}

private fun SettingsCategory.toPaneRoute(): Any = when (this) {
    SettingsCategory.LookAndFeel -> SettingsLookAndFeel
    SettingsCategory.AniList -> SettingsAniList
    SettingsCategory.Notifications -> SettingsNotifications
    SettingsCategory.Storage -> SettingsStorage
    SettingsCategory.MediaUpload -> SettingsMediaUpload
    SettingsCategory.Updates -> SettingsUpdates
    SettingsCategory.Sponsors -> SettingsSponsors
    SettingsCategory.About -> SettingsAbout
    SettingsCategory.DeveloperTools -> SettingsDeveloperTools
}

// Persists the open category across configuration changes (stored by enum name).
private val SettingsCategorySaver = Saver<SettingsCategory?, String>(
    save = { it?.name ?: "" },
    restore = { if (it.isEmpty()) null else SettingsCategory.valueOf(it) },
)

/**
 * Hosts the selected [category]'s subscreen inside the detail pane. A nested [NavHost] gives each
 * subscreen a route-scoped entry (so its ViewModel is fresh) and lets in-category drill-downs push
 * within the pane; selecting another category navigates with a cleared back stack. System back steps
 * through the pane's own stack first, then closes the pane.
 */
@Composable
private fun SettingsDetailPane(
    category: SettingsCategory,
    navController: NavHostController,
    onClose: () -> Unit,
) {
    val paneNav = rememberNavController()
    val popOrClose: () -> Unit = { if (!paneNav.popBackStack()) onClose() }

    BackHandler(enabled = true) { popOrClose() }

    var isFirstSelection by remember { mutableStateOf(true) }
    LaunchedEffect(category) {
        if (isFirstSelection) {
            isFirstSelection = false
        } else {
            paneNav.navigate(category.toPaneRoute()) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(
        navController = paneNav,
        startDestination = remember { category.toPaneRoute() },
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeIn()
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start) + fadeOut()
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeIn()
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) + fadeOut()
        },
    ) {
        composable<SettingsLookAndFeel> {
            LookAndFeelScreen(
                onBackClick = popOrClose,
                onNavigateToTheme = { paneNav.navigate(SettingsTheme) },
                onNavigateToLanguage = { paneNav.navigate(SettingsLanguage) },
            )
        }
        composable<SettingsTheme> { ThemeScreen(onBackClick = popOrClose) }
        composable<SettingsLanguage> { LanguageScreen(onBackClick = popOrClose) }
        composable<SettingsAniList> {
            AniListSettingsScreen(
                onLogout = { navController.navigate(Login) { popUpTo(0) { inclusive = true } } },
                onBackClick = popOrClose,
            )
        }
        composable<SettingsNotifications> { NotificationsScreen(onBackClick = popOrClose) }
        composable<SettingsStorage> { StorageScreen(onBackClick = popOrClose) }
        composable<SettingsMediaUpload> { MediaUploadSettingsScreen(onBackClick = popOrClose) }
        composable<SettingsUpdates> { UpdatesScreen(onBackClick = popOrClose) }
        composable<SettingsSponsors> { SponsorsScreen(onBackClick = popOrClose) }
        composable<SettingsAbout> {
            AboutScreen(
                onBackClick = popOrClose,
                onNavigateToOpenSourceLicenses = { paneNav.navigate(SettingsOpenSourceLicenses) },
                onNavigateToAcknowledgments = { paneNav.navigate(SettingsAcknowledgments) },
                onNavigateToLinks = { paneNav.navigate(SettingsLinks) },
            )
        }
        composable<SettingsOpenSourceLicenses> { OpenSourceLicensesScreen(onBackClick = popOrClose) }
        composable<SettingsAcknowledgments> { AcknowledgmentsScreen(onBackClick = popOrClose) }
        composable<SettingsLinks> { LinksScreen(onBackClick = popOrClose) }
        composable<SettingsDeveloperTools> {
            DeveloperToolsScreen(
                onBackClick = popOrClose,
                onFontPlaygroundClick = { paneNav.navigate(SettingsFontPlayground) },
            )
        }
        composable<SettingsFontPlayground> { FontSettingsScreen(onBackClick = popOrClose) }
    }
}
