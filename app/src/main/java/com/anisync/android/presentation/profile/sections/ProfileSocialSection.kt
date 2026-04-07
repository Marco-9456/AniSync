package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.profile.ProfileSocialTab
import com.anisync.android.presentation.profile.components.PlaceholderTabContent

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.profileSocialTab(
    selectedTab: ProfileSocialTab,
    onTabSelected: (ProfileSocialTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = ProfileSocialTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    item(key = "social_tabs") {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(tabs) { index, tab ->
                AnimatedTab(
                    index = index,
                    selectedIndex = selectedIndex,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = socialTabIcon(tab),
                    label = stringResource(tab.labelRes)
                )
            }
        }
    }

    item(key = "social_placeholder") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            PlaceholderTabContent(
                message = stringResource(
                    R.string.profile_social_placeholder,
                    stringResource(selectedTab.labelRes)
                )
            )
        }
    }
}

private fun socialTabIcon(tab: ProfileSocialTab): ImageVector {
    return when (tab) {
        ProfileSocialTab.FOLLOWING -> Icons.Default.Person
        ProfileSocialTab.FOLLOWERS -> Icons.Default.Groups
        ProfileSocialTab.FORUM_THREADS -> Icons.Default.RateReview
        ProfileSocialTab.FORUM_COMMENTS -> Icons.Default.ChatBubbleOutline
    }
}
