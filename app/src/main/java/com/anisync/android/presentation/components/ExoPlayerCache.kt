package com.anisync.android.presentation.components

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.anisync.android.data.media.MediaHttp

/**
 * CompositionLocal providing an [ExoPlayerCache] to composables in the tree.
 *
 * The default value is `null`, meaning no caching — the [VideoPlayer] will create
 * and release its own ExoPlayer instance (original behavior).
 */
val LocalExoPlayerCache = compositionLocalOf<ExoPlayerCache?> { null }

/**
 * A scoped cache of [ExoPlayer] instances keyed by video URL.
 */
class ExoPlayerCache internal constructor(private val context: Context) {

    private val maxSize = 6

    private val players = object : LinkedHashMap<String, ExoPlayer>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ExoPlayer>?): Boolean {
            val evict = size > maxSize
            if (evict) {
                eldest?.value?.release()
                Log.d(
                    "PerfMetrics",
                    "ExoPlayer evicted & released. Cache limit ($maxSize) exceeded."
                )
            }
            return evict
        }
    }

    /**
     * Returns an existing [ExoPlayer] for [url] if one is cached, or creates and
     * caches a new one.
     */
    fun getOrCreate(url: String): ExoPlayer {
        return players.getOrPut(url) {
            val start = System.currentTimeMillis()
            buildVideoExoPlayer(context, url).also {
                val initTime = System.currentTimeMillis() - start
                Log.d("PerfMetrics", "New ExoPlayer cached for $url in ${initTime}ms")
            }
        }
    }

    /** Releases all cached players and clears the cache. */
    fun releaseAll() {
        players.values.forEach { it.release() }
        players.clear()
        Log.d("PerfMetrics", "All cached ExoPlayers released.")
    }
}

/**
 * Builds a prepared, muted, looping [ExoPlayer] for an inline video [url].
 *
 * Single source of truth so the cached ([ExoPlayerCache]) and self-managed ([VideoPlayer])
 * paths can't drift. Two things make user-embedded clips actually play instead of erroring:
 *  - **OkHttp data source with a browser UA** ([MediaHttp.videoClient]). Hosts like catbox
 *    reject ExoPlayer's default User-Agent and answer 403, which surfaces as a "broken"
 *    video that plays fine in a browser. OkHttp also transparently follows the
 *    cross-protocol (http↔https) redirects these CDNs use — the platform
 *    `DefaultHttpDataSource` refuses those by default.
 *  - **Eager [ExoPlayer.prepare]** so buffering starts the instant the player is created
 *    (i.e. when scrolled near), not when its surface finally attaches.
 */
@OptIn(UnstableApi::class)
internal fun buildVideoExoPlayer(context: Context, url: String): ExoPlayer {
    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            1500, // minBufferMs: minimum buffered before playback starts
            5000, // maxBufferMs: maximum buffered
            500,  // bufferForPlaybackMs
            1500  // bufferForPlaybackAfterRebufferMs
        )
        // 4 MB cap holds a typical short AniList clip in full, so the REPEAT_MODE_ONE loop
        // replays from memory instead of re-downloading every pass, while bounding total
        // memory across the (max 6) cached players.
        .setTargetBufferBytes(4 * 1024 * 1024)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    val dataSourceFactory = OkHttpDataSource.Factory(MediaHttp.videoClient)
        .setUserAgent(MediaHttp.USER_AGENT)

    return ExoPlayer.Builder(context)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = false
            prepare()
        }
}

/**
 * Creates and remembers an [ExoPlayerCache] scoped to the calling composable.
 */
@Composable
fun rememberExoPlayerCache(): ExoPlayerCache {
    val context = LocalContext.current.applicationContext
    val cache = remember { ExoPlayerCache(context) }

    DisposableEffect(Unit) {
        onDispose { cache.releaseAll() }
    }

    return cache
}