package com.anisync.android.presentation.profile.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ActivityType
import com.anisync.android.domain.UserProfile
import com.anisync.android.presentation.components.AnimatedTab
import com.anisync.android.presentation.profile.ProfileActivityFilter
import com.anisync.android.presentation.profile.components.ActivityCard
import com.anisync.android.presentation.profile.components.PlaceholderTabContent

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.profileActivityTab(
    profile: UserProfile,
    selectedFilter: ProfileActivityFilter,
    onFilterSelected: (ProfileActivityFilter) -> Unit,
    onUserClick: (String) -> Unit = {},
    onActivityClick: (Int) -> Unit = {},
    onMediaClick: (Int) -> Unit = {},
    onLastReplyClick: (activityId: Int, replyId: Int) -> Unit = { _, _ -> },
    onSubscribeClick: (Int) -> Unit = {},
    onLikeActivity: ((Int) -> Unit)? = null,
    onDeleteActivity: ((Int) -> Unit)? = null,
    onEditActivity: ((Int) -> Unit)? = null,
    viewerId: Int? = null,
    modifier: Modifier = Modifier
) {
    item(key = "activity_filters", contentType = "filters") {
        val filters = remember { ProfileActivityFilter.entries }
        val selectedIndex = remember(selectedFilter) { filters.indexOf(selectedFilter).coerceAtLeast(0) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(filters) { index, filter ->
                    AnimatedTab(
                        index = index,
                        selectedIndex = selectedIndex,
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        icon = activityFilterIcon(filter),
                        label = stringResource(filter.labelRes)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val filteredActivities = when (selectedFilter) {
        ProfileActivityFilter.ALL -> profile.activities
        ProfileActivityFilter.STATUS -> profile.activities.filter { it.type == ActivityType.TEXT }
        ProfileActivityFilter.MESSAGES -> profile.activities.filter { it.type == ActivityType.MESSAGE }
        ProfileActivityFilter.LISTS -> profile.activities.filter { it.type == ActivityType.MEDIA_LIST }
    }

    if (filteredActivities.isEmpty()) {
        item(key = "activity_empty", contentType = "empty") {
            PlaceholderTabContent(
                message = stringResource(
                    R.string.profile_no_activity_for_filter,
                    stringResource(selectedFilter.labelRes)
                ),
                modifier = modifier
            )
        }
    } else {
        items(
            items = filteredActivities,
            key = { "activity_${it.id}" },
            contentType = { "activity_item" }
        ) { activity ->
            val canDelete = viewerId != null && (
                activity.userId == viewerId ||
                    (activity.type == ActivityType.MESSAGE && activity.recipientId == viewerId && !activity.isAuthorMod)
            )
            // Edit only on own TEXT or MESSAGE — never on server-derived MEDIA_LIST.
            val canEdit = viewerId != null &&
                activity.userId == viewerId &&
                (activity.type == ActivityType.TEXT || activity.type == ActivityType.MESSAGE)
            val cardLike = onLikeActivity?.let { cb -> { cb(activity.id) } }
            val cardDelete = if (canDelete) {
                onDeleteActivity?.let { cb -> { cb(activity.id) } }
            } else null
            val cardEdit = if (canEdit) {
                onEditActivity?.let { cb -> { cb(activity.id) } }
            } else null

            ActivityCard(
                activity = activity,
                onClick = { onActivityClick(activity.id) },
                modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                onUserClick = onUserClick,
                onMediaClick = onMediaClick,
                onLastReplyClick = onLastReplyClick,
                onSubscribeClick = { onSubscribeClick(activity.id) },
                onLikeClick = cardLike,
                onDeleteClick = cardDelete,
                onEditClick = cardEdit
            )
        }
        
        item(key = "activity_bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun activityFilterIcon(filter: ProfileActivityFilter): ImageVector {
    return when (filter) {
        ProfileActivityFilter.ALL -> Icons.AutoMirrored.Filled.ViewList
        ProfileActivityFilter.STATUS -> Icons.Default.Schedule
        ProfileActivityFilter.MESSAGES -> Icons.Default.ChatBubbleOutline
        ProfileActivityFilter.LISTS -> Icons.AutoMirrored.Filled.List
    }
}

