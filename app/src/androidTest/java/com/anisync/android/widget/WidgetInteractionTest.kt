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
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Binds real widget instances and drives their taps end to end.
 *
 * The Glance version could not have this test. Its unit harness does not render and cannot click,
 * which is exactly the part that kept breaking. Here each widget is bound to a real AppWidgetHost,
 * the tap PendingIntent is fired the way the launcher fires it, and we assert on the state and the
 * views the host gets back.
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

        // No waiting between sends. This is the mash-the-same-button case, where the old version
        // could leave the day strip and the list disagreeing.
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
            // apply() throws on a view type RemoteViews will not take. Otherwise that only shows up
            // as a blank widget on someone home screen.
            views.apply(context, FrameLayout(context))
        }
    }

    /**
     * The data path behind the API 26 to 30 fallback.
     *
     * Below API 31 the rows do not ride inside the RemoteViews. The host binds
     * WidgetCollectionService and its factory calls loadItems to rebuild them from scratch in
     * whatever process it is in. If that comes back empty those devices get a header and an empty
     * list, with no error anywhere.
     *
     * Covers the rebuild, not the binding. A host only binds the service once it really inflates the
     * list, which the recording host here does not do.
     */
    @Test
    fun collectionRowsRebuildFromScratch() {
        val provider = WeeklyCalendarWidgetProvider()
        val id = bind(WeeklyCalendarWidgetProvider::class.java)

        val rebuilt = runBlocking { provider.loadItems(context, id) }
        val direct = runBlocking {
            val snapshot = provider.snapshot(context, id)
            provider.items(context, id, snapshot, emptyMap())
        }

        assertEquals(
            "The service would serve a different number of rows than the widget shows",
            direct.size,
            rebuilt.size
        )
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
     * The case the rewrite exists for.
     *
     * Under Doze the Glance session times out after five seconds and the next tap has to enqueue a
     * WorkManager job to rebuild it. Here the tap is a broadcast the provider answers itself, so the
     * budget is the same idle or not.
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
            // Coming out of idle takes a moment, and the next test would inherit the throttling.
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
     * Fires a state changing tap and returns how long until that state actually applied.
     *
     * Waiting on "the host got an update" is too loose here. A periodic refresh or a queued resize
     * can trip that latch while the tap is still in flight, and then we assert on state the tap never
     * wrote. Polling the state itself is what we actually promised, so that is what we measure.
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
     * Polling for "the value stopped changing" does not work: the gap between two deliveries looks
     * the same as the end of the burst. Broadcasts to one receiver are ordered and each handler
     * commits before it renders, so the burst is done once the last value lands.
     */
    private fun awaitValue(appWidgetId: Int, key: String, expected: Int) {
        // Generous on purpose. This is about which value wins, not how fast one tap lands, and seven
        // renders back to back is more work than a single tap.
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

        /** The bar we promised, plus slack for instrumentation overhead. */
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
