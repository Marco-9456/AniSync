package com.anisync.android.presentation.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
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
import com.anisync.android.type.MediaType

@Composable
fun ExternalLinksSection(
    externalLinks: List<ExternalLink>,
    mediaType: MediaType?,
    modifier: Modifier = Modifier
) {
    // Group links by type: STREAMING first, then SOCIAL, then INFO
    val streamingLinks = externalLinks.filter { it.type == ExternalLinkType.STREAMING }
    val otherLinks = externalLinks.filter { it.type != ExternalLinkType.STREAMING }

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
    val uriHandler = LocalUriHandler.current
    
    // Parse the color from hex string (fallback to primary)
    val chipColor = remember(link.color) {
        link.color?.let { colorHex ->
            try {
                Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                null
            }
        }
    }
    
    AssistChip(
        onClick = {
            link.url?.let { url ->
                try {
                    uriHandler.openUri(url)
                } catch (e: Exception) {
                    // Ignore error
                }
            }
        },
        label = {
            Text(
                text = remember(link) {
                    val info = listOfNotNull(link.language, link.notes)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    if (info.isNotEmpty()) "${link.site} ($info)" else link.site
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
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
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = chipColor ?: MaterialTheme.colorScheme.primary
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
    )
}
