package com.anisync.android.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Parses markdown-like character descriptions into structured attributes and biography.
 * Handles styles including bold, spoiler tags, and basic markdown stripping.
 */
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

        // Normalize breaks
        val normalized = description.replace(Regex("<br\\s*/?>"), "\n")

        // Extended Regex to catch lines starting with bold keys or simple "Key: Value" patterns
        // Catch: "__Key__ Value", "**Key** Value", "Key: Value" (if line is short-ish)
        // Updated to allow () in keys for cases like "Devil Fruit (Type):"
        // Also handles spoiler-wrapped attributes like "~!Key: Value!~"
        val attributeRegex = Regex("^(~!)?\\s*(__|\\*\\*)?([a-zA-Z0-9\\s\\-_()]+?)(:|\\2)\\s*(.*)(!~)?$")

        normalized.lines().forEach { line ->
            val trimLine = line.trim()
            val match = attributeRegex.find(trimLine)
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
                        
                        // Strip markdown links [text](url) -> text
                        cleanValue = cleanValue.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
                        
                        // Strip single-underscore italics _text_ -> text
                        cleanValue = cleanValue.replace(Regex("(?<![_])_([^_]+)_(?![_])"), "$1")

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
        
        // Pre-process: Strip markdown links [text](url) -> text
        fullBioText = fullBioText.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        
        // Pre-process: Strip single-underscore italics _text_ -> text (but not double __ which is bold)
        fullBioText = fullBioText.replace(Regex("(?<![_])_([^_]+)_(?![_])"), "$1")
        
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
            // Regex to find markers: **bold**, __bold__, ~!spoiler!~
            // Use DOTALL flag (?s) to make . match newlines for multi-line spoilers
            val tokenRegex = Regex("(\\*\\*|__|~!)(.+?)(\\1|!~)", RegexOption.DOT_MATCHES_ALL)
            
            val matches = tokenRegex.findAll(fullBioText)
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
