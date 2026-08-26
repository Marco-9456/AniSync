package com.anisync.android.presentation.details.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisync.android.R
import com.anisync.android.presentation.components.ReadMoreToggle

/**
 * The viewer's own freeform note on a media entry.
 *
 * It rides with the tracking card rather than sitting inside the Overview tab, because a note is
 * the viewer's data about this entry in the same way status and progress are, and it should not be
 * behind a tab.
 *
 * It is deliberately NOT built like [ExpandableSynopsis] any more. The two used to be identical
 * cards, which put a viewer's private note and AniList's marketing blurb at the same weight and
 * made them read as twins once the redesign placed them near each other. This one belongs to the
 * strip family instead — the corner radius, the surface and the tinted round icon are the ones the
 * next-episode strip and the information rows use — so it reads as entry metadata.
 *
 * Callers should only place this when [notes] is non-blank.
 */
@Composable
fun UserNotesCard(
    notes: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 3
) {
    var expanded by rememberSaveable(notes) { mutableStateOf(false) }
    // Only show the toggle when the collapsed note actually overflows.
    var hasOverflow by remember(notes) { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_normal)))
                Text(
                    text = stringResource(R.string.your_notes),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.a11y_edit_notes),
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
