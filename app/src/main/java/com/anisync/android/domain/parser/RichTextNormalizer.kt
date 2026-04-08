package com.anisync.android.domain.parser

internal object RichTextNormalizer {
    fun normalize(html: String): String {
        var processed = html.replace("\r", "")
        processed = decodeAniListEscapedParenthesis(processed)
        processed = replaceCenterTags(processed)
        processed = replaceCenterMarkdownBlocks(processed)
        processed = replaceMarkdownSpoilers(processed)
        processed = preserveYoutubeDivs(processed)
        processed = normalizeMarkdownBlockquotes(processed)
        return processed
    }

    private fun decodeAniListEscapedParenthesis(text: String): String =
        text
            .replace("&amp;rpar;", ")")
            .replace("&amp;rpar", ")")
            .replace("&rpar;", ")")
            .replace("&rpar", ")")
            .replace("&amp;lpar;", "(")
            .replace("&amp;lpar", "(")
            .replace("&lpar;", "(")
            .replace("&lpar", "(")
            .replace("&#41;", ")")
            .replace("&#40;", "(")

    private fun replaceCenterTags(text: String): String =
        text
            .replace("<center>", "<div align=\"center\">")
            .replace("</center>", "</div>")

    private fun replaceCenterMarkdownBlocks(text: String): String {
        val regex = Regex("""~~~(?:center)?(.*?)~~~""", RegexOption.DOT_MATCHES_ALL)
        return text.replace(regex) { match ->
            "<div align=\"center\">${match.groupValues[1]}</div>"
        }
    }

    private fun replaceMarkdownSpoilers(text: String): String {
        val spoilerRegex = Regex("""~!(.*?)!~""", RegexOption.DOT_MATCHES_ALL)
        return text.replace(spoilerRegex) { match ->
            "<spoiler>${match.groupValues[1]}</spoiler>"
        }
    }

    private fun preserveYoutubeDivs(text: String): String {
        val youtubeRegex = Regex(
            """<div([^>]*)class=['\"]youtube['\"]([^>]*)>(.*?)</div>""",
            RegexOption.DOT_MATCHES_ALL
        )
        return text.replace(youtubeRegex) { match ->
            "<youtube${match.groupValues[1]}class=\"youtube\"${match.groupValues[2]}>${match.groupValues[3]}</youtube>"
        }
    }

    private fun normalizeMarkdownBlockquotes(text: String): String {
        val lines = text.split("\n")
        val out = StringBuilder()
        var currentDepth = 0
        var inCodeBlock = false

        for (line in lines) {
            val trimmed = line.trimStart()

            if (!inCodeBlock) {
                if (trimmed.startsWith("```") || trimmed.startsWith("<pre")) {
                    inCodeBlock = true
                }
            } else {
                if (trimmed.startsWith("```") || trimmed.contains("</pre>")) {
                    inCodeBlock = false
                }
            }

            if (inCodeBlock) {
                while (currentDepth > 0) {
                    out.append("</blockquote>\n")
                    currentDepth--
                }
                out.append(line).append("\n")
                continue
            }

            var depth = 0
            var content = trimmed
            while (content.startsWith(">")) {
                depth++
                content = content.substring(1).trimStart()
            }

            while (currentDepth < depth) {
                out.append("<blockquote>\n")
                currentDepth++
            }
            while (currentDepth > depth) {
                out.append("</blockquote>\n")
                currentDepth--
            }

            out.append(content).append("\n")
        }

        while (currentDepth > 0) {
            out.append("</blockquote>\n")
            currentDepth--
        }

        return out.toString()
    }
}
