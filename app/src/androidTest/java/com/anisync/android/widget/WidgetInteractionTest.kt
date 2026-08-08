package com.anisync.android.widget

import android.content.Context
import android.os.ParcelFileDescriptor
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.widget.core.WidgetIntents
import com.anisync.android.widget.core.WidgetState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Binds real widget instances and drives their taps end to end.
 *
 * This is the test the Glance implementation could not have: its unit harness does not render and
 * cannot click, which is exactly the surface that kept breaking. Here each widget is bound to a
 * real `AppWidgetHost`, the tap `PendingIntent` is fired the way the launcher fires it, and the
 * assertion is on the state and the views the host is handed back.
 *
 * Requires bind permission:
 *
 *     adb shell appwidget grantbind --package com.anisync.android.debug
 */
@RunWith(AndroidJUnit4::class)
class WidgetInteractionTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var host: BoundWidgetHost

    @Before
    fun setUp() {
        host = BoundWidgetHost(context)
    }

    @After
    fun tearDown() = host.release()

    @Test
    fun airingTodayFilterTap_appliesImmediately() {
        val id = bind(AiringTodayWidgetProvider::class.java)
        assertEquals(false, WidgetState.getBoolean(context, id, MY_LIST, false))

        val elapsed = fireAndAwaitState(
            action = {
                WidgetIntents
                    .setState(context, AiringTodayWidgetProvider::class.java, id, MY_LIST, "true")
                    .send()
            },
            applied = { WidgetState.getBoolean(context, id, MY_LIST, false) }
        )

        assertTrue("Tap took ${elapsed}ms to apply", elapsed < TAP_BUDGET_MS)
    }

    @Test
    fun calendarDayTaps_inRapidSuccession_settleOnTheLastOne() {
        val id = bind(WeeklyCalendarWidgetProvider::class.java)

        // No waiting between sends. This is the "tapping the same control repeatedly" case, where
        // the previous implementation could leave the day strip and the list disagreeing.
        (0..6).forEach { day ->
            WidgetIntents
                .setState(context, WeeklyCalendarWidgetProvider::class.java, id, DAY, "$day")
                .send()
        }

        awaitValue(id, DAY, expected = 6)
        assertEquals(6, WidgetState.getInt(context, id, DAY, 0))
    }

    @Test
    fun watchProgressTypeSwitch_flipsBothWays() {
        val id = bind(WatchProgressWidgetProvider::class.java)

        fireAndAwaitState(
            action = {
                WidgetIntents
                    .setState(context, WatchProgressWidgetProvider::class.java, id, TYPE, "MANGA")
                    .send()
            },
            applied = { WidgetState.getString(context, id, TYPE, "ANIME") == "MANGA" }
        )

        fireAndAwaitState(
            action = {
                WidgetIntents
                    .setState(context, WatchProgressWidgetProvider::class.java, id, TYPE, "ANIME")
                    .send()
            },
            applied = { WidgetState.getString(context, id, TYPE, "MANGA") == "ANIME" }
        )
    }

    @Test
    fun stateIsPerInstance() {
        val first = bind(AiringTodayWidgetProvider::class.java)
        val second = bind(AiringTodayWidgetProvider::class.java)
        assertNotEquals(first, second)

        fireAndAwaitState(
            action = {
                WidgetIntents
                    .setState(context, AiringTodayWidgetProvider::class.java, first, MY_LIST, "true")
                    .send()
            },
            applied = { WidgetState.getBoolean(context, first, MY_LIST, false) }
        )

        assertEquals(false, WidgetState.getBoolean(context, second, MY_LIST, false))
    }

    @Test
    fun everyProviderPublishesInflatableViews() {
        PROVIDERS.forEach { provider ->
            val id = bind(provider)
            fireAndAwait(id) { host.broadcastUpdate(provider, id) }
            val views = requireNotNull(host.lastPublished(id)) {
                "${provider.simpleName} published nothing"
            }
            // apply() throws on a view type RemoteViews does not allow, which is the failure mode
            // that otherwise only shows up as a blank widget on someone's home screen.
            views.apply(context, FrameLayout(context))
        }
    }

    @Test
    fun everyProviderSurvivesEverySizeItDeclares() {
        PROVIDERS.forEach { provider ->
            val id = bind(provider)
            listOf(110 to 100, 180 to 110, 250 to 220, 310 to 310, 400 to 420).forEach { (w, h) ->
                val elapsed = fireAndAwait(id) { host.resize(id, w, h) }
                assertTrue(
                    "${provider.simpleName} published nothing at ${w}x$h",
                    elapsed < AWAIT_TIMEOUT_MS && host.lastPublished(id) != null
                )
            }
        }
    }

    /**
     * The case the whole rewrite exists for.
     *
     * Under Doze, Glance's session times out after five seconds and the next tap has to enqueue a
     * WorkManager job to rebuild it. Here the tap is a broadcast the provider answers directly, so
     * the budget is the same idle or not.
     */
    @Test
    fun tapAfterDeviceIdle_stillApplies() {
        val id = bind(AiringTodayWidgetProvider::class.java)

        shell("dumpsys battery unplug")
        shell("dumpsys deviceidle enable")
        try {
            shell("dumpsys deviceidle force-idle")
            assumeTrue(
                "Device would not enter deep idle",
                shell("dumpsys deviceidle get deep").contains("IDLE")
            )

            val elapsed = fireAndAwaitState(
                action = {
                    WidgetIntents
                        .setState(context, AiringTodayWidgetProvider::class.java, id, MY_LIST, "true")
                        .send()
                },
                applied = { WidgetState.getBoolean(context, id, MY_LIST, false) }
            )

            assertTrue("Idle tap took ${elapsed}ms to apply", elapsed < TAP_BUDGET_MS)
        } finally {
            shell("dumpsys deviceidle unforce")
            shell("dumpsys battery reset")
            // Leaving idle is not instant, and the next test would inherit the throttling.
            val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline &&
                shell("dumpsys deviceidle get deep").contains("IDLE")
            ) {
                Thread.sleep(POLL_MS)
            }
        }
    }

    private fun bind(provider: Class<*>): Int {
        val id = host.bind(provider)
        assumeTrue(
            "Needs: adb shell appwidget grantbind --package ${context.packageName}",
            id != null
        )
        return requireNotNull(id)
    }

    private fun fireAndAwait(appWidgetId: Int, action: () -> Unit): Long {
        val elapsed = host.fireAndAwait(appWidgetId, AWAIT_TIMEOUT_MS, action)
        assertTrue("No update reached the host within ${AWAIT_TIMEOUT_MS}ms", elapsed != null)
        return requireNotNull(elapsed)
    }

    /**
     * Fires a state-changing tap and returns how long until that state was actually applied.
     *
     * Waiting on "the host got an update" is not precise enough for these: a periodic refresh or a
     * queued resize can trip that latch while the tap is still in flight, and the assertion then
     * reads state the tap has not written. Polling the state itself is what the acceptance bar
     * actually says, so it is what gets measured.
     */
    private fun fireAndAwaitState(action: () -> Unit, applied: () -> Boolean): Long {
        val start = System.nanoTime()
        action()
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (applied()) return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            Thread.sleep(POLL_MS)
        }
        throw AssertionError("Tap never applied within ${AWAIT_TIMEOUT_MS}ms")
    }

    /**
     * Waits for a burst of taps to reach [expected].
     *
     * Polling for "the value stopped changing" is wrong here: the gap between two deliveries looks
     * exactly like the end of the burst. Broadcasts to one receiver are ordered and each handler
     * commits before it renders, so the burst is finished when the last value has landed.
     */
    private fun awaitValue(appWidgetId: Int, key: String, expected: Int) {
        // Generous on purpose. This test is about which value wins, not how fast one tap lands,
        // and seven renders back to back is more work than any single tap does.
        val deadline = System.currentTimeMillis() + BURST_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (WidgetState.getInt(context, appWidgetId, key, -1) == expected) return
            Thread.sleep(POLL_MS)
        }
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
            .let { ParcelFileDescriptor.AutoCloseInputStream(it) }
            .use { it.readBytes().decodeToString() }
            .trim()

    private companion object {
        const val MY_LIST = "my_list_only"
        const val DAY = "selected_day"
        const val TYPE = "media_type"

        /** The acceptance bar, with slack for instrumentation overhead. */
        const val TAP_BUDGET_MS = 1_000L
        const val AWAIT_TIMEOUT_MS = 5_000L
        const val BURST_TIMEOUT_MS = 20_000L
        const val POLL_MS = 25L

        val PROVIDERS = listOf(
            UpNextWidgetProvider::class.java,
            AiringTodayWidgetProvider::class.java,
            WeeklyCalendarWidgetProvider::class.java,
            TrendingWidgetProvider::class.java,
            WatchProgressWidgetProvider::class.java,
        )
    }
}
