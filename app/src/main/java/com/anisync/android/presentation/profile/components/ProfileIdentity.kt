package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.DonatorBadge
import com.anisync.android.presentation.components.DonatorBadgePlacement
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.WhatsNewPill
import com.anisync.android.presentation.components.bannerScrimBrush
import com.anisync.android.presentation.components.bannerScrimHeight
import com.anisync.android.presentation.components.formatRelativeTimeSeconds
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.components.navigation.BackTrailButton
import com.anisync.android.presentation.components.rememberActiveWhatsNew
import com.anisync.android.presentation.util.copyOnLongPress
import com.anisync.android.presentation.util.openUrl
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.ui.theme.emphasis
import com.anisync.android.util.AniListUrls
import com.anisync.android.util.ShareUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared profile-header building blocks, extracted from [ProfileTopSection] (the compact header) so
 * the expanded two-pane layout (`ProfileWideLayout`) can reuse them. The compact and wide headers
 * differ only in arrangement (one overlapping card vs. a banner above a left identity pane); these
 * pieces — the banner surface, the identity info column, the action buttons, and the donator badges
 * — are identical in both, so they live here and are composed differently by each host.
 */

/** Avatar diameter shared by the compact header overlay and the wide identity pane. */
val ProfileAvatarSize = 100.dp
val ProfileAvatarHalfSize = ProfileAvatarSize / 2

/**
 * The full-width cover banner: the user's banner image with a top/bottom scrim and the top-end
 * action button (settings on your own profile, a menu on others'). [height] lets the compact header
 * use its taller banner while the wide layout uses a shorter strip.
 */
