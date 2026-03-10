package com.anisync.android.data.update

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.anisync.android.BuildConfig
import com.anisync.android.data.update.UpdateManager.Companion.VERSION_SEGMENT_COUNT
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the full app update lifecycle: checking for updates via the GitHub Releases API,
 * downloading APKs, and triggering installation.
 *
 * Exposes [updateState] as a [StateFlow] so both the settings screen and the main activity
 * can observe the same state without duplication.
 *
 * The download runs in its own [CoroutineScope] so it survives navigation changes
 * (e.g., the user leaving the Updates screen mid-download).
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "UpdateManager"
        private const val REPO_OWNER = "Marco-9456"
        private const val REPO_NAME = "AniSync"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val DOWNLOAD_READ_TIMEOUT_MS = 60_000
        private const val DOWNLOAD_BUFFER_SIZE = 8192
        private const val VERSION_SEGMENT_COUNT = 3
        private const val VERSION_SEGMENT_MULTIPLIER = 1000
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var downloadJob: Job? = null

    // =========================================================================
    // Check for update
    // =========================================================================

    /**
     * Queries the GitHub Releases API and returns a typed result.
     * Also updates [updateState] so observers (dialogs) react immediately.
     *
     * @param allowPrerelease Whether to include pre-release tags in the comparison.
     * @return [UpdateCheckResult] indicating the outcome.
     */
    suspend fun checkForUpdate(allowPrerelease: Boolean): UpdateCheckResult {
        _updateState.value = UpdateState.Checking
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases"
                )
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                }

                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        _updateState.value = UpdateState.Idle
                        return@withContext UpdateCheckResult.Error(
                            Exception("GitHub API returned HTTP ${connection.responseCode}")
                        )
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val releases = json.parseToJsonElement(response).jsonArray
                    val currentVersionCode = versionToCode(BuildConfig.VERSION_NAME)

                    var latestRelease: Release? = null
                    var latestVersionCode = currentVersionCode

                    for (element in releases) {
                        val releaseJson = element.jsonObject
                        val isPrerelease =
                            releaseJson["prerelease"]?.jsonPrimitive?.boolean ?: false
                        if (isPrerelease && !allowPrerelease) continue

                        val tagName =
                            releaseJson["tag_name"]?.jsonPrimitive?.content ?: continue
                        val versionCode = versionToCode(tagName)

                        if (versionCode > latestVersionCode) {
                            val assets = releaseJson["assets"]?.jsonArray ?: continue
                            val downloadUrl = assets
                                .mapNotNull { asset ->
                                    val obj = asset.jsonObject
                                    val name =
                                        obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                                    if (name.endsWith(".apk")) {
                                        obj["browser_download_url"]?.jsonPrimitive?.content
                                    } else null
                                }
                                .firstOrNull()

                            if (!downloadUrl.isNullOrEmpty()) {
                                latestVersionCode = versionCode
                                latestRelease = Release(
                                    tagName = tagName,
                                    prerelease = isPrerelease,
                                    body = releaseJson["body"]?.jsonPrimitive?.content ?: "",
                                    downloadUrl = downloadUrl
                                )
                            }
                        }
                    }

                    if (latestRelease != null) {
                        _updateState.value = UpdateState.UpdateAvailable(latestRelease)
                        UpdateCheckResult.Available(latestRelease)
                    } else {
                        _updateState.value = UpdateState.Idle
                        UpdateCheckResult.UpToDate
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for updates", e)
                _updateState.value = UpdateState.Idle
                UpdateCheckResult.Error(e)
            }
        }
    }

    // =========================================================================
    // Download
    // =========================================================================

    /**
     * Starts downloading the APK for [release] in the background.
     * Progress and completion are reflected in [updateState].
     *
     * Downloads to a temporary file first, then atomically renames to `latest.apk`
     * on success so a partial download can never be mistaken for a valid APK.
     *
     * @param onError Callback invoked on the Main dispatcher if the download fails.
     */
    fun startDownload(release: Release, onError: (Exception) -> Unit = {}) {
        if (downloadJob?.isActive == true) return

        downloadJob = scope.launch {
            _updateState.value = UpdateState.Downloading(release, 0)
            try {
                val finalFile = withContext(Dispatchers.IO) {
                    val apkDir = context.getExternalFilesDir("apk")
                        ?: throw Exception("External files directory unavailable")
                    val tempFile = File(apkDir, "latest.apk.tmp")
                    val targetFile = File(apkDir, "latest.apk")

                    try {
                        val url = URL(release.downloadUrl)
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = CONNECT_TIMEOUT_MS
                            readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                            instanceFollowRedirects = true
                        }

                        connection.inputStream.use { input ->
                            tempFile.outputStream().use { output ->
                                val fileLength = connection.contentLength.toLong()
                                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                                var totalBytesRead = 0L
                                var bytesRead: Int

                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    ensureActive()
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                    if (fileLength > 0) {
                                        val progress =
                                            (totalBytesRead * 100 / fileLength).toInt()
                                                .coerceIn(0, 100)
                                        _updateState.value =
                                            UpdateState.Downloading(release, progress)
                                    }
                                }
                                output.flush()
                            }
                        }

                        // Atomic rename: delete old file first, then rename temp
                        if (targetFile.exists()) targetFile.delete()
                        if (!tempFile.renameTo(targetFile)) {
                            throw Exception("Failed to finalize downloaded APK")
                        }
                        targetFile
                    } catch (e: Exception) {
                        // Clean up temp file on any failure
                        tempFile.delete()
                        throw e
                    }
                }

                _updateState.value = UpdateState.ReadyToInstall(release, finalFile)
            } catch (e: CancellationException) {
                Log.i(TAG, "Download cancelled")
                _updateState.value = UpdateState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                // Revert to UpdateAvailable so the user can retry
                _updateState.value = UpdateState.UpdateAvailable(release)
                onError(e)
            }
        }
    }

    /**
     * Cancels an in-progress download and resets state to Idle.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    // =========================================================================
    // Install
    // =========================================================================

    /**
     * Launches the system package installer for the downloaded APK.
     * Uses [FileProvider] to grant the installer read access.
     */
    fun installApk() {
        try {
            val apkFile = File(context.getExternalFilesDir("apk"), "latest.apk")
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file not found at ${apkFile.absolutePath}")
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer", e)
        }
    }

    // =========================================================================
    // State management
    // =========================================================================

    /**
     * Dismisses the current update dialog / state.
     * Cannot dismiss while a download is actively in progress;
     * call [cancelDownload] first.
     */
    fun dismissUpdate() {
        val currentState = _updateState.value
        if (currentState is UpdateState.Downloading) return
        downloadJob?.cancel()
        downloadJob = null
        _updateState.value = UpdateState.Idle
    }

    /**
     * Fetches the actual latest release from GitHub (debug builds only).
     * Bypasses version comparison so the update dialog can be tested with real release notes.
     */
    suspend fun fetchLatestRelease(allowPrerelease: Boolean): UpdateCheckResult {
        _updateState.value = UpdateState.Checking
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases"
                )
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                }

                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        _updateState.value = UpdateState.Idle
                        return@withContext UpdateCheckResult.Error(
                            Exception("GitHub API returned HTTP ${connection.responseCode}")
                        )
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val releases = json.parseToJsonElement(response).jsonArray

                    for (element in releases) {
                        val releaseJson = element.jsonObject
                        val isPrerelease =
                            releaseJson["prerelease"]?.jsonPrimitive?.boolean ?: false
                        if (isPrerelease && !allowPrerelease) continue

                        val tagName =
                            releaseJson["tag_name"]?.jsonPrimitive?.content ?: continue
                        val assets = releaseJson["assets"]?.jsonArray ?: continue
                        val downloadUrl = assets
                            .mapNotNull { asset ->
                                val obj = asset.jsonObject
                                val name =
                                    obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                                if (name.endsWith(".apk")) {
                                    obj["browser_download_url"]?.jsonPrimitive?.content
                                } else null
                            }
                            .firstOrNull()

                        if (!downloadUrl.isNullOrEmpty()) {
                            val release = Release(
                                tagName = tagName,
                                prerelease = isPrerelease,
                                body = releaseJson["body"]?.jsonPrimitive?.content ?: "",
                                downloadUrl = downloadUrl
                            )
                            _updateState.value = UpdateState.UpdateAvailable(release)
                            return@withContext UpdateCheckResult.Available(release)
                        }
                    }

                    _updateState.value = UpdateState.Idle
                    UpdateCheckResult.UpToDate
                } finally {
                    connection.disconnect()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch latest release", e)
                _updateState.value = UpdateState.Idle
                UpdateCheckResult.Error(e)
            }
        }
    }

    // =========================================================================
    // Version comparison
    // =========================================================================

    /**
     * Converts a semver-like version string to a comparable integer code.
     * Always uses exactly [VERSION_SEGMENT_COUNT] segments for consistent comparison.
     *
     * Examples:
     * - "1.0.1" -> 1_000_001
     * - "v2.3.0" -> 2_003_000
     * - "1.0" -> 1_000_000
     */
    internal fun versionToCode(version: String): Int {
        val cleanVersion = version.replace(Regex("[^0-9.]"), "")
        val parts = cleanVersion.split(".")
        var code = 0
        for (i in 0 until VERSION_SEGMENT_COUNT) {
            val part = parts.getOrNull(i)?.toIntOrNull() ?: 0
            code = code * VERSION_SEGMENT_MULTIPLIER + part
        }
        return code
    }
}
