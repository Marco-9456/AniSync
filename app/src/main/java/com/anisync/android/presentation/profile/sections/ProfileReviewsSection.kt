package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.anisync.android.R
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.profile.components.PlaceholderTabContent

fun LazyListScope.profileReviewsTab(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    item(key = "reviews_placeholder") {
        PlaceholderTabContent(
            message = stringResource(R.string.profile_placeholder_reviews),
            modifier = modifier
        )
    }
}
