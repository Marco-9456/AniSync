package com.anisync.android.presentation.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Displays styled bio text with hashtags highlighted in primary color.
 * Fully memoized to prevent unnecessary recompositions.
 *
 * @param bio The raw bio text, which may contain HTML tags.
 * @param modifier Modifier for the text component.
 */
@Composable
fun StyledBioText(
    bio: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    // Memoize the entire styled text including color application
    val styledText = remember(bio, primaryColor) {
        // Clean HTML tags from bio
        val cleanBio = bio.replace(Regex("<[^>]*>"), "").trim()
        val hashtagPattern = Regex("#\\w+")
        
        buildAnnotatedString {
            var lastIndex = 0
            hashtagPattern.findAll(cleanBio).forEach { match ->
                // Append text before hashtag
                append(cleanBio.substring(lastIndex, match.range.first))
                // Append hashtag with primary color
                withStyle(SpanStyle(color = primaryColor)) {
                    append(match.value)
                }
                lastIndex = match.range.last + 1
            }
            // Append remaining text
            if (lastIndex < cleanBio.length) {
                append(cleanBio.substring(lastIndex))
            }
        }
    }
    
    Text(
        text = styledText,
        style = MaterialTheme.typography.bodyMedium,
        color = textColor,
        lineHeight = 20.sp,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
