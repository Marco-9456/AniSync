package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.AppCircularProgressIndicator
import com.anisync.android.presentation.components.hiddenWhilePulled
import com.anisync.android.presentation.components.navigation.BackTrailButton
import com.anisync.android.presentation.profile.ProfileSocialTab
import com.anisync.android.presentation.profile.ProfileTab
import com.anisync.android.presentation.profile.ProfileTabsButtonGroup
import com.anisync.android.presentation.profile.ProfileWideLoadingSkeleton
import com.anisync.android.presentation.profile.profileTwoPane
import com.anisync.android.presentation.profile.sections.BioPreview
import com.anisync.android.presentation.util.LocalAdaptiveInfo

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
    onAnimeClick: (() -> Unit)? = null,
    onMangaClick: (() -> Unit)? = null,
    onStatsClick: (() -> Unit)? = null,
    onPeopleClick: ((ProfileSocialTab?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val band = profile.bannerUrl == null
    val headerHeight = profileBackdropHeight(band)
    val avatarTop = if (band) headerHeight + 8.dp else headerHeight - ProfileAvatarHalfSize
    val actionsTop = if (band) headerHeight + 64.dp else headerHeight + 8.dp
    val identityTop = if (band) headerHeight + 116.dp else headerHeight + 60.dp

    Box(modifier = modifier.fillMaxWidth()) {
        ProfileBackdrop(
            profile = profile,
            isOwnProfile = isOwnProfile,
            onSettingsClick = onSettingsClick,
            onBackClick = onBackClick,
            modifier = Modifier.hiddenWhilePulled()
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
                onAnimeClick = onAnimeClick,
                onMangaClick = onMangaClick,
                onStatsClick = onStatsClick,
                onPeopleClick = onPeopleClick,
                accounts = accounts,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            BioPreview(
                about = profile.about,
                previousNamesCount = profile.previousNames.size,
                onOpen = onShowBiography
            )
        }

        ProfileHeaderAvatar(
            url = profile.avatarUrl,
            modifier = Modifier
                .padding(start = 16.dp)
                .offset(y = avatarTop)
        )

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
                    ProfileOwnTopBar(onSettingsClick = onSettingsClick)
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
                            colors = bandChromeColors()
                        )
                    }
                }
            }
        }
    } else {
        ProfileBannerSurface(
            profile = profile,
            height = height,
            onBackClick = onBackClick,
            // Settings on your own banner sits where the docked bar has it.
            chromeHeight = if (isOwnProfile) 64.dp else 72.dp,
            endControl = {
                if (isOwnProfile) {
                    ProfileSettingsButton(onBanner = true, onClick = onSettingsClick)
                } else {
                    ProfileMenuButton(name = profile.name, colors = bannerChromeColors())
                }
            },
            modifier = modifier
        )
    }
}

@Composable
internal fun profileBackdropHeight(band: Boolean): Dp {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return if (band) statusBar + 64.dp else maxOf(BannerHeight, statusBar + 120.dp)
}

/**
 * The first load: the header's shape, with only the name the link already gave, so nothing moves
 * when the profile lands. The rate limit toast sits over this too, and the screen waits it out.
 */
@Composable
fun ProfileLoadingSkeleton(name: String?, onBackClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    if (LocalAdaptiveInfo.current.profileTwoPane) {
        ProfileWideLoadingSkeleton(name = name, onBackClick = onBackClick, modifier = modifier)
        return
    }
    val headerHeight = profileBackdropHeight(band = false)
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
            IdentityPlaceholder(
                name = name,
                lineFractions = listOf(0.55f, 0.75f, 0.57f),
                modifier = Modifier.padding(horizontal = 20.dp)
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
        AvatarPlaceholder(
            modifier = Modifier
                .padding(start = 16.dp)
                .offset(y = headerHeight - ProfileAvatarHalfSize)
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

/** Who the profile belongs to while it loads: the name the link gave, three detail lines and the bio. */
@Composable
internal fun IdentityPlaceholder(name: String?, lineFractions: List<Float>, modifier: Modifier = Modifier) {
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier = modifier) {
        if (name != null) {
            Text(
                text = name,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        lineFractions.forEach { fraction ->
            Box(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .fillMaxWidth(fraction)
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(block)
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(block)
        )
    }
}

/** The avatar's place while the profile loads, ringed in the page colour like the real one. */
@Composable
internal fun AvatarPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ProfileAvatarSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .padding(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    )
}
