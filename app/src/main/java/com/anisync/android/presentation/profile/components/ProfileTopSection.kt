package com.anisync.android.presentation.profile.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
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
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.profile.util.formatProfileRelativeTime
import com.anisync.android.ui.theme.emphasis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Hoisted static layout constants to prevent reallocation
private val BannerHeight = 320.dp
private val CardOverlap = 64.dp
private val AvatarSize = 132.dp
private val AvatarHalfSize = AvatarSize / 2
private val ContentCardShape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)

private val RainbowColors = listOf(
    Color(0xFFE91E63),
    Color(0xFFFF9800),
    Color(0xFFFFEB3B),
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFF9C27B0)
)

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
    unreadNotificationCount: Int = 0,
    topActionIcon: ImageVector = Icons.Default.Settings,
    onTopActionClick: () -> Unit = onSettingsClick,
    modifier: Modifier = Modifier
) {


    // Cached to avoid format initialization over multiple re-renders
    val joinedDate = remember(profile.createdAt) {
        profile.createdAt?.let {
            val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            "Joined ${sdf.format(Date(it))}"
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BannerHeight)
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

                // Cache the gradient brush
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
                .padding(top = BannerHeight - CardOverlap),
            shape = ContentCardShape,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = AvatarHalfSize + 24.dp, bottom = 16.dp),
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
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(4.dp))

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
                                    style = MaterialTheme.typography.labelLarge.emphasis(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            if (profile.donatorTier > 0) {
                                val badgeText = profile.donatorBadge?.takeIf { it.isNotBlank() } ?: "Donator"
                                val isRainbow = profile.donatorTier >= 5

                                if (isRainbow) {
                                    RainbowDonatorBadge(badgeText = badgeText)
                                } else {
                                    RegularDonatorBadge(
                                        badgeText = badgeText,
                                        tier = profile.donatorTier
                                    )
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
                                    val formattedRole = remember(role) { formatModeratorRole(role) }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = formattedRole,
                                            style = MaterialTheme.typography.labelSmall.emphasis(),
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (joinedDate != null) {
                                    Text(
                                        text = joinedDate,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (joinedDate != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = joinedDate,
                                style = MaterialTheme.typography.bodyMedium,
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
                            style = MaterialTheme.typography.labelLarge.emphasis()
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = BannerHeight - CardOverlap - AvatarHalfSize),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(MaterialShapes.Clover8Leaf.toShape())
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialShapes.Clover8Leaf.toShape()
                    )
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialShapes.Clover8Leaf.toShape()
                    )
            ) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = stringResource(R.string.content_description_profile_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
            ) {
                if (isOwnProfile) {
                    FilledTonalIconButton(
                        onClick = onEditProfileClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.profile_edit),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onNotificationsClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (unreadNotificationCount > 0) {
                            val openLabel = stringResource(R.string.notifications_open)
                            val countLabel = unreadCountAccessibilityLabel(unreadNotificationCount)
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(formatBadgeCount(unreadNotificationCount))
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "$openLabel, $countLabel",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.notifications_open),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    if (isFollowing) {
                        FilledTonalIconButton(
                            onClick = onFollowClick,
                            enabled = !isFollowLoading,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                                contentDescription = if (isFollowing) stringResource(R.string.profile_following) else stringResource(
                                    R.string.profile_follow
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        OutlinedIconButton(
                            onClick = onFollowClick,
                            enabled = !isFollowLoading,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(48.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                                contentDescription = if (isFollowing) stringResource(R.string.profile_following) else stringResource(R.string.profile_follow),
                                modifier = Modifier.size(20.dp)
                            )
                        }
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

/**
 * Isolated composable for the rainbow animation.
 * Moving this out of [ProfileTopSection] prevents the entire profile header
 * from recomposing on every frame of the animation.
 */
@Composable
private fun RainbowDonatorBadge(badgeText: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = RainbowColors.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = RainbowColors.size * 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbowProgress"
    )

    val colorIndex = progress.toInt() % RainbowColors.size
    val nextIndex = (colorIndex + 1) % RainbowColors.size
    val fraction = progress - progress.toInt()
    val animatedColor = lerp(RainbowColors[colorIndex], RainbowColors[nextIndex], fraction)

    Surface(
        color = animatedColor,
        shape = CircleShape,
        modifier = modifier
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelLarge.emphasis(),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun RegularDonatorBadge(badgeText: String, tier: Int, modifier: Modifier = Modifier) {
    val donatorColor = remember(tier) {
        when (tier) {
            1 -> Color(0xFF78909C) // Blue-grey for $1
            2 -> Color(0xFF5C6BC0) // Indigo for $3
            3 -> Color(0xFFFFB300) // Amber for $5
            else -> Color(0xFFFF6F00) // Deep amber for $10
        }
    }
    Surface(
        color = donatorColor,
        shape = CircleShape,
        modifier = modifier
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelLarge.emphasis(),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Extracted pure formatting function outside of Compose scope.
 */
private fun formatModeratorRole(role: String): String {
    return role.replace("_", " ").lowercase()
        .replaceFirstChar { it.uppercase() }
        .replace(" [a-z]".toRegex()) { it.value.uppercase() }
}

/**
 * Per Material 3, large numeric badges overflow to "999+" when the count
 * exceeds three digits — keeps the badge legible without truncating.
 */
private fun formatBadgeCount(count: Int): String =
    if (count > 999) "999+" else count.toString()

/**
 * TalkBack label for the inbox badge. M3 accessibility guidance: numeric
 * badges should announce the count via a pluralised string; counts beyond
 * the displayed cap are read as "more than {cap}" so the spoken value
 * matches what the user sees.
 */
@Composable
private fun unreadCountAccessibilityLabel(count: Int): String =
    if (count > 999) {
        stringResource(R.string.notifications_unread_overflow_a11y)
    } else {
        androidx.compose.ui.res.pluralStringResource(
            R.plurals.notifications_unread_count_a11y,
            count,
            count
        )
    }
