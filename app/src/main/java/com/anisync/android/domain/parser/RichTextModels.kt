package com.anisync.android.domain.parser

data class ParsedRichText(
    val blocks: List<RichTextBlock>,
    val imageUrls: List<String>,
    val warnings: List<ParseWarning> = emptyList()
)

data class ParseWarning(
    val message: String,
    val location: String? = null
)

enum class RichTextAlignment {
    Start,
    Center,
    End,
    Justify
}

/**
 * Float behaviour for an image, from HTML `align="left|right"` or CSS `float`. Unlike block
 * [RichTextAlignment], a floated image lets the following content wrap alongside it.
 */
enum class RichTextFloat {
    None,
    Start,
    End
}

enum class RichTextTextKind {
    Paragraph,
    Heading1,
    Heading2,
    Heading3,
    Heading4,
    Heading5
}

sealed interface RichTextInline {
    data class Text(val value: String) : RichTextInline
    data object LineBreak : RichTextInline
    data class Bold(val children: List<RichTextInline>) : RichTextInline
    data class Italic(val children: List<RichTextInline>) : RichTextInline
    data class BoldItalic(val children: List<RichTextInline>) : RichTextInline
    data class Strikethrough(val children: List<RichTextInline>) : RichTextInline
    data class Link(val url: String, val children: List<RichTextInline>) : RichTextInline
    data class InlineCode(val code: String) : RichTextInline

    /** A small image that flows within text (e.g. an emoji-sized `img20(...)` inside a heading). */
    data class Image(val url: String, val width: Int?, val height: Int?) : RichTextInline
}

sealed interface RichTextBlock {
    val align: RichTextAlignment

    data class Text(
        val inlines: List<RichTextInline>,
        val kind: RichTextTextKind = RichTextTextKind.Paragraph,
        override val align: RichTextAlignment = RichTextAlignment.Start,
        /** Rendered in a monospace font — set for text inside a `<code>` styling wrapper. */
        val monospace: Boolean = false
    ) : RichTextBlock

    data class InlineGroup(
        val children: List<RichTextBlock>,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock

    data class Image(
        val url: String,
        val width: Int?,
        val height: Int?,
        val isPercent: Boolean,
        val linkUrl: String?,
        override val align: RichTextAlignment = RichTextAlignment.Start,
        /** When not [RichTextFloat.None], following content wraps beside this image. */
        val floatSide: RichTextFloat = RichTextFloat.None
    ) : RichTextBlock

    data class Table(
        val rows: List<TableRow>,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock

    data class ListBlock(
        val items: List<ListItem>,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock

    data class CodeBlock(
        val code: String,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock

    data class Spoiler(
        val children: List<RichTextBlock>,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock

    data class Blockquote(
        val children: List<RichTextBlock>,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock

    data class HorizontalRule(
        val widthPercent: Int? = null,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock

    data class YouTube(
        val videoIdOrUrl: String,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock

    data class Video(
        val url: String,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock

    data class AnilistLink(
        val type: String,
        val id: Int,
        val url: String,
        val slug: String? = null,
        override val align: RichTextAlignment = RichTextAlignment.Start
    ) : RichTextBlock {
        val isUser: Boolean get() = type.equals("user", ignoreCase = true)

        val displayTitle: String
            get() = when {
                // User links are addressed by username (slug), not a numeric id.
                isUser -> slug ?: "User"
                slug != null -> decodeSlug(slug)
                else -> "${type.replaceFirstChar { it.uppercase() }} #$id"
            }

        val previewKey: LinkPreviewKey
            get() = if (isUser) LinkPreviewKey("user", 0, slug)
            else LinkPreviewKey(type.lowercase(), id)
    }
}

data class LinkPreviewKey(
    val type: String,
    val id: Int,
    /** Username for user links, which have no numeric id. */
    val name: String? = null
)

data class TableRow(val cells: List<TableCell>)
data class TableCell(
    val children: List<RichTextBlock>,
    val isHeader: Boolean,
    val align: RichTextAlignment = RichTextAlignment.Start
)
data class ListItem(val children: List<RichTextBlock>, val bullet: String?)

private fun decodeSlug(slug: String): String =
    slug
        .replace("--", "\u0000")
        .replace('-', ' ')
        .replace("\u0000", ": ")
        .trim()
