package com.anisync.android.domain.parser

internal class ParseContext(
    val config: ParserConfig,
    val blocks: MutableList<RichTextBlock> = mutableListOf(),
    val warnings: MutableList<ParseWarning> = mutableListOf(),
    val align: RichTextAlignment = RichTextAlignment.Start,
    val currentLinkUrl: String? = null,
    val listDepth: Int = 0
) {
    private val inlineBuffer = mutableListOf<RichTextInline>()
    var hasBufferedInlineContent: Boolean = false
        private set

    fun appendText(text: String) {
        if (text.isEmpty()) return
        val last = inlineBuffer.lastOrNull()
        if (last is RichTextInline.Text) {
            inlineBuffer[inlineBuffer.lastIndex] = RichTextInline.Text(last.value + text)
        } else {
            inlineBuffer.add(RichTextInline.Text(text))
        }
        hasBufferedInlineContent = true
    }

    fun appendInline(inline: RichTextInline) {
        if (inline is RichTextInline.Text) {
            appendText(inline.value)
            return
        }
        inlineBuffer.add(inline)
        hasBufferedInlineContent = true
    }

    fun flushText(kind: RichTextTextKind = RichTextTextKind.Paragraph) {
        if (!hasBufferedInlineContent) {
            inlineBuffer.clear()
            return
        }

        val trimmed = trimEdgeInlineText(inlineBuffer)
        if (!isBlankInlineList(trimmed)) {
            blocks.add(
                RichTextBlock.Text(
                    inlines = trimmed,
                    kind = kind,
                    align = align
                )
            )
        }

        inlineBuffer.clear()
        hasBufferedInlineContent = false
    }

    fun consumeInlineBufferTrimmed(): List<RichTextInline> {
        val trimmed = trimEdgeInlineText(inlineBuffer)
        inlineBuffer.clear()
        hasBufferedInlineContent = false
        return trimmed
    }

    fun appendInlines(inlines: List<RichTextInline>) {
        for (inline in inlines) {
            appendInline(inline)
        }
    }

    fun emitBlock(block: RichTextBlock) {
        blocks.add(block)
    }

    fun endsWithNewline(): Boolean {
        val tail = inlineBuffer.lastOrNull() ?: return false
        return when (tail) {
            is RichTextInline.LineBreak -> true
            is RichTextInline.Text -> tail.value.endsWith("\n")
            else -> false
        }
    }

    fun hasOnlyInlineBufferContent(): Boolean = blocks.isEmpty()

    fun detached(
        align: RichTextAlignment = this.align,
        currentLinkUrl: String? = this.currentLinkUrl,
        listDepth: Int = this.listDepth
    ): ParseContext = ParseContext(
        config = config,
        blocks = mutableListOf(),
        warnings = warnings,
        align = align,
        currentLinkUrl = currentLinkUrl,
        listDepth = listDepth
    )

    fun shared(
        align: RichTextAlignment = this.align,
        currentLinkUrl: String? = this.currentLinkUrl,
        listDepth: Int = this.listDepth
    ): ParseContext = ParseContext(
        config = config,
        blocks = blocks,
        warnings = warnings,
        align = align,
        currentLinkUrl = currentLinkUrl,
        listDepth = listDepth
    )
}
