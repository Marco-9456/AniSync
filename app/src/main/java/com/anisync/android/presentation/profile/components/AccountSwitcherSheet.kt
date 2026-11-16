package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.account.Account
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.util.pluralStringResourceOrDefault

/**
 * The accounts on this device, from the own-profile header. Tapping another account switches to it,
 * and the sheet ends with ways to add one or manage them all in Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    accounts: List<Account>,
    activeAccountId: Int?,
    onSwitch: (Int) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    unreadNotificationCount: Int = 0,
    onManageAccounts: (() -> Unit)? = null,
) {
    AppModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.account_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                accounts.forEach { account ->
                    val isActive = account.id == activeAccountId
                    AccountRow(
                        account = account,
                        isActive = isActive,
                        unreadNotificationCount = if (isActive) unreadNotificationCount else 0,
                        onClick = { if (!isActive) onSwitch(account.id) }
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            SheetAction(Icons.Outlined.PersonAddAlt, stringResource(R.string.account_add_another), onAddAccount)
            if (onManageAccounts != null) {
                SheetAction(Icons.Outlined.ManageAccounts, stringResource(R.string.account_manage), onManageAccounts)
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    isActive: Boolean,
    unreadNotificationCount: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            UserAvatar(url = account.avatarUrl, contentDescription = null, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name.ifBlank { stringResource(R.string.settings_account_unknown) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val status = when {
                    account.isExpired -> stringResource(R.string.account_expired)
                    isActive && unreadNotificationCount > 0 -> stringResource(
                        R.string.account_signed_in_with,
                        pluralStringResourceOrDefault(R.plurals.account_new_notifications, unreadNotificationCount, unreadNotificationCount)
                    )
                    isActive -> stringResource(R.string.account_signed_in)
                    else -> stringResource(R.string.account_switch_to)
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        account.isExpired -> MaterialTheme.colorScheme.error
                        isActive -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isActive) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.account_active),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
