package com.anisync.android.presentation.components

import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.anisync.android.R
import com.anisync.android.presentation.util.shimmerEffect
import kotlinx.coroutines.delay

/** Visual state of the video player. Shared with [FullscreenPlayerActivity]. */
internal enum class PlayerState { Loading, Ready, Buffering, Error }

/**
 * Tallest aspect ratio (width/height) the inline frame will take. A portrait clip taller than
 * this (e.g. a 9:16 phone capture) is pillar-boxed inside a 4:5 frame instead of growing to
 * ~1.8× the screen width and shoving the rest of the post off-screen.
 */
private const val MIN_DISPLAY_ASPECT = 0.8f // 4:5

/** Widest aspect ratio the inline frame will take, so an ultrawide clip keeps a usable height. */
private const val MAX_DISPLAY_ASPECT = 2.4f // ~21:9

internal const val DEFAULT_VIDEO_ASPECT = 16f / 9f

/** How long the controls linger after the last interaction before fading, while playing. */
private const val CONTROLS_HIDE_DELAY_MS = 3000L

/**
 * An inline video player for the short, looping, user-embedded clips that appear in rich text
 * (activity posts, forum replies, bios). Built on Media3 [ExoPlayer] + [PlayerView] with a fully
 * custom Material 3 overlay.
 *
 * UX:
 * - **Adaptive framing** — the frame's aspect ratio is clamped to a sane range and the video is
 *   letterboxed (RESIZE_MODE_FIT) inside themed bars, so odd-shaped, tiny, or huge clips all sit
 *   tidily in the feed instead of distorting or dominating the screen.
 * - **Auto-hiding controls** — controls appear on tap and fade [CONTROLS_HIDE_DELAY_MS] after the
 *   last interaction while playing; they stay put while paused. A single tap toggles them; the
 *   centre button (not the whole surface) controls playback, so there are no accidental pauses.
 * - **Real scrubber** — current/total time with drag-to-seek.
 * - **Fullscreen** — hands the *same* player to a dedicated [FullscreenPlayerActivity] (own
 *   lifecycle + orientation), which auto-rotates to landscape for wide clips. Using a separate
 *   activity rather than an in-list dialog is what makes rotation robust: a dialog hosted inside a
 *   `LazyColumn` item is torn down the moment a rotation relayouts (and disposes) that item.
 * - **Robust states** — loading skeleton, mid-playback buffering spinner, and a friendly retry card.
 *
 * @param url The URL of the video to play.
 * @param modifier Optional [Modifier] for the root container.
 * @param playerCache [ExoPlayerCache] for retaining player state across recomposition / scrolling.
 * Defaults to [LocalExoPlayerCache]. When `null`, the player is self-managed and released on dispose.
 */
