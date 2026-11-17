package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.AppModalBottomSheet
import com.anisync.android.presentation.profile.sections.ProfileAboutContent
import com.anisync.android.ui.theme.emphasis

/**
 * About, over the profile rather than in place of it, so Read more and the way back are the sheet
 * sliding up and down while the page under it stays where it was.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAboutSheet(
    profile: UserProfile,
    isOwnProfile: Boolean,
    onEditAbout: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AppModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (isOwnProfile) {
                    stringResource(R.string.profile_about_title_own)
                } else {
                    stringResource(R.string.profile_about_title, profile.name)
                },
                style = MaterialTheme.typography.headlineLarge.emphasis(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            )
            // About was drawn on the page, and the sheet is one step above it, so every surface
            // under it moves up a step with it.
            val scheme = MaterialTheme.colorScheme
            val raised = remember(scheme) {
                scheme.copy(
                    background = scheme.surfaceContainerLow,
                    surfaceContainerLow = scheme.surfaceContainer,
                    surfaceContainer = scheme.surfaceContainerHigh,
                    surfaceContainerHigh = scheme.surfaceContainerHighest
                )
            }
            MaterialTheme(colorScheme = raised) {
                ProfileAboutContent(profile = profile, isOwnProfile = isOwnProfile, onEditAbout = onEditAbout)
            }
        }
    }
}
