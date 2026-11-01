package com.anisync.android.presentation.profile.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anisync.android.presentation.components.PinnedRail
import com.anisync.android.presentation.components.revealChip

/** A profile tab's filters as a rail of chips, sitting under the tabs where the Statistics rail does. */
@Composable
internal fun <T : Enum<T>> ProfileTabRail(
    options: List<T>,
    selected: T,
    modifier: Modifier = Modifier,
    chip: @Composable (option: T, selected: Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        val index = options.indexOf(selected)
        if (index >= 0) listState.revealChip(index)
    }
    PinnedRail(
        pinned = null,
        listState = listState,
        modifier = modifier.padding(top = 4.dp, bottom = 8.dp),
    ) {
        items(options, key = { it.name }) { option -> chip(option, option == selected) }
    }
}
