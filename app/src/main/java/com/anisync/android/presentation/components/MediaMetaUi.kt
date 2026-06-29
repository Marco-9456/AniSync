package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatScore
import com.anisync.android.ui.theme.emphasis

private val StarColor = Color(0xFFFFC107)

/**
 * Small rounded metadata pill (status, progress, format, rewatches…). Shared so the profile media
 * list and the per-media Following cards speak one chip language (#78).
 *
 * @param accent when set, tints the pill (a translucent fill + matching text) — used for the
 *  library-status colour; otherwise it's a neutral surface chip.
 */
@Composable
fun MetaChip(
    label: String,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color? = null
) {
    val container = accent?.copy(alpha = 0.15f) ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val content = accent ?: MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = leadingIconTint ?: content,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** Score as a [MetaChip], rendered in the owner's score format. Hidden when there's no score. */
@Composable
fun ScoreChip(score: Double?, format: ScoreFormat?, modifier: Modifier = Modifier) {
    if (score == null || score <= 0.0) return
    val resolved = format ?: ScoreFormat.POINT_10_DECIMAL
    // POINT_5 / POINT_3 already render as stars/smileys, so the leading star only helps numeric formats.
    val showStar = resolved != ScoreFormat.POINT_5 && resolved != ScoreFormat.POINT_3
    MetaChip(
        label = formatScore(score, resolved),
        modifier = modifier,
        leadingIcon = if (showStar) Icons.Default.Star else null,
        leadingIconTint = StarColor
    )
}

/**
 * One-line preview of a freeform note. Plain text while it fits; once the single line overflows it
 * gains a trailing chevron and becomes tappable, calling [onOpen] to read the rest in a sheet.
 */
@Composable
fun NotePreview(
    note: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var overflowed by remember(note) { mutableStateOf(false) }

    Row(
        modifier = modifier.then(
            if (overflowed) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onOpen)
            else Modifier
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_note_stack_24px),
            contentDescription = stringResource(R.string.a11y_has_notes),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { overflowed = it.hasVisualOverflow },
            modifier = Modifier.weight(1f, fill = false)
        )
        if (overflowed) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Read-only bottom sheet showing a full note under a [heading] (the media title on a profile list,
 * or the note author's name in the Following section).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBottomSheet(note: String, heading: String, onDismiss: () -> Unit) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_note_stack_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.notes),
                    style = MaterialTheme.typography.labelSmall.emphasis(),
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = heading,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            SelectionContainer {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}
