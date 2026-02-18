package com.anisync.android.presentation.details.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anisync.android.R
import com.anisync.android.domain.ExternalLink
import com.anisync.android.domain.ExternalLinkType
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader
import com.anisync.android.presentation.util.bouncyCombinedClickable
import com.anisync.android.presentation.util.rememberHapticFeedback
import com.anisync.android.type.MediaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExternalLinksSection(
    externalLinks: List<ExternalLink>,
    mediaType: MediaType?,
    modifier: Modifier = Modifier
) {
    // Memoize filtered lists to avoid allocation each frame
    val streamingLinks = remember(externalLinks) {
        externalLinks.filter { it.type == ExternalLinkType.STREAMING }
    }

    val otherLinks = remember(externalLinks) {
        externalLinks.filter { it.type != ExternalLinkType.STREAMING }
    }

    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.section_external_links),
            level = HeaderLevel.Section
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

        // Streaming links (if any) - prominent display
        if (streamingLinks.isNotEmpty()) {
            Text(
                text = stringResource(
                    if (mediaType == MediaType.MANGA) R.string.subsection_reading
                    else R.string.subsection_streaming
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            LazyRow(
                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                items(streamingLinks, key = { it.id }) { link ->
                    ExternalLinkChip(link)
                }
            }
        }

        // Other links
        if (otherLinks.isNotEmpty()) {
            if (streamingLinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            }
            Text(
                text = stringResource(R.string.subsection_external),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            LazyRow(
                contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                items(otherLinks, key = { it.id }) { link ->
                    ExternalLinkChip(link)
                }
            }
        }
    }
}

@Composable
fun ExternalLinkChip(
    link: ExternalLink,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()

    // Confirmation animation trigger - increments when copy happens
    var confirmationTrigger by remember { mutableIntStateOf(0) }

    // Confirmation bounce animation (triggers immediately after copy)
    val confirmationScale by animateFloatAsState(
        targetValue = if (confirmationTrigger > 0) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = 0.4f,  // Bouncy spring
            stiffness = 800f
        ),
        label = "ConfirmationBounce"
    )

    // Parse the color from hex string (fallback to primary)
    val chipColor = remember(link.color) {
        link.color?.let { colorHex ->
            try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (_: Exception) {
                null
            }
        }
    }

    // Format the label text
    val labelText = remember(link) {
        val info = listOfNotNull(link.language, link.notes)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (info.isNotEmpty()) "${link.site} ($info)" else link.site
    }

    Surface(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .graphicsLayer {
                // Apply confirmation bounce scale
                scaleX = confirmationScale
                scaleY = confirmationScale
            }
            .bouncyCombinedClickable(
                onClick = {
                    link.url?.let { url ->
                        try {
                            uriHandler.openUri(url)
                        } catch (_: Exception) {
                            // Ignore error
                        }
                    }
                },
                onLongClick = {
                    link.url?.let { url ->
                        // Perform haptic feedback
                        haptic.longPress()

                        // Copy to clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Link from ${link.site}", url)
                        clipboard.setPrimaryClip(clip)

                        // Trigger confirmation bounce immediately
                        confirmationTrigger++

                        // Reset confirmation trigger after animation completes
                        scope.launch {
                            delay(150) // Short delay while still pressed
                            confirmationTrigger = 0
                        }

                        // Show toast notification
                        Toast.makeText(
                            context,
                            "Copied ${link.site} link to clipboard",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Leading icon
            if (link.icon != null) {
                AsyncImage(
                    model = link.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = chipColor?.let { ColorFilter.tint(it) }
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = chipColor ?: MaterialTheme.colorScheme.primary
                )
            }

            // Label
            Text(
                text = labelText,
                style = LocalTextStyle.current.copy(
                    fontSize = MaterialTheme.typography.labelLarge.fontSize
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}