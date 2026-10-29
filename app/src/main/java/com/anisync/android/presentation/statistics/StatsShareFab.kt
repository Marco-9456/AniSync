package com.anisync.android.presentation.statistics

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R

/** Shares the statistics, floating over the dashboard on phones and tablets alike. */
@Composable
fun StatsShareFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.share_stats_card)
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Default.Share, contentDescription = null) },
        text = { Text(stringResource(R.string.share_action_share)) },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.semantics { contentDescription = description },
    )
}

/** Bottom padding that lets a list scroll its last card clear of the share button. */
@Composable
fun statsFabClearance(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FabClearance

/** The button's 16dp margin, its 56dp height and 16dp between it and the last card. */
private val FabClearance = 88.dp
