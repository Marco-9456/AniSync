package com.anisync.android.presentation.profile.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.data.account.Account
import com.anisync.android.presentation.components.UserAvatar
import com.anisync.android.presentation.components.menu.Menu
import com.anisync.android.presentation.util.pluralStringResourceOrDefault

/** The accounts on this device, which the own profile's name opens. */
@Stable
class ProfileAccounts(
    val accounts: List<Account>,
    val activeAccountId: Int?,
    val unreadNotificationCount: Int,
    val onSwitch: (Int) -> Unit,
    val onAddAccount: () -> Unit,
    val onManageAccounts: (() -> Unit)?
)

/** Anchored to the name like the forum's feed menu, the current account marked. */
@Composable
internal fun AccountMenu(expanded: Boolean, accounts: ProfileAccounts, onDismiss: () -> Unit) {
    Menu(expanded = expanded, onDismissRequest = onDismiss) {
        accounts.accounts.forEach { account ->
            val active = account.id == accounts.activeAccountId
            item(
                text = account.name.ifBlank { stringResource(R.string.settings_account_unknown) },
                supportingText = when {
                    account.isExpired -> stringResource(R.string.account_expired)
                    active && accounts.unreadNotificationCount > 0 -> stringResource(
                        R.string.account_signed_in_with,
                        pluralStringResourceOrDefault(
                            R.plurals.account_new_notifications,
                            accounts.unreadNotificationCount,
                            accounts.unreadNotificationCount
                        )
                    )
                    active -> stringResource(R.string.account_signed_in)
                    else -> stringResource(R.string.account_switch_to)
                },
                selected = active,
                maxLines = 1,
                leading = { UserAvatar(url = account.avatarUrl, contentDescription = null, size = 32.dp) },
                onClick = {
                    onDismiss()
                    if (!active) accounts.onSwitch(account.id)
                }
            )
        }
        gap()
        item(
            text = stringResource(R.string.account_add_another),
            leadingIcon = Icons.Outlined.PersonAddAlt,
            onClick = {
                onDismiss()
                accounts.onAddAccount()
            }
        )
        accounts.onManageAccounts?.let { manage ->
            item(
                text = stringResource(R.string.account_manage),
                leadingIcon = Icons.Outlined.ManageAccounts,
                onClick = {
                    onDismiss()
                    manage()
                }
            )
        }
    }
}
