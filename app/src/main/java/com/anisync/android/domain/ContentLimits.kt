package com.anisync.android.domain

/**
 * Server-enforced character bounds per content type.
 *
 * Sourced from AniList GraphQL API constraints (provided by AniList devs ahead of
 * official documentation). Use these to pre-validate user input so we don't burn
 * a network round-trip on rejections.
 *
 * Bounds are inclusive: text is valid iff `length in min..max`.
 */
object ContentLimits {
    val TextActivity = Bounds(min = 5, max = 10_000)
    val MessageActivity = Bounds(min = 2, max = 10_000)
    val ThreadTitle = Bounds(min = 6, max = 120)
    val ThreadBody = Bounds(min = 10, max = 30_000)
    val ThreadComment = Bounds(min = 1, max = 12_000)
    val Reply = Bounds(min = 2, max = 8_000)
    val ListNotes = Bounds(min = 0, max = 6_000)
    val Rewatches = Bounds(min = 0, max = 1_000)

    // AniList review constraints: body min 2600 chars counted as [reviewBodyLength] does,
    // summary 20..120 chars, score 0..100. The body has no documented upper bound, so the cap
    // sits high enough that the editor never rejects a long review.
    val ReviewBody = Bounds(min = 2_600, max = 200_000)
    val ReviewSummary = Bounds(min = 20, max = 120)
    val ReviewScore = Bounds(min = 0, max = 100)

    data class Bounds(val min: Int, val max: Int) {
        fun isValid(length: Int) = length in min..max
    }

    /** AniList counts characters, so an emoji is one even though it takes two UTF-16 units. */
    fun characterCount(text: CharSequence): Int = Character.codePointCount(text, 0, text.length)

    /** [text] cut to at most [max] characters, never through the middle of an emoji. */
    fun cap(text: String, max: Int): String =
        if (characterCount(text) <= max) text else text.substring(0, text.offsetByCodePoints(0, max))

    private val ImageMarkdown = Regex("""!\[[\s\S]*?]\([\s\S]*?\)""")
    private val LinkTarget = Regex("""]\([\s\S]*?\)""")
    private val Embed = Regex("""(img\d*%?|webm|youtube)\([\s\S]*?\)""", RegexOption.IGNORE_CASE)
    private val HtmlTag = Regex("""<[\s\S]*?>""")
    private val MarkdownSymbol = Regex("""[\[\]_>~\-#`*]""")
    private val Invisible = Regex("[\u3164\u200E\u200B\u200C\u200D\uFEFF]")
    private val WhitespaceRun = Regex("""\s{2,}""")

    /**
     * A review body's length the way AniList's review editor measures it: images, embeds, link
     * targets, tags and markdown symbols stripped, invisible characters read as spaces and runs
     * of whitespace collapsed.
     */
    fun reviewBodyLength(body: String): Int = body
        .replace(ImageMarkdown, "")
        .replace(LinkTarget, "")
        .replace(Embed, "")
        .replace(HtmlTag, "")
        .replace(MarkdownSymbol, "")
        .trim()
        .replace(Invisible, " ")
        .replace(WhitespaceRun, " ")
        .trim()
        .let(::characterCount)
}
