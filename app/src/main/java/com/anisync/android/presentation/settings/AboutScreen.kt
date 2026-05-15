package com.anisync.android.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.anisync.android.BuildConfig
import com.anisync.android.R
import com.anisync.android.presentation.util.LocalAppSettings
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.util.AppInfo
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

private const val AVATAR_URL = "https://avatars.githubusercontent.com/u/41828058?v=4"

@Composable
private fun AboutHero(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHapticFeedback()
    var heroState by remember { mutableStateOf(HeroState.Idle) }

    // Preload the avatar image to ensure it's instantly available when the easter egg triggers
    LaunchedEffect(Unit) {
        val request = ImageRequest.Builder(context)
            .data(AVATAR_URL)
            .build()
        context.imageLoader.enqueue(request)
    }

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

    // Use animateColor instead of an instant swap for higher polish
    val containerColor by transition.animateColor(
        label = "containerColor",
        transitionSpec = { tween(durationMillis = 400, easing = FastOutSlowInEasing) }
    ) { state ->
        if (state == HeroState.Revealed) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
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
            // We use graphicsLayer for rotation to bypass recomposition entirely
            Box(
                modifier = Modifier
                    .size(115.dp)
                    .align(Alignment.Center)
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
                                .data(AVATAR_URL)
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
                            .data(AVATAR_URL)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.cd_developer_avatar),
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

        // Hidden gesture: tapping the version label 7x unlocks Developer Tools in any build.
        val context = LocalContext.current
        val appSettings = LocalAppSettings.current
        val devToolsUnlocked by appSettings.devToolsUnlocked
            .collectAsStateWithLifecycle(initialValue = false)
        var versionTapCount by remember { mutableIntStateOf(0) }
        val unlockTaps = 7
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {
                if (devToolsUnlocked) {
                    Toast.makeText(
                        context,
                        R.string.dev_tools_already_unlocked,
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@clickable
                }
                versionTapCount++
                val remaining = unlockTaps - versionTapCount
                when {
                    remaining <= 0 -> {
                        appSettings.unlockDevTools()
                        Toast.makeText(
                            context,
                            R.string.dev_tools_unlocked,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    remaining <= 3 -> Toast.makeText(
                        context,
                        context.getString(R.string.dev_tools_taps_remaining, remaining),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        )
    }
}

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onNavigateToOpenSourceLicenses: () -> Unit,
    onNavigateToAcknowledgments: () -> Unit,
    onNavigateToLinks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSponsorSheet by remember { mutableStateOf(false) }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_about),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        AboutHero(modifier = Modifier.padding(top = 40.dp, bottom = 16.dp))

        ElevatedButton(
            onClick = { showSponsorSheet = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .height(56.dp),
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 24.dp,
                bottomEnd = 8.dp
            ),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.VolunteerActivism,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.sponsor_project),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.settings_privacy_policy),
                onClick = { context.launchUrl("https://anisync.app/privacy") }
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(R.string.settings_terms_of_service),
                onClick = { context.launchUrl("https://anisync.app/terms") }
            )
            SettingsDivider()
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
            SettingsDivider()
            SettingsItem(
                title = stringResource(R.string.settings_anilist_api),
                subtitle = stringResource(R.string.settings_anilist_api_desc),
                onClick = { context.launchUrl("https://anilist.co") }
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(R.string.settings_links),
                subtitle = stringResource(R.string.settings_links_desc),
                onClick = onNavigateToLinks
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(R.string.about_improve_translations),
                subtitle = stringResource(R.string.about_improve_translations_desc, stringResource(R.string.app_name)),
                onClick = { context.launchUrl("https://hosted.weblate.org/engage/anisync/") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.settings_copy_app_info),
                subtitle = stringResource(R.string.settings_copy_app_info_desc),
                onClick = {
                    context.copyAppInfo()
                }
            )
        }
    }

    if (showSponsorSheet) {
        SponsorBottomSheet(
            onDismissRequest = { showSponsorSheet = false },
            onSponsorClick = {
                showSponsorSheet = false
                context.launchUrl("https://github.com/sponsors/Marco-9456")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SponsorBottomSheet(
    onDismissRequest: () -> Unit,
    onSponsorClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.VolunteerActivism,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.support_anisync, stringResource(R.string.app_name)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.support_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Developer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Static easter egg avatar
                    StaticDeveloperAvatar(modifier = Modifier.size(56.dp))

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mohammed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.lead_developer),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = onSponsorClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.sponsor))
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun StaticDeveloperAvatar(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val imageScale = 1.3f
        val yOffset = maxWidth * (-12f / 115f)

        // Body Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ExpressiveMorphShape(vertices = 12f, roundness = 0.2f))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(AVATAR_URL)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(imageScale)
                    .offset(y = yOffset)
            )
        }

        // Head Layer (Popping out)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(AVATAR_URL)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
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

private fun Context.launchUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun Context.copyAppInfo() {
    val clipboard = getSystemService<ClipboardManager>() ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("AniSync app info", AppInfo.formatted()))
}