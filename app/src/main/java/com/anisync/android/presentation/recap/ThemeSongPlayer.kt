package com.anisync.android.presentation.recap

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.ExoPlayer
import com.anisync.android.presentation.components.buildVideoExoPlayer
import java.util.concurrent.atomic.AtomicInteger
import android.media.AudioAttributes as PlatformAudioAttributes

/**
 * The recap's song, looped behind the story.
 *
 * Muted, it neither asks for audio focus nor pauses whatever else is playing. Turning the sound on
 * asks for focus like any music player, so another app pauses or ducks and a call pauses the song.
 *
 * [onSettled] hears when the song starts playing or fails, so a story waiting for it can go on.
 */
class ThemeSongPlayer(context: Context, url: String, muted: Boolean, cache: Cache, onSettled: () -> Unit) {

    /** True while this player is in [Playing]. */
    private var counted = false

    private val player: ExoPlayer = buildVideoExoPlayer(context, url, muted = muted, loop = true, autoPlay = false, cache = cache).apply {
        setAudioAttributes(MusicAttributes, !muted)
        setHandleAudioBecomingNoisy(true)
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                count(isPlaying)
                if (isPlaying) onSettled()
            }

            override fun onPlayerError(error: PlaybackException) = onSettled()
        })
    }

    var muted: Boolean = muted
        private set

    fun play() {
        player.playWhenReady = true
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun setMuted(value: Boolean) {
        muted = value
        player.volume = if (value) 0f else 1f
        player.setAudioAttributes(MusicAttributes, !value)
    }

    fun restart() {
        player.seekTo(0)
    }

    fun release() {
        count(false)
        player.release()
    }

    private fun count(playing: Boolean) {
        if (playing == counted) return
        counted = playing
        if (playing) Playing.incrementAndGet() else Playing.decrementAndGet()
    }

    companion object {
        private val MusicAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        /** The app's own songs playing right now. The system counts a muted one as music too. */
        private val Playing = AtomicInteger()

        private val MusicUsages = setOf(
            PlatformAudioAttributes.USAGE_MEDIA,
            PlatformAudioAttributes.USAGE_GAME,
            PlatformAudioAttributes.USAGE_UNKNOWN,
        )

        /**
         * True while another app is playing music, when a story starts muted rather than talk over
         * it. Music playing while none of the app's own songs do is someone else's. Otherwise the
         * system's list of playing players is counted with the app's own songs taken off.
         */
        fun otherAudioPlaying(context: Context): Boolean {
            val audio = context.getSystemService(AudioManager::class.java) ?: return false
            if (!audio.isMusicActive) return false
            val own = Playing.get()
            if (own == 0) return true
            return audio.activePlaybackConfigurations.count { it.audioAttributes.usage in MusicUsages } > own
        }

        /** True on a metered connection, where songs wait for the "Use mobile data for songs" setting. */
        fun onMeteredNetwork(context: Context): Boolean =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.isActiveNetworkMetered != false
    }
}
