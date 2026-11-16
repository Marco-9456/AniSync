package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.domain.SocialUser
import com.anisync.android.presentation.components.AppAlertDialog
import com.anisync.android.presentation.components.LocalWhatsNew
import com.anisync.android.presentation.components.SegmentedTabGroup
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.profile.PeopleOrder
import com.anisync.android.presentation.profile.ProfileSocialTab
import com.anisync.android.presentation.profile.ProfileUiState
import com.anisync.android.presentation.util.openUrl
import com.anisync.android.presentation.util.pluralStringResourceOrDefault
import com.anisync.android.presentation.util.rememberCopyToClipboard
import com.anisync.android.util.AniListUrls
import com.anisync.android.util.ShareUtils
import java.text.NumberFormat

private val PeopleTabs = listOf(ProfileSocialTab.FOLLOWING, ProfileSocialTab.FOLLOWERS, ProfileSocialTab.MUTUALS)

@Composable
internal fun PeopleSwitcher(
    uiState: ProfileUiState,
    onSelect: (ProfileSocialTab) -> Unit,
    onOrderSelected: (PeopleOrder) -> Unit
) {
    val selected = uiState.selectedSocialTab.takeUnless { it.isForum } ?: ProfileSocialTab.FOLLOWING
    val whatsNewStore = LocalWhatsNew.current
    LaunchedEffect(selected) {
        whatsNewStore.markSeen(WhatsNewItem.PROFILE_PEOPLE_MOVED)
        if (selected == ProfileSocialTab.MUTUALS) whatsNewStore.markSeen(WhatsNewItem.PROFILE_MUTUALS)
    }
    val numbers = remember { NumberFormat.getIntegerInstance() }
    Column(modifier = Modifier.fillMaxWidth()) {
        SegmentedTabGroup(
            options = PeopleTabs,
            selected = selected,
            onSelect = onSelect,
            label = { tab ->
                val total = peopleTotal(uiState, tab)
                val name = stringResource(if (tab == ProfileSocialTab.MUTUALS) R.string.profile_people_mutual else tab.labelRes)
                if (total != null) stringResource(R.string.profile_people_tab, name, numbers.format(total)) else name
            },
            fillEqually = true,
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        // An empty list says so below, with nothing to count or order.
        if (peopleTotal(uiState, selected) != 0) Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp)
        ) {
            val total = peopleTotal(uiState, selected)
            val order = stringResource(uiState.peopleOrder.phraseRes)
            Text(
                text = if (total != null) {
                    stringResource(
                        R.string.profile_people_summary,
                        pluralStringResourceOrDefault(R.plurals.profile_people_count, total, numbers.format(total)),
                        order
                    )
                } else {
                    order
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            PeopleOrderButton(selected = uiState.peopleOrder, onSelect = onOrderSelected)
        }
    }
}

private fun peopleTotal(uiState: ProfileUiState, tab: ProfileSocialTab): Int? = when (tab) {
    ProfileSocialTab.FOLLOWING -> uiState.followingTotal
    ProfileSocialTab.FOLLOWERS -> uiState.followersTotal
    ProfileSocialTab.MUTUALS -> uiState.mutualsTotal
    else -> null
}

@Composable
private fun PeopleOrderButton(selected: PeopleOrder, onSelect: (PeopleOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val labels = PeopleOrder.entries.associateWith { stringResource(it.labelRes) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.profile_people_sort))
        }
        Menu(expanded = open, onDismissRequest = { open = false }) {
            PeopleOrder.entries.forEach { order ->
                item(
                    text = labels.getValue(order),
                    selected = order == selected,
                    onClick = {
                        open = false
                        if (order != selected) onSelect(order)
                    }
                )
            }
        }
    }
}

@Composable
internal fun PersonRow(
    user: SocialUser,
    showFollow: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val copy = rememberCopyToClipboard()
    val clipLabel = stringResource(R.string.profile_username)
    val copiedMessage = stringResource(R.string.profile_username_copied, user.name)
    val copyLabel = stringResource(R.string.profile_copy_username)
    val shareLabel = stringResource(R.string.profile_share)
    val openLabel = stringResource(R.string.profile_open_on_anilist)
    val removeLabel = stringResource(R.string.a11y_remove_follower)
    val unfollowLabel = stringResource(R.string.profile_unfollow_confirm)
    var menuOpen by remember { mutableStateOf(false) }
    var confirmUnfollow by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = user.name, onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
    ) {
        UserAvatar(contentDescription = null, size = 44.dp, url = user.avatarUrl)
        Text(
            text = user.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showFollow && !user.isFollowing) {
            FilledTonalButton(
                onClick = onToggleFollow,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.profile_follow), maxLines = 1)
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.profile_person_menu, user.name))
            }
            Menu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                item(
                    text = copyLabel,
                    leadingIcon = Icons.Default.ContentCopy,
                    onClick = {
                        menuOpen = false
                        copy(clipLabel, user.name, copiedMessage)
                    }
                )
                item(
                    text = shareLabel,
                    leadingIcon = Icons.Default.Share,
                    onClick = {
                        menuOpen = false
                        ShareUtils.shareText(context, "${user.name}\n${AniListUrls.userUrl(user.name)}")
                    }
                )
                item(
                    text = openLabel,
                    leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = {
                        menuOpen = false
                        openUrl(context, AniListUrls.userUrl(user.name))
                    }
                )
                if (showFollow && user.isFollowing) {
                    gap()
                    item(
                        text = unfollowLabel,
                        leadingIcon = Icons.Default.PersonRemove,
                        onClick = {
                            menuOpen = false
                            confirmUnfollow = true
                        }
                    )
                }
                if (onRemove != null) {
                    gap()
                    item(
                        text = removeLabel,
                        leadingIcon = Icons.Default.PersonRemove,
                        destructive = true,
                        onClick = {
                            menuOpen = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }

    if (confirmUnfollow) {
        AppAlertDialog(
            onDismissRequest = { confirmUnfollow = false },
            title = { Text(stringResource(R.string.profile_unfollow_title, user.name)) },
            text = { Text(stringResource(R.string.profile_unfollow_body, user.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnfollow = false
                    onToggleFollow()
                }) {
                    Text(stringResource(R.string.profile_unfollow_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnfollow = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
