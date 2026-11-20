package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.DonatorBadge
import com.anisync.android.presentation.components.DonatorBadgePlacement
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.WhatsNewDot
import com.anisync.android.presentation.components.bannerScrimBrush
import com.anisync.android.presentation.components.bannerScrimHeight
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.components.menu.MenuScope
import com.anisync.android.presentation.components.navigation.BackTrailButton
import com.anisync.android.presentation.components.rememberActiveWhatsNew
import com.anisync.android.presentation.profile.ProfileSocialTab
import com.anisync.android.presentation.util.copyOnLongPress
import com.anisync.android.presentation.util.formatMonthYear
import com.anisync.android.presentation.util.openUrl
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.util.AniListUrls
import com.anisync.android.util.ShareUtils
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import com.anisync.android.presentation.util.rememberIntegerFormat

/**
 * Header pieces shared by the phone header ([ProfileTopSection]) and the wide identity column
 * (`ProfileWideLayout`), which arrange them each their own way.
 */

/** Avatar diameter shared by the compact header overlay and the wide identity pane. */
val ProfileAvatarSize = 100.dp
val ProfileAvatarHalfSize = ProfileAvatarSize / 2

/**
 * The user's banner image with a top and bottom scrim, back at its start and [endControl] at its
 * end. [height] lets the phone header use its taller banner while the wide column uses a card.
 */
@Composable
fun ProfileBannerSurface(
    profile: UserProfile,
    height: Dp,
    endControl: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    onBackClick: (() -> Unit)? = null,
    /** The controls' row, which keeps them as far from its sides as from its top. */
    chromeHeight: Dp = 72.dp,
    /** False for a card that does not reach the status bar, which has no system icons to carry. */
    statusScrim: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (profile.bannerUrl != null) {
            AsyncImage(
                model = profile.bannerUrl,
                contentDescription = stringResource(R.string.content_description_cover, profile.name),
                contentScale = ContentScale.Crop,
                // Anchor the crop to the top so the banner shows from above — banners often carry the
                // details the user put up top, which center-cropping would hide.
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

        // The banner runs under the status bar, and its own gradient has faded too far by the time
        // it reaches the clock to carry the system icons.
        if (statusScrim) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerScrimHeight())
                    .background(bannerScrimBrush())
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(chromeHeight)
                .padding(horizontal = (chromeHeight - ChromeButtonSize) / 2)
        ) {
            if (onBackClick != null) BackTrailButton(onClick = onBackClick, colors = bannerChromeColors())
            Spacer(modifier = Modifier.weight(1f))
            endControl()
        }
    }
}

/** Every control on the banner takes a 48dp touch target. */
private val ChromeButtonSize = 48.dp

/** Settings on the wide column's banner card, a filled button with the What's new dot. */
@Composable
internal fun ProfileCardSettingsButton(onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(ChromeButtonSize),
        shape = CircleShape,
        colors = bannerChromeColors()
    ) {
        val showDot = rememberActiveWhatsNew().any { it.place == WhatsNewItem.Place.SETTINGS }
        BadgedBox(badge = { if (showDot) Badge() }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Report and block are website only, the API has neither. */
@Composable
internal fun ProfileMenuButton(
    name: String,
    colors: IconButtonColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reportLabel = stringResource(R.string.profile_report_on_anilist)
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilledTonalIconButton(
            onClick = { expanded = true },
            shape = CircleShape,
            colors = colors,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.profile_menu))
        }
        Menu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UserLinkItems(name = name, onClose = { expanded = false })
            gap()
            item(
                text = reportLabel,
                leadingIcon = Icons.Outlined.Flag,
                onClick = {
                    expanded = false
                    openUrl(context, AniListUrls.userUrl(name))
                }
            )
        }
    }
}

/** Copy username, Share and Open on AniList, the items every menu about a person starts with. */
@Composable
internal fun MenuScope.UserLinkItems(name: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val copy = rememberCopyToClipboard()
    val copiedMessage = stringResource(R.string.profile_username_copied, name)
    val clipLabel = stringResource(R.string.profile_username)
    item(
        text = stringResource(R.string.profile_copy_username),
        leadingIcon = Icons.Default.ContentCopy,
        onClick = {
            onClose()
            copy(clipLabel, name, copiedMessage)
        }
    )
    item(
        text = stringResource(R.string.profile_share),
        leadingIcon = Icons.Default.Share,
        onClick = {
            onClose()
            ShareUtils.shareUser(context, name)
        }
    )
    item(
        text = stringResource(R.string.profile_open_on_anilist),
        leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
        onClick = {
            onClose()
            openUrl(context, AniListUrls.userUrl(name))
        }
    )
}

