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
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Renders every widget at every size it declares and writes the result to a PNG.
 *
 * Not an assertion suite. It exists so a layout change can be looked at rather than reasoned about,
 * which is the part of widget work no unit test replaces. Output:
 *
 *     adb pull /data/local/tmp/widget-shots
 *
 * It does fail loudly on a layout that cannot be inflated, because `RemoteViews.apply` throws on
 * any view type the framework does not allow in a widget. That failure otherwise shows up as a
 * blank widget on someone's home screen and nowhere else.
 *
 * [AniSyncWidgetProvider.build] is called per size rather than going through a bound host. A
 * published widget carries one `RemoteViews` per declared size and the host picks between them, but
 * `RemoteViews.apply` with no size hint always resolves to the smallest, so screenshots taken that
 * way would all be pictures of the compact layout.
 */
@RunWith(AndroidJUnit4::class)
class WidgetScreenshotTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun renderEveryWidgetAtEverySize() {
        seed()
        val outDir = File(context.getExternalFilesDir(null), "widget-shots").apply {
            deleteRecursively()
            mkdirs()
        }

        PROVIDERS.forEach { (name, provider) -> capture(name, provider, outDir) }

        // connectedAndroidTest uninstalls the app when the run finishes, which takes its external
        // files dir with it. Copying to /data/local/tmp is what makes the output outlive the run.
        shell("rm -rf $PULL_DIR")
        shell("mkdir -p $PULL_DIR")
        shell("cp ${outDir.absolutePath}/. $PULL_DIR -r")
    }

    /**
     * Writes representative rows straight into Room.
     *
     * Without this the screenshots depend on whatever the device happens to have cached, and
     * `connectedAndroidTest` uninstalls the app after every run, so in practice that is nothing and
     * every widget renders its empty state. Seeding makes the output the same on any machine.
     *
     * Safe because the run ends with the app uninstalled and its database deleted.
     */
    private fun seed() = runBlocking {
        val deps = context.widgetDeps()
        val owner = deps.activeOwnerId()
        val start = WidgetTime.startOfToday()

        deps.airingScheduleDao().insertAll(
            SEED_MEDIA.mapIndexed { index, media ->
                AiringScheduleEntity(
                    id = 900_000 + index,
                    ownerId = owner,
                    mediaId = media.id,
                    // Spread across the day so the time column shows a range.
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
                val total = media.episodes ?: (12 + index * 4)
                LibraryEntryEntity(
                    id = 700_000 + index,
                    ownerId = owner,
                    mediaId = media.id,
                    titleRomaji = media.title,
                    titleEnglish = media.title,
                    titleNative = media.title,
                    titleUserPreferred = media.title,
                    coverUrl = media.cover,
                    // Close to the end, which is what Watch Progress is meant to surface.
                    progress = (total - (index + 1)).coerceAtLeast(0),
                    totalEpisodes = total,
                    totalChapters = null,
                    totalVolumes = null,
                    mediaType = MediaType.ANIME,
                    status = LibraryStatus.CURRENT,
                    nextAiringEpisode = total,
                    timeUntilAiring = 3600,
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

        provider.declaredSizes.forEach { size ->
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
     * Draws the list rows in place of the `ListView`.
     *
     * A `ListView` fed by `RemoteCollectionItems` only populates once a widget host has bound it,
     * so outside a host it renders as an empty box and the screenshots would show a widget with no
     * content. Swapping in a plain column of the same row `RemoteViews` shows the rows the host
     * would show. It cannot scroll, which for a still image is not a difference.
     */
    private fun substituteListRows(root: View, listViewId: Int, rows: List<RemoteViews>) {
        if (listViewId == 0 || rows.isEmpty()) return
        val list = root.findViewById<View>(listViewId) ?: return
        val parent = list.parent as? ViewGroup ?: return

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = list.layoutParams
        }
        rows.forEach { column.addView(it.apply(context, column)) }

        val index = parent.indexOfChild(list)
        parent.removeView(list)
        parent.addView(column, index)
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
            .let { ParcelFileDescriptor.AutoCloseInputStream(it) }
            .use { it.readBytes().decodeToString() }
            .trim()

    private companion object {
        /** No widget is bound here, so nothing in `build` may touch AppWidgetManager with this id. */
        const val FAKE_ID = 1

        /** Survives the post-run uninstall. Pull with `adb pull /data/local/tmp/widget-shots`. */
        const val PULL_DIR = "/data/local/tmp/widget-shots"

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
