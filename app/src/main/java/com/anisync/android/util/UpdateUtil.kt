package com.anisync.android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.anisync.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

object UpdateUtil {
    private const val REPO_OWNER = "Marco-9456"
    private const val REPO_NAME = "AniSync"
    
    data class Release(
        val tagName: String,
        val prerelease: Boolean,
        val body: String,
        val downloadUrl: String
    )
    
    sealed class DownloadStatus {
        object NotYet : DownloadStatus()
        data class Progress(val percent: Int) : DownloadStatus()
        data class Finished(val file: File) : DownloadStatus()
    }

    suspend fun checkForUpdate(allowPrerelease: Boolean): Release? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val releases = JSONArray(response)
                
                var latestRelease: Release? = null
                var latestVersionCode = versionToCode(BuildConfig.VERSION_NAME)
                
                for (i in 0 until releases.length()) {
                    val releaseJson = releases.getJSONObject(i)
                    val isPrerelease = releaseJson.getBoolean("prerelease")
                    if (isPrerelease && !allowPrerelease) continue
                    
                    val tagName = releaseJson.getString("tag_name")
                    val versionCode = versionToCode(tagName)
                    
                    if (versionCode > latestVersionCode) {
                        val assets = releaseJson.getJSONArray("assets")
                        var downloadUrl = ""
                        for (j in 0 until assets.length()) {
                            val asset = assets.getJSONObject(j)
                            if (asset.getString("name").endsWith(".apk")) {
                                downloadUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                        
                        if (downloadUrl.isNotEmpty()) {
                            latestVersionCode = versionCode
                            latestRelease = Release(
                                tagName = tagName,
                                prerelease = isPrerelease,
                                body = releaseJson.getString("body"),
                                downloadUrl = downloadUrl
                            )
                        }
                    }
                }
                return@withContext latestRelease
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun downloadApk(context: Context, release: Release): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Progress(0))
        val apkFile = File(context.getExternalFilesDir("apk"), "latest.apk")
        var deleteFile = true
        
        try {
            val url = URL(release.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            
            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = apkFile.outputStream()
            
            val data = ByteArray(8192)
            var total = 0L
            var count: Int
            
            while (input.read(data).also { count = it } != -1) {
                total += count
                output.write(data, 0, count)
                if (fileLength > 0) {
                    emit(DownloadStatus.Progress((total * 100 / fileLength).toInt()))
                }
            }
            output.flush()
            output.close()
            input.close()
            deleteFile = false
            emit(DownloadStatus.Finished(apkFile))
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            if (deleteFile && apkFile.exists()) {
                apkFile.delete()
            }
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(context: Context) {
        try {
            val apkFile = File(context.getExternalFilesDir("apk"), "latest.apk")
            if (!apkFile.exists()) return
            
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
            e.printStackTrace()
        }
    }

    private fun versionToCode(version: String): Int {
        val cleanVersion = version.replace(Regex("[^0-9.]"), "")
        val parts = cleanVersion.split(".")
        var code = 0
        for (i in 0 until max(3, parts.size)) {
            val part = parts.getOrNull(i)?.toIntOrNull() ?: 0
            code = code * 1000 + part
        }
        return code
    }
}
