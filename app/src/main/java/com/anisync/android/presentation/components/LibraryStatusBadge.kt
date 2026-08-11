package com.anisync.android.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.LibraryStatus
import com.anisync.android.presentation.util.toColor
import com.anisync.android.presentation.util.toIcon
import com.anisync.android.presentation.util.toLabel
import com.anisync.android.type.MediaType

/**
 * Marks a poster as already being on one of the viewer's lists.
 *
 * Sits over the cover on browsing surfaces (discover, search, related media) where the entry is
 * not otherwise visible. Colour alone would not carry the meaning, so the status icon and the
 * spoken label both come along.
 */
@Composable
fun LibraryStatusBadge(
    status: LibraryStatus,
    type: MediaType?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = status.toColor(),
        contentColor = Color.White,
        shape = CircleShape,
        shadowElevation = 2.dp,
        modifier = modifier
            .size(24.dp)
            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
    ) {
        Icon(
            imageVector = status.toIcon(type),
            contentDescription = stringResource(R.string.a11y_on_list, status.toLabel(type)),
            modifier = Modifier.padding(5.dp)
        )
    }
}
