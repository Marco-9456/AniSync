package com.anisync.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import androidx.core.util.SizeFCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.widget.core.AniSyncWidgetProvider
import com.anisync.android.widget.core.WidgetImageLoader
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

        provider.declaredSizes.forEach { size ->
            val views = provider.build(context, FAKE_ID, size, snapshot, covers)
            val file = File(outDir, "$name-${size.width.toInt()}x${size.height.toInt()}.png")
            writePng(views, size, file)
        }
    }

    private fun writePng(views: RemoteViews, size: SizeFCompat, file: File) {
        val density = context.resources.displayMetrics.density
        val widthPx = (size.width * density).toInt()
        val heightPx = (size.height * density).toInt()

        val inflated: View = views.apply(context, FrameLayout(context))
        inflated.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        inflated.layout(0, 0, widthPx, heightPx)

        val bitmap = createBitmap(widthPx, heightPx)
        inflated.draw(Canvas(bitmap))
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
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

        val PROVIDERS: List<Pair<String, AniSyncWidgetProvider<*>>> = listOf(
            "up-next" to UpNextWidgetProvider(),
            "airing-today" to AiringTodayWidgetProvider(),
            "weekly-calendar" to WeeklyCalendarWidgetProvider(),
            "trending" to TrendingWidgetProvider(),
            "watch-progress" to WatchProgressWidgetProvider(),
        )
    }
}
