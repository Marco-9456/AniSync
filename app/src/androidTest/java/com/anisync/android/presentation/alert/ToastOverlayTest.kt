package com.anisync.android.presentation.alert

import android.os.SystemClock
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.R
import com.anisync.android.data.AppSettings
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.components.alert.ProvideToastManager
import com.anisync.android.presentation.components.alert.ToastCountdown
import com.anisync.android.presentation.components.alert.ToastManager
import com.anisync.android.presentation.components.alert.ToastMessage
import com.anisync.android.presentation.components.alert.ToastType
import com.anisync.android.presentation.components.alert.TopAlertToast
import com.anisync.android.presentation.library.components.EditLibraryEntrySheet
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.type.MediaType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The two toast behaviours that only exist on a device.
 *
 * A live region and a modal window are both platform things. Neither has a JVM equivalent, and the
 * failures they produce are silent in exactly the way that keeps them from being noticed: a screen
 * reader talking over itself, and a report rendering behind a scrim where nobody will see it.
 */
class ToastOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * The toast frame is a polite live region, so any semantics change inside it is announced.
     *
     * The countdown redraws every frame and its label changes every second. While that label was a
     * `contentDescription`, a one minute block meant about sixty announcements, each interrupting
     * whatever the user was reading. The clock the eye reads keeps moving; the sentence a screen
     * reader is given is fixed at the wait the toast appeared with.
     *
     * The wait is real rather than a clock the test drives, because the countdown measures itself
     * against [SystemClock] and would otherwise sit still while the frames went by. Whatever the
     * exact drift, a description tracking the clock is no longer the one it started with, so the
     * assertion does not depend on hitting a particular second.
     */
    @Test
    fun theCountdownIsAnnouncedOnceHoweverLongTheBlockRuns() {
        val spokenAtFirstSight = context.getString(R.string.alert_retrying_in, 60)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            MaterialTheme {
                TopAlertToast(
                    toast = rateLimitToast(seconds = 60),
                    onDismiss = {},
                )
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithContentDescription(spokenAtFirstSight).assertIsDisplayed()

        // Real seconds, because the countdown measures itself against SystemClock and would sit
        // still while a driven clock went by. The frames are then pumped by hand so the loop
        // observes them.
        Thread.sleep(3_500)
        repeat(FRAMES_TO_SETTLE) { composeTestRule.mainClock.advanceTimeByFrame() }

        composeTestRule.onNodeWithContentDescription(spokenAtFirstSight).assertIsDisplayed()
    }

    /**
     * Material reads the progress lambda inside its own `semantics` block, so the bar's range info
     * changes on every frame it draws. Sixty seconds of that is a semantics change per frame inside
     * the live region, and the countdown beside it already says everything the bar is showing.
     */
    @Test
    fun theProgressBarAddsNoSemanticsOfItsOwn() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            MaterialTheme {
                TopAlertToast(
                    toast = rateLimitToast(seconds = 60),
                    onDismiss = {},
                )
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertCountEquals(0)
    }

    /**
     * The countdown still has to run. Clearing the semantics on the label and the bar must not take
     * the frame loop with it, or a toast that waits out a block would never end.
     */
    @Test
    fun theCountdownStillFinishesOnTime() {
        var finished = false

        composeTestRule.setContent {
            MaterialTheme {
                TopAlertToast(
                    toast = rateLimitToast(seconds = 2),
                    onDismiss = {},
                    onCountdownFinished = { finished = true },
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) { finished }
    }

    /**
     * A `ModalBottomSheet` renders in its own platform window, above the app window the global
     * toast host lives in, so a toast raised while one is open lands behind its scrim.
     *
     * The library edit sheet reports a failed save and stays open with the edits still in it. That
     * report reaching nobody is worse than the silence it replaced: the spinner clears, the sheet
     * sits there, and nothing says why. So the sheet hosts the toast itself and the global host
     * stands down while it does.
     */
    @Test
    fun theEditSheetHostsItsOwnToastRatherThanLeavingItBehindTheScrim() {
        val toastManager = ToastManager(context)
        val message = "AniList refused the save."

        composeTestRule.setContent {
            TestTheme {
                ProvideToastManager(toastManager) {
                    EditLibraryEntrySheet(
                        entry = entry(),
                        onDismiss = {},
                        onSave = {},
                        onDelete = {},
                    )
                }
            }
        }

        composeTestRule.runOnIdle {
            toastManager.showToast(
                type = ToastType.ERROR,
                title = "Could not save",
                message = message,
            )
        }

        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        assertTrue(
            "the sheet has to take the toast over, or the app window's host draws it behind the scrim",
            toastManager.overlayHostActive.value,
        )
    }

    /**
     * The countdown asks for a frame for as long as it runs, so a composition holding one never
     * goes idle and every assertion that waits for idle would time out instead. These tests drive
     * the frame clock themselves rather than shortening the wait, because a minute long block is
     * the case being tested.
     */
    private companion object {
        const val FRAMES_TO_SETTLE = 4
    }

    private fun rateLimitToast(seconds: Long) = ToastMessage(
        type = ToastType.RATE_LIMITED,
        title = "Rate limited",
        message = "AniList is limiting requests right now.",
        countdown = ToastCountdown(
            retryAtElapsedMs = SystemClock.elapsedRealtime() + seconds * 1_000,
            totalSeconds = seconds,
        ),
    )

    private fun entry() = LibraryEntry(
        id = 1,
        mediaId = 100,
        titleRomaji = "Frieren: Beyond Journey's End",
        titleEnglish = "Frieren: Beyond Journey's End",
        titleNative = "Sousou no Frieren",
        titleUserPreferred = "Frieren: Beyond Journey's End",
        coverUrl = null,
        type = MediaType.ANIME,
        status = LibraryStatus.CURRENT,
        progress = 5,
        totalEpisodes = 28,
        totalChapters = null,
        totalVolumes = null,
    )

    /** The sheet reaches for haptics, which read the app settings through the composition. */
    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        val local = LocalContext.current
        val appSettings = remember(local) { AppSettings(local) }
        CompositionLocalProvider(LocalAppSettings provides appSettings) {
            MaterialTheme(content = content)
        }
    }
}
