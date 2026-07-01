package com.anisync.android

import android.app.Application
import android.os.Process
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import com.anisync.android.data.AppSettings
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

@HiltAndroidApp
class AniSyncApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var userOptionsSyncManager: com.anisync.android.data.UserOptionsSyncManager

    @Inject
    lateinit var appLockManager: com.anisync.android.data.security.AppLockManager

    private val applicationScope = CoroutineScope(
        SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
            Log.e("AniSyncApp", "Unhandled coroutine exception", throwable)
        } + Dispatchers.Default
    )

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()

        if (currentProcessName().endsWith(":crash")) return

        // Apply the saved light/dark choice to the resource configuration up front (before any window
        // is drawn) so the cold-start splash + window background follow the in-app theme, not only the
        // system setting. Read directly from prefs here — Hilt-injected AppSettings isn't needed this
        // early, and this keeps it off the critical path of building the graph. See issue #84.
        AppCompatDelegate.setDefaultNightMode(AppSettings.persistedNightMode(this))

        installCrashHandler()

        // App lock: observe app-level background/foreground so the lock re-arms when AniSync is sent
        // to the background. Registered here (main thread, once) rather than per-activity.
        appLockManager.bindTo(androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle)

        val mainThreadTime = measureTimeMillis {
            com.anisync.android.worker.NotificationChannels.createChannels(this)
        }
        Log.d("PerfMetrics", "Main thread AppInit took $mainThreadTime ms")

        // Prime the WebView/Chromium provider early (posted, off the cold-start critical path) so
        // the first SVG-heavy bio/activity doesn't pay the WebView init stall on screen entry.
        com.anisync.android.presentation.components.WebViewWarmer.warmUp(this)

        applicationScope.launch {
            val backgroundInitTime = measureTimeMillis {
                scheduleWorkersBackground()
            }
            Log.d("PerfMetrics", "Background Worker scheduling took $backgroundInitTime ms")
        }

        // Keep AniList account options (adult-content, languages, score format, …) in sync with the
        // web so the app respects them. Off the cold-start critical path.
        userOptionsSyncManager.start(applicationScope)
    }

    private fun currentProcessName(): String = try {
        java.io.File("/proc/self/cmdline").readText().trim('\u0000').trim()
    } catch (_: Throwable) {
        ""
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is OutOfMemoryError) {
                Log.e("AniSyncCrash", "Out of memory — skipping crash report UI")
                previous?.uncaughtException(thread, throwable)
                Process.killProcess(Process.myPid())
                exitProcess(10)
                return@setDefaultUncaughtExceptionHandler
            }
            try {
                Log.e("AniSyncCrash", "Uncaught exception on ${thread.name}", throwable)
                val intent = CrashReportActivity.newIntent(this, throwable)
                startActivity(intent)
            } catch (t: Throwable) {
                Log.e("AniSyncCrash", "Failed to launch CrashReportActivity", t)
                previous?.uncaughtException(thread, throwable)
            } finally {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    private fun scheduleWorkersBackground() {
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workManager = WorkManager.getInstance(this@AniSyncApplication)

        // Schedule Airing Updates
        val airingRequest =
            PeriodicWorkRequestBuilder<com.anisync.android.worker.AiringScheduleWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(networkConstraints)
                .build()

        workManager.enqueueUniquePeriodicWork(
            "AiringScheduleWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            airingRequest
        )

        com.anisync.android.worker.AiringScheduleWorker.enqueueImmediate(this@AniSyncApplication)

        // Schedule Trending Worker
        val trendingRequest = PeriodicWorkRequestBuilder<com.anisync.android.worker.TrendingWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "TrendingWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            trendingRequest
        )

        // Schedule Widget Refresh
        com.anisync.android.worker.WidgetRefreshWorker.schedule(this@AniSyncApplication)

        // Schedule periodic update check (every 6 hours, requires network).
        // The worker itself checks whether auto-update is enabled, so the work
        // is always enqueued but becomes a no-op when the feature is off.
        val updateCheckRequest =
            PeriodicWorkRequestBuilder<com.anisync.android.worker.UpdateCheckWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(networkConstraints)
                .build()

        workManager.enqueueUniquePeriodicWork(
            "UpdateCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            updateCheckRequest
        )
    }
}
