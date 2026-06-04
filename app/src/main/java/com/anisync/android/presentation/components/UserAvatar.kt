package com.anisync.android.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.data.AvatarShape
import com.anisync.android.ui.theme.LocalAvatarBackgroundEnabled
import com.anisync.android.ui.theme.LocalAvatarShape
import com.anisync.android.ui.theme.LocalAvatarShapeId

/**
 * Single source of truth for rendering a user avatar.
 *
 * Applies the user-selected avatar shape ([LocalAvatarShape]) and frame — a thin
 * border plus an optional background fill — consistently everywhere. The fill is
 * gated by [LocalAvatarBackgroundEnabled] so the Look & Feel "avatar background"
 * setting takes effect app-wide, not just on the profile header. Falls back to a
 * person glyph when [url] is null.
 *
 * Callers pass only layout/click modifiers; clip, border, background and shape are
 * owned here so every avatar in the app stays in sync with the settings.
 */
@Composable
fun UserAvatar(
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    url: String? = null,
    model: Any? = url,
    shape: Shape = LocalAvatarShape.current,
    showFrame: Boolean = LocalAvatarBackgroundEnabled.current,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    framePadding: Dp = 0.dp,
    // The large profile-header avatar opts into rendering the "None" shape uncropped
    // (natural aspect, no frame). Other avatars keep the framed square even for "None".
    isProfileHeader: Boolean = false,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    // The profile-header avatar with the "None" shape renders uncropped at its natural
    // aspect ratio — no square crop, no frame — so tall avatars aren't clipped into a
    // box. Everywhere else (and other shapes) keeps the framed, shaped avatar.
    val noneHeader = isProfileHeader && LocalAvatarShapeId.current == AvatarShape.NONE
    if (noneHeader && model != null) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.FillWidth,
            modifier = modifier.width(size)
        )
        return
    }

    // Single "frame" toggle: border + background fill + inset move together.
    val frame = showFrame && !noneHeader
    val showBackground = frame
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                if (showBackground) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape)
                } else {
                    Modifier
                }
            )
            .then(
                if (frame) Modifier.border(borderWidth, borderColor, shape) else Modifier
            )
            .padding(if (frame) framePadding else 0.dp)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.7f)
            )
        }
        overlay()
    }
}
