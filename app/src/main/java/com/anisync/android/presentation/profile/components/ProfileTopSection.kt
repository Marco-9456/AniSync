package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.LocalPullRefreshOffset
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.navigation.BackTrailButton
import com.anisync.android.presentation.profile.ProfileTab
import com.anisync.android.presentation.profile.ProfileTabsButtonGroup
import com.anisync.android.presentation.profile.ProfileWideLoadingSkeleton
import com.anisync.android.presentation.profile.sections.BioPreview
import com.anisync.android.presentation.util.LocalAdaptiveInfo

// Hoisted static layout constants to prevent reallocation. The avatar size is shared with the wide
// identity pane (see ProfileIdentity.kt).
private val BannerHeight = 196.dp

/**
 * The compact (phone) profile header: the banner, or a tonal band when there is none, the avatar
 * on its lower edge and the action buttons opposite it. The banner /
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
    isFollowerOfViewer: Boolean = false,
    isFollowLoading: Boolean = false,
    onFollowClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    accounts: ProfileAccounts? = null,
    onBackClick: (() -> Unit)? = null,
    followingTotal: Int? = null,
    followersTotal: Int? = null,
    onPeopleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val band = profile.bannerUrl == null
    val headerHeight = profileBackdropHeight(band)
    val avatarTop = if (band) headerHeight + 8.dp else headerHeight - ProfileAvatarHalfSize
    val actionsTop = if (band) headerHeight + 64.dp else headerHeight + 8.dp
    val identityTop = if (band) headerHeight + 116.dp else headerHeight + 60.dp
    // A pull carries this copy down with the list, so it steps aside for the one held behind it.
    val pullOffset = LocalPullRefreshOffset.current

    Box(modifier = modifier.fillMaxWidth()) {
        ProfileBackdrop(
            profile = profile,
            isOwnProfile = isOwnProfile,
            onSettingsClick = onSettingsClick,
            onBackClick = onBackClick,
            modifier = Modifier.graphicsLayer { alpha = if (pullOffset() > 0f) 0f else 1f }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = identityTop)
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            ProfileIdentityInfo(
                profile = profile,
                isOwnProfile = isOwnProfile,
                viewerFollows = isFollowing,
                followsViewer = isFollowerOfViewer,
                followingTotal = followingTotal,
                followersTotal = followersTotal,
                onPeopleClick = onPeopleClick,
                accounts = accounts,
                modifier = Modifier.fillMaxWidth()
            )

            val about = profile.about
            if (!about.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                BioPreview(about = about, onOpen = onShowBiography)
            }
        }

        // An unshaped avatar taller than wide may overflow upward rather than be cropped.
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .offset(y = avatarTop)
                .height(ProfileAvatarSize),
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

        ProfileActionButtons(
            isOwnProfile = isOwnProfile,
            isFollowing = isFollowing,
            isFollowLoading = isFollowLoading,
            onFollowClick = onFollowClick,
            onMessageClick = onMessageClick,
            onEditProfileClick = onEditProfileClick,
            onNotificationsClick = onNotificationsClick,
            unreadNotificationCount = unreadNotificationCount,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = actionsTop)
                .padding(end = 20.dp)
        )
    }
}

/**
 * The banner, or the tonal band when there is none, with the controls that sit on it. The list
 * draws one at the top of the header, and the pull to refresh holds a second one still behind the
 * list while a pull carries everything from the avatar down.
 */
@Composable
internal fun ProfileBackdrop(
    profile: UserProfile,
    isOwnProfile: Boolean,
    onSettingsClick: () -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val band = profile.bannerUrl == null
    val height = profileBackdropHeight(band)
    if (band) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = modifier
                .fillMaxWidth()
                .height(height)
        ) {
            Box(modifier = Modifier.statusBarsPadding()) {
                if (isOwnProfile) {
                    ProfileOwnTopBar(onBanner = false, onSettingsClick = onSettingsClick)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        if (onBackClick != null) BackTrailButton(onClick = onBackClick)
                        Spacer(modifier = Modifier.weight(1f))
                        ProfileMenuButton(
                            name = profile.name,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    } else {
        ProfileBannerSurface(
            profile = profile,
            isOwnProfile = isOwnProfile,
            topActionIcon = Icons.Default.Settings,
            onTopActionClick = onSettingsClick,
            height = height,
            onBackClick = onBackClick,
            ownTopBar = { ProfileOwnTopBar(onBanner = true, onSettingsClick = onSettingsClick) },
            modifier = modifier
        )
    }
}

@Composable
private fun profileBackdropHeight(band: Boolean): Dp {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return if (band) statusBar + 64.dp else maxOf(BannerHeight, statusBar + 120.dp)
}

/**
 * The first load: the header's shape, with only the name the link already gave, so nothing moves
 * when the profile lands. The rate limit toast sits over this too, and the screen waits it out.
 */
@Composable
fun ProfileLoadingSkeleton(name: String?, onBackClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    if (LocalAdaptiveInfo.current.supportsTwoPane) {
        ProfileWideLoadingSkeleton(name = name, onBackClick = onBackClick, modifier = modifier)
        return
    }
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerHeight = maxOf(BannerHeight, statusBar + 120.dp)
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    val quiet = MaterialTheme.colorScheme.surfaceContainerLow
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(block)
        )
        Column(modifier = Modifier.padding(top = headerHeight + 60.dp)) {
            if (name != null) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            listOf(0.55f, 0.75f, 0.57f).forEach { fraction ->
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp, bottom = 10.dp)
                        .fillMaxWidth(fraction)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(block)
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(block)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ProfileTabsButtonGroup(selectedTab = ProfileTab.OVERVIEW, lastListTab = ProfileTab.ANIME, onTabSelected = {})
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(quiet)
            )
            Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                AppCircularProgressIndicator()
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .offset(y = headerHeight - ProfileAvatarHalfSize)
                .size(ProfileAvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )
        if (onBackClick != null) {
            BackTrailButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 12.dp)
            )
        }
    }
}
