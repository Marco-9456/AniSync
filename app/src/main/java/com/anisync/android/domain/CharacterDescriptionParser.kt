package com.anisync.android.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val BR_TAG_REGEX = Regex("<br\\s*/?>")
private val ATTRIBUTE_REGEX = Regex("^(~!)?\\s*(__|\\*\\*)?([a-zA-Z0-9\\s\\-_()]+?)(:|\\2)\\s*(.*)(!~)?$")
private val MD_LINK_REGEX = Regex("\\[([^\\]]+)\\]\\([^)]+\\)")
private val MD_ITALIC_REGEX = Regex("(?<![_])_([^_]+)_(?![_])")
private val INLINE_TOKEN_REGEX = Regex("(\\*\\*|__|~!)(.+?)(\\1|!~)", RegexOption.DOT_MATCHES_ALL)

object CharacterDescriptionParser {

    /**
     * Parses the raw description string into key-value attributes and an AnnotatedString for the bio.
     * 
     * @param description Raw description text from API (may contain internal custom markdown).
     * @param spoilerBackgroundColor Color to use for hidden spoiler background.
     * @param spoilerContentColor Color to use for revealed spoiler text (or hidden if same as background).
     * @return A pair containing:
     *  - List<Pair<String, String>>: Extracted key-value attributes (e.g., "Height" -> "172cm")
     *  - AnnotatedString: The formatted biography text with spoiler annotations.
     */
    fun parse(
        description: String?,
        spoilerBackgroundColor: Color,
        spoilerContentColor: Color
    ): Pair<List<Pair<String, String>>, AnnotatedString> {
        if (description.isNullOrBlank()) return emptyList<Pair<String, String>>() to AnnotatedString("")

        val attributes = mutableListOf<Pair<String, String>>()
        val bioLines = mutableListOf<String>()

        val normalized = description.replace(BR_TAG_REGEX, "\n")

        normalized.lines().forEach { line ->
            val trimLine = line.trim()
            val match = ATTRIBUTE_REGEX.find(trimLine)
            var isAttribute = false

            if (trimLine.isNotBlank()) {
                if (match != null) {
                    val hasSpoilerMarker = match.groupValues[1].isNotEmpty() // ~!
                    val hasBoldMarkers = match.groupValues[2].isNotEmpty() // __ or **
                    val key = match.groupValues[3].trim()
                    val value = match.groupValues[5].trim()

                    // Heuristic: It's an attribute if:
                    // 1. It explicitly used bold markers (__ or **) OR spoiler markers (~!)
                    // 2. OR the line is reasonably short AND contains a colon
                    // Increased length limit to 200 to catch long attribute values (e.g. descriptions)
                    // Increased key length limit to 50
                    val isShortAndHasColon = trimLine.length < 200 && match.groupValues[4] == ":"

                    if ((hasBoldMarkers || hasSpoilerMarker || isShortAndHasColon) && key.length < 50 && value.isNotEmpty()) {
                        // Clean spoiler tags, markdown, links, and italics from value
                        // Also trim leading colons that might have been captured
                        var cleanValue = value.trimStart { it == ':' || it.isWhitespace() }
                            .replace("~!", "")
                            .replace("!~", "")
                            .replace("__", "")
                            .replace("**", "")
                        
                        cleanValue = cleanValue.replace(MD_LINK_REGEX, "$1")
                        cleanValue = cleanValue.replace(MD_ITALIC_REGEX, "$1")

                        attributes.add(key to cleanValue)
                        isAttribute = true
                    }
                }

                if (!isAttribute) {
                    bioLines.add(trimLine)
                }
            } else {
                 // Preserve empty lines for paragraph spacing, but handle them in the builder
                 if (bioLines.isNotEmpty() && bioLines.last().isNotEmpty()) {
                     bioLines.add("")
                 }
            }
        }

        // Drop empty lines at start of bio
        val cleanBioLines = bioLines.dropWhile { it.isBlank() }
        var fullBioText = cleanBioLines.joinToString("\n")
        
        fullBioText = fullBioText.replace(MD_LINK_REGEX, "$1")
        fullBioText = fullBioText.replace(MD_ITALIC_REGEX, "$1")
        
        // Build AnnotatedString for Bio - process entire text for multi-line spoilers
        // Spoilers are annotated with "SPOILER" tag for click-to-reveal functionality
        val bio = buildAnnotatedString {
            val hiddenSpoilerStyle = SpanStyle(
                background = spoilerBackgroundColor,
                color = spoilerBackgroundColor // Same color = text hidden
            )
            val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

            var currentIndex = 0
            var spoilerIndex = 0
            val matches = INLINE_TOKEN_REGEX.findAll(fullBioText)
            for (match in matches) {
                // Append text before match
                if (match.range.first > currentIndex) {
                    append(fullBioText.substring(currentIndex, match.range.first))
                }
                
                val token = match.groupValues[1] // **, __, or ~!
                val content = match.groupValues[2]
                
                if (token == "~!") {
                    // Add annotation for click handling
                    pushStringAnnotation(tag = "SPOILER", annotation = spoilerIndex.toString())
                    withStyle(hiddenSpoilerStyle) {
                        append(content)
                    }
                    pop()
                    spoilerIndex++
                } else {
                    withStyle(boldStyle) {
                        append(content)
                    }
                }
                
                currentIndex = match.range.last + 1
            }
            
            // Append remaining text
            if (currentIndex < fullBioText.length) {
                append(fullBioText.substring(currentIndex))
            }
        }

        return attributes to bio
    }
}
