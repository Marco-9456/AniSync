package com.anisync.android.data

import android.content.Context
import com.anisync.android.domain.SponsorList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SponsorsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val remoteUrl =
        "https://raw.githubusercontent.com/Marco-9456/AniSync/main/app/src/main/assets/sponsors.json"

    suspend fun loadBundled(): SponsorList = withContext(Dispatchers.IO) {
        context.assets.open("sponsors.json").use { stream ->
            json.decodeFromString(stream.bufferedReader().readText())
        }
    }

    suspend fun refreshFromRemote(): Result<SponsorList> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
            }
            try {
                if (conn.responseCode !in 200..299) {
                    error("HTTP ${conn.responseCode}")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<SponsorList>(body)
            } finally {
                conn.disconnect()
            }
        }
    }
}