/**
 * The identity column: display name, donator badge, moderator role chips, and the
 * active/joined meta. Self-contained (no avatar or banner) so it drops into both the compact header
 * card and the wide identity pane.
 */
@Composable
fun ProfileIdentityInfo(
    profile: UserProfile,
    isOwnProfile: Boolean,
    modifier: Modifier = Modifier,
    /** Whether the authenticated viewer follows this user (drives the Mutual vs Follows you label). */
    viewerFollows: Boolean = false,
    /** Whether this user follows the authenticated viewer back. */
    followsViewer: Boolean = false,
    followingTotal: Int? = null,
    followersTotal: Int? = null,
    onAnimeClick: (() -> Unit)? = null,
    onMangaClick: (() -> Unit)? = null,
    onStatsClick: (() -> Unit)? = null,
    onPeopleClick: ((ProfileSocialTab?) -> Unit)? = null,
    /** On your own profile the name opens the accounts on this device. */
    accounts: ProfileAccounts? = null
) {
    Column(modifier = modifier) {
        ProfileDisplayName(name = profile.name, accounts = accounts)
        ProfileIdentityDetails(
            profile = profile,
            isOwnProfile = isOwnProfile,
            viewerFollows = viewerFollows,
            followsViewer = followsViewer,
            followingTotal = followingTotal,
            followersTotal = followersTotal,
            onAnimeClick = onAnimeClick,
            onMangaClick = onMangaClick,
            onStatsClick = onStatsClick,
            onPeopleClick = onPeopleClick
        )
    }
}

/**
 * The display name. On your own profile it is also the account switcher, the name and an arrow
 * opening the accounts menu the way the forum's feed button opens its feeds. A long press copies it
 * either way.
 */
