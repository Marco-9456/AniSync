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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer

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
    @OptIn(UnstableApi::class)
    fun getOrCreate(url: String): ExoPlayer {
        return players.getOrPut(url) {
            val start = System.currentTimeMillis()
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    1500, // minBufferMs: Minimum audio/video to buffer before starting playback
                    5000, // maxBufferMs: Maximum audio/video to buffer
                    500,  // bufferForPlaybackMs
                    1500  // bufferForPlaybackAfterRebufferMs
                )
                .setTargetBufferBytes(2 * 1024 * 1024) // 2 MB strict memory limit
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build().apply {
                    setMediaItem(MediaItem.fromUri(url))
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = 0f
                    playWhenReady = false

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