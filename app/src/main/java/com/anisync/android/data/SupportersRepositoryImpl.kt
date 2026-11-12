package com.anisync.android.data

import android.content.Context
import com.anisync.android.BuildConfig
import com.anisync.android.domain.Supporter
import com.anisync.android.domain.SupportersRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The supporters list. Every build bundles it, and the Support screen refreshes it from main so a
 * new name shows up without waiting for a release.
 *
 * The fetched copy is kept under the running build's version code. A newer build bundles a newer
 * list than anything an older one fetched, so a copy left by another build is ignored and swept.
 */
@Singleton
class SupportersRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SupportersRepository {

    private val supporters = MutableStateFlow<List<Supporter>?>(null)
    private val refreshLock = Mutex()

    override fun observeSupporters(): Flow<List<Supporter>> =
        supporters.onStart { loadLocal() }.filterNotNull()

    override suspend fun refreshSupporters() {
        refreshLock.withLock { withContext(Dispatchers.IO) { refreshNow() } }
    }

    private fun refreshNow() {
        val cache = cacheFile()
        val age = System.currentTimeMillis() - cache.lastModified()
        if (cache.isFile && age in 0 until REFRESH_INTERVAL_MS) return
        val body = download() ?: return
        val list = SupportersJson.parse(body) ?: return
        runCatching {
            cache.writeAtomically(body)
            sweepOtherBuilds(cache)
        }
        supporters.value = list
    }

    private suspend fun loadLocal() {
        if (supporters.value != null) return
        val list = withContext(Dispatchers.IO) { readCache() ?: readBundled() }
        supporters.compareAndSet(null, list)
    }

    private fun readCache(): List<Supporter>? {
        val cache = cacheFile()
        if (!cache.isFile) return null
        return runCatching { SupportersJson.parse(cache.readText()) }.getOrNull()
    }

    private fun readBundled(): List<Supporter> = runCatching {
        context.assets.open(FILE_NAME).bufferedReader().use { SupportersJson.parse(it.readText()) }
    }.getOrNull().orEmpty()

    private fun download(): String? {
        val connection = try {
            URL(REMOTE_URL).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            return null
        }
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.use { it.readCapped(MAX_BYTES) }?.toString(Charsets.UTF_8)
        } catch (e: IOException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile() = File(context.cacheDir, "$CACHE_PREFIX${BuildConfig.VERSION_CODE}.json")

    private fun sweepOtherBuilds(current: File) {
        context.cacheDir
            .listFiles { file -> file.name.startsWith(CACHE_PREFIX) && file.name != current.name }
            ?.forEach { it.delete() }
    }

    private fun File.writeAtomically(text: String) {
        val temp = File(parentFile, "$name.tmp")
        temp.writeText(text)
        if (!temp.renameTo(this)) temp.delete()
    }

    /** The whole stream, or null once it runs past [limit] bytes. */
    private fun InputStream.readCapped(limit: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = read(buffer)
            if (read < 0) return out.toByteArray()
            out.write(buffer, 0, read)
            if (out.size() > limit) return null
        }
    }

    private companion object {
        const val FILE_NAME = "supporters.json"
        const val REMOTE_URL =
            "https://raw.githubusercontent.com/Marco-9456/AniSync/main/app/src/main/assets/$FILE_NAME"
        const val CACHE_PREFIX = "supporters-v"
        const val TIMEOUT_MS = 10_000
        const val MAX_BYTES = 1 shl 20
        val REFRESH_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)
    }
}
