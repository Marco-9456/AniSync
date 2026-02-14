package com.anisync.android.presentation.login

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R

private const val CLIENT_ID = "32893"
private const val AUTH_URL =
    "https://anilist.co/api/v2/oauth/authorize?client_id=$CLIENT_ID&response_type=token"

/**
 * The primary login screen for AniSync.
 *
 * Features:
 * - Animated vector illustration with floating elements.
 * - Material 3 theming for colors and typography.
 * - OAuth 2.0 implicit grant entry point via browser intent.
 */
@Composable
fun LoginScreen() {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Title and Tagline
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically { -40 } + fadeIn(tween(600))
            ) {
                TitleHeader()
            }

            Spacer(modifier = Modifier.weight(1f))

            // Central Illustration
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, delayMillis = 200)) + androidx.compose.animation.scaleIn(
                    initialScale = 0.9f
                )
            ) {
                VectorIllustration()
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Action Bar
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ) + fadeIn()
            ) {
                BottomActionBar(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL))
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TitleHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "AniSync",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                fontSize = 56.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your anime library, synced seamlessly.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * A custom illustration combining Canvas drawing and composable icons.
 * The icons float up and down to add liveliness to the static login page.
 */
@Composable
private fun VectorIllustration() {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainer

    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y_offset"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(320.dp)
    ) {
        // Backdrop shapes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Back Tilted Card (Darker variant)
            rotate(degrees = -8f, pivot = center) {
                drawRoundRect(
                    color = surface.copy(alpha = 0.6f),
                    topLeft = Offset(width * 0.15f, height * 0.15f),
                    size = Size(width * 0.7f, height * 0.6f),
                    cornerRadius = CornerRadius(32.dp.toPx())
                )
            }

            // Main Central Card
            drawRoundRect(
                color = surface,
                topLeft = Offset(width * 0.1f, height * 0.25f),
                size = Size(width * 0.8f, height * 0.5f),
                cornerRadius = CornerRadius(40.dp.toPx())
            )

            // Decorative Circle (Bottom Right)
            drawCircle(
                color = surface.copy(alpha = 0.8f),
                radius = 36.dp.toPx(),
                center = Offset(width * 0.82f, height * 0.75f)
            )
        }

        // Central App Icon
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(130.dp)
                .scale(1.2f)
        )

        // Floating Icons
        Icon(
            imageVector = Icons.Rounded.Movie,
            contentDescription = null,
            tint = primary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 50.dp)
                .size(36.dp)
                .graphicsLayer {
                    translationY = floatOffset
                    rotationZ = 15f
                }
        )

        Icon(
            imageVector = Icons.Rounded.Book,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 70.dp, start = 40.dp)
                .size(32.dp)
                .graphicsLayer {
                    translationY = -floatOffset
                    rotationZ = -20f
                }
        )
    }
}

/**
 * A large, clickable bottom action bar that initiates the login flow.
 */
@Composable
private fun BottomActionBar(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "button_press"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            )
    ) {
        Row(
            modifier = Modifier
                .padding(start = 32.dp, end = 20.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Let's Get Started",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Login with AniList",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Button
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Login",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}