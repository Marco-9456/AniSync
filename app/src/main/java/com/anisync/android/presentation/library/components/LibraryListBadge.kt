package com.anisync.android.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.LIBRARY_ALL_TAB_ID
import com.anisync.android.presentation.util.LIBRARY_FAVORITES_TAB_ID
import com.anisync.android.presentation.util.toListIcon
import com.anisync.android.ui.theme.ListIndicatorKind
import com.anisync.android.ui.theme.listIndicatorColor

/**
 * A list's mark, in its own colour and its family's shape.
 *
 * Shape says the family the way [com.anisync.android.presentation.components.ListIndicator]
 * documents it: a circle while a title is in motion, a rounded square while it is parked, a square
 * once it is finished with. Colour is the same list colour pair the rail chips and the cover corner
 * tabs use, so one list reads the same wherever it appears.
 */
@Composable
fun LibraryListBadge(
    tabId: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    iconSize: Dp = 16.dp
) {
    val kind = tabId.toIndicatorKind()
    val colors = listIndicatorColor(kind)
    Box(
        modifier = modifier.size(size).clip(kind.badgeShape()).background(colors.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = tabId.badgeIcon(),
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(iconSize)
        )
    }
}

fun ListIndicatorKind.badgeShape(): Shape = when (this) {
    ListIndicatorKind.WATCHING, ListIndicatorKind.REPEATING -> CircleShape
    ListIndicatorKind.COMPLETED, ListIndicatorKind.DROPPED -> RoundedCornerShape(4.dp)
    else -> RoundedCornerShape(9.dp)
}

fun LibraryStatus.toIndicatorKind(): ListIndicatorKind = when (this) {
    LibraryStatus.CURRENT -> ListIndicatorKind.WATCHING
    LibraryStatus.REPEATING -> ListIndicatorKind.REPEATING
    LibraryStatus.PLANNING -> ListIndicatorKind.PLANNING
    LibraryStatus.PAUSED -> ListIndicatorKind.PAUSED
    LibraryStatus.COMPLETED -> ListIndicatorKind.COMPLETED
    LibraryStatus.DROPPED -> ListIndicatorKind.DROPPED
    LibraryStatus.UNKNOWN -> ListIndicatorKind.CUSTOM
}

private fun String.toIndicatorKind(): ListIndicatorKind = when {
    this == LIBRARY_ALL_TAB_ID || this == LIBRARY_FAVORITES_TAB_ID -> ListIndicatorKind.CUSTOM
    startsWith("status:") -> LibraryStatus.entries
        .find { it.name == removePrefix("status:") }
        ?.toIndicatorKind()
        ?: ListIndicatorKind.CUSTOM

    else -> ListIndicatorKind.CUSTOM
}

@Composable
private fun String.badgeIcon(): ImageVector = when {
    this == LIBRARY_ALL_TAB_ID -> Icons.Default.AllInclusive
    this == LIBRARY_FAVORITES_TAB_ID -> Icons.Default.Favorite
    startsWith("status:") -> LibraryStatus.entries
        .find { it.name == removePrefix("status:") }
        ?.toListIcon()
        ?: Icons.AutoMirrored.Filled.List

    else -> Icons.AutoMirrored.Filled.List
}
