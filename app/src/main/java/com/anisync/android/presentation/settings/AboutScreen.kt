package com.anisync.android.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.anisync.android.BuildConfig
import com.anisync.android.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom clover shape that can morph between different petal counts.
 * Uses smooth float interpolation for fluid animation between petal counts.
 * @param petalCount Number of petals (supports fractional values for smooth animation)
 * @param petalDepth How deep the indentations are (0 = circle, 1 = very deep)
 */
private class CloverShape(
    private val petalCount: Float,
    private val petalDepth: Float = 0.3f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(centerX, centerY)
        
        val points = 360
        
        for (i in 0..points) {
            val angle = (i.toFloat() / points) * 2 * PI
            // Use petalCount directly as a float for smooth interpolation
            val petalEffect = 1f - petalDepth * (1 - cos(petalCount * angle).toFloat()) / 2f
            val r = radius * petalEffect
            
            val x = centerX + r * cos(angle).toFloat()
            val y = centerY + r * sin(angle).toFloat()
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        
        return Outline.Generic(path)
    }
}

/**
 * About app screen.
 * Displays app information, version, and legal links.
 */
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onNavigateToOpenSourceLicenses: () -> Unit,
    onNavigateToAcknowledgments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // State for clover morphing animation
    var isPressed by remember { mutableStateOf(false) }
    var isLongPressed by remember { mutableStateOf(false) }
    
    // Animate petal count: 4 -> 8 on press/long press
    val targetPetalCount = when {
        isLongPressed -> 8f
        isPressed -> 6f
        else -> 4f
    }
    
    val animatedPetalCount by animateFloatAsState(
        targetValue = targetPetalCount,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "petalCount"
    )
    
    // Scale animation for long press
    val scale by animateFloatAsState(
        targetValue = if (isLongPressed) 1.15f else if (isPressed) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_about),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        // App icon and info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App icon with morphing clover shape
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .clip(CloverShape(petalCount = animatedPetalCount, petalDepth = 0.15f))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                                isLongPressed = false
                            },
                            onLongPress = {
                                isLongPressed = true
                            },
                            onTap = {
                                // Just animate on tap
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.a11y_app_icon),
                    modifier = Modifier.size(96.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App name
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Version
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Links group
        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.settings_privacy_policy),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://anisync.app/privacy"))
                    context.startActivity(intent)
                }
            )
            SettingsDivider(startPadding = 20.dp)
            SettingsItem(
                title = stringResource(R.string.settings_terms_of_service),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://anisync.app/terms"))
                    context.startActivity(intent)
                }
            )
            SettingsDivider(startPadding = 20.dp)
            SettingsItem(
                title = stringResource(R.string.settings_open_source_licenses),
                onClick = onNavigateToOpenSourceLicenses
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Credits group
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
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://anilist.co"))
                    context.startActivity(intent)
                }
            )
        }
    }
}
