package com.anisync.android.presentation.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.onboarding.SyncProgress
import com.anisync.android.presentation.onboarding.TaskState
import com.anisync.android.presentation.profile.components.ProfileAvatarHalfSize
import com.anisync.android.presentation.profile.components.ProfileAvatarSize

/**
 * Banner geometry lifted from the profile header (`ProfileTopSection`) so the first thing the user
 * sees of their account is arranged the way the Profile tab will arrange it: banner, a rounded
 * content card overlapping its lower edge, and the avatar straddling the seam. The banner is
 * shorter than the profile's 320.dp because the import checklist has to fit underneath it.
 */
private val BannerHeight = 260.dp
private val WideBannerHeight = 200.dp
private val CardOverlap = 64.dp
private val ContentCardShape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)
private val HeaderMargin = 24.dp

/** Widest the phone composition runs before the banner becomes a billboard. */
private val HeaderMaxWidth = 760.dp

/**
 * The one-time account import, shown while it runs. Every row reports a real outcome — the entry
 * count is what actually landed in Room, the airing count is what that library says is due this
 * week — so the screen is a receipt rather than a loading animation.
 */
@Composable
fun SyncingStep(
    username: String,
    avatarUrl: String?,
    bannerUrl: String?,
    progress: SyncProgress,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isWideOnboarding()) {
        WideSyncing(username, avatarUrl, bannerUrl, progress, onContinue, modifier)
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            IdentityHeader(
                username = username,
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                bannerHeight = BannerHeight,
                margin = HeaderMargin,
                rounded = false,
                modifier = Modifier.onboardingColumn(HeaderMaxWidth)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                ImportChecklist(progress = progress)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        OnboardingNote(
            text = stringResource(R.string.onboarding_sync_note),
            modifier = Modifier
                .onboardingColumn(HeaderMaxWidth)
                .padding(horizontal = onboardingMargin())
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingPrimaryButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            modifier = Modifier
                .onboardingColumn()
                .padding(horizontal = onboardingMargin())
                .padding(bottom = 24.dp)
        )
    }
}

/**
 * Wide layout: who you are signed in as on the left with the action under it, what is being brought
 * over on the right. The checklist is the part that changes while you watch, so it gets the pane
 * with room to breathe.
 */
@Composable
private fun WideSyncing(
    username: String,
    avatarUrl: String?,
    bannerUrl: String?,
    progress: SyncProgress,
    onContinue: () -> Unit,
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
                .weight(OnboardingContextPaneWeight)
                .fillMaxHeight()
        ) {
            IdentityHeader(
                username = username,
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                bannerHeight = WideBannerHeight,
                margin = 20.dp,
                rounded = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            OnboardingNote(text = stringResource(R.string.onboarding_sync_note))

            Spacer(modifier = Modifier.height(16.dp))

            OnboardingPrimaryButton(
                text = stringResource(R.string.onboarding_continue),
                onClick = onContinue
            )
        }

        OnboardingCentredScroll(
            modifier = Modifier
                .weight(OnboardingContentPaneWeight)
                .fillMaxHeight()
        ) {
            ImportChecklist(progress = progress)
        }
    }
}

/**
 * Banner, overlapping content card and the avatar straddling the seam, with the signed-in-as block
 * underneath. [content] is whatever the layout wants below the intro line — the checklist on a
 * phone, nothing on a tablet where it lives in the other pane.
 */
@Composable
private fun IdentityHeader(
    username: String,
    avatarUrl: String?,
    bannerUrl: String?,
    bannerHeight: Dp,
    margin: Dp,
    rounded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Banner(
            bannerUrl = bannerUrl,
            username = username,
            height = bannerHeight,
            modifier = if (rounded) Modifier.clip(RoundedCornerShape(28.dp)) else Modifier
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = bannerHeight - CardOverlap),
            shape = ContentCardShape,
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = margin)
                    .padding(top = ProfileAvatarHalfSize + 20.dp, bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_sync_signed_in_as),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = username,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.onboarding_sync_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                content()
            }
        }

        // Straddles the banner/card seam exactly as the profile header does.
        UserAvatar(
            url = avatarUrl,
            contentDescription = username,
            size = ProfileAvatarSize,
            borderWidth = 2.dp,
            framePadding = 3.dp,
            isProfileHeader = true,
            modifier = Modifier
                .padding(horizontal = margin)
                .offset(y = bannerHeight - CardOverlap - ProfileAvatarHalfSize)
        )
    }
}

