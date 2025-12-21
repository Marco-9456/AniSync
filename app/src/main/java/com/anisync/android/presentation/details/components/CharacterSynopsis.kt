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
    
    // Build display text with revealed/hidden spoiler styles
    val displayText = remember(text, revealedSpoilers) {
        buildAnnotatedString {
            // Copy the original text structure but update spoiler styles based on revealed state
            var i = 0
            while (i < text.length) {
                val annotations = text.getStringAnnotations(tag = "SPOILER", start = i, end = i + 1)
                if (annotations.isNotEmpty()) {
                    val annotation = annotations.first()
                    val spoilerIndex = annotation.item.toIntOrNull() ?: -1
                    val isRevealed = spoilerIndex in revealedSpoilers
                    
                    // Extract the spoiler content
                    val spoilerContent = text.substring(annotation.start, annotation.end)
                    
                    val listener = LinkInteractionListener {
                         revealedSpoilers = if (spoilerIndex in revealedSpoilers) {
                             revealedSpoilers - spoilerIndex
                         } else {
                             revealedSpoilers + spoilerIndex
                         }
                    }

                    // Add clickable link annotation for spoiler toggle
                    addLink(
                        LinkAnnotation.Clickable(
                            tag = "SPOILER",
                            linkInteractionListener = listener
                        ),
                        start = length,
                        end = length + spoilerContent.length
                    )

                    withStyle(SpanStyle(
                        background = spoilerBackground,
                        color = if (isRevealed) spoilerRevealedColor else spoilerBackground
                    )) {
                        append(spoilerContent)
                    }
                    
                    i = annotation.end
                } else {
                    // Check for other styles (bold, etc)
                    val spanStyles = text.spanStyles.filter { it.start <= i && it.end > i }
                    if (spanStyles.isNotEmpty()) {
                        val span = spanStyles.first()
                        withStyle(span.item) {
                            append(text.substring(i, minOf(i + 1, span.end)))
                        }
                        i++
                    } else {
                        append(text[i])
                        i++
                    }
                }
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
            // Note: Header is now external to this component
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
