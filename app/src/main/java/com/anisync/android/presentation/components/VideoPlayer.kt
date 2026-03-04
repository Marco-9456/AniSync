package com.anisync.android.presentation.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * A highly optimized, inline video player using Media3 [ExoPlayer] and [PlayerView].
 * Features a completely custom, minimalistic Material 3 Expressive UI overlay.
 *
 * @param url The URL of the video to play.
 * @param modifier Optional [Modifier] for the root container.
 */
@ExperimentalMaterial3Api
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isMuted by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }

    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (isMuted) 0f else 1f
            playWhenReady = false

            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    isPlaying = isPlayingNow
                }
            })
            prepare()
        }
    }

    // Polling for progress bar updates when playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val duration = exoPlayer.duration.coerceAtLeast(1)
            progress = (exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            delay(50) // Update UI at roughly 20fps for smooth slider
        }
    }

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

    DisposableEffect(url) {
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(videoAspectRatio)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // --- 1. The Native Video Surface ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Completely disable default ugly UI
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. Tap-to-Play/Pause Overlay ---
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
            // Immersive Play Button (Visible only when paused)
            AnimatedVisibility(
                visible = !isPlaying,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
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

        // --- 3. Mute Button (Top Right) ---
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

        // --- 4. Gradient & Minimalist Seek Bar (Bottom) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Subtle gradient to ensure the progress bar is visible on light videos
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
                    // Custom tiny thumb so it doesn't obstruct the video
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
                    .height(24.dp) // Keeps touch target reasonable while looking slim
            )
        }
    }
}