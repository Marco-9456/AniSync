package com.anisync.android.domain.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

data class ParserConfig(
    val enableAniListLinkBlocks: Boolean = true
)

/** Normalizes AniList HTML (or typed markdown), parses it and wraps its spoilers. */
internal fun parseRichTextBody(html: String): Element {
    val document = Jsoup.parseBodyFragment(RichTextNormalizer.normalize(html))
    document.outputSettings().prettyPrint(false)
    val body = document.body()
    RichTextSpoilers.wrap(body)
    return body
}

object RichTextParser {
    suspend fun parse(
        html: String,
        config: ParserConfig = ParserConfig()
    ): ParsedRichText = withContext(Dispatchers.Default) {
        if (html.isBlank()) return@withContext ParsedRichText(emptyList(), emptyList())

        try {
            val inlineParser = RichTextInlineParser(config)
            val htmlParser = RichTextHtmlParser(inlineParser)
            val rawResult = htmlParser.parse(parseRichTextBody(html))

            val groupedBlocks = RichTextPostProcessor.groupInlineBlocks(rawResult.blocks)
            val imageUrls = RichTextPostProcessor.extractImageUrls(groupedBlocks)

            ParsedRichText(
                blocks = groupedBlocks,
                imageUrls = imageUrls,
                warnings = rawResult.warnings
            )
        } catch (e: Exception) {
            ParsedRichText(
                blocks = listOf(
                    RichTextBlock.Text(
                        inlines = listOf(RichTextInline.Text(html))
                    )
                ),
                imageUrls = emptyList(),
                warnings = listOf(ParseWarning("Parse failed: ${e.message}", "RichTextParser.parse"))
            )
        }
    }
}
