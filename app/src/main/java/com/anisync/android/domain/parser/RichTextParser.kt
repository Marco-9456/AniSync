package com.anisync.android.domain.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class ParserConfig(
    val enableAniListLinkBlocks: Boolean = true
)

object RichTextParser {
    suspend fun parse(
        html: String,
        config: ParserConfig = ParserConfig()
    ): ParsedRichText = withContext(Dispatchers.Default) {
        if (html.isBlank()) return@withContext ParsedRichText(emptyList(), emptyList())

        try {
            val normalized = RichTextNormalizer.normalize(html)
            val document = Jsoup.parseBodyFragment(normalized)
            document.outputSettings().prettyPrint(false)

            val inlineParser = RichTextInlineParser(config)
            val htmlParser = RichTextHtmlParser(inlineParser)
            val rawResult = htmlParser.parse(document.body())

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
