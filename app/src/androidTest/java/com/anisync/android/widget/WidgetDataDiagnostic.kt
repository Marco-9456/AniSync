package com.anisync.android.widget

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.widget.core.WidgetTime
import com.anisync.android.widget.core.activeOwnerId
import com.anisync.android.widget.core.widgetDeps
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reports what the widgets can actually see in Room.
 *
 * Run this against a device that has been signed in and had the app opened, to tell an empty
 * widget caused by an empty table apart from one caused by rows the widget cannot reach. The two
 * look identical on the home screen and have completely different fixes.
 *
 *     adb logcat -s WidgetData
 */
@RunWith(AndroidJUnit4::class)
class WidgetDataDiagnostic {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    // runBlocking<Unit> is deliberate: the block ends on Log.i, which returns Int on Android, so
    // an inferred return type makes this a non-void method and JUnit refuses to run the class.
    @Test
    fun reportWhatTheWidgetsCanSee() = runBlocking<Unit> {
        val deps = context.widgetDeps()
        val owner = deps.activeOwnerId()
        val start = WidgetTime.startOfToday()
        val weekEnd = start + 7 * WidgetTime.DAY

        Log.i(TAG, "activeOwnerId = $owner")

        // Raw SQLite, not the scoped DAOs, so rows belonging to another owner are still visible.
        // That difference is the whole point of the check.
        val raw = context.openOrCreateDatabase("anisync.db", Context.MODE_PRIVATE, null)
        raw.rawQuery("SELECT ownerId, COUNT(*) FROM airing_schedule GROUP BY ownerId", null)
            .use { cursor ->
                if (cursor.count == 0) Log.i(TAG, "airing_schedule is EMPTY")
                while (cursor.moveToNext()) {
                    Log.i(TAG, "airing_schedule ownerId=${cursor.getInt(0)} rows=${cursor.getInt(1)}")
                }
            }
        raw.rawQuery(
            "SELECT COUNT(*) FROM airing_schedule WHERE airingAt >= ? AND airingAt <= ?",
            arrayOf(start.toString(), weekEnd.toString())
        ).use { cursor ->
            cursor.moveToFirst()
            Log.i(TAG, "airing_schedule rows in the next 7 days, any owner = ${cursor.getInt(0)}")
        }
        raw.rawQuery("SELECT ownerId, COUNT(*) FROM library_entries GROUP BY ownerId", null)
            .use { cursor ->
                if (cursor.count == 0) Log.i(TAG, "library_entries is EMPTY")
                while (cursor.moveToNext()) {
                    Log.i(TAG, "library_entries ownerId=${cursor.getInt(0)} rows=${cursor.getInt(1)}")
                }
            }
        raw.rawQuery("SELECT COUNT(*) FROM trending_media", null).use { cursor ->
            cursor.moveToFirst()
            Log.i(TAG, "trending_media rows = ${cursor.getInt(0)}")
        }
        raw.close()

        // Now the same questions through the DAOs the widgets actually call.
        val dao = deps.airingScheduleDao()
        Log.i(TAG, "widget sees today (all)  = ${dao.getAiringBetween(owner, start, start + WidgetTime.DAY).size}")
        Log.i(TAG, "widget sees today (mine) = ${dao.getAiringBetweenForUser(owner, start, start + WidgetTime.DAY).size}")
        Log.i(TAG, "widget sees week (all)   = ${dao.getAiringBetween(owner, start, weekEnd).size}")
        Log.i(TAG, "widget sees trending     = ${deps.trendingDao().getTopTrending(10).size}")
    }

    private companion object {
        const val TAG = "WidgetData"
    }
}