@Composable
fun ProfileBannerSurface(
    profile: UserProfile,
    isOwnProfile: Boolean,
    topActionIcon: ImageVector,
    onTopActionClick: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    onBackClick: (() -> Unit)? = null,
    ownTopBar: (@Composable () -> Unit)? = null
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerScrimHeight())
                .background(bannerScrimBrush())
        )

        val chromeColors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.4f),
            contentColor = Color.White
        )
        if (onBackClick != null) {
            BackTrailButton(
                onClick = onBackClick,
                colors = chromeColors,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 12.dp)
            )
        }
        if (isOwnProfile && ownTopBar != null) {
            Box(modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()) { ownTopBar() }
        } else if (isOwnProfile) {
            FilledTonalIconButton(
                onClick = onTopActionClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .size(48.dp),
                shape = CircleShape,
                colors = chromeColors
            ) {
                val showDot = rememberActiveWhatsNew().any { it.place == WhatsNewItem.Place.SETTINGS }
                BadgedBox(badge = { if (showDot) Badge() }) {
                    Icon(
                        imageVector = topActionIcon,
                        contentDescription = stringResource(R.string.settings),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            ProfileMenuButton(
                name = profile.name,
                colors = chromeColors,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 12.dp)
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
    val copy = rememberCopyToClipboard()
    val copiedMessage = stringResource(R.string.profile_username_copied, name)
    val clipLabel = stringResource(R.string.profile_username)
    val copyLabel = stringResource(R.string.profile_copy_username)
    val shareLabel = stringResource(R.string.profile_share)
    val openLabel = stringResource(R.string.profile_open_on_anilist)
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
            item(
                text = copyLabel,
                leadingIcon = Icons.Default.ContentCopy,
                onClick = {
                    expanded = false
                    copy(clipLabel, name, copiedMessage)
                }
            )
            item(
                text = shareLabel,
                leadingIcon = Icons.Default.Share,
                onClick = {
                    expanded = false
                    ShareUtils.shareText(context, "$name\n${AniListUrls.userUrl(name)}")
                }
            )
            item(
                text = openLabel,
                leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = {
                    expanded = false
                    openUrl(context, AniListUrls.userUrl(name))
                }
            )
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

/**
 * The identity column: display name, donator badge, moderator role chips, and the
 * active/joined meta. Self-contained (no avatar or banner) so it drops into both the compact header
 * card and the wide identity pane.
 *
 * Composed from [ProfileDisplayName] and [ProfileIdentityDetails], which the wide pane also stacks
 * itself so it can collapse the details away under a pinned name as the bio scrolls.
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
    onPeopleClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        ProfileDisplayName(name = profile.name)
        ProfileIdentityDetails(
            profile = profile,
            isOwnProfile = isOwnProfile,
            viewerFollows = viewerFollows,
            followsViewer = followsViewer,
            followingTotal = followingTotal,
            followersTotal = followersTotal,
            onPeopleClick = onPeopleClick
        )
    }
}

/** The display name alone, so a host can pin it while the rest of the identity collapses. */
@Composable
fun ProfileDisplayName(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.ExtraBold
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.copyOnLongPress(
            text = name,
            clipLabel = stringResource(R.string.profile_username),
            copiedMessage = stringResource(R.string.profile_username_copied, name)
        )
    )
}

/**
 * Everything under the display name: level + donator badges, the mutual-follow chip, moderator role
 * chips, and the active/joined meta. Split out from [ProfileIdentityInfo] so the wide identity pane
 * can collapse this block on its own.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileIdentityDetails(
    profile: UserProfile,
    isOwnProfile: Boolean,
    modifier: Modifier = Modifier,
    viewerFollows: Boolean = false,
    followsViewer: Boolean = false,
    followingTotal: Int? = null,
    followersTotal: Int? = null,
    onPeopleClick: (() -> Unit)? = null
) {
    val joinedDate = remember(profile.createdAt) {
        profile.createdAt?.let {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            sdf.format(Date(it))
        }
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

        val numbers = remember { NumberFormat.getIntegerInstance() }
        Spacer(modifier = Modifier.height(12.dp))
        val counts = listOf(
            boldNumbers(stringResource(R.string.profile_count_anime, numbers.format(profile.animeCount)), numbers.format(profile.animeCount)),
            boldNumbers(stringResource(R.string.profile_count_manga, numbers.format(profile.mangaCount)), numbers.format(profile.mangaCount)),
            boldNumbers(
                pluralStringResourceOrDefault(
                    R.plurals.profile_days_watched,
                    profile.daysWatched.toInt(),
                    numbers.format(profile.daysWatched.toInt())
                ),
                numbers.format(profile.daysWatched.toInt())
            )
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            counts.forEachIndexed { index, part ->
                Text(
                    text = if (index < counts.lastIndex) part + AnnotatedString("  ·") else part,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        if (onPeopleClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onPeopleClick)
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = if (followingTotal != null && followersTotal != null) {
                        boldNumbers(
                            stringResource(R.string.profile_people_counts, numbers.format(followingTotal), numbers.format(followersTotal)),
                            numbers.format(followingTotal),
                            numbers.format(followersTotal)
                        )
                    } else {
                        AnnotatedString(stringResource(R.string.profile_people))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                if (WhatsNewItem.PROFILE_PEOPLE_MOVED in rememberActiveWhatsNew()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    WhatsNewPill(WhatsNewItem.Kind.MOVED)
                }
            }
        }
    }
}

/**
 * The header action buttons: edit / notifications (+ optional account switch) on your own profile,
 * follow / message on others'. The caller positions the [Row] (the compact header overlaps it on the
 * banner seam; the wide identity pane stacks it under the meta).
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
    showAccountSwitcher: Boolean,
    onAccountSwitchClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        if (isOwnProfile && onShareClick != null) {
            val labelPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            FilledTonalButton(onClick = onEditProfileClick, contentPadding = labelPadding) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                ButtonLabel(stringResource(R.string.profile_edit))
            }
            OutlinedButton(onClick = onShareClick, contentPadding = labelPadding) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                ButtonLabel(stringResource(R.string.action_share))
            }
        } else if (isOwnProfile) {
            if (showAccountSwitcher) {
                FilledTonalIconButton(
                    onClick = onAccountSwitchClick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.account_switch),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
            val labelPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            if (isFollowing) {
                FilledTonalButton(
                    onClick = onFollowClick,
                    enabled = !isFollowLoading,
                    contentPadding = labelPadding
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    ButtonLabel(stringResource(R.string.profile_following))
                }
            } else {
                Button(
                    onClick = onFollowClick,
                    enabled = !isFollowLoading,
                    contentPadding = labelPadding
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    ButtonLabel(stringResource(R.string.profile_follow))
                }
            }
            FilledTonalButton(onClick = onMessageClick, contentPadding = labelPadding) {
                Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                ButtonLabel(stringResource(R.string.profile_message))
            }
        }
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

@Composable
private fun ButtonLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp)
    )
}

private fun boldNumbers(text: String, vararg numbers: String): AnnotatedString = buildAnnotatedString {
    append(text)
    var from = 0
    numbers.forEach { number ->
        val at = text.indexOf(number, from)
        if (at >= 0) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), at, at + number.length)
            from = at + number.length
        }
    }
}

@Composable
internal fun ProfileOwnTopBar(
    name: String,
    avatarUrl: String?,
    onBanner: Boolean,
    showAccountSwitcher: Boolean,
    onAccountSwitchClick: () -> Unit,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = if (onBanner) {
        IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.4f),
            contentColor = Color.White
        )
    } else {
        IconButtonDefaults.iconButtonColors()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 16.dp, end = 8.dp)
    ) {
        Surface(
            color = if (onBanner) Color.Black.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (onBanner) Color.White else MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
            onClick = onAccountSwitchClick,
            enabled = showAccountSwitcher
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .padding(start = 4.dp, end = if (showAccountSwitcher) 8.dp else 14.dp)
            ) {
                UserAvatar(contentDescription = null, size = 32.dp, url = avatarUrl)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                if (showAccountSwitcher) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = stringResource(R.string.account_switch),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onNotificationsClick, colors = chrome) {
            if (unreadNotificationCount > 0) {
                BadgedBox(badge = { Badge { Text(formatBadgeCount(unreadNotificationCount)) } }) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.notifications_open) + ", " +
                            unreadCountAccessibilityLabel(unreadNotificationCount)
                    )
                }
            } else {
                Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notifications_open))
            }
        }
        val settingsDot = rememberActiveWhatsNew().any { it.place == WhatsNewItem.Place.SETTINGS }
        IconButton(onClick = onSettingsClick, colors = chrome) {
            BadgedBox(badge = { if (settingsDot) Badge() }) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
            }
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
            contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            ButtonLabel(stringResource(if (isFollowing) R.string.profile_following else R.string.profile_follow))
        }
        ProfileMenuButton(
            name = name,
            colors = IconButtonDefaults.iconButtonColors()
        )
    }
}