@Composable
private fun ProfileDisplayName(name: String, modifier: Modifier = Modifier, accounts: ProfileAccounts? = null) {
    val style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
    val clipLabel = stringResource(R.string.profile_username)
    val copiedMessage = stringResource(R.string.profile_username_copied, name)
    if (accounts == null) {
        Text(
            text = name,
            style = style,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = modifier.copyOnLongPress(text = name, clipLabel = clipLabel, copiedMessage = copiedMessage)
        )
        return
    }
    var expanded by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()
    val arrowColor = MaterialTheme.colorScheme.onBackground
    Box(modifier = modifier) {
        Text(
            // The joiner keeps the arrow on the name's last line when a long name wraps.
            text = buildAnnotatedString {
                append(name)
                append('\u2060')
                appendInlineContent(NAME_SWITCH_ARROW)
            },
            inlineContent = mapOf(
                NAME_SWITCH_ARROW to InlineTextContent(
                    Placeholder(width = 1.em, height = 1.em, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = arrowColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            ),
            style = style,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                // Read as the name alone, without the joiner and the arrow's placeholder.
                .semantics { contentDescription = name }
                .copyOnLongPress(
                    text = name,
                    clipLabel = clipLabel,
                    copiedMessage = copiedMessage,
                    onClickLabel = stringResource(R.string.account_switch),
                    role = Role.Button
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    expanded = true
                }
        )
        AccountMenu(expanded = expanded, accounts = accounts, onDismiss = { expanded = false })
    }
}

private const val NAME_SWITCH_ARROW = "account_switch_arrow"

/**
 * Everything under the display name: level + donator badges, the mutual-follow chip, moderator role
 * chips, the active/joined meta, the counts and the way to People.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileIdentityDetails(
    profile: UserProfile,
    isOwnProfile: Boolean,
    modifier: Modifier = Modifier,
    viewerFollows: Boolean = false,
    followsViewer: Boolean = false,
    followingTotal: Int? = null,
    followersTotal: Int? = null,
    onAnimeClick: (() -> Unit)? = null,
    onMangaClick: (() -> Unit)? = null,
    onStatsClick: (() -> Unit)? = null,
    onPeopleClick: ((ProfileSocialTab?) -> Unit)? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val joinedDate = remember(profile.createdAt, locale) {
        profile.createdAt?.let { formatMonthYear(YearMonth.from(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())), locale) }
    }

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

            DonatorBadge(
                tier = profile.donatorTier,
                badgeText = profile.donatorBadge,
                placement = DonatorBadgePlacement.ProfileHeader
            )

            // Mutual-follow indicator (#81): only on others' profiles, and only when they
            // follow the viewer. "Mutual" when the viewer follows back, else "Follows you".
            if (!isOwnProfile && followsViewer) {
                FollowRelationshipChip(isMutual = viewerFollows)
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
            val resources = LocalResources.current
            val activeTime = remember(profile.activeAt, resources) {
                formatRelativeTimeSeconds(resources, (profile.activeAt ?: 0L) / 1000L)
            }
            val meta = listOfNotNull(
                activeTime.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.profile_active, it) },
                joinedDate?.let { stringResource(R.string.profile_joined, it) }
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                meta.forEachIndexed { index, part ->
                    Text(
                        text = if (index < meta.lastIndex) "$part  ·" else part,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        } else if (joinedDate != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.profile_joined, joinedDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        ProfileMetricStrip(
            animeCount = profile.animeCount,
            mangaCount = profile.mangaCount,
            daysWatched = profile.daysWatched.toInt(),
            followingTotal = followingTotal,
            followersTotal = followersTotal,
            onAnimeClick = onAnimeClick,
            onMangaClick = onMangaClick,
            onStatsClick = onStatsClick,
            onPeopleClick = onPeopleClick
        )
    }
}

private val MetricOuterRadius = 20.dp
private val MetricInnerRadius = 6.dp
private val MetricTileGap = 4.dp
private val MetricLeadingShape = RoundedCornerShape(
    topStart = MetricOuterRadius,
    bottomStart = MetricOuterRadius,
    topEnd = MetricInnerRadius,
    bottomEnd = MetricInnerRadius
)
private val MetricMiddleShape = RoundedCornerShape(MetricInnerRadius)
private val MetricTrailingShape = RoundedCornerShape(
    topStart = MetricInnerRadius,
    bottomStart = MetricInnerRadius,
    topEnd = MetricOuterRadius,
    bottomEnd = MetricOuterRadius
)

/**
 * Four semi-connected tiles for Anime, Manga, Days watched, and People.
 * Each tile opens its corresponding tab or page.
 */
@Composable
private fun ProfileMetricStrip(
    animeCount: Int,
    mangaCount: Int,
    daysWatched: Int,
    followingTotal: Int?,
    followersTotal: Int?,
    onAnimeClick: (() -> Unit)?,
    onMangaClick: (() -> Unit)?,
    onStatsClick: (() -> Unit)?,
    onPeopleClick: ((ProfileSocialTab?) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val numbers = rememberIntegerFormat()
    val anime = numbers.format(animeCount)
    val manga = numbers.format(mangaCount)
    val days = numbers.format(daysWatched)

    val showPeople = onPeopleClick != null
    val showFollowing = followersTotal != null && followersTotal == 0 && (followingTotal ?: 0) > 0
    val peopleValue = when {
        showFollowing -> numbers.format(followingTotal ?: 0)
        followersTotal != null -> numbers.format(followersTotal)
        followingTotal != null -> numbers.format(followingTotal)
        else -> "-"
    }
    val peopleLabel = stringResource(
        if (showFollowing || (followersTotal == null && followingTotal != null)) {
            R.string.profile_social_following
        } else {
            R.string.profile_social_followers
        }
    )
    val peopleTargetTab = when {
        showFollowing || (followersTotal == null && followingTotal != null) -> ProfileSocialTab.FOLLOWING
        followersTotal != null -> ProfileSocialTab.FOLLOWERS
        else -> null
    }
    val peopleDescription = if (followingTotal != null && followersTotal != null) {
        stringResource(
            R.string.profile_people_counts,
            numbers.format(followingTotal),
            numbers.format(followersTotal)
        )
    } else {
        stringResource(R.string.profile_people)
    }
    val showPeopleBadge = showPeople && WhatsNewItem.PROFILE_PEOPLE_MOVED in rememberActiveWhatsNew()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(MetricTileGap)
    ) {
        ProfileMetricTile(
            value = anime,
            label = stringResource(R.string.media_type_anime),
            contentDescription = stringResource(R.string.profile_count_anime, anime),
            shape = MetricLeadingShape,
            onClick = onAnimeClick,
            modifier = Modifier.weight(1f)
        )
        ProfileMetricTile(
            value = manga,
            label = stringResource(R.string.media_type_manga),
            contentDescription = stringResource(R.string.profile_count_manga, manga),
            shape = MetricMiddleShape,
            onClick = onMangaClick,
            modifier = Modifier.weight(1f)
        )
        ProfileMetricTile(
            value = days,
            label = pluralStringResourceOrDefault(R.plurals.profile_metric_days, daysWatched),
            contentDescription = pluralStringResourceOrDefault(R.plurals.profile_days_watched, daysWatched, days),
            shape = if (showPeople) MetricMiddleShape else MetricTrailingShape,
            onClick = onStatsClick,
            modifier = Modifier.weight(1f)
        )
        if (showPeople) {
            ProfileMetricTile(
                value = peopleValue,
                label = peopleLabel,
                contentDescription = peopleDescription,
                shape = MetricTrailingShape,
                trailingArrow = true,
                showBadge = showPeopleBadge,
                onClick = { onPeopleClick(peopleTargetTab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfileMetricTile(
    value: String,
    label: String,
    contentDescription: String,
    shape: Shape,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailingArrow: Boolean = false,
    showBadge: Boolean = false
) {
    val haptic = rememberHapticFeedback()
    val valueStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.W700,
        fontFeatureSettings = "tnum"
    )
    val labelStyle = MaterialTheme.typography.labelMedium

    Surface(
        onClick = {
            if (onClick != null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        enabled = onClick != null,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxHeight()
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 62.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
                    .padding(horizontal = 6.dp, vertical = 10.dp)
            ) {
                Text(
                    text = value,
                    style = valueStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 18.sp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f, fill = false),
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = labelStyle.fontSize)
                    )
                    if (trailingArrow) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            if (showBadge) {
                WhatsNewDot(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 10.dp)
                )
            }
        }
    }
}

/**
 * The header action buttons: Edit profile and notifications on your own profile, Follow and Message
 * on others'. The caller positions the [Row].
 */
@Composable
fun ProfileActionButtons(
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (isOwnProfile) {
            FilledTonalButton(onClick = onEditProfileClick, contentPadding = LabelledButtonPadding) {
                ButtonContent(Icons.Default.Edit, stringResource(R.string.profile_edit))
            }
            OutlinedIconButton(onClick = onNotificationsClick) {
                NotificationsIcon(unreadNotificationCount)
            }
        } else {
            if (isFollowing) {
                FilledTonalButton(
                    onClick = onFollowClick,
                    enabled = !isFollowLoading,
                    contentPadding = LabelledButtonPadding
                ) {
                    ButtonContent(Icons.Default.Check, stringResource(R.string.profile_following))
                }
            } else {
                Button(
                    onClick = onFollowClick,
                    enabled = !isFollowLoading,
                    contentPadding = LabelledButtonPadding
                ) {
                    ButtonContent(Icons.Default.PersonAdd, stringResource(R.string.profile_follow))
                }
            }
            FilledTonalButton(onClick = onMessageClick, contentPadding = LabelledButtonPadding) {
                ButtonContent(Icons.Default.Mail, stringResource(R.string.profile_message))
            }
        }
    }
}

/** The bell with the unread count on it, which TalkBack reads out with the button. */
@Composable
private fun NotificationsIcon(unreadNotificationCount: Int) {
    if (unreadNotificationCount > 0) {
        BadgedBox(badge = { Badge { Text(formatBadgeCount(unreadNotificationCount)) } }) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = stringResource(R.string.notifications_open) + ", " +
                    unreadCountAccessibilityLabel(unreadNotificationCount)
            )
        }
    } else {
        Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notifications_open))
    }
}

