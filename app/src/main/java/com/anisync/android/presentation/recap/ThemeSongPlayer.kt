package com.anisync.android.presentation.recap

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.anisync.android.presentation.components.buildVideoExoPlayer

/**
 * The recap's song, looped behind the story.
 *
 * Muted, it neither asks for audio focus nor pauses whatever else is playing. Turning the sound on
 * asks for focus like any music player, so another app pauses or ducks and a call pauses the song.
 *
 * [onSettled] hears when the song starts playing or fails, so a story waiting for it can go on.
 */
class ThemeSongPlayer(context: Context, url: String, muted: Boolean, onSettled: () -> Unit) {

    private val player: ExoPlayer = buildVideoExoPlayer(context, url, muted = muted, loop = true, autoPlay = false).apply {
        setAudioAttributes(MusicAttributes, !muted)
        setHandleAudioBecomingNoisy(true)
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
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
        player.release()
    }

    companion object {
        private val MusicAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        /** True while another app is playing music, when a story starts muted rather than talk over it. */
        fun otherAudioPlaying(context: Context): Boolean =
            (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.isMusicActive == true

        /** True on a metered connection, where songs wait for the "Use mobile data for songs" setting. */
        fun onMeteredNetwork(context: Context): Boolean =
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.isActiveNetworkMetered != false
    }
}
