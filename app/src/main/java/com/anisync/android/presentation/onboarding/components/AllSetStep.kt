package com.anisync.android.presentation.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R

/** The closing screen is a single centred statement; it gains nothing from a tablet's width. */
private val DoneMaxWidth = 560.dp

/**
 * The closing screen: what the flow actually achieved, then the one thing worth doing next. The
 * widget nudge renders the same rows the widget picker shows, so what the user taps to add looks
 * like what they saw a moment ago in the picker.
 */
@Composable
fun AllSetStep(
    libraryEntries: Int,
    alertsOn: Boolean,
    linksOn: Boolean,
    widgetPinSupported: Boolean,
    onAddWidget: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isWideOnboarding()) {
        WideAllSet(
            libraryEntries = libraryEntries,
            alertsOn = alertsOn,
            linksOn = linksOn,
            widgetPinSupported = widgetPinSupported,
            onAddWidget = onAddWidget,
            onFinish = onFinish,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onboardingColumn(DoneMaxWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        CompletionEmblem()

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.onboarding_done_headline),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboarding_done_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = onboardingMargin())
        )

        Spacer(modifier = Modifier.height(22.dp))

        StatPills(libraryEntries = libraryEntries, alertsOn = alertsOn, linksOn = linksOn)

        Spacer(modifier = Modifier.height(26.dp))

        if (widgetPinSupported) {
            WidgetNudge(
                onClick = onAddWidget,
                modifier = Modifier.padding(horizontal = onboardingMargin())
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OnboardingPrimaryButton(
            text = stringResource(R.string.onboarding_done_cta),
            onClick = onFinish,
            modifier = Modifier
                .padding(horizontal = onboardingMargin())
                .padding(bottom = 24.dp)
        )
    }
}

/**
 * Wide layout: the outcome and its action on the left, the one thing worth doing next on the right.
 * Stacking them centred would leave a tablet's whole lower half empty.
 */
@Composable
private fun WideAllSet(
    libraryEntries: Int,
    alertsOn: Boolean,
    linksOn: Boolean,
    widgetPinSupported: Boolean,
    onAddWidget: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingWidePadding)
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(OnboardingPaneGap)
    ) {
        OnboardingCentredScroll(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompletionEmblem()
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.onboarding_done_headline),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.onboarding_done_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(22.dp))
            StatPills(libraryEntries = libraryEntries, alertsOn = alertsOn, linksOn = linksOn)
            Spacer(modifier = Modifier.height(32.dp))
            OnboardingPrimaryButton(
                text = stringResource(R.string.onboarding_done_cta),
                onClick = onFinish,
                modifier = Modifier.widthIn(max = 360.dp)
            )
        }

        OnboardingCentredScroll(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (widgetPinSupported) {
                WidgetNudge(onClick = onAddWidget)
            }
        }
    }
}

/**
 * Same plate construction as the set-up rows, at emblem scale, so the green reads as the flow's own
 * "granted" colour rather than a one-off.
 */
@Composable
private fun CompletionEmblem() {
    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(MaterialShapes.Cookie12Sided.toShape())
            .background(OnboardingAccents.Green.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = OnboardingAccents.Green,
            modifier = Modifier.size(64.dp)
        )
    }
}

/** What the flow actually achieved, in three readings. */
@Composable
private fun StatPills(libraryEntries: Int, alertsOn: Boolean, linksOn: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatPill(
            dot = OnboardingAccents.Blue,
            text = stringResource(R.string.onboarding_sync_entries, libraryEntries)
        )
        StatPill(
            dot = if (alertsOn) OnboardingAccents.Green else MaterialTheme.colorScheme.outline,
            text = stringResource(
                if (alertsOn) R.string.onboarding_done_pill_alerts_on
                else R.string.onboarding_done_pill_alerts_off
            )
        )
        StatPill(
            dot = if (linksOn) OnboardingAccents.Amber else MaterialTheme.colorScheme.outline,
            text = stringResource(
                if (linksOn) R.string.onboarding_done_pill_links_on
                else R.string.onboarding_done_pill_links_off
            )
        )
    }
}

@Composable
private fun StatPill(dot: Color, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WidgetNudge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_done_widget_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.onboarding_done_widget_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                WidgetPreviewRow(
                    poster = Brush.linearGradient(
                        listOf(
                            colorResource(R.color.widget_tertiary_container),
                            colorResource(R.color.widget_primary)
                        )
                    ),
                    title = stringResource(R.string.widget_preview_title_1),
                    episode = stringResource(R.string.widget_preview_episode_1),
                    pill = stringResource(R.string.widget_preview_time_1)
                )
                WidgetPreviewRow(
                    poster = Brush.linearGradient(
                        listOf(
                            colorResource(R.color.widget_primary_container),
                            colorResource(R.color.widget_secondary_container)
                        )
                    ),
                    title = stringResource(R.string.widget_preview_title_2),
                    episode = stringResource(R.string.widget_preview_episode_2),
                    pill = stringResource(R.string.widget_preview_time_2)
                )
            }
        }
    }
}

/**
 * One row of the Up Next widget, in the widget's own palette rather than the app theme — these
 * colours resolve to the system accent on API 31+ exactly as the real widget does.
 */
@Composable
private fun WidgetPreviewRow(
    poster: Brush,
    title: String,
    episode: String,
    pill: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorResource(R.color.widget_surface))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The picker's stand-in cover is a <shape> gradient, redrawn here rather than loaded:
        // painterResource rejects shape drawables outright.
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(57.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(poster)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colorResource(R.color.widget_on_surface)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = episode,
                fontSize = 12.sp,
                maxLines = 1,
                color = colorResource(R.color.widget_on_surface_variant)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colorResource(R.color.widget_primary_container))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = pill,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = colorResource(R.color.widget_on_primary_container)
            )
        }
    }
}