/**
 * Mutual-follow chip shown in the identity badge row on another user's profile. Reads "Mutual" when
 * the viewer and the user follow each other, or "Follows you" when only the user follows the viewer.
 * Styled like the sibling level/donator chips so it sits inline with them.
 */
@Composable
private fun FollowRelationshipChip(isMutual: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = CircleShape,
        modifier = modifier
    ) {
        Text(
            text = stringResource(
                if (isMutual) R.string.profile_mutual else R.string.profile_follows_you
            ),
            style = MaterialTheme.typography.labelLarge.emphasis(),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/** Extracted pure formatting function outside of Compose scope. */
private fun formatModeratorRole(role: String): String {
    return role.replace("_", " ").lowercase()
        .replaceFirstChar { it.uppercase() }
        .replace(" [a-z]".toRegex()) { it.value.uppercase() }
}

/**
 * Per Material 3, large numeric badges overflow to "999+" when the count exceeds three digits —
 * keeps the badge legible without truncating.
 */
private fun formatBadgeCount(count: Int): String =
    if (count > 999) "999+" else count.toString()

/**
 * TalkBack label for the inbox badge. M3 accessibility guidance: numeric badges announce the count
 * via a pluralised string; counts beyond the displayed cap read as "more than {cap}".
 */
@Composable
private fun unreadCountAccessibilityLabel(count: Int): String =
    if (count > 999) {
        stringResource(R.string.notifications_unread_overflow_a11y)
    } else {
        pluralStringResourceOrDefault(
            R.plurals.notifications_unread_count_a11y,
            count,
            count
        )
    }

private val LabelledButtonPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)

@Composable
private fun ButtonContent(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.size(8.dp))
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp)
    )
}

