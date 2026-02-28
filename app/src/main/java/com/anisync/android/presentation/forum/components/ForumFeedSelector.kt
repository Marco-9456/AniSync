package com.anisync.android.presentation.forum.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.forum.ForumFeed
import com.anisync.android.presentation.util.rememberHapticFeedback

private data class FeedOption(
    val feed: ForumFeed,
    val icon: ImageVector
)

/**
 * A horizontally scrollable row of connected toggle buttons for selecting the forum feed.
 * Visually matches the global [MediaTypeSelector] component style.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForumFeedSelector(
    selected: ForumFeed,
    onSelect: (ForumFeed) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()

    val options = remember {
        listOf(
            FeedOption(ForumFeed.OVERVIEW, Icons.Default.ViewList),
            FeedOption(ForumFeed.RECENT, Icons.Default.Schedule),
            FeedOption(ForumFeed.NEW, Icons.Default.NewReleases),
            FeedOption(ForumFeed.SUBSCRIBED, Icons.Default.Notifications),
            FeedOption(ForumFeed.SAVED, Icons.Default.Bookmark)
        )
    }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            ToggleButton(
                checked = selected == option.feed,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(option.feed)
                },
                shapes = shapes
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = option.feed.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
