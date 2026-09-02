package com.anisync.android.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.anisync.android.presentation.util.bouncyClickable

/**
 * The app's one empty state: a 72dp emblem, a title, a sentence, and a single way out.
 *
 * Extracted from the library so Discover draws the same block rather than a second one that would
 * drift from it. Callers supply the emblem's shape and colours, which is what lets a library list
 * keep its own mark while a Discover failure stays neutral.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    /**
     * All three go together, and all three may be absent: a state with nothing useful to offer
     * should say so and stop, rather than show a button that does not change anything.
     */
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    emblemShape: Shape = RoundedCornerShape(9.dp),
    emblemContainer: Color = MaterialTheme.colorScheme.secondaryContainer,
    emblemContent: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    /**
     * Reserved for the states the viewer caused and can undo in one tap, which is the only place
     * the library uses it (a list filtered down to nothing). A failure they did not cause gets the
     * quieter treatment, so emphasis keeps meaning something.
     */
    actionEmphasised: Boolean = false,
    /** Fades in on first appearance; pass a key that changes when the reason for emptiness does. */
    animationKey: Any? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(animationKey) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "EmptyStateAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(emblemShape)
                .background(emblemContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = emblemContent,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
        )

        if (actionLabel != null && actionIcon != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            EmptyStateAction(
                label = actionLabel,
                icon = actionIcon,
                emphasised = actionEmphasised,
                onClick = onAction
            )
        }
    }
}

@Composable
fun EmptyStateAction(
    label: String,
    icon: ImageVector,
    emphasised: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (emphasised) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (emphasised) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = container,
        shape = CircleShape,
        modifier = modifier
            .height(44.dp)
            .bouncyClickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = label,
                clipShape = CircleShape
            )
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = content
            )
        }
    }
}
