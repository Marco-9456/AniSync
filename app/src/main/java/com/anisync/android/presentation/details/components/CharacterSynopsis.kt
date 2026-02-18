package com.anisync.android.presentation.details.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableCharacterSynopsis(text: AnnotatedString) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // Track which spoiler indices have been revealed
    var revealedSpoilers by rememberSaveable { mutableStateOf(setOf<Int>()) }

    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = effectsSpec,
        label = "ArrowRotation"
    )

    // Colors for spoiler styles
    val spoilerBackground = MaterialTheme.colorScheme.surfaceVariant
    val spoilerRevealedColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Linear scan: O(N+S log S) vs O(N²) for repeated getStringAnnotations
    val displayText = remember(text, revealedSpoilers) {
        // 1. Get all spoiler annotations in one go
        val spoilers = text.getStringAnnotations(tag = "SPOILER", start = 0, end = text.length)
            .sortedBy { it.start }

        // Fast path: if no spoilers, return original text
        if (spoilers.isEmpty()) return@remember text

        buildAnnotatedString {
            var lastIndex = 0

            // 2. Linear iteration through sorted spoilers
            for (spoiler in spoilers) {
                // Append text before this spoiler (preserves original styles implicitly via logic or we assume plain text between)
                // Note: AnnotatedString.subSequence preserves styles, so we use that.
                if (spoiler.start > lastIndex) {
                    append(text.subSequence(lastIndex, spoiler.start))
                }

                // Logic for the spoiler part
                val annotation = spoiler
                val spoilerIndex = annotation.item.toIntOrNull() ?: -1
                val isRevealed = spoilerIndex in revealedSpoilers
                val spoilerContent = text.substring(annotation.start, annotation.end)

                // Define listener for this specific spoiler
                val listener = LinkInteractionListener {
                    revealedSpoilers = if (spoilerIndex in revealedSpoilers) {
                        revealedSpoilers - spoilerIndex
                    } else {
                        revealedSpoilers + spoilerIndex
                    }
                }

                addLink(
                    LinkAnnotation.Clickable(
                        tag = "SPOILER",
                        linkInteractionListener = listener
                    ),
                    start = this.length,
                    end = this.length + spoilerContent.length
                )

                withStyle(
                    SpanStyle(
                        background = spoilerBackground,
                        color = if (isRevealed) spoilerRevealedColor else spoilerBackground
                    )
                ) {
                    append(spoilerContent)
                }

                lastIndex = spoiler.end
            }

            // 3. Append remaining text
            if (lastIndex < text.length) {
                append(text.subSequence(lastIndex, text.length))
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box {
                Text(
                    text = displayText,
                    modifier = Modifier.clickable { expanded = !expanded },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    ),
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
                Text(
                    text = if (expanded) "Show less" else "Read more",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }
    }
}