@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@ExperimentalMaterial3Api
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    playerCache: ExoPlayerCache? = LocalExoPlayerCache.current
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isMuted by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var videoAspectRatio by remember { mutableFloatStateOf(DEFAULT_VIDEO_ASPECT) }
    var playerState by remember { mutableStateOf(PlayerState.Loading) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // True while the player has been handed to the fullscreen activity; the inline surface detaches
    // and the lifecycle observer stops pausing, so the same instance keeps playing over there.
    var handedOff by remember { mutableStateOf(false) }

    // Cached player survives scrolling; self-managed one is built+released here. Both go through
    // buildVideoExoPlayer so the OkHttp/browser-UA data source and eager prepare apply identically.
    // The self-managed instance uses the application context so it can be safely handed to the
    // fullscreen activity without leaking this composition's context.
    val exoPlayer = if (playerCache != null) {
        remember(url) { playerCache.getOrCreate(url) }
    } else {
        remember(url) { buildVideoExoPlayer(context.applicationContext, url) }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val d = exoPlayer.duration
                    if (d > 0) durationMs = d
                }
                playerState = mapPlaybackState(playbackState, playerState)
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e(
                    "VideoPlayer",
                    "Playback error for URL: $url, code: ${error.errorCode}, message: ${error.message}",
                    error
                )
                playerState = PlayerState.Error
                errorMessage = playbackErrorMessage(error)
            }
        }

        // Sync initial state from an already-prepared cached player.
        if (exoPlayer.playbackState == Player.STATE_READY) {
            playerState = PlayerState.Ready
            if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
        }
        if (exoPlayer.videoSize.width > 0 && exoPlayer.videoSize.height > 0) {
            videoAspectRatio = exoPlayer.videoSize.width.toFloat() / exoPlayer.videoSize.height.toFloat()
        }
        isPlaying = exoPlayer.isPlaying
        isMuted = exoPlayer.volume == 0f

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            // Only release if self-managed; cached players are released by ExoPlayerCache.releaseAll().
            if (playerCache == null) exoPlayer.release() else exoPlayer.pause()
        }
    }

    // Drive the scrubber while playing (paused when the user is dragging it).
    LaunchedEffect(isPlaying, isScrubbing) {
        while (isPlaying && !isScrubbing) {
            positionMs = exoPlayer.currentPosition
            val d = exoPlayer.duration
            if (d > 0) durationMs = d
            delay(250)
        }
    }

    // Pause on background, resume on foreground (only if it was playing). Skipped while handed off
    // to fullscreen — that activity owns playback then, and pausing here would stop it.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> if (!handedOff) exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying && !handedOff) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // When the fullscreen activity returns, reclaim the surface.
    val fullscreenLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { handedOff = false }

    val onPlayPause: () -> Unit = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }
    val onToggleMute: () -> Unit = {
        isMuted = !isMuted
        exoPlayer.volume = if (isMuted) 0f else 1f
    }
    val onSeek: (Float) -> Unit = { fraction ->
        isScrubbing = true
        val d = durationMs
        if (d > 0) {
            positionMs = (fraction * d).toLong()
            exoPlayer.seekTo(positionMs)
        }
    }
    val onSeekFinished: () -> Unit = { isScrubbing = false }
    val onRetry: () -> Unit = {
        playerState = PlayerState.Loading
        errorMessage = null
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
    }
    val onEnterFullscreen: () -> Unit = {
        FullscreenVideoBridge.player = exoPlayer
        handedOff = true
        fullscreenLauncher.launch(
            Intent(context, FullscreenPlayerActivity::class.java)
                .putExtra(FullscreenPlayerActivity.EXTRA_ASPECT, videoAspectRatio)
        )
    }

    val displayAspect = videoAspectRatio.coerceIn(MIN_DISPLAY_ASPECT, MAX_DISPLAY_ASPECT)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(displayAspect)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (playerState != PlayerState.Error) {
            PlayerSurface(
                exoPlayer = exoPlayer,
                active = !handedOff,
                modifier = Modifier.fillMaxSize()
            )
        }

        PlayerStatusVisuals(
            isLoading = playerState == PlayerState.Loading,
            isBuffering = playerState == PlayerState.Buffering,
            errorMessage = if (playerState == PlayerState.Error) errorMessage else null,
            showShimmer = true,
            onRetry = onRetry
        )

        if (playerState == PlayerState.Ready || playerState == PlayerState.Buffering) {
            VideoControlsOverlay(
                isPlaying = isPlaying,
                isBuffering = playerState == PlayerState.Buffering,
                isMuted = isMuted,
                positionMs = positionMs,
                durationMs = durationMs,
                isFullscreen = false,
                onPlayPause = onPlayPause,
                onToggleMute = onToggleMute,
                onSeek = onSeek,
                onSeekFinished = onSeekFinished,
                onToggleFullscreen = onEnterFullscreen,
                onBack = null
            )
        }
    }
}

/** Maps an ExoPlayer playback-state int to a [PlayerState], preserving the buffering/loading nuance. */
internal fun mapPlaybackState(playbackState: Int, current: PlayerState): PlayerState = when (playbackState) {
    Player.STATE_READY -> PlayerState.Ready
    Player.STATE_BUFFERING ->
        if (current == PlayerState.Ready || current == PlayerState.Buffering) PlayerState.Buffering
        else PlayerState.Loading
    Player.STATE_ENDED -> PlayerState.Ready
    Player.STATE_IDLE -> current
    else -> current
}

/** Friendly, user-facing copy for a playback failure. */
internal fun playbackErrorMessage(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Network error — check your connection"
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "This video is no longer available"
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FAILED ->
        "Unsupported video format"
    else -> "Unable to play this video"
}

