package com.anisync.android.presentation.profile.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.profile.util.formatProfileRelativeTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileTopSection(
    profile: UserProfile,
    isOwnProfile: Boolean,
    onSettingsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onShowBiography: () -> Unit,
    isFollowing: Boolean = false,
    isFollowLoading: Boolean = false,
    onFollowClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    topActionIcon: ImageVector = Icons.Default.Settings,
    onTopActionClick: () -> Unit = onSettingsClick,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.background

    val bannerHeight = 320.dp
    val cardOverlap = 64.dp
    val avatarSize = 132.dp

    val contentCardShape = remember {
        RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)
    }
    val expressiveAvatarShape = remember {
        ExpressiveBadgeShape(waves = 16, amplitude = 0.04f)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (profile.bannerUrl != null) {
                AsyncImage(
                    model = profile.bannerUrl,
                    contentDescription = stringResource(
                        R.string.content_description_cover,
                        profile.name
                    ),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.2f)
                                )
                            )
                        )
                )
            }

            // Top action button (settings/share)
            FilledTonalIconButton(
                onClick = onTopActionClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = topActionIcon,
                    contentDescription = if (isOwnProfile) stringResource(R.string.settings) else stringResource(R.string.action_share),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = bannerHeight - cardOverlap),
            shape = contentCardShape,
            color = surfaceColor,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = (avatarSize / 2) + 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val level = remember(profile.animeCount, profile.mangaCount) {
                                (profile.animeCount + profile.mangaCount) / 10
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = stringResource(R.string.profile_level, level),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            if (profile.donatorTier > 0) {
                                val badgeText = profile.donatorBadge?.takeIf { it.isNotBlank() } ?: "Donator"
                                val isRainbow = profile.donatorTier >= 5

                                if (isRainbow) {
                                    val rainbowColors = remember {
                                        listOf(
                                            Color(0xFFE91E63),
                                            Color(0xFFFF9800),
                                            Color(0xFFFFEB3B),
                                            Color(0xFF4CAF50),
                                            Color(0xFF2196F3),
                                            Color(0xFF9C27B0)
                                        )
                                    }
                                    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")
                                    val progress by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = rainbowColors.size.toFloat(),
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(
                                                durationMillis = rainbowColors.size * 1000,
                                                easing = LinearEasing
                                            ),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "rainbowProgress"
                                    )
                                    val colorIndex = progress.toInt() % rainbowColors.size
                                    val nextIndex = (colorIndex + 1) % rainbowColors.size
                                    val fraction = progress - progress.toInt()
                                    val animatedColor = lerp(rainbowColors[colorIndex], rainbowColors[nextIndex], fraction)

                                    Surface(
                                        color = animatedColor,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                } else {
                                    val donatorColor = when (profile.donatorTier) {
                                        1 -> Color(0xFF78909C) // Blue-grey for $1
                                        2 -> Color(0xFF5C6BC0) // Indigo for $3
                                        3 -> Color(0xFFFFB300) // Amber for $5
                                        else -> Color(0xFFFF6F00) // Deep amber for $10
                                    }
                                    Surface(
                                        color = donatorColor,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (profile.moderatorRoles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.moderatorRoles.forEach { role ->
                                    val formattedRole = remember(role) {
                                        role.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                            .replace(" [a-z]".toRegex()) { it.value.uppercase() }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = formattedRole,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (!isOwnProfile) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val activeTime = remember(profile.activeAt) {
                                    formatProfileRelativeTime(profile.activeAt)
                                }
                                Text(
                                    text = stringResource(R.string.profile_active, activeTime),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (profile.createdAt != null) {
                                    val joinedDate = remember(profile.createdAt) {
                                        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                                        "Joined ${sdf.format(Date(profile.createdAt))}"
                                    }
                                    Text(
                                        text = joinedDate,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (profile.createdAt != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val joinedDate = remember(profile.createdAt) {
                                val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                                "Joined ${sdf.format(Date(profile.createdAt))}"
                            }
                            Text(
                                text = joinedDate,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val hasBiography = !profile.about.isNullOrBlank()
                if (hasBiography) {
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = onShowBiography,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.profile_view_biography),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = bannerHeight - cardOverlap - (avatarSize / 2)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(surfaceColor, expressiveAvatarShape)
                    .padding(6.dp)
                    .clip(expressiveAvatarShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = stringResource(R.string.content_description_profile_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                if (isOwnProfile) {
                    Button(
                        onClick = onEditProfileClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.profile_edit),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onNotificationsClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.notifications_open),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        enabled = !isFollowLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isFollowLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                contentDescription = if (isFollowing) stringResource(R.string.profile_following) else stringResource(R.string.profile_follow),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFollowing) stringResource(R.string.profile_following) else stringResource(R.string.profile_follow),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onMessageClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = stringResource(R.string.profile_message),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
