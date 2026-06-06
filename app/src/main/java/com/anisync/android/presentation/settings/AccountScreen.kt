package com.anisync.android.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.data.account.Account
import com.anisync.android.presentation.login.AniListAuth

/**
 * Account settings screen: lists every signed-in account with add / switch / remove / re-auth,
 * plus logout for the active account. Switching or removing the active account recreates the
 * activity so all ViewModels rebuild against the new (reset) account state.
 */
@Composable
fun AccountScreen(
    onLogout: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()

    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var accountToRemove by remember { mutableStateOf<Account?>(null) }

    fun launchOAuth() {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AniListAuth.AUTH_URL)))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_dialog_title)) },
            text = { Text(stringResource(R.string.logout_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logoutActive()
                    }
                ) {
                    Text(
                        stringResource(R.string.logout_dialog_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    accountToRemove?.let { target ->
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = { Text(stringResource(R.string.account_remove_dialog_title)) },
            text = {
                Text(stringResource(R.string.account_remove_dialog_message, target.name.ifBlank { "?" }))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToRemove = null
                        viewModel.remove(target.id)
                    }
                ) {
                    Text(
                        stringResource(R.string.account_remove),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_account),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsGroup {
            accounts.forEach { account ->
                AccountRow(
                    account = account,
                    isActive = account.id == activeAccount?.id,
                    onSwitch = {
                        if (account.isExpired) launchOAuth()
                        else viewModel.switch(account.id)
                    },
                    onReauthenticate = ::launchOAuth,
                    onRemove = { accountToRemove = account }
                )
            }

            SettingsItem(
                title = stringResource(R.string.account_add),
                subtitle = stringResource(R.string.account_add_desc),
                icon = Icons.Outlined.PersonAddAlt,
                onClick = ::launchOAuth
            )
        }

        // Active-account actions
        val active = activeAccount
        if (active != null && active.name.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup {
                SettingsItem(
                    title = stringResource(R.string.settings_view_on_anilist),
                    subtitle = stringResource(R.string.settings_view_on_anilist_desc),
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://anilist.co/user/${active.name}")
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { showLogoutDialog = true },
            enabled = activeAccount != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(stringResource(R.string.control_log_out))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.settings_logout_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun AccountRow(
    account: Account,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onReauthenticate: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isActive, onClick = onSwitch)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp).fillMaxWidth()
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = account.avatarUrl
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = account.name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            ) {
                Text(
                    text = account.name.ifBlank { stringResource(R.string.settings_account_unknown) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val status = when {
                    account.isExpired -> stringResource(R.string.account_expired)
                    isActive -> stringResource(R.string.account_active)
                    else -> stringResource(R.string.settings_account_anilist)
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (account.isExpired) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isActive) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.account_active),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (account.isExpired) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.account_reauthenticate)) },
                                onClick = {
                                    menuExpanded = false
                                    onReauthenticate()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.account_switch)) },
                                onClick = {
                                    menuExpanded = false
                                    onSwitch()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.account_remove),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }
    }
}
