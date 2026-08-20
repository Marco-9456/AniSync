package com.anisync.android.presentation.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.presentation.onboarding.PermissionStates

/** The four system toggles the set-up step offers, in the order they are listed. */
enum class PermissionRow { Notifications, Battery, Links, Hibernation }

/**
 * Step 1 of 2: the background-work asks. Every row states what breaks without it, because none of
 * these are grantable in-app — each one hands the user off to a system screen and reads the answer
 * back on resume.
 */
@Composable
fun PermissionsStep(
    permissions: PermissionStates,
    onRequest: (PermissionRow) -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        OnboardingStepHeader(
            step = 1,
            total = 2,
            onSkip = onSkip,
            modifier = Modifier
                .padding(horizontal = OnboardingMargin)
                .padding(top = 12.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OnboardingMargin)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            OnboardingHeadline(stringResource(R.string.onboarding_permissions_headline))

            Spacer(modifier = Modifier.height(12.dp))

            OnboardingBody(stringResource(R.string.onboarding_permissions_body))

            Spacer(modifier = Modifier.height(22.dp))

            GrantedCounter(granted = permissions.grantedCount, total = permissions.total)

            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionCard(
                    icon = Icons.Outlined.NotificationsActive,
                    tint = OnboardingAccents.Red,
                    title = stringResource(R.string.onboarding_perm_alerts_title),
                    body = stringResource(R.string.onboarding_perm_alerts_body),
                    granted = permissions.notifications,
                    actionLabel = stringResource(R.string.onboarding_perm_action_allow),
                    onClick = { onRequest(PermissionRow.Notifications) }
                )
                PermissionCard(
                    icon = Icons.Outlined.Autorenew,
                    tint = OnboardingAccents.Amber,
                    title = stringResource(R.string.onboarding_perm_battery_title),
                    body = stringResource(R.string.onboarding_perm_battery_body),
                    granted = permissions.batteryExempt,
                    actionLabel = stringResource(R.string.onboarding_perm_action_allow),
                    onClick = { onRequest(PermissionRow.Battery) }
                )
                PermissionCard(
                    icon = Icons.Outlined.Link,
                    tint = OnboardingAccents.Green,
                    title = stringResource(R.string.onboarding_perm_links_title),
                    body = stringResource(R.string.onboarding_perm_links_body),
                    granted = permissions.linksVerified,
                    actionLabel = stringResource(R.string.onboarding_perm_action_open),
                    onClick = { onRequest(PermissionRow.Links) }
                )
                PermissionCard(
                    icon = Icons.Outlined.Bedtime,
                    tint = OnboardingAccents.Blue,
                    title = stringResource(R.string.onboarding_perm_hibernation_title),
                    body = stringResource(R.string.onboarding_perm_hibernation_body),
                    badge = stringResource(R.string.onboarding_perm_badge_android12),
                    granted = permissions.hibernationExempt,
                    actionLabel = stringResource(R.string.onboarding_perm_action_turn_off),
                    onClick = { onRequest(PermissionRow.Hibernation) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        OnboardingNote(
            text = stringResource(R.string.onboarding_permissions_note),
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

@Composable
private fun GrantedCounter(granted: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.onboarding_permissions_progress, granted, total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                val filled = index < granted
                val alpha by animateFloatAsState(if (filled) 1f else 0.28f, label = "GrantTick")
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) {
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBlob(icon = icon, tint = tint)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(OnboardingAccents.Green),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.onboarding_state_on),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
