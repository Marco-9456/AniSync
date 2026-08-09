package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import androidx.core.util.SizeFCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.R
import com.anisync.android.data.local.entity.AiringScheduleEntity
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.data.local.entity.TrendingEntity
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.type.MediaType
import com.anisync.android.widget.core.AniSyncWidgetProvider
import com.anisync.android.widget.core.WidgetImageLoader
import com.anisync.android.widget.core.WidgetTime
import com.anisync.android.widget.core.activeOwnerId
import com.anisync.android.widget.core.widgetDeps
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Renders every widget at every size it declares and writes each one to a PNG.
 *
 * Not an assertion suite. It is here so a layout change can be looked at instead of reasoned about,
 * which is the part of widget work no unit test covers. Output:
 *
 *     adb pull /data/local/tmp/widget-shots
 *
 * It does fail loudly on a layout that will not inflate, since RemoteViews.apply throws on any view
 * type the framework does not allow. Otherwise that shows up as a blank widget on someone home
 * screen and nowhere else.
 *
 * Calls [AniSyncWidgetProvider.build] per size instead of going through a bound host. A published
 * widget carries one RemoteViews per declared size and the host picks, but RemoteViews.apply with no
 * size hint always lands on the smallest, so those screenshots would all be the compact layout.
 */
@RunWith(AndroidJUnit4::class)
class WidgetScreenshotTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Opt in with -e seed true. Off by default so a real device keeps its own data. */
    private val shouldSeed: Boolean =
        InstrumentationRegistry.getArguments().getString("seed")?.toBoolean() == true

    /**
     * The picker previews have to survive RemoteViews too.
     *
     * A host inflates previewLayout through RemoteViews, so the same allowed-view rules apply. One
     * bare View in the Watch Progress preview was enough to make it fail silently and show nothing
     * in the picker. Rendered here as well so the previews can be looked at with the widgets.
     */
    @Test
    fun everyPickerPreviewInflates() {
        val outDir = File(context.getExternalFilesDir(null), "widget-shots").apply { mkdirs() }
        PREVIEWS.forEach { (name, layout) ->
            val views = RemoteViews(context.packageName, layout)
            writePng(views, PREVIEW_SIZE, File(outDir, "preview-$name.png"), emptyList(), 0)
        }
        copyOut(outDir)
    }

    @Test
    fun renderEveryWidgetAtEverySize() {
        if (shouldSeed) seed()
        val outDir = File(context.getExternalFilesDir(null), "widget-shots").apply { mkdirs() }

        PROVIDERS.forEach { (name, provider) -> capture(name, provider, outDir) }
        copyOut(outDir)
    }

    /**
     * Removes whatever [seed] wrote.
     *
     * Not optional. Seeding writes into the real database, and connectedAndroidTest uninstalls the
     * app afterwards but running the instrumentation by hand with am instrument does not. Leave a
     * seeded row behind and it sits in the widget next to the real entry for the same series, which
     * is how a duplicate One Piece ended up on an actual launcher.
     */
    @After
    fun removeSeed() {
        if (!shouldSeed) return
        val db = context.openOrCreateDatabase("anisync.db", Context.MODE_PRIVATE, null)
        db.use {
            it.execSQL("DELETE FROM airing_schedule WHERE id < 0")
            it.execSQL("DELETE FROM library_entries WHERE id < 0")
            // Trending is keyed by real media id, so seeded rows cannot be picked out by id. The
            // table is a cache TrendingWorker refills, so clearing it is fine.
            it.execSQL("DELETE FROM trending_media")
        }
    }

    /**
     * Writes a few representative rows straight into Room.
     *
     * Off unless asked for, since it touches the real database:
     *
     *     am instrument -e seed true ...
     *
     * On, the screenshots come out the same anywhere. Off, they show whatever the device really has,
     * which is the right default for a device someone actually uses.
     */
    private fun seed() = runBlocking {
        val deps = context.widgetDeps()
        val owner = deps.activeOwnerId()
        val start = WidgetTime.startOfToday()
        val now = System.currentTimeMillis() / 1000

        deps.airingScheduleDao().insertAll(
            SEED_MEDIA.mapIndexed { index, media ->
                AiringScheduleEntity(
                    id = SEED_SCHEDULE_ID - index,
                    ownerId = owner,
                    mediaId = media.id,
                    // Spread over the day so the time column shows a range.
                    airingAt = start + (9 + index * 2) * 3600L,
                    episode = index + 3,
                    titleUserPreferred = media.title,
                    coverUrl = media.cover,
                    format = "TV",
                    isWatching = index % 2 == 0,
                    streamingSeriesUrl = null
                )
            }
        )

        // Cleared first. Rank orders this table, and inserting on top of what the device cached
        // gives two rows both claiming number one. removeSeed empties it and TrendingWorker refills.
        deps.trendingDao().clearAll()
        deps.trendingDao().insertAll(
            SEED_MEDIA.mapIndexed { index, media ->
                TrendingEntity(
                    id = media.id,
                    titleUserPreferred = media.title,
                    coverUrl = media.cover,
                    averageScore = media.score,
                    rank = index + 1
                )
            }
        )

        deps.libraryDao().insertAll(
            SEED_MEDIA.mapIndexed { index, media ->
                // Keeps the null episode count the API gave us, so the screenshots cover the
                // unknown-total row and not just the tidy case.
                val total = media.episodes
                LibraryEntryEntity(
                    id = SEED_LIBRARY_ID - index,
                    ownerId = owner,
                    mediaId = media.id,
                    titleRomaji = media.title,
                    titleEnglish = media.title,
                    titleNative = media.title,
                    titleUserPreferred = media.title,
                    coverUrl = media.cover,
                    // Near the end, which is what Watch Progress is for.
                    progress = total?.let { (it - (index + 1)).coerceAtLeast(0) } ?: (40 + index),
                    totalEpisodes = total,
                    totalChapters = null,
                    totalVolumes = null,
                    mediaType = MediaType.ANIME,
                    status = LibraryStatus.CURRENT,
                    // Finale where there is a total so we get the finale line, a mid season episode
                    // otherwise so we get the countdown line.
                    nextAiringEpisode = total ?: (50 + index),
                    timeUntilAiring = 3600,
                    nextAiringEpisodeTime = now + (index + 1) * WidgetTime.DAY,
                    mediaStatus = "RELEASING"
                )
            }
        )
    }

    data class SeedMedia(
        val id: Int,
        val title: String,
        val cover: String,
        val score: Int,
        val episodes: Int?
    )

    private fun <S : Any> capture(name: String, provider: AniSyncWidgetProvider<S>, outDir: File) {
        val snapshot = runBlocking { provider.snapshot(context, FAKE_ID) }
        val requests = provider.coverRequests(context, snapshot)
        val covers = runBlocking {
            if (requests.isEmpty()) {
                emptyMap()
            } else {
                WidgetImageLoader.loadCovers(context, requests, provider.coverSize(context, snapshot))
            }
        }
        val rows = provider.items(context, FAKE_ID, snapshot, covers)

        // Declared sizes plus two taller ones. The ladder stops at 200dp because a scrolling widget
        // needs nothing above it, but a 200dp still only fits one row, which is no use for looking
        // at a list.
        (provider.declaredSizes + INSPECTION_SIZES).forEach { size ->
            val views = provider.build(context, FAKE_ID, size, snapshot, covers)
            val file = File(outDir, "$name-${size.width.toInt()}x${size.height.toInt()}.png")
            writePng(views, size, file, rows, provider.listViewId)
        }
    }

    private fun writePng(
        views: RemoteViews,
        size: SizeFCompat,
        file: File,
        rows: List<RemoteViews>,
        listViewId: Int
    ) {
        val density = context.resources.displayMetrics.density
        val widthPx = (size.width * density).toInt()
        val heightPx = (size.height * density).toInt()

        val inflated: View = views.apply(context, FrameLayout(context))
        substituteListRows(inflated, listViewId, rows)
        inflated.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        inflated.layout(0, 0, widthPx, heightPx)

        val bitmap = createBitmap(widthPx, heightPx)
        inflated.draw(Canvas(bitmap))
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /**
     * Draws the rows in place of the ListView.
     *
     * A ListView fed by RemoteCollectionItems only fills once a host has bound it, so outside one it
     * draws as an empty box and every screenshot would be a widget with no content. A plain column of
     * the same row RemoteViews shows what the host would. It cannot scroll, which a still image
     * cannot show anyway.
     */
    private fun substituteListRows(root: View, listViewId: Int, rows: List<RemoteViews>) {
        if (listViewId == 0 || rows.isEmpty()) return
        val list = root.findViewById<View>(listViewId) ?: return
        val parent = list.parent as? ViewGroup ?: return

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = list.layoutParams
            // Take the list visibility with it. Airing Today hides the list for a hero card at short
            // sizes, and a column that ignored that would draw rows straight through it.
            visibility = list.visibility
        }
        rows.forEach { column.addView(it.apply(context, column)) }

        val index = parent.indexOfChild(list)
        parent.removeView(list)
        parent.addView(column, index)
    }

    /**
     * connectedAndroidTest uninstalls the app at the end of a run and takes the external files dir
     * with it. Copying to /data/local/tmp is what makes the output survive. Merged rather than
     * replaced, since previews and widgets come from separate tests and JUnit promises no order.
     */
    private fun copyOut(outDir: File) {
        shell("mkdir -p $PULL_DIR")
        shell("cp ${outDir.absolutePath}/. $PULL_DIR -r")
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
            .let { ParcelFileDescriptor.AutoCloseInputStream(it) }
            .use { it.readBytes().decodeToString() }
            .trim()

    private companion object {
        /** Nothing is bound here, so nothing in build may hand this id to AppWidgetManager. */
        const val FAKE_ID = 1

        /** Survives the uninstall. Pull with adb pull /data/local/tmp/widget-shots. */
        const val PULL_DIR = "/data/local/tmp/widget-shots"

        /** Not declared sizes. Only rendered so a list can be seen a few rows deep. */
        val INSPECTION_SIZES = listOf(SizeFCompat(320f, 420f), SizeFCompat(400f, 560f))

        /**
         * Seed rows use negative ids and the cleanup deletes exactly those.
         *
         * A high positive base does not work: AniList MediaList and schedule ids are large positive
         * numbers, so "anything above 700000" matches real rows too and the cleanup would take the
         * cached library with it. Nothing real is ever negative.
         */
        const val SEED_SCHEDULE_ID = -900_000
        const val SEED_LIBRARY_ID = -700_000

        /** Roughly the cell the picker gives a preview. */
        val PREVIEW_SIZE = SizeFCompat(320f, 300f)

        val PREVIEWS = listOf(
            "up-next" to R.layout.widget_preview_up_next,
            "airing-today" to R.layout.widget_preview_airing_today,
            "weekly-calendar" to R.layout.widget_preview_weekly_calendar,
            "trending" to R.layout.widget_preview_trending,
            "watch-progress" to R.layout.widget_preview_watch_progress,
        )

        /**
         * Real AniList media, so the screenshots show real posters and a tap opens a real page.
         *
         * Ids, titles and cover hashes came from the AniList API rather than being written by hand.
         * A cover URL carries a per-image hash that cannot be guessed, and a wrong one fails
         * silently as a grey placeholder, which looks exactly like a broken image pipeline.
         *
         * Refresh with:
         *
         *     curl -s -X POST https://graphql.anilist.co -H "Content-Type: application/json" \
         *       -d '{"query":"query { Page(perPage:6) { media(sort: TRENDING_DESC, type: ANIME) {
         *            id title { userPreferred } coverImage { extraLarge } averageScore episodes } } }"}'
         */
        val SEED_MEDIA = listOf(
            SeedMedia(
                185874,
                "BLEACH: Sennen Kessen-hen - Kashin-tan",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx185874-aU3e6tBT6wwA.jpg",
                88,
                10
            ),
            SeedMedia(
                182205,
                "Tensei Shitara Slime Datta Ken 4th Season",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx182205-q2AeO1owuQbO.jpg",
                82,
                null
            ),
            SeedMedia(
                187538,
                "BLACK TORCH",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx187538-fXVXKYUA3VV6.jpg",
                71,
                null
            ),
            SeedMedia(
                21,
                "ONE PIECE",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21-ELSYx3yMPcKM.jpg",
                87,
                null
            ),
            SeedMedia(
                171110,
                "Honzuki no Gekokujou: Ryoushu no Youjo",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx171110-7zOdInS6DQNL.jpg",
                76,
                24
            ),
            SeedMedia(
                209983,
                "Hell Mode: Yarikomi-zuki no Gamer wa Haisettei no Isekai de Musou Suru 2nd Season",
                "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx209983-sFOcKyqMufxb.jpg",
                72,
                13
            ),
        )

        val PROVIDERS: List<Pair<String, AniSyncWidgetProvider<*>>> = listOf(
            "up-next" to UpNextWidgetProvider(),
            "airing-today" to AiringTodayWidgetProvider(),
            "weekly-calendar" to WeeklyCalendarWidgetProvider(),
            "trending" to TrendingWidgetProvider(),
            "watch-progress" to WatchProgressWidgetProvider(),
        )
    }
}
