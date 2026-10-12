package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anisync.android.R
import com.anisync.android.data.WhatsNewItem
import com.anisync.android.data.WhatsNewStore

val LocalWhatsNew = compositionLocalOf<WhatsNewStore> {
    error("WhatsNewStore not provided. Make sure to provide it using CompositionLocalProvider.")
}

/** The items still worth pointing at, empty when nothing is. */
@Composable
fun rememberActiveWhatsNew(): Set<WhatsNewItem> {
    val active by LocalWhatsNew.current.active.collectAsStateWithLifecycle()
    return active
}

/** The pill that sits on the new or moved thing itself. */
@Composable
fun WhatsNewPill(kind: WhatsNewItem.Kind, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            when (kind) {
                WhatsNewItem.Kind.NEW -> R.string.whats_new_new
                WhatsNewItem.Kind.MOVED -> R.string.whats_new_moved
            }
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        maxLines = 1,
        modifier = modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** The dot on every screen on the way to something new. */
@Composable
fun WhatsNewDot(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.a11y_whats_new_inside)
    Box(
        modifier = modifier
            .size(8.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .semantics { contentDescription = description }
    )
}