@Composable
private fun Banner(
    bannerUrl: String?,
    username: String,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (bannerUrl != null) {
            AsyncImage(
                model = bannerUrl,
                contentDescription = stringResource(R.string.content_description_cover, username),
                contentScale = ContentScale.Crop,
                // Top-anchored like the profile banner: AniList banners carry their subject up top.
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )

            val scrimBrush = remember {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.2f)
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimBrush)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportChecklist(progress: SyncProgress, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.onboarding_sync_card_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            ChecklistRow(
                state = progress.library,
                label = stringResource(R.string.onboarding_sync_row_library),
                value = if (progress.library == TaskState.Done) {
                    stringResource(R.string.onboarding_sync_entries, progress.libraryEntries)
                } else {
                    stringResource(R.string.onboarding_sync_checking)
                }
            )
            ChecklistRow(
                state = progress.airing,
                label = stringResource(R.string.onboarding_sync_row_airing),
                value = when (progress.airing) {
                    TaskState.Done -> stringResource(R.string.onboarding_sync_this_week, progress.airingThisWeek)
                    TaskState.Running -> stringResource(R.string.onboarding_sync_checking)
                    TaskState.Pending -> stringResource(R.string.onboarding_sync_waiting)
                }
            )
            ChecklistRow(
                state = progress.notifications,
                label = stringResource(R.string.onboarding_sync_row_notifications),
                value = when (progress.notifications) {
                    TaskState.Done -> if (progress.notificationsOn) {
                        stringResource(R.string.onboarding_state_on)
                    } else {
                        stringResource(R.string.onboarding_state_off)
                    }

                    TaskState.Running -> stringResource(R.string.onboarding_sync_checking)
                    TaskState.Pending -> stringResource(R.string.onboarding_sync_waiting)
                }
            )
            ChecklistRow(
                state = progress.widgets,
                label = stringResource(R.string.onboarding_sync_row_widgets),
                value = when (progress.widgets) {
                    TaskState.Done -> if (progress.widgetsPlaced > 0) {
                        stringResource(R.string.onboarding_sync_widgets_placed, progress.widgetsPlaced)
                    } else {
                        stringResource(R.string.onboarding_sync_widgets_none)
                    }

                    TaskState.Running -> stringResource(R.string.onboarding_sync_checking)
                    TaskState.Pending -> stringResource(R.string.onboarding_sync_waiting)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            val fraction by animateFloatAsState(progress.fraction, label = "ImportProgress")
            LinearWavyProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChecklistRow(state: TaskState, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaskStateIcon(state)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (state == TaskState.Pending) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The three row states share a 24.dp slot. The running one is the app's wavy spinner with its wave
 * geometry scaled to that slot: the component's defaults (48.dp container, 15.dp wavelength, 4.dp
 * strokes and gap) are only coherent at full size, and shrinking the container alone leaves barely
 * one visible wave with an outsized gap.
 */
@Composable
private fun TaskStateIcon(state: TaskState) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    when (state) {
        TaskState.Done -> Box(
            modifier = Modifier
                .size(TaskIconSize)
                .clip(CircleShape)
                .background(OnboardingAccents.Green),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(15.dp)
            )
        }

        TaskState.Running -> AppCircularProgressIndicator(
            modifier = Modifier.size(TaskIconSize),
            strokeWidth = 2.dp,
            wavelength = 7.5.dp,
            gapSize = 2.dp
        )

        TaskState.Pending -> Canvas(modifier = Modifier.size(TaskIconSize)) {
            drawCircle(
                color = outline,
                radius = size.minDimension / 2 - 1.dp.toPx(),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(3.dp.toPx(), 3.dp.toPx())
                    )
                )
            )
        }
    }
}

/** Half the wavy indicator's 48.dp natural container, so its geometry halves cleanly. */
private val TaskIconSize = 24.dp
