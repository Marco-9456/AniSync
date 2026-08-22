package com.anisync.android.presentation.onboarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.presentation.util.LocalAdaptiveInfo

/**
 * Shared furniture for the first-run steps: the numbered header, the type scale the steps share, the
 * scalloped icon plate, and the one full-width action at the bottom of every screen.
 */

/** Side margin every onboarding screen sits on. */
val OnboardingMargin = 20.dp

/**
 * Largest a step's content column is allowed to get. Past this the flow stops reading as a focused
 * task and turns into a banner of stretched text, which is exactly what a phone-only layout does to
 * a tablet.
 */
val OnboardingColumnMaxWidth = 520.dp

/** Wide enough to put a step's artwork beside its content instead of above it. */
@Composable
fun isWideOnboarding(): Boolean = LocalAdaptiveInfo.current.isExpandedOrWider

/**
 * Pane split used by every wide step: context and the primary action on the left, the step's own
 * controls on the right. Keeping one ratio across the flow means the headline and the Continue
 * button never move between steps.
 */
const val OnboardingContextPaneWeight = 0.42f
const val OnboardingContentPaneWeight = 0.58f

/** Outer inset and gutter for the wide two-pane steps. */
val OnboardingWidePadding = 40.dp
val OnboardingPaneGap = 32.dp

/**
 * A column that centres its content in the available height while it fits, and scrolls once it does
 * not. Top-aligning instead would strand a short step against the top of a tablet with a screen's
 * worth of empty space under it.
 */
@Composable
fun OnboardingCentredScroll(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/** Horizontal page inset for the current width. */
@Composable
fun onboardingMargin(): Dp =
    if (LocalAdaptiveInfo.current.isCompact) OnboardingMargin else 32.dp

/**
 * Caps and centres a column so nothing stretches across a wide window. A no-op on compact, where the
 * step already fills a phone exactly as designed.
 */
@Composable
fun Modifier.onboardingColumn(max: Dp = OnboardingColumnMaxWidth): Modifier {
    val adaptive = LocalAdaptiveInfo.current
    if (adaptive.isCompact) return this
    // Never wider than the app's own reading cap for this width class, so a step that asks for a
    // two-pane width still collapses sensibly on a tablet in portrait.
    val cap = adaptive.contentMaxWidth
    val limit = if (cap == Dp.Unspecified) max else minOf(max, cap)
    return this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = limit)
}

/**
 * Status hues the flow uses for the permission plates and the completion pills. They are semantic
 * (this is on, this is off, this is waiting) rather than themed, so they stay legible against every
 * accent the user can pick on the personalise step.
 */
object OnboardingAccents {
    val Green = Color(0xFF57C77F)
    val Amber = Color(0xFFE0B23C)
    val Blue = Color(0xFF5C9DF5)
    val Red = Color(0xFFE2685C)
}

@Composable
fun OnboardingStepHeader(
    step: Int,
    total: Int,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            // Same control the settings screens use: a default-size IconButton so the touch
            // target and the glyph match the rest of the app rather than shrinking for the header.
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                val filled = index < step
                val color by animateColorAsState(
                    targetValue = if (filled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    label = "StepSegment"
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = stringResource(R.string.onboarding_step_of, step, total),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        TextButton(onClick = onSkip, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun OnboardingHeadline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 40.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@Composable
fun OnboardingBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** The small uppercase label above a group of choices. */
@Composable
fun OnboardingSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** The muted aside that sits directly above the action button. */
@Composable
fun OnboardingNote(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * The scalloped plate the flow puts behind every icon. Same construction the Settings rows use: a
 * tinted shape at low opacity with the glyph at full strength over it.
 */
@Composable
fun IconBlob(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(tint.copy(alpha = 0.22f))
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Vertical rhythm helper so the steps do not each invent their own spacing constants. */
@Composable
fun OnboardingColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = OnboardingMargin),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}
