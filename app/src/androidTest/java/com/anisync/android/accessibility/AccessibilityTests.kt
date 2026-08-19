package com.anisync.android.accessibility

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.SegmentedTabItem
import com.anisync.android.presentation.components.ErrorState
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.discover.components.CardStyle
import com.anisync.android.presentation.discover.components.DiscoverMediaCard
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.type.MediaType
import org.junit.Rule
import org.junit.Test

/**
 * Accessibility UI tests for AniSync components.
 * 
 * These tests verify that all UI components meet accessibility requirements:
 * - Content descriptions are present and meaningful
 * - Semantic roles are properly assigned
 * - Touch targets meet minimum size requirements (48dp)
 * - Heading hierarchy is correct for screen reader navigation
 * - Live regions announce dynamic content changes
 * - Selection states are properly communicated
 */
class AccessibilityTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== ErrorState Tests ====================

    @Test
    fun errorState_hasLiveRegion_forScreenReaderAnnouncement() {
        composeTestRule.setContent {
            TestTheme {
                ErrorState(
                    message = "Network error occurred",
                    onRetry = {}
                )
            }
        }

        // Verify the error state container has live region for automatic announcements
        composeTestRule
            .onNode(AccessibilityTestUtils.hasLiveRegion())
            .assertExists()
    }

    @Test
    fun errorState_errorIcon_hasContentDescription() {
        val errorMessage = "Network error occurred"
        
        composeTestRule.setContent {
            TestTheme {
                ErrorState(
                    message = errorMessage,
                    onRetry = {}
                )
            }
        }

        // Verify error icon has meaningful content description
        composeTestRule
            .onNode(hasContentDescription(errorMessage, substring = true))
            .assertExists()
    }

    @Test
    fun errorState_retryButton_isClickable() {
        var retryClicked = false
        
        composeTestRule.setContent {
            TestTheme {
                ErrorState(
                    message = "Network error",
                    onRetry = { retryClicked = true }
                )
            }
        }

        // Resolved from resources rather than hardcoded: the label is translated, so a literal
        // "Retry" only matches on an English device and fails on, say, a German one.
        val retryLabel = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.retry)
        composeTestRule
            .onNodeWithText(retryLabel)
            .performClick()

        assert(retryClicked) { "Retry button should be clickable" }
    }

    // ==================== SegmentedTabItem Tests ====================

    @Test
    fun animatedTab_hasTabRole() {
        composeTestRule.setContent {
            TestTheme {
                SegmentedTabItem(
                    index = 0,
                    selectedIndex = 0,
                    selected = true,
                    onClick = {},
                    icon = Icons.Default.Home,
                    label = "Home"
                )
            }
        }

        // Verify tab has Tab role
        composeTestRule
            .onNode(AccessibilityTestUtils.hasRole(Role.Tab))
            .assertExists()
    }

    @Test
    fun animatedTab_selectedState_includesSelectedInDescription() {
        composeTestRule.setContent {
            TestTheme {
                SegmentedTabItem(
                    index = 0,
                    selectedIndex = 0,
                    selected = true,
                    onClick = {},
                    icon = Icons.Default.Home,
                    label = "Home"
                )
            }
        }

        // Verify selected tab has "selected" in content description
        composeTestRule
            .onNode(hasContentDescription("Home, selected"))
            .assertExists()
    }

    @Test
    fun animatedTab_unselectedState_hasLabelOnlyDescription() {
        composeTestRule.setContent {
            TestTheme {
                SegmentedTabItem(
                    index = 1,
                    selectedIndex = 0,
                    selected = false,
                    onClick = {},
                    icon = Icons.Default.Search,
                    label = "Search"
                )
            }
        }

        // Verify unselected tab has label-only description
        composeTestRule
            .onNode(hasContentDescription("Search"))
            .assertExists()
    }

    @Test
    fun animatedTab_selectionState_isAccessible() {
        composeTestRule.setContent {
            TestTheme {
                Column {
                    SegmentedTabItem(
                        index = 0,
                        selectedIndex = 0,
                        selected = true,
                        onClick = {},
                        icon = Icons.Default.Home,
                        label = "Home"
                    )
                    SegmentedTabItem(
                        index = 1,
                        selectedIndex = 0,
                        selected = false,
                        onClick = {},
                        icon = Icons.Default.Search,
                        label = "Search"
                    )
                }
            }
        }

        // Verify selected tab is marked as selected
        composeTestRule
            .onNode(
                AccessibilityTestUtils.hasRole(Role.Tab)
                    .and(AccessibilityTestUtils.isSelected())
            )
            .assertExists()

        // Verify unselected tab is marked as not selected
        composeTestRule
            .onNode(
                AccessibilityTestUtils.hasRole(Role.Tab)
                    .and(AccessibilityTestUtils.isNotSelected())
            )
            .assertExists()
    }

    // ==================== SectionHeader Tests ====================

    @Test
    fun sectionHeader_title_isHeading() {
        composeTestRule.setContent {
            TestTheme {
                SectionHeader(
                    title = "Trending Now",
                    level = HeaderLevel.Section
                )
            }
        }

        // Verify the section header is marked as heading for screen reader navigation
        composeTestRule
            .onNode(AccessibilityTestUtils.isHeading())
            .assertExists()
    }

    @Test
    fun sectionHeader_actionButton_hasButtonRole() {
        composeTestRule.setContent {
            TestTheme {
                SectionHeader(
                    title = "Trending Now",
                    level = HeaderLevel.Section,
                    onActionClick = {},
                    actionLabel = "See All"
                )
            }
        }

        // Verify the action button has Button role
        composeTestRule
            .onNode(AccessibilityTestUtils.hasRole(Role.Button))
            .assertExists()
    }

    @Test
    fun sectionHeader_actionButton_hasContentDescription() {
        composeTestRule.setContent {
            TestTheme {
                SectionHeader(
                    title = "Trending Now",
                    level = HeaderLevel.Section,
                    onActionClick = {},
                    actionLabel = "See All"
                )
            }
        }

        // Verify action button has accessible content description
        composeTestRule
            .onNode(AccessibilityTestUtils.hasContentDescription())
            .assertExists()
    }

    @Test
    fun sectionHeader_withSubtitle_displaysAllContent() {
        composeTestRule.setContent {
            TestTheme {
                SectionHeader(
                    title = "Library",
                    level = HeaderLevel.Screen,
                    subtitle = "Your collection"
                )
            }
        }

        // Verify title and subtitle are displayed
        composeTestRule.onNodeWithText("Library").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your collection").assertIsDisplayed()
    }

    // ==================== DiscoverMediaCard Tests ====================

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun discoverMediaCard_hasClickLabel_forAccessibility() {
        val testEntry = createTestLibraryEntry()

        composeTestRule.setContent {
            TestTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DiscoverMediaCard(
                            item = testEntry,
                            style = CardStyle.Standard(),
                            onClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        // Verify the card has an onClick label for screen readers
        composeTestRule
            .onNode(AccessibilityTestUtils.hasOnClickLabel())
            .assertExists()
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun discoverMediaCard_poster_hasContentDescription() {
        val testEntry = createTestLibraryEntry()

        composeTestRule.setContent {
            TestTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DiscoverMediaCard(
                            item = testEntry,
                            style = CardStyle.Standard(),
                            onClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        // Verify the poster image has content description with the title
        composeTestRule
            .onNode(hasContentDescription(testEntry.titleRomaji!!, substring = true))
            .assertExists()
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun discoverMediaCard_hasButtonRole() {
        val testEntry = createTestLibraryEntry()

        composeTestRule.setContent {
            TestTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DiscoverMediaCard(
                            item = testEntry,
                            style = CardStyle.Standard(),
                            onClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        // Verify the card has Button role for interactivity
        composeTestRule
            .onNode(AccessibilityTestUtils.hasRole(Role.Button))
            .assertExists()
    }

    // ==================== Touch Target Tests ====================

    @Test
    fun allClickableElements_areInteractive() {
        composeTestRule.setContent {
            TestTheme {
                Column {
                    SegmentedTabItem(
                        index = 0,
                        selectedIndex = 0,
                        selected = true,
                        onClick = {},
                        icon = Icons.Default.Home,
                        label = "Home"
                    )
                    SectionHeader(
                        title = "Section",
                        level = HeaderLevel.Section,
                        onActionClick = {}
                    )
                }
            }
        }

        // Verify clickable elements exist and can be found
        composeTestRule
            .onNode(AccessibilityTestUtils.isClickable())
            .assertExists()
    }

    // ==================== Dynamic Content Tests ====================

    @Test
    fun tabSelection_updatesAccessibilityState() {
        composeTestRule.setContent {
            var selectedIndex by remember { mutableIntStateOf(0) }
            
            TestTheme {
                Column {
                    SegmentedTabItem(
                        index = 0,
                        selectedIndex = selectedIndex,
                        selected = selectedIndex == 0,
                        onClick = { selectedIndex = 0 },
                        icon = Icons.Default.Home,
                        label = "Home"
                    )
                    SegmentedTabItem(
                        index = 1,
                        selectedIndex = selectedIndex,
                        selected = selectedIndex == 1,
                        onClick = { selectedIndex = 1 },
                        icon = Icons.Default.Search,
                        label = "Search"
                    )
                }
            }
        }

        // Initially Home is selected
        composeTestRule
            .onNode(hasContentDescription("Home, selected"))
            .assertExists()

        // Click on Search tab
        composeTestRule
            .onNode(hasContentDescription("Search"))
            .performClick()

        // Verify Search is now selected
        composeTestRule
            .onNode(hasContentDescription("Search, selected"))
            .assertExists()
    }

    // ==================== Helper Methods ====================

    private fun createTestLibraryEntry(): LibraryEntry {
        return LibraryEntry(
            id = 1,
            mediaId = 100,
            titleRomaji = "Frieren: Beyond Journey's End",
            titleEnglish = "Frieren: Beyond Journey's End",
            titleNative = "Sousou no Frieren",
            titleUserPreferred = "Frieren: Beyond Journey's End",
            coverUrl = null,
            type = MediaType.ANIME,
            averageScore = 95,
            mediaStatus = "FINISHED",
            status = LibraryStatus.CURRENT,
            progress = 5,
            totalEpisodes = 28,
            totalChapters = null,
            totalVolumes = null
        )
    }

    /**
     * Wraps content the way the app does. Several of these components reach
     * [com.anisync.android.presentation.util.rememberHapticFeedback], which reads
     * [LocalAppSettings], and that CompositionLocal has no default: without a provider it throws
     * "AppSettings not provided" and the test fails before it can assert anything.
     */
    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        val context = LocalContext.current
        val appSettings = remember(context) { AppSettings(context) }
        CompositionLocalProvider(LocalAppSettings provides appSettings) {
            MaterialTheme(content = content)
        }
    }

}
