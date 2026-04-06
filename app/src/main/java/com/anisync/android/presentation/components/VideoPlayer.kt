package com.anisync.android.presentation.components

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.media3.ui.PlayerView
import com.anisync.android.R
import com.anisync.android.presentation.util.shimmerEffect
import kotlinx.coroutines.delay

/** Represents the visual state of the video player. */
private enum class PlayerState { Loading, Ready, Buffering, Error }

/**
 * A polished, inline video player using Media3 [ExoPlayer] and [PlayerView].
 * Features a completely custom Material 3 Expressive UI overlay with:
 * - **Loading skeleton**: shimmer + ContainedLoadingIndicator while preparing
 * - **Buffering spinner**: subtle LoadingIndicator during mid-playback rebuffering
 * - **Error state**: friendly card with retry for deleted/broken/network-failed videos
 * - **Immersive controls**: tap-to-play, mute toggle, seek slider
 * - **State persistence**: when [playerCache] is provided, the ExoPlayer instance
 *   survives scrolling off-screen in a LazyColumn, so the user won't re-fetch the
 *   video when scrolling back.
 *
 * @param url The URL of the video to play.
 * @param modifier Optional [Modifier] for the root container.
 * @param playerCache [ExoPlayerCache] for retaining player state across recomposition.
 *                    Defaults to [LocalExoPlayerCache]. When `null`, the player is
 *                    self-managed and released when leaving composition (legacy behavior).
 *                    To enable caching, provide [LocalExoPlayerCache] via
 *                    [CompositionLocalProvider] at the screen level.
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

    LaunchedEffect(url) {
        android.util.Log.d("VideoPlayer", "Loading video URL: $url")
    }

    var isMuted by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var playerState by remember { mutableStateOf(PlayerState.Loading) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Use cached player if available, otherwise create a local one
    val exoPlayer = if (playerCache != null) {
        remember(url) { playerCache.getOrCreate(url) }
    } else {
        remember(url) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = if (isMuted) 0f else 1f
                playWhenReady = false
                prepare()
            }
        }
    }

    // Attach listener for state tracking (detach on dispose to avoid leaks)
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
                playerState = when (playbackState) {
                    Player.STATE_READY -> PlayerState.Ready
                    Player.STATE_BUFFERING -> {
                        if (playerState == PlayerState.Ready || playerState == PlayerState.Buffering) {
                            PlayerState.Buffering
                        } else {
                            PlayerState.Loading
                        }
                    }
                    Player.STATE_ENDED -> PlayerState.Ready
                    Player.STATE_IDLE -> playerState
                    else -> playerState
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("VideoPlayer", "Playback error for URL: $url, code: ${error.errorCode}, message: ${error.message}", error)
                playerState = PlayerState.Error
                errorMessage = when (error.errorCode) {
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
            }
        }

        // Sync initial state from an already-prepared cached player
        if (exoPlayer.playbackState == Player.STATE_READY) {
            playerState = PlayerState.Ready
        }
        if (exoPlayer.videoSize.width > 0 && exoPlayer.videoSize.height > 0) {
            videoAspectRatio = exoPlayer.videoSize.width.toFloat() / exoPlayer.videoSize.height.toFloat()
        }
        isPlaying = exoPlayer.isPlaying
        isMuted = exoPlayer.volume == 0f

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            // Only release if self-managed (no cache). Cached players are
            // released by ExoPlayerCache.releaseAll() at the screen level.
            if (playerCache == null) {
                exoPlayer.release()
            } else {
                // Pause when scrolling away so audio doesn't play off-screen
                exoPlayer.pause()
            }
        }
    }

    // Polling for progress bar updates when playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val duration = exoPlayer.duration.coerceAtLeast(1)
            progress = (exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            delay(50)
        }
    }

    // Lifecycle observer — pause on background, resume on foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(videoAspectRatio)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        // --- 1. The Native Video Surface ---
        if (playerState != PlayerState.Error) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(
                        R.layout.view_texture_player,
                        null,
                        false
                    ) as PlayerView).apply {
                        player = exoPlayer
                        useController = false
                        setEnableComposeSurfaceSyncWorkaround(true)
                    }
                },
                update = { view -> view.player = exoPlayer },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 2. Loading Skeleton (Initial Load) ---
        AnimatedVisibility(
            visible = playerState == PlayerState.Loading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .shimmerEffect(),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- 3. Mid-Playback Buffering Spinner ---
        AnimatedVisibility(
            visible = playerState == PlayerState.Buffering,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LoadingIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // --- 4. Error State ---
        AnimatedVisibility(
            visible = playerState == PlayerState.Error,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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

                    FilledTonalButton(
                        onClick = {
                            playerState = PlayerState.Loading
                            errorMessage = null
                            exoPlayer.apply {
                                setMediaItem(MediaItem.fromUri(url))
                                prepare()
                            }
                        },
                        shape = RoundedCornerShape(100)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "Retry",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --- 5. Tap-to-Play/Pause Overlay (only when ready/buffering) ---
        if (playerState == PlayerState.Ready || playerState == PlayerState.Buffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = !isPlaying && playerState != PlayerState.Buffering,
                    enter = fadeIn(animationSpec = spring()) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                    ),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // --- 6. Mute Button (Top Right) ---
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
            ) {
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else 1f
                    }
                ) {
                    Crossfade(targetState = isMuted, label = "mute_crossfade") { muted ->
                        Icon(
                            imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (muted) "Unmute" else "Mute",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // --- 7. Gradient & Minimalist Seek Bar (Bottom) ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Slider(
                    value = progress,
                    onValueChange = { newProgress ->
                        progress = newProgress
                        val newPosition = (exoPlayer.duration * newProgress).toLong()
                        exoPlayer.seekTo(newPosition)
                    },
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = remember { MutableInteractionSource() },
                            thumbSize = DpSize(8.dp, 8.dp),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                        )
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )
            }
        }
    }
}
