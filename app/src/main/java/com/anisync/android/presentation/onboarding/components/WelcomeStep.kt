package com.anisync.android.presentation.onboarding.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R

/**
 * The first screen: a drifting wall of cover art behind the lockup, with the AniList handoff as the
 * only action. Nothing here needs a token, so it renders identically before and after sign-in.
 */
@Composable
fun WelcomeStep(
    covers: List<String>,
    onContinue: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        PosterMarquee(
            covers = covers,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
                .align(Alignment.TopCenter)
        )

        // Two scrims: the art has to sit under the lockup at the top and dissolve into the page
        // before the headline starts, or neither reads.
        val background = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to background.copy(alpha = 0.86f),
                        0.16f to background.copy(alpha = 0.30f),
                        0.44f to background.copy(alpha = 0.42f),
                        0.66f to background,
                        1f to background
                    )
                )
        )

        BrandLockup(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = OnboardingMargin)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_headline),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            OnboardingPrimaryButton(
                text = stringResource(R.string.onboarding_welcome_cta),
                onClick = onContinue,
                leading = {
                    Image(
                        painter = painterResource(R.drawable.anilist_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.onboarding_welcome_new_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.onboarding_welcome_new_action),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onCreateAccount)
                )
            }
        }
    }
}

@Composable
private fun BrandLockup(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_monochrome),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(52.dp)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = Color.White
        )
    }
}

/**
 * Three columns of covers on a slow vertical drift, rotated off-axis so the grid reads as artwork
 * rather than as a list. The whole thing is one `graphicsLayer` rotation over an oversized box, so
 * the rotated corners never expose the background.
 */
@Composable
private fun PosterMarquee(
    covers: List<String>,
    modifier: Modifier = Modifier
) {
    if (covers.isEmpty()) {
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
        )
        return
    }

    val columns = remember(covers) {
        val padded = if (covers.size >= 9) covers else List(9) { covers[it % covers.size] }
        List(3) { column -> padded.filterIndexed { index, _ -> index % 3 == column } }
    }

    val transition = rememberInfiniteTransition(label = "Marquee")

    Box(modifier = modifier.clipToBounds()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = -14f
                    scaleX = 1.9f
                    scaleY = 1.9f
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                columns.forEachIndexed { index, columnCovers ->
                    val duration = 46_000 + index * 9_000
                    val offset by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(duration, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "MarqueeColumn$index"
                    )
                    MarqueeColumn(
                        covers = columnCovers,
                        offset = if (index % 2 == 0) offset else 1f - offset,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarqueeColumn(
    covers: List<String>,
    offset: Float,
    modifier: Modifier = Modifier
) {
    // The list is drawn twice and shifted by a full copy's height, so the wrap point is seamless.
    val doubled = remember(covers) { covers + covers }

    Column(
        modifier = modifier
            .wrapContentHeight(align = Alignment.Top, unbounded = true)
            .graphicsLayer { translationY = -offset * size.height / 2f },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        doubled.forEach { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        }
    }
}
