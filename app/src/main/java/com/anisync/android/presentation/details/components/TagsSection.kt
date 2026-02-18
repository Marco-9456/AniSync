package com.anisync.android.presentation.details.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anisync.android.R
import com.anisync.android.domain.Tag
import com.anisync.android.presentation.components.HeaderLevel
import com.anisync.android.presentation.components.SectionHeader

/**
 * Displays content metadata (Genres and Tags) in a unified section.
 * Organized into subsections: Genres, Tags, and Spoilers.
 */
@Composable
fun ContentMetadataSection(
    genres: List<String>,
    tags: List<Tag>,
    modifier: Modifier = Modifier
) {
    if (genres.isEmpty() && tags.isEmpty()) return

    // Memoize: sorting/filtering runs on every frame during scroll otherwise
    val sortedTags = remember(tags) {
        tags.filter { it.rank != null }.sortedByDescending { it.rank }
    }

    val spoilerTags = remember(sortedTags) {
        sortedTags.filter { it.isMediaSpoiler || it.isGeneralSpoiler }
    }

    val regularTags = remember(sortedTags) {
        sortedTags.filter { !it.isMediaSpoiler && !it.isGeneralSpoiler }
    }

    Column(modifier = modifier) {
        SectionHeader(
            title = "Categories",
            level = HeaderLevel.Section
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

        // 1. Genres Subsection
        if (genres.isNotEmpty()) {
            MetadataGroup(
                title = "Genres",
                items = genres,
                keySelector = { it }
            ) { genre ->
                GenreChip(genre = genre)
            }
        }

        // 2. Regular Tags Subsection
        if (regularTags.isNotEmpty()) {
            if (genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            }
            MetadataGroup(
                title = "Tags",
                items = regularTags,
                keySelector = { it.name }
            ) { tag ->
                TagChip(tag = tag, isSpoiler = false)
            }
        }

        // 3. Spoiler Tags Subsection
        if (spoilerTags.isNotEmpty()) {
            if (genres.isNotEmpty() || regularTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            }
            MetadataGroup(
                title = "Spoilers",
                titleColor = MaterialTheme.colorScheme.error,
                items = spoilerTags,
                keySelector = { it.name }
            ) { tag ->
                TagChip(tag = tag, isSpoiler = true)
            }
        }
    }
}

@Composable
private fun <T> MetadataGroup(
    title: String,
    items: List<T>,
    keySelector: (T) -> Any,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable (T) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = items,
                key = keySelector
            ) { item ->
                content(item)
            }
        }
    }
}

@Composable
private fun GenreChip(
    genre: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
    ) {
        Box(
            modifier = Modifier
                .clickable { /* TODO: Filter by genre */ }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = genre,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagChip(
    tag: Tag,
    isSpoiler: Boolean,
    modifier: Modifier = Modifier
) {
    // State to toggle spoiler visibility
    var isVisible by remember { mutableStateOf(!isSpoiler) }

    val colorScheme = MaterialTheme.colorScheme
    val tagColors = rememberTagColors(tag.category, colorScheme, isSpoiler)

    val shape = RoundedCornerShape(8.dp)

    // Tooltip state
    val tooltipState = rememberTooltipState()

    // Only show tooltip if:
    // 1. Tag has a description
    // 2. For spoilers: only when revealed (isVisible == true)
    val showTooltip = tag.description?.isNotBlank() == true && (!isSpoiler || isVisible)

    val tagContent = @Composable {
        Surface(
            modifier = modifier
                .height(32.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            shape = shape,
            color = tagColors.containerColor.copy(alpha = 0.12f),
            border = BorderStroke(
                1.dp,
                tagColors.borderColor.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .clip(shape)
                    .clickable(
                        enabled = isSpoiler,
                        onClick = { isVisible = !isVisible }
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimatedContent(
                    targetState = isVisible,
                    label = "spoiler_reveal"
                ) { visible ->
                    if (visible) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = tagColors.textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // Hidden Spoiler State: Icon + "Spoiler" text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = tagColors.textColor
                            )
                            Text(
                                text = "Spoiler",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = tagColors.textColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTooltip) {
        TooltipBox(
            // Use the standard default provider as requested
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(text = tag.description ?: "")
                }
            },
            state = tooltipState,
            enableUserInput = true
        ) {
            tagContent()
        }
    } else {
        tagContent()
    }
}

/**
 * Returns color scheme using Material Tokens.
 */
@Composable
private fun rememberTagColors(
    category: String,
    colorScheme: androidx.compose.material3.ColorScheme,
    isSpoiler: Boolean
): TagColors {
    if (isSpoiler) {
        return TagColors(
            containerColor = colorScheme.error,
            borderColor = colorScheme.error,
            textColor = colorScheme.error
        )
    }

    return when (category.lowercase()) {
        "themes" -> TagColors(
            containerColor = colorScheme.primary,
            borderColor = colorScheme.primary,
            textColor = colorScheme.primary
        )

        "demographics" -> TagColors(
            containerColor = colorScheme.secondary,
            borderColor = colorScheme.secondary,
            textColor = colorScheme.secondary
        )

        "genre" -> TagColors(
            containerColor = colorScheme.tertiary,
            borderColor = colorScheme.tertiary,
            textColor = colorScheme.tertiary
        )

        "cast", "setting" -> TagColors(
            containerColor = colorScheme.secondaryContainer,
            borderColor = colorScheme.onSecondaryContainer,
            textColor = colorScheme.onSurface
        )

        "technical" -> TagColors(
            containerColor = colorScheme.outline,
            borderColor = colorScheme.outline,
            textColor = colorScheme.onSurfaceVariant
        )

        "content-warning", "explicit-content" -> TagColors(
            containerColor = colorScheme.error,
            borderColor = colorScheme.error,
            textColor = colorScheme.error
        )

        else -> TagColors(
            containerColor = colorScheme.surfaceVariant,
            borderColor = colorScheme.outline,
            textColor = colorScheme.onSurface
        )
    }
}

private data class TagColors(
    val containerColor: Color,
    val borderColor: Color,
    val textColor: Color
)