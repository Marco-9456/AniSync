package com.anisync.android.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.anisync.android.R

@Composable
fun LinksScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    SettingsScreenScaffold(
        title = stringResource(R.string.settings_links),
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsGroup {
            SettingsItem(
                title = "Repository",
                subtitle = "https://github.com/Marco-9456/AniSync",
                icon = Icons.Outlined.Code,
                onClick = { context.launchUrl("https://github.com/Marco-9456/AniSync") }
            )
            SettingsDivider(startPadding = 20.dp)
            SettingsItem(
                title = "Developer",
                subtitle = "https://github.com/Marco-9456",
                icon = Icons.Outlined.Person,
                onClick = { context.launchUrl("https://github.com/Marco-9456") }
            )
        }
    }
}

private fun Context.launchUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
