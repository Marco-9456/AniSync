package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.ui.theme.emphasis

// Hoisted static layout constants to prevent reallocation. The avatar size is shared with the wide
// identity pane (see ProfileIdentity.kt).
private val BannerHeight = 320.dp
private val CardOverlap = 64.dp
private val ContentCardShape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)

/**
 * The compact (phone) profile header: a tall banner with the rounded content card overlapping its
 * lower edge, the avatar straddling the seam, and the action buttons opposite it. The banner /
 * identity info / action buttons are shared with the expanded two-pane layout via ProfileIdentity.kt.
 */
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
    showAccountSwitcher: Boolean = false,
    onAccountSwitchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        ProfileBannerSurface(
            profile = profile,
            isOwnProfile = isOwnProfile,
            topActionIcon = topActionIcon,
            onTopActionClick = onTopActionClick,
            height = BannerHeight
        )

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
                    .padding(top = ProfileAvatarHalfSize + 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                ProfileIdentityInfo(
                    profile = profile,
                    isOwnProfile = isOwnProfile,
                    modifier = Modifier.fillMaxWidth()
                )

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
                .offset(y = BannerHeight - CardOverlap - ProfileAvatarHalfSize),
            verticalAlignment = Alignment.Bottom
        ) {
            // Layout slot stays AvatarSize tall (so the name/content below isn't pushed),
            // but an unshaped "None" avatar that's taller than wide is allowed to overflow
            // upward into the banner instead of being cropped or covering the name.
            Box(
                modifier = Modifier.height(ProfileAvatarSize),
                contentAlignment = Alignment.BottomStart
            ) {
                UserAvatar(
                    url = profile.avatarUrl,
                    contentDescription = stringResource(R.string.content_description_profile_avatar),
                    size = ProfileAvatarSize,
                    borderWidth = 2.dp,
                    framePadding = 3.dp,
                    isProfileHeader = true,
                    modifier = Modifier.wrapContentHeight(Alignment.Bottom, unbounded = true)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ProfileActionButtons(
                isOwnProfile = isOwnProfile,
                isFollowing = isFollowing,
                isFollowLoading = isFollowLoading,
                onFollowClick = onFollowClick,
                onMessageClick = onMessageClick,
                onEditProfileClick = onEditProfileClick,
                onNotificationsClick = onNotificationsClick,
                unreadNotificationCount = unreadNotificationCount,
                showAccountSwitcher = showAccountSwitcher,
                onAccountSwitchClick = onAccountSwitchClick,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
