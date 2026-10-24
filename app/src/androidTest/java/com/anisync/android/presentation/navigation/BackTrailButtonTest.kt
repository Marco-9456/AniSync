package com.anisync.android.presentation.navigation

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import com.anisync.android.presentation.components.navigation.BackTrailButton
import com.anisync.android.presentation.util.LocalAppSettings
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The back arrow's trail, driven through a real navigation host: the screen reader action, the long
 * press, and the single pop a row makes.
 */
class BackTrailButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("anisync_settings", Context.MODE_PRIVATE)
    private var openedBefore = false
    private lateinit var navController: NavHostController

    @Before
    fun rememberOpenedFlag() {
        openedBefore = prefs.getBoolean(OPENED_KEY, false)
    }

    // Opening the trail is recorded so the tips stop, and the test must not spend that on the device.
    @After
    fun restoreOpenedFlag() {
        prefs.edit().putBoolean(OPENED_KEY, openedBefore).commit()
    }

    private fun setUpTrail(vararg pages: Any) {
        val appSettings = AppSettings(context)
        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalAppSettings provides appSettings) {
                    navController = rememberNavController()
                    val host = rememberBackTrailHost(navController)
                    CompositionLocalProvider(LocalBackTrailHost provides host) {
                        NavHost(navController = navController, startDestination = Library) {
                            composable<Library> { Text("Library page") }
                            composable<Calendar> { Page() }
                            composable<Notes> { Page() }
                        }
                    }
                }
            }
        }
        composeTestRule.runOnIdle { pages.forEach { navController.navigate(it) } }
        composeTestRule.waitForIdle()
    }

    @Composable
    private fun Page() {
        BackTrailButton(onClick = { navController.popBackStack() })
    }

    private val back get() = composeTestRule.onNodeWithContentDescription(context.getString(R.string.back))

    @Test
    fun theArrowOffersTheTrailToScreenReaders() {
        setUpTrail(Calendar, Notes)

        val label = context.getString(R.string.a11y_back_trail)
        back.assert(
            SemanticsMatcher("has the trail action") { node ->
                node.config.getOrNull(SemanticsActions.CustomActions)?.any { it.label == label } == true
            }
        )
    }

    @Test
    fun aRowPopsStraightToItsPage() {
        setUpTrail(Calendar, Notes)

        back.performTouchInput { longClick() }
        composeTestRule.onNodeWithText(context.getString(R.string.calendar_title)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_library)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertTrue(navController.currentDestination?.hasRoute<Library>() == true)
            assertNull(navController.previousBackStackEntry)
        }
    }

    @Test
    fun oneVisibleRowKeepsThePlainBack() {
        setUpTrail(Calendar)

        val label = context.getString(R.string.a11y_back_trail)
        back.assert(
            SemanticsMatcher("has no trail action") { node ->
                node.config.getOrNull(SemanticsActions.CustomActions)?.none { it.label == label } != false
            }
        )
        back.performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertTrue(navController.currentDestination?.hasRoute<Library>() == true)
        }
    }

    @Test
    fun theScreenReaderActionOpensTheTrail() {
        setUpTrail(Calendar, Notes)

        val label = context.getString(R.string.a11y_back_trail)
        val actions = back.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeTestRule.runOnUiThread { actions.first { it.label == label }.action() }

        composeTestRule.onNodeWithText(context.getString(R.string.calendar_title)).assertExists()
    }

    private companion object {
        const val OPENED_KEY = "back_trail_opened"
    }
}
