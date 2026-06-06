package com.anisync.android.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.util.TranslateUtils.openTranslator
import com.anisync.android.util.stripHtml

/**
 * Icon button that sends [text] to an external translation app. The text may contain AniList HTML;
 * it is stripped to plain text before being handed off. Disabled (and a no-op) when [text] is null
 * or blank.
 *
 * @param iconSize glyph size — use a smaller value (~18–20.dp) inside compact section-label rows,
 * the 24.dp default to sit alongside other top-bar actions.
 */
@Composable
fun TranslateIconButton(
    text: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    val context = LocalContext.current
    IconButton(
        onClick = { text?.let { context.openTranslator(it.stripHtml()) } },
        enabled = !text.isNullOrBlank(),
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = stringResource(R.string.translate),
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
