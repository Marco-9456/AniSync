package com.anisync.android.presentation.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.profile.components.PlaceholderTabContent
import com.anisync.android.presentation.profile.components.ProfileBioSheet
import com.anisync.android.presentation.profile.components.ProfileTopSection
import com.anisync.android.presentation.profile.sections.ProfileActivitySection
import com.anisync.android.presentation.profile.sections.ProfileOverviewSection
import com.anisync.android.presentation.profile.sections.ProfileSocialSection
import com.anisync.android.presentation.util.rememberHapticFeedback

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileContent(
    profile: UserProfile,
    uiState: ProfileUiState,
    isOwnProfile: Boolean,
    onAction: (ProfileAction) -> Unit,
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        item(key = "profile_header", contentType = "header") {
            ProfileTopSection(
                profile = profile,
                isOwnProfile = isOwnProfile,
                onSettingsClick = onSettingsClick,
                onEditProfileClick = {
                    onAction(ProfileAction.SetEditProfileDialogVisible(true))
                },
                onStatisticsClick = onStatisticsClick,
                onShowBiography = {
                    onAction(ProfileAction.SetBiographySheetVisible(true))
                }
            )
        }

        stickyHeader {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileTabsButtonGroup(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { onAction(ProfileAction.SelectTab(it)) }
                )
            }
        }

        when (uiState.selectedTab) {
            ProfileTab.OVERVIEW -> {
                item(key = "tab_overview", contentType = "overview") {
                    ProfileOverviewSection(profile = profile)
                }
            }

            ProfileTab.ACTIVITY -> {
                item(key = "tab_activity", contentType = "activity") {
                    ProfileActivitySection(
                        profile = profile,
                        selectedFilter = uiState.selectedActivityFilter,
                        onFilterSelected = { onAction(ProfileAction.SelectActivityFilter(it)) }
                    )
                }
            }

            ProfileTab.SOCIAL -> {
                item(key = "tab_social", contentType = "social") {
                    ProfileSocialSection(
                        selectedTab = uiState.selectedSocialTab,
                        onTabSelected = { onAction(ProfileAction.SelectSocialTab(it)) }
                    )
                }
            }

            ProfileTab.ANIME -> {
                item(key = "tab_anime") {
                    PlaceholderTabContent(stringResource(R.string.profile_placeholder_anime))
                }
            }

            ProfileTab.MANGA -> {
                item(key = "tab_manga") {
                    PlaceholderTabContent(stringResource(R.string.profile_placeholder_manga))
                }
            }

            ProfileTab.FAVORITES -> {
                item(key = "tab_favorites") {
                    PlaceholderTabContent(stringResource(R.string.profile_placeholder_favorites))
                }
            }

            ProfileTab.REVIEWS -> {
                item(key = "tab_reviews") {
                    PlaceholderTabContent(stringResource(R.string.profile_placeholder_reviews))
                }
            }

            ProfileTab.STATS -> {
                item(key = "tab_stats") {
                    PlaceholderTabContent(stringResource(R.string.profile_placeholder_stats))
                }
            }
        }
    }

    if (uiState.isBiographySheetVisible) {
        ProfileBioSheet(
            about = profile.about.orEmpty(),
            onDismissRequest = {
                onAction(ProfileAction.SetBiographySheetVisible(false))
            }
        )
    }
}

private fun profileTabIcon(tab: ProfileTab): ImageVector {
    return when (tab) {
        ProfileTab.OVERVIEW -> Icons.Default.Person
        ProfileTab.ACTIVITY -> Icons.Default.Schedule
        ProfileTab.ANIME -> Icons.Default.Tv
        ProfileTab.MANGA -> Icons.AutoMirrored.Filled.MenuBook
        ProfileTab.FAVORITES -> Icons.Default.Favorite
        ProfileTab.SOCIAL -> Icons.Default.Forum
        ProfileTab.REVIEWS -> Icons.Default.RateReview
        ProfileTab.STATS -> Icons.Default.BarChart
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileTabsButtonGroup(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
            ButtonGroupDefaults.ConnectedSpaceBetween
        )
    ) {
        ProfileTab.entries.forEachIndexed { index, tab ->
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                ProfileTab.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            ToggleButton(
                checked = selectedTab == tab,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(tab)
                },
                shapes = shapes
            ) {
                Icon(
                    imageVector = profileTabIcon(tab),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(tab.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
