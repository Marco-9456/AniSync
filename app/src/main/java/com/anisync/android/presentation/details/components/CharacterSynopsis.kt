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

    // PERFORMANCE OPTIMIZATION: Chunk-based string building
    // Old Complexity: O(N) builder calls (where N is char count)
    // New Complexity: O(S) builder calls (where S is number of spans/spoilers)
    val displayText = remember(text, revealedSpoilers) {
        // Trace for verification
        val startTime = System.nanoTime()

        val result = buildAnnotatedString {
            var currentIndex = 0
            val length = text.length

            // We iterate through the string looking for the next interesting point (Spoiler or Style)
            while (currentIndex < length) {
                // Find the next spoiler at or after current index
                val spoilerAnnotations =
                    text.getStringAnnotations(tag = "SPOILER", start = currentIndex, end = length)
                // Find the next generic span style
                // Note: optimization relies on text.spanStyles being sorted or easy to access.
                // Since accessing spanStyles inside the loop can be expensive, we prioritize the logic:
                // If the ORIGINAL implementation relied on strictly following the structure, we replicate it efficiently.

                // Optimized approach: Check if current position starts a spoiler
                val activeSpoiler = text.getStringAnnotations(
                    tag = "SPOILER",
                    start = currentIndex,
                    end = currentIndex + 1
                ).firstOrNull()

                if (activeSpoiler != null) {
                    val annotation = activeSpoiler
                    val spoilerIndex = annotation.item.toIntOrNull() ?: -1
                    val isRevealed = spoilerIndex in revealedSpoilers

                    val spoilerContent = text.substring(annotation.start, annotation.end)

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

                    currentIndex = annotation.end
                } else {
                    // It's standard text or other styles.
                    // Instead of appending char by char, find distance to next "event"
                    // An event is the start of a SPOILER tag.

                    // Look ahead for the next spoiler
                    val nextSpoilerIndex = text.getStringAnnotations(
                        tag = "SPOILER",
                        start = currentIndex,
                        end = length
                    )
                        .minByOrNull { it.start }?.start ?: length

                    // Append everything until the next spoiler
                    // We preserve existing styles by appending the sub-sequence of the original AnnotatedString
                    if (nextSpoilerIndex > currentIndex) {
                        append(text.subSequence(currentIndex, nextSpoilerIndex))
                        currentIndex = nextSpoilerIndex
                    } else {
                        // Fallback safety (shouldn't happen if logic is correct, prevents infinite loop)
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
            }
        }

        // Log verification (Remove in production)
        // Log.d("Perf", "String build time: ${(System.nanoTime() - startTime) / 1000}us")
        result
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