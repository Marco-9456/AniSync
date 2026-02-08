package com.anisync.android.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.UserProfile

/**
 * Account settings screen.
 * Displays user profile information and logout option.
 */
@Composable
fun AccountScreen(
    onLogout: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_account),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        // User profile section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = userProfile?.avatarUrl
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = stringResource(R.string.content_description_profile_avatar),
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = userProfile?.name?.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username
            Text(
                text = userProfile?.name ?: stringResource(R.string.settings_account_unknown),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // AniList indicator
            Text(
                text = stringResource(R.string.settings_account_anilist),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Account actions
        SettingsGroup {
            SettingsItem(
                title = stringResource(R.string.settings_view_on_anilist),
                subtitle = stringResource(R.string.settings_view_on_anilist_desc),
                onClick = { /* Open AniList profile in browser */ }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Logout button
        OutlinedButton(
            onClick = {
                viewModel.logout {
                    onLogout()
                }
            },
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

        // Warning text
        Text(
            text = stringResource(R.string.settings_logout_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
