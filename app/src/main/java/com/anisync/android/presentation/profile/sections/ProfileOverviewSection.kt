package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.profile.RecentUpdatesSection
import com.anisync.android.presentation.profile.components.PlaceholderTabContent

@Composable
fun ProfileOverviewSection(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    if (profile.activities.isNotEmpty()) {
        RecentUpdatesSection(
            activities = profile.activities,
            modifier = modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
        )
    } else {
        PlaceholderTabContent(
            message = stringResource(R.string.profile_no_recent_updates),
            modifier = modifier
        )
    }
}
