package com.anisync.android.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anisync.android.BuildConfig
import com.anisync.android.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A configurable shape that mimics Material Design 3 Expressive shapes.
 * Optimized for smooth animation between standard polygons and starbursts.
 */
private class ExpressiveMorphShape(
    private val vertices: Float,
    private val roundness: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = minOf(centerX, centerY)

        val points = 200
        val lowCount = kotlin.math.floor(vertices)
        val highCount = kotlin.math.ceil(vertices)
        val fraction = vertices - lowCount

        for (i in 0..points) {
            val angle = 2 * PI * i / points

            // Wave 1 (Current integer N)
            val waveLow = cos(lowCount * angle).toFloat()
            val rLow = 1f - roundness * (1 - waveLow) / 2f

            // Wave 2 (Next integer N)
            val waveHigh = cos(highCount * angle).toFloat()
            val rHigh = 1f - roundness * (1 - waveHigh) / 2f

            val normalizedR = rLow + (rHigh - rLow) * fraction

            val x = centerX + (maxRadius * normalizedR) * cos(angle).toFloat()
            val y = centerY + (maxRadius * normalizedR) * sin(angle).toFloat()

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

private enum class HeroState { Idle, Pressed, Revealed }

@Composable
private fun AboutHero(
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var heroState by remember { mutableStateOf(HeroState.Idle) }

    val transition = updateTransition(targetState = heroState, label = "HeroTransition")

    val vertices by transition.animateFloat(
        label = "vertices",
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        }
    ) { state ->
        when (state) {
            HeroState.Revealed -> 12f
            HeroState.Pressed -> 6f
            HeroState.Idle -> 4f
        }
    }

    val roundness by transition.animateFloat(
        label = "roundness",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
    ) { state ->
        if (state == HeroState.Revealed) 0.2f else 0.12f
    }

    val rotationY by transition.animateFloat(
        label = "rotationY",
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        }
    ) { state ->
        if (state == HeroState.Revealed) 180f else 0f
    }

    val containerColor = if (heroState == HeroState.Revealed) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val infiniteTransition = rememberInfiniteTransition(label = "passive")
    val levitationOffset by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "levitation"
    )
    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "spin"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    this.rotationY = rotationY
                    cameraDistance = 12f * density
                    if (rotationY > 90f) translationY = levitationOffset
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            if (heroState != HeroState.Revealed) {
                                heroState = HeroState.Pressed
                                tryAwaitRelease()
                                if (heroState != HeroState.Revealed) {
                                    heroState = HeroState.Idle
                                }
                            } else {
                                heroState = HeroState.Idle
                            }
                        },
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            heroState = HeroState.Revealed
                        }
                    )
                }
        ) {
            // Background Shape
            Box(
                modifier = Modifier
                    .size(115.dp)
                    .align(Alignment.Center)
                    // Rotate inversely when revealed to sync with the un-flipped content
                    .rotate(if (heroState == HeroState.Revealed) -spinRotation else 0f)
                    .clip(ExpressiveMorphShape(vertices, roundness))
                    .background(containerColor)
            )

            if (rotationY <= 90f) {
                // Front: App Icon
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.a11y_app_icon),
                        modifier = Modifier.size(96.dp)
                    )
                }
            } else {
                // Back: Developer Avatar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.rotationY = 180f },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = "https://i.ibb.co/6cqF2CfY/Adobe-Express-file.png"
                    val imageScale = 1.3f
                    val yOffset = (-12).dp

                    // Body Layer: Rotates with the shape, image counter-rotates to stay upright
                    Box(
                        modifier = Modifier
                            .size(115.dp)
                            .rotate(if (heroState == HeroState.Revealed) spinRotation else 0f)
                            .clip(ExpressiveMorphShape(vertices, roundness))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(if (heroState == HeroState.Revealed) -spinRotation else 0f)
                                .scale(imageScale)
                                .offset(y = yOffset)
                        )
                    }

                    // Head Layer: Pops out of the shape
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Developer Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(115.dp)
                            .scale(imageScale)
                            .offset(y = yOffset)
                            .clip(CircleShape)
                            .drawWithContent {
                                clipRect(bottom = size.height * 0.45f) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedContent(
            targetState = heroState == HeroState.Revealed,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn())
                    .togetherWith(slideOutVertically { height -> -height } + fadeOut())
            },
            label = "TitleSwap"
        ) { isRevealed ->
            Text(
                text = if (isRevealed) "Developed by Mohammed" else stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onNavigateToOpenSourceLicenses: () -> Unit,
    onNavigateToAcknowledgments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_about),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        AboutHero(modifier = Modifier.padding(vertical = 40.dp))

        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.settings_privacy_policy),
                onClick = { context.launchUrl("https://anisync.app/privacy") }
            )
            SettingsDivider(startPadding = 20.dp)
            SettingsItem(
                title = stringResource(R.string.settings_terms_of_service),
                onClick = { context.launchUrl("https://anisync.app/terms") }
            )
            SettingsDivider(startPadding = 20.dp)
            SettingsItem(
                title = stringResource(R.string.settings_open_source_licenses),
                onClick = onNavigateToOpenSourceLicenses
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.settings_acknowledgments),
                subtitle = stringResource(R.string.settings_acknowledgments_desc),
                onClick = onNavigateToAcknowledgments
            )
            SettingsDivider(startPadding = 20.dp)
            SettingsItem(
                title = stringResource(R.string.settings_anilist_api),
                subtitle = stringResource(R.string.settings_anilist_api_desc),
                onClick = { context.launchUrl("https://anilist.co") }
            )
        }
    }
}

private fun Context.launchUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}