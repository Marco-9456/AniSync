package com.anisync.android.presentation.details.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.presentation.components.ReadMoreToggle
import com.anisync.android.ui.theme.emphasis

/**
 * The viewer's own freeform note on a media entry, surfaced read-first on the detail page so it can
 * be re-read without opening the edit sheet (#75).
 *
 * Deliberately built to read as a native detail section — it mirrors [ExpandableSynopsis] (same
 * extra-large surfaceContainerLow card, primary small-caps label + trailing icon, shared
 * [ReadMoreToggle]) so it sits naturally beside Synopsis/Categories rather than as a foreign widget.
 * The line breaks are preserved and the text is selectable; the pencil opens the edit sheet.
 *
 * Callers should only place this when [notes] is non-blank.
 */
@Composable
fun UserNotesCard(
    notes: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 5
) {
    var expanded by rememberSaveable(notes) { mutableStateOf(false) }
    // Only show the toggle when the collapsed note actually overflows.
    var hasOverflow by remember(notes) { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_extra_large))
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.your_notes),
                    style = MaterialTheme.typography.labelSmall.emphasis(),
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.a11y_edit_notes),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))

            SelectionContainer {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (!expanded) hasOverflow = result.hasVisualOverflow
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (hasOverflow || expanded) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacing_normal)))
                ReadMoreToggle(
                    expanded = expanded,
                    onToggle = { expanded = !expanded }
                )
            }
        }
    }
}
