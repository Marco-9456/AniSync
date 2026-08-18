package com.anisync.android.presentation.components

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.anisync.android.ui.theme.AppTheme
import kotlinx.coroutines.delay

/**
 * Process-wide handoff slot for the [ExoPlayer] the inline [VideoPlayer] is sharing with the
 * fullscreen activity. A single reference (not a parcelled URL) so the exact same instance — with
 * its buffered data and current position — is what plays in fullscreen: no re-fetch, no double audio.
 */
@OptIn(UnstableApi::class)
internal object FullscreenVideoBridge {
    var player: ExoPlayer? = null
}

/**
 * Dedicated immersive, rotatable fullscreen video player.
 *
 * Lives in its own activity (rather than a Compose `Dialog` inside the feed) on purpose: forcing an
 * orientation change relayouts the host `LazyColumn`, which disposes the inline item — and a dialog
 * parented to that item would die with it. A separate activity has its own window, lifecycle and
 * orientation, so it survives rotation cleanly. It renders the *same* [ExoPlayer] handed over via
 * [FullscreenVideoBridge], so closing returns to the inline player exactly where it left off.
 */
@OptIn(UnstableApi::class)
class FullscreenPlayerActivity : ComponentActivity() {

    companion object {
        /** Float extra: the clip's width/height, used to pick the launch orientation. */
        const val EXTRA_ASPECT = "com.anisync.android.extra.ASPECT"

        /** Smallest width at which the window fits the clip in either orientation. */
        private const val LARGE_SCREEN_SW_DP = 600
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aspect = intent.getFloatExtra(EXTRA_ASPECT, DEFAULT_VIDEO_ASPECT)
        // A phone only fits the clip one way round, so rotate it there. A tablet or unfolded
        // foldable has room either way and is often docked or held in portrait, so forcing a
        // rotation there fights the user for nothing (Android 16 ignores the request on those
        // windows anyway). Sensor variants still allow a 180° flip.
        val largeScreen = resources.configuration.smallestScreenWidthDp >= LARGE_SCREEN_SW_DP
        requestedOrientation = when {
            largeScreen -> ActivityInfo.SCREEN_ORIENTATION_USER
            aspect >= 1f -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

        // Edge-to-edge + hide the system bars for a true immersive view (swipe brings them back).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val player = FullscreenVideoBridge.player
        if (player == null) {
            // Nothing to show (e.g. process death restored this activity) — bail back to the app.
            finish()
            return
        }

        setContent {
            AppTheme(darkTheme = true) {
                FullscreenVideoContent(
                    player = player,
                    aspectRatio = aspect,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the shared reference; the inline player keeps its own and reattaches on return.
        FullscreenVideoBridge.player = null
    }
}

/**
 * Fullscreen content: the shared [player] rendered into a fitted surface with the same auto-hiding
 * [VideoControlsOverlay] used inline. Tracks playback off its own listener so the controls stay live.
 */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FullscreenVideoContent(
    player: ExoPlayer,
    aspectRatio: Float,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isMuted by remember { mutableStateOf(player.volume == 0f) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var positionMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0)) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    var isScrubbing by remember { mutableStateOf(false) }
    var playerState by remember {
        mutableStateOf(mapPlaybackState(player.playbackState, PlayerState.Loading))
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Seeded from the launch extra, then corrected off the player itself, so the fitted box tracks
    // the real picture even when the inline player never reported a size before the handoff.
    var aspect by remember {
        mutableFloatStateOf(aspectRatio.takeIf { it > 0f } ?: DEFAULT_VIDEO_ASPECT)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val d = player.duration
                    if (d > 0) durationMs = d
                }
                playerState = mapPlaybackState(playbackState, playerState)
            }

            override fun onPlayerError(error: PlaybackException) {
                playerState = PlayerState.Error
                errorMessage = playbackErrorMessage(context, error)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Drive the scrubber while playing (paused while dragging).
    LaunchedEffect(isPlaying, isScrubbing) {
        while (isPlaying && !isScrubbing) {
            positionMs = player.currentPosition
            val d = player.duration
            if (d > 0) durationMs = d
            delay(250)
        }
    }

    // Pause when this activity is backgrounded; resume if it was playing.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) player.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Fit a box to the clip rather than letting the surface letterbox itself inside the whole
        // window. On a phone the two are the same box; on a tablet — especially in portrait, where
        // a wide clip fills a band across the middle — it keeps the controls on the video instead
        // of stranding the scrubber at the bottom of the screen, far from the picture.
        val videoModifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
            .aspectRatio(aspect)

        Box(videoModifier) {
            if (playerState != PlayerState.Error) {
                PlayerSurface(exoPlayer = player, active = true, modifier = Modifier.fillMaxSize())
            }
        }

        PlayerStatusVisuals(
            isLoading = playerState == PlayerState.Loading,
            isBuffering = playerState == PlayerState.Buffering,
            errorMessage = if (playerState == PlayerState.Error) errorMessage else null,
            showShimmer = false,
            onRetry = {
                playerState = PlayerState.Loading
                errorMessage = null
                player.prepare()
            }
        )

        if (playerState == PlayerState.Ready || playerState == PlayerState.Buffering) {
            VideoControlsOverlay(
                modifier = videoModifier,
                isPlaying = isPlaying,
                isBuffering = playerState == PlayerState.Buffering,
                isMuted = isMuted,
                positionMs = positionMs,
                durationMs = durationMs,
                isFullscreen = true,
                onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                onToggleMute = {
                    isMuted = !isMuted
                    player.volume = if (isMuted) 0f else 1f
                },
                onSeek = { fraction ->
                    isScrubbing = true
                    val d = durationMs
                    if (d > 0) {
                        positionMs = (fraction * d).toLong()
                        player.seekTo(positionMs)
                    }
                },
                onSeekFinished = { isScrubbing = false },
                onToggleFullscreen = onClose,
                onBack = onClose
            )
        }
    }
}
