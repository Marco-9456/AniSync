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

internal fun bulletSymbol(depth: Int): String = when (depth % 3) {
    0 -> "•"
    1 -> "◦"
    else -> "▪"
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
    fallback: RichTextAlignment,
    styleAttr: String = ""
): RichTextAlignment {
    if (tagName == "center") return RichTextAlignment.Center
    // The HTML `align` attribute wins over CSS, then fall back to `style="text-align: …"`.
    // AniList's renderer emits CSS text-align in many places, so reading only the attribute
    // silently dropped alignment (e.g. `<p style="text-align:center">`).
    alignmentKeyword(alignAttr)?.let { return it }
    cssTextAlign(styleAttr)?.let { alignmentKeyword(it)?.let { resolved -> return resolved } }
    return fallback
}

private fun alignmentKeyword(value: String): RichTextAlignment? =
    when (value.trim().lowercase()) {
        "center" -> RichTextAlignment.Center
        "right" -> RichTextAlignment.End
        "justify" -> RichTextAlignment.Justify
        "left" -> RichTextAlignment.Start
        else -> null
    }

private fun cssTextAlign(style: String): String? {
    if (style.isEmpty()) return null
    val keyIndex = style.indexOf("text-align", ignoreCase = true)
    if (keyIndex < 0) return null
    val colon = style.indexOf(':', keyIndex)
    if (colon < 0) return null
    val end = style.indexOf(';', colon).let { if (it < 0) style.length else it }
    return style.substring(colon + 1, end).trim().ifEmpty { null }
}

/**
 * Upgrades a cleartext `http://` image URL to `https://`. AniList content frequently embeds
 * legacy `http` imgur links; with the app's default network policy (no cleartext permission on
 * modern target SDKs) Coil silently fails to load them. Nearly every image host serves the same
 * asset over TLS, so the upgrade lets these images render instead of leaving blank gaps.
 */
internal fun upgradeImageScheme(url: String): String =
    if (url.startsWith("http://", ignoreCase = true)) "https://" + url.substring(7) else url