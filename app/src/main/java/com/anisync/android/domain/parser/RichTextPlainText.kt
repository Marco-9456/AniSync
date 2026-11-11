package com.anisync.android.domain.parser

import org.jsoup.nodes.TextNode

/**
 * Posts flattened to plain text for previews, which have no way to hide anything. Spoilers go
 * together with what they hide, the way the website's own plain text helper drops them.
 */
object RichTextPlainText {

    private const val SPOILER_SELECTOR =
        "${RichTextSpoilers.BLOCK_TAG}, ${RichTextSpoilers.INLINE_TAG}, [rel=spoiler], " +
            ".markdown-spoiler, .markdown_spoiler"

    // asHtml sometimes fences a whole post into <pre><code>, escaped spoilers and all. The renderer
    // re-reads such a block as rich text, so the excerpt does too.
    private val WRAPPED_RICH_TEXT = Regex("""markdown_spoiler|~!|<\s*[a-zA-Z]""")

    /** The text of AniList HTML with every spoiler removed, whitespace collapsed. */
    fun excerpt(html: String): String = excerpt(html, recoverWrapped = true)

    /** Markdown (or AniList HTML) with every spoiler cut out, the rest left as it was. */
    fun withoutSpoilers(text: String): String = RichTextSpoilers.stripSpoilers(text)

    private fun excerpt(html: String, recoverWrapped: Boolean): String {
        if (html.isBlank()) return ""
        val body = runCatching { parseRichTextBody(html) }.getOrNull() ?: return ""
        body.select(SPOILER_SELECTOR).remove()
        if (recoverWrapped) {
            for (pre in body.select("pre")) {
                val code = pre.selectFirst("code")
                // The misparse can swallow a spoiler's opener into the language token, so it goes back in front.
                val swallowed = code?.let { swallowedLanguageContent(it.attr("class")) }
                val text = code?.wholeText() ?: pre.wholeText()
                val source = if (swallowed != null) "$swallowed\n$text" else text
                if (swallowed != null || WRAPPED_RICH_TEXT.containsMatchIn(source)) {
                    pre.replaceWith(TextNode(" ${excerpt(source, recoverWrapped = false)} "))
                }
            }
        }
        return body.text().trim()
    }
}
