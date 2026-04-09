package com.anisync.android.domain.parser

private val SPACE_COLLAPSE_REGEX = Regex("[ \\t\\x0B\\f\\r]+")

internal fun normalizeWhitespace(text: String): String =
    text.replace(SPACE_COLLAPSE_REGEX, " ")

internal fun trimEdgeInlineText(inlines: List<RichTextInline>): List<RichTextInline> {
    if (inlines.isEmpty()) return emptyList()

    val mutable = inlines.toMutableList()
    while (mutable.isNotEmpty()) {
        val first = mutable.first()
        if (first is RichTextInline.Text) {
            val trimmed = first.value.trimStart('\n', '\r', ' ')
            if (trimmed.isEmpty()) {
                mutable.removeAt(0)
                continue
            }
            mutable[0] = RichTextInline.Text(trimmed)
        }
        break
    }

    while (mutable.isNotEmpty()) {
        val last = mutable.last()
        if (last is RichTextInline.Text) {
            val trimmed = last.value.trimEnd('\n', '\r', ' ')
            if (trimmed.isEmpty()) {
                mutable.removeAt(mutable.lastIndex)
                continue
            }
            mutable[mutable.lastIndex] = RichTextInline.Text(trimmed)
        }
        break
    }

    return mutable
}

internal fun isBlankInlineList(inlines: List<RichTextInline>): Boolean {
    if (inlines.isEmpty()) return true
    // Explicit LineBreaks are formatting blocks and should not be swallowed/treated as completely blank
    return inlines.all { inline ->
        inline is RichTextInline.Text && inline.value.isBlank()
    }
}

internal fun headingKind(level: Int): RichTextTextKind = when (level) {
    1 -> RichTextTextKind.Heading1
    2 -> RichTextTextKind.Heading2
    3 -> RichTextTextKind.Heading3
    4 -> RichTextTextKind.Heading4
    5 -> RichTextTextKind.Heading5
    else -> RichTextTextKind.Paragraph
}

internal fun parseAlignment(
    tagName: String,
    alignAttr: String,
    fallback: RichTextAlignment
): RichTextAlignment {
    if (tagName == "center") return RichTextAlignment.Center
    return when (alignAttr.lowercase()) {
        "center" -> RichTextAlignment.Center
        "right" -> RichTextAlignment.End
        "justify" -> RichTextAlignment.Justify
        else -> fallback
    }
}