@Composable
internal fun bannerChromeColors(): IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = Color.Black.copy(alpha = 0.4f),
    contentColor = Color.White
)

/** The menu button where there is no banner to sit on, on the tonal band or above the wide column. */
@Composable
internal fun bandChromeColors(): IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onSurface
)

/** Drawn from its bottom edge, so an unshaped avatar taller than wide overflows upward rather than be cropped. */
@Composable
internal fun ProfileHeaderAvatar(url: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(ProfileAvatarSize), contentAlignment = Alignment.BottomStart) {
        UserAvatar(
            url = url,
            contentDescription = stringResource(R.string.content_description_profile_avatar),
            size = ProfileAvatarSize,
            borderWidth = 2.dp,
            framePadding = 3.dp,
            isProfileHeader = true,
            modifier = Modifier.wrapContentHeight(Alignment.Bottom, unbounded = true)
        )
    }
}

/** Settings, the one control left over your own profile's banner or band. */
@Composable
internal fun ProfileOwnTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 16.dp, end = 8.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        ProfileSettingsButton(onBanner = false, onClick = onSettingsClick)
    }
}

/**
 * Your own profile once the header has scrolled away. The name switches accounts here too, with the
 * same menu as the name in the header.
 */
@Composable
internal fun ProfileOwnDockedBar(
    name: String,
    avatarUrl: String?,
    accounts: ProfileAccounts?,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 8.dp, end = 8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .copyOnLongPress(
                        text = name,
                        clipLabel = stringResource(R.string.profile_username),
                        copiedMessage = stringResource(R.string.profile_username_copied, name),
                        onClickLabel = accounts?.let { stringResource(R.string.account_switch) },
                        role = accounts?.let { Role.Button }
                    ) {
                        if (accounts != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            expanded = true
                        }
                    }
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            ) {
                UserAvatar(contentDescription = null, size = 32.dp, url = avatarUrl)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (accounts != null) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(24.dp)
                    )
                }
            }
            if (accounts != null) {
                AccountMenu(expanded = expanded, accounts = accounts, onDismiss = { expanded = false })
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        ProfileSettingsButton(onBanner = false, onClick = onSettingsClick)
    }
}

@Composable
internal fun ProfileSettingsButton(onBanner: Boolean, onClick: () -> Unit) {
    val colors = if (onBanner) bannerChromeColors() else IconButtonDefaults.iconButtonColors()
    val settingsDot = rememberActiveWhatsNew().any { it.place == WhatsNewItem.Place.SETTINGS }
    IconButton(onClick = onClick, colors = colors) {
        BadgedBox(badge = { if (settingsDot) Badge() }) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
        }
    }
}

/** Who this is once the header has scrolled away: back, their name, Follow and the profile menu. */
@Composable
internal fun ProfileVisitBar(
    name: String,
    avatarUrl: String?,
    onBackClick: (() -> Unit)?,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 4.dp, end = 4.dp)
    ) {
        if (onBackClick != null) {
            BackTrailButton(onClick = onBackClick)
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }
        UserAvatar(contentDescription = null, size = 32.dp, url = avatarUrl)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
            onClick = onFollowClick,
            enabled = !isFollowLoading,
            contentPadding = LabelledButtonPadding
        ) {
            ButtonContent(
                icon = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                text = stringResource(if (isFollowing) R.string.profile_following else R.string.profile_follow)
            )
        }
        ProfileMenuButton(
            name = name,
            colors = IconButtonDefaults.iconButtonColors()
        )
    }
}
