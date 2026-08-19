package com.anisync.android

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Process
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import com.anisync.android.data.AppSettings
import com.anisync.android.presentation.components.ExoPlayerCache
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

    @Inject
    lateinit var updateManager: com.anisync.android.data.update.UpdateManager

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

            // An update APK is only wanted between its download finishing and the install tap,
            // and the app is not restarted in between. Anything still here is dead weight.
            updateManager.cleanUpDownloads()
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
        // Six hourly, not hourly. The query window is bucketed to the start of the day, so an
        // hourly run refetched three pages of near-identical data twenty four times over, and the
        // countdown users actually see is computed locally from each episode's airingAt. Episode
        // notifications do not read this table, they fetch their own.
        val airingRequest =
            PeriodicWorkRequestBuilder<com.anisync.android.worker.AiringScheduleWorker>(
                6, TimeUnit.HOURS
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

        // Nothing evicts the normalized cache on its own.
        com.anisync.android.worker.CacheMaintenanceWorker.schedule(this@AniSyncApplication)
    }

    /**
     * Hands memory back when the system asks for it. Nothing used to listen, so the two largest
     * in-memory holdings, decoded bitmaps and prepared players, survived every trim until the
     * process was killed outright.
     *
     * Bitmaps go first because they are pure cache and cost only a re-decode. Players are only
     * released once the app is off screen or the pressure is severe, since dropping them mid-scroll
     * would restart playback the user is watching.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        // Matched on the documented states rather than compared numerically, because the
        // constants are not ordered by severity: BACKGROUND is 40 while RUNNING_CRITICAL is 15.
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> imageLoader.memoryCache?.clear()

            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                imageLoader.memoryCache?.clear()
                ExoPlayerCache.releaseAllCaches()
            }
        }
    }
}
