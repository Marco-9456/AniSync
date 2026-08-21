package com.anisync.android.presentation.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.onboarding.SyncProgress
import com.anisync.android.presentation.onboarding.TaskState

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
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.34f)
        ) {
            if (bannerUrl != null) {
                AsyncImage(
                    model = bannerUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            val background = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to background.copy(alpha = 0.55f),
                            0.55f to background.copy(alpha = 0.72f),
                            1f to background
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = OnboardingMargin),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    contentDescription = username,
                    size = 104.dp,
                    url = avatarUrl
                )
                Spacer(modifier = Modifier.height(14.dp))
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
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_sync_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OnboardingMargin)
        )

        Spacer(modifier = Modifier.height(24.dp))

        ImportChecklist(
            progress = progress,
            modifier = Modifier.padding(horizontal = OnboardingMargin)
        )

        Spacer(modifier = Modifier.weight(1f))

        OnboardingNote(
            text = stringResource(R.string.onboarding_sync_note),
            modifier = Modifier.padding(horizontal = OnboardingMargin)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingPrimaryButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            modifier = Modifier
                .padding(horizontal = OnboardingMargin)
                .padding(bottom = 24.dp)
        )
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

@Composable
private fun TaskStateIcon(state: TaskState) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    when (state) {
        TaskState.Done -> Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(OnboardingAccents.Green),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(14.dp)
            )
        }

        TaskState.Running -> AppCircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp
        )

        TaskState.Pending -> Canvas(modifier = Modifier.size(22.dp)) {
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