/**
 * The native video surface. [active] hands the shared [exoPlayer] to exactly one surface at a time
 * (inline vs. fullscreen activity), so the two never fight over it. RESIZE_MODE_FIT letterboxes the
 * real video inside whatever frame it's given.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun PlayerSurface(
    exoPlayer: ExoPlayer,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.view_texture_player, null, false) as PlayerView).apply {
                useController = false
                setEnableComposeSurfaceSyncWorkaround(true)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = { view ->
            view.player = if (active) exoPlayer else null
            if (active && exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare()
        },
        onRelease = { it.player = null },
        modifier = modifier
    )
}

/**
 * Non-interactive (except retry) status layer: loading skeleton, mid-playback buffering spinner, and
 * error card. Shared by the inline and fullscreen presentations.
 */
@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayerStatusVisuals(
    isLoading: Boolean,
    isBuffering: Boolean,
    errorMessage: String?,
    showShimmer: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (showShimmer) Modifier.shimmerEffect() else Modifier.background(Color.Black)),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(visible = isBuffering, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        LoadingIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        AnimatedVisibility(visible = errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = errorMessage ?: "Unable to play this video",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    FilledTonalButton(onClick = onRetry, shape = RoundedCornerShape(100)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(text = stringResource(R.string.retry), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * The auto-hiding control surface drawn over the video. Owns its own visibility so the inline and
 * fullscreen instances hide independently. A single tap on empty space toggles the controls; the
 * centre button — not the whole surface — toggles playback, which avoids accidental pauses.
 *
 * @param onBack When non-null, a back affordance is shown (used by fullscreen to close).
 */
@Composable
internal fun VideoControlsOverlay(
    isPlaying: Boolean,
    isBuffering: Boolean,
    isMuted: Boolean,
    positionMs: Long,
    durationMs: Long,
    isFullscreen: Boolean,
    onPlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    // Bumped on every interaction to restart the auto-hide countdown.
    var interactions by remember { mutableIntStateOf(0) }

    // Auto-hide while playing; idle countdown restarts whenever an interaction bumps [interactions].
    LaunchedEffect(controlsVisible, isPlaying, isBuffering, interactions) {
        if (controlsVisible && isPlaying && !isBuffering) {
            delay(CONTROLS_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }
    // Always reveal controls when playback pauses or stalls.
    LaunchedEffect(isPlaying, isBuffering) {
        if (!isPlaying || isBuffering) controlsVisible = true
    }

    val poke: () -> Unit = { interactions++ }
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Box(modifier.fillMaxSize()) {
        // Tap layer: toggles control visibility (sits below the controls, which consume their own taps).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                }
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            ) {
                // Back (fullscreen only).
                if (onBack != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(12.dp)
                            .size(40.dp)
                    ) {
                        IconButton(onClick = { poke(); onBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Mute (top-end).
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
                        .padding(12.dp)
                        .size(40.dp)
                ) {
                    IconButton(onClick = { poke(); onToggleMute() }) {
                        Crossfade(targetState = isMuted, label = "mute_crossfade") { muted ->
                            Icon(
                                imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (muted) stringResource(R.string.unmute) else stringResource(R.string.mute),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Centre play / pause (hidden while buffering — the spinner shows instead).
                if (!isBuffering) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(if (isFullscreen) 72.dp else 64.dp)
                    ) {
                        IconButton(onClick = { poke(); onPlayPause() }, modifier = Modifier.fillMaxSize()) {
                            Crossfade(targetState = isPlaying, label = "playpause_crossfade") { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playing) stringResource(R.string.cd_pause) else stringResource(R.string.cd_play),
                                    modifier = Modifier.size(if (isFullscreen) 36.dp else 30.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom bar: elapsed · scrubber · duration · fullscreen toggle.
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .then(if (isFullscreen) Modifier.navigationBarsPadding() else Modifier)
                        .padding(start = 12.dp, end = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )

                    Slider(
                        value = fraction,
                        onValueChange = { poke(); onSeek(it) },
                        onValueChangeFinished = onSeekFinished,
                        thumb = {
                            SliderDefaults.Thumb(
                                interactionSource = remember { MutableInteractionSource() },
                                thumbSize = DpSize(12.dp, 12.dp),
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                            )
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(32.dp)
                    )

                    Text(
                        text = formatTime(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )

                    Spacer(Modifier.width(4.dp))

                    IconButton(onClick = { poke(); onToggleFullscreen() }) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = if (isFullscreen) stringResource(R.string.cd_exit_fullscreen) else stringResource(R.string.cd_fullscreen),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Formats a millisecond position as `m:ss` (or `0:00` when unknown). */
internal fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
