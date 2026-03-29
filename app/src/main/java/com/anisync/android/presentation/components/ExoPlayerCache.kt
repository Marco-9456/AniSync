package com.anisync.android.presentation.components

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * CompositionLocal providing an [ExoPlayerCache] to composables in the tree.
 *
 * The default value is `null`, meaning no caching — the [VideoPlayer] will create
 * and release its own ExoPlayer instance (original behavior).
 *
 * To enable caching, call [rememberExoPlayerCache] at the screen level and
 * provide it via [CompositionLocalProvider]:
 * ```kotlin
 * val playerCache = rememberExoPlayerCache()
 * CompositionLocalProvider(LocalExoPlayerCache provides playerCache) {
 *     LazyColumn { ... }
 * }
 * ```
 */
val LocalExoPlayerCache = compositionLocalOf<ExoPlayerCache?> { null }

/**
 * A scoped cache of [ExoPlayer] instances keyed by video URL.
 *
 * **Problem**: `LazyColumn` tears down composables when items scroll off-screen,
 * releasing the ExoPlayer and forcing a full re-fetch when the user scrolls back.
 *
 * **Solution**: This cache lives at the parent composable scope (outside the
 * LazyColumn item). Players are created on first access and reused on subsequent
 * accesses. When the parent scope is disposed (e.g., user leaves the screen),
 * all players are released.
 */
class ExoPlayerCache internal constructor(private val context: Context) {
    private val players = mutableMapOf<String, ExoPlayer>()

    /**
     * Returns an existing [ExoPlayer] for [url] if one is cached, or creates, prepares,
     * and caches a new one. Reused players retain their buffered data and playback position.
     */
    @OptIn(UnstableApi::class)
    fun getOrCreate(url: String): ExoPlayer {
        return players.getOrPut(url) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f          // Default muted (consistent with VideoPlayer)
                playWhenReady = false
                prepare()
            }
        }
    }

    /** Releases all cached players and clears the cache. */
    fun releaseAll() {
        players.values.forEach { it.release() }
        players.clear()
    }
}

/**
 * Creates and remembers an [ExoPlayerCache] scoped to the calling composable.
 * All cached players are automatically released when the composable leaves composition
 * (e.g., when the user navigates away from the screen).
 *
 * Should be called **once** at the screen level and provided via [LocalExoPlayerCache].
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
