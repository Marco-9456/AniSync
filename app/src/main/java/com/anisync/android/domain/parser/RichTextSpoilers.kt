package com.anisync.android.domain.parser

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import java.util.Collections
import java.util.IdentityHashMap

/**
 * AniList's spoiler rules as the website applies them since 4/11/2026: it pairs `~!` and `!~` in the
 * markdown itself (`markdownHtml()` in its main bundle), while the API's `asHtml` still emits the old
 * span, keeps a `{label}` as text and often leaves both markers raw once a spoiler crosses paragraphs.
 *
 * So [markers] turns AniList's spans back into markers, pairs every marker the way the website does
 * and swaps each kept pair for two empty marker elements. [wrap] then moves the DOM between each
 * pair into one spoiler element, splitting whatever it crosses, so nothing between the markers can
 * end up outside the spoiler.
 */
internal object RichTextSpoilers {
    const val BLOCK_TAG = "spoiler"
    const val INLINE_TAG = "spoiler-inline"
    const val LABEL_ATTR = "data-label"

    private const val OPEN_TAG = "spoiler-open"
    private const val CLOSE_TAG = "spoiler-close"
    private const val INDEX_ATTR = "data-i"
    private const val INLINE_ATTR = "data-inline"

    // Deeper spoilers stay literal text on the website. They still sit inside a third level.
    private const val MAX_DEPTH = 3

    private val SERVER_OPEN = Regex("""<span\s+class=['"]markdown_spoiler['"]\s*>\s*<span\s*>""")
    private val SPAN_TAG = Regex("""<(/?)span\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val LABEL = Regex("""(?:\s|<br\s*/?>)*\{([^{}<>\n]+)\}(?:\s|<br\s*/?>)*""", RegexOption.IGNORE_CASE)
    private val MEDIA_TAG = Regex("""<\s*(?:img|video|iframe|source|youtube)\b""", RegexOption.IGNORE_CASE)

    private val BLOCK_CONTAINERS = setOf(
        "p", "div", "li", "ul", "ol", "blockquote", "center", "pre", "dd", "dt",
        "h1", "h2", "h3", "h4", "h5", "h6", "table", "thead", "tbody", "tfoot", "tr", "td", "th",
        "body", BLOCK_TAG
    )
    private val LIST_TAGS = setOf("ul", "ol")
    private val PRUNABLE = setOf(
        "p", "div", "li", "ul", "ol", "blockquote", "center", "h1", "h2", "h3", "h4", "h5", "h6",
        "span", "strong", "b", "em", "i", "u", "del", "s", "strike", "a", "code", "sub", "sup"
    )
    // Markers count as content so a wrap never prunes the element holding the next pair's marker.
    private const val CONTENT_SELECTOR =
        "img, video, iframe, source, youtube, hr, table, div.youtube, " +
            "$BLOCK_TAG, $INLINE_TAG, $OPEN_TAG, $CLOSE_TAG"

    private class Span(val start: Int, val end: Int) {
        var depth = 0
    }

    private class Edit(val start: Int, val end: Int, val replacement: String)

    /** Replaces every spoiler AniList would draw with a pair of marker elements for [wrap]. */
    fun markers(html: String): String {
        if (!html.contains("~!") && !html.contains("markdown_spoiler")) return html
        val text = revertServerSpans(html)
        val spans = pair(text)
        if (spans.isEmpty()) return text

        val boundaries = spans.flatMap { listOf(it.start, it.end - 2) }.toSortedSet()
        val edits = ArrayList<Edit>(spans.size * 3)
        spans.forEachIndexed { index, span ->
            val contentStart = span.start + 2
            val contentEnd = span.end - 2
            val content = text.substring(contentStart, contentEnd)
            val inline = !content.contains('\n') &&
                !(blankToLineEdge(text, span.start - 1, -1) && blankToLineEdge(text, span.end, 1))

            val labelMatch = LABEL.matchAt(text, contentStart)
                ?.takeIf { it.range.last < contentEnd }
                ?.takeIf { match -> boundaries.subSet(contentStart, match.range.last + 1).isEmpty() }
                ?.takeIf { match -> decode(match.groupValues[1]).none { it in LABEL_FORBIDDEN } }
            val label = labelMatch?.let { decode(it.groupValues[1]).trim() }

            val open = buildString {
                append('<').append(OPEN_TAG)
                append(" $INDEX_ATTR=\"").append(index).append('"')
                append(" $INLINE_ATTR=\"").append(inline).append('"')
                if (!label.isNullOrEmpty()) append(" $LABEL_ATTR=\"").append(escapeAttribute(label)).append('"')
                append("></").append(OPEN_TAG).append('>')
            }
            edits.add(Edit(span.start, contentStart, open))
            if (labelMatch != null) edits.add(Edit(contentStart, labelMatch.range.last + 1, ""))
            edits.add(Edit(contentEnd, span.end, "<$CLOSE_TAG $INDEX_ATTR=\"$index\"></$CLOSE_TAG>"))
        }
        return applyEdits(text, edits)
    }

    /**
     * Removes every spoiler from markdown or AniList HTML along with what it hides, the way the
     * website's own plain text helper does before it shows a post as one line.
     */
    fun stripSpoilers(source: String): String {
        if (!source.contains("~!") && !source.contains("markdown_spoiler")) return source
        val text = revertServerSpans(source)
        val spans = pair(text)
        if (spans.isEmpty()) return text
        val sb = StringBuilder(text.length)
        var last = 0
        for (span in spans.sortedBy { it.start }) {
            if (span.start < last) continue
            sb.append(text, last, span.start)
            last = span.end
        }
        sb.append(text, last, text.length)
        return sb.toString()
    }

    /** Moves the content between each marker pair from [markers] into a spoiler element. */
    fun wrap(root: Element) {
        val opens = root.getElementsByTag(OPEN_TAG)
        if (opens.isEmpty()) {
            root.getElementsByTag(CLOSE_TAG).remove()
            return
        }
        val closes = root.getElementsByTag(CLOSE_TAG).associateBy { it.attr(INDEX_ATTR) }
        val order = IdentityHashMap<Element, Int>()
        root.getAllElements().forEachIndexed { i, element -> order[element] = i }

        // Inner spoilers open later than the ones around them, so the last opener goes first and an
        // outer wrap later carries the finished inner one along.
        for (open in opens.reversed()) {
            val close = closes[open.attr(INDEX_ATTR)]
            if (close == null || close.parent() == null) {
                open.remove()
                continue
            }
            val (first, last) = if ((order[open] ?: 0) <= (order[close] ?: 0)) open to close else close to open
            val label = open.attr(LABEL_ATTR).takeIf { it.isNotBlank() }
            val container = blockContainer(first)
            val inline = open.attr(INLINE_ATTR) == "true" && blockContainer(last) === container
            val wrapper = Element(if (inline) INLINE_TAG else BLOCK_TAG)
            if (label != null) wrapper.attr(LABEL_ATTR, label)

            val touched = ArrayList<Element>()
            if (inline || last.isInside(container)) {
                wrapAtCommonAncestor(first, last, wrapper, touched)
            } else {
                wrapFromOpener(first, last, container, wrapper, touched)
            }
            first.parent()?.let(touched::add)
            last.parent()?.let(touched::add)
            first.remove()
            last.remove()
            touched.forEach(::pruneIfEmpty)
        }
        root.getElementsByTag(CLOSE_TAG).remove()
    }

    private fun revertServerSpans(html: String): String {
        if (!html.contains("markdown_spoiler")) return html
        val opens = SERVER_OPEN.findAll(html).toList()
        if (opens.isEmpty()) return html
        val edits = ArrayList<Edit>(opens.size * 2)
        val taken = HashSet<Int>()
        for (open in opens) {
            edits.add(Edit(open.range.first, open.range.last + 1, "~!"))
            val close = serverClose(html, open.range.last + 1, taken)
            if (close != null) {
                taken.add(close.first)
                edits.add(Edit(close.first, close.last + 1, "!~"))
            } else {
                // AniList always closes its spans. Hiding the rest beats showing it if one ever doesn't.
                edits.add(Edit(html.length, html.length, "!~"))
            }
        }
        return applyEdits(html, edits)
    }

    /** The `</span></span>` that closes the two spans an AniList opener opened, counted by depth. */
    private fun serverClose(html: String, from: Int, taken: Set<Int>): IntRange? {
        var depth = 2
        var pairStart = -1
        for (tag in SPAN_TAG.findAll(html, from)) {
            if (tag.groupValues[1].isEmpty()) {
                depth++
                continue
            }
            depth--
            if (depth == 1) pairStart = tag.range.first
            if (depth == 0) {
                return if (pairStart >= 0 && pairStart !in taken) pairStart..tag.range.last else null
            }
        }
        return null
    }

    /**
     * The website's pairing: a closer takes the nearest open opener, a closer with nothing open is
     * skipped one character at a time, and pairs nested deeper than [MAX_DEPTH] stay text. Tags are
     * skipped so a marker inside an attribute never counts.
     */
    private fun pair(text: String): List<Span> {
        val opens = ArrayList<Int>()
        val spans = ArrayList<Span>()
        var i = 0
        val last = text.length - 1
        while (i < last) {
            val c = text[i]
            if (c == '<' && isTagStart(text, i)) {
                val close = text.indexOf('>', i + 1)
                if (close > 0) {
                    i = close + 1
                    continue
                }
            }
            if (c == '~' && text[i + 1] == '!') {
                opens.add(i)
                i += 2
            } else if (c == '!' && text[i + 1] == '~') {
                if (opens.isNotEmpty()) {
                    spans.add(Span(opens.removeAt(opens.lastIndex), i + 2))
                    i += 2
                } else {
                    i += 1
                }
            } else {
                i++
            }
        }
        if (spans.isEmpty()) return spans
        spans.sortWith(compareBy<Span> { it.start }.thenByDescending { it.end })
        val stack = ArrayList<Span>()
        for (span in spans) {
            while (stack.isNotEmpty() && stack.last().end < span.end) stack.removeAt(stack.lastIndex)
            span.depth = stack.size + 1
            stack.add(span)
        }
        return spans.filter { it.depth <= MAX_DEPTH }
    }

    /**
     * True when only whitespace sits between [from] and the line's edge in [step]'s direction. Tags
     * read as nothing, except media, which the website sees as text on the line.
     */
    private fun blankToLineEdge(text: String, from: Int, step: Int): Boolean {
        var i = from
        while (i in text.indices && text[i] != '\n') {
            val c = text[i]
            if (step > 0 && c == '<' && isTagStart(text, i)) {
                val close = text.indexOf('>', i + 1)
                if (close > 0) {
                    if (MEDIA_TAG.matchesAt(text, i)) return false
                    i = close + 1
                    continue
                }
            }
            if (step < 0 && c == '>') {
                val lineStart = text.lastIndexOf('\n', i) + 1
                val open = text.lastIndexOf('<', i)
                if (open >= lineStart && isTagStart(text, open)) {
                    if (MEDIA_TAG.matchesAt(text, open)) return false
                    i = open - 1
                    continue
                }
            }
            if (!c.isWhitespace()) return false
            i += step
        }
        return true
    }

    private fun isTagStart(text: String, i: Int): Boolean {
        val next = text.getOrNull(i + 1) ?: return false
        return next.isLetter() || next == '/' || next == '!'
    }

    private fun applyEdits(text: String, edits: List<Edit>): String {
        if (edits.isEmpty()) return text
        val sorted = edits.sortedWith(compareBy<Edit> { it.start }.thenBy { it.end })
        val sb = StringBuilder(text.length + sorted.size * 48)
        var last = 0
        for (edit in sorted) {
            if (edit.start < last) continue
            sb.append(text, last, edit.start)
            sb.append(edit.replacement)
            last = edit.end
        }
        sb.append(text, last, text.length)
        return sb.toString()
    }

    // asHtml escapes what the writer typed, so a label is checked for these once decoded.
    private const val LABEL_FORBIDDEN = "{}<>"

    private fun decode(value: String): String = Parser.unescapeEntities(value, false)

    private fun escapeAttribute(value: String): String =
        value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

    /** Wraps everything from [first] to [last] at their lowest common ancestor. */
    private fun wrapAtCommonAncestor(first: Element, last: Element, wrapper: Element, touched: MutableList<Element>) {
        val ancestor = commonAncestor(first, last) ?: return
        val top = liftStart(first, ancestor, touched)
        val bottom = liftEnd(last, ancestor, touched)
        top.before(wrapper)
        moveRun(top, bottom, wrapper)
    }

    /**
     * A block spoiler that leaves the block its opener is in. The website keeps such a spoiler where
     * the opener's line is (inside its list item or quote) and lets it take in everything up to the
     * closer, so the wrapper goes in at the opener and the rest is moved into it.
     */
    private fun wrapFromOpener(
        first: Element,
        last: Element,
        container: Element,
        wrapper: Element,
        touched: MutableList<Element>
    ) {
        val ancestor = commonAncestor(first, last) ?: return
        val top = liftStart(first, container, touched)
        top.before(wrapper)
        moveRun(top, null, wrapper)

        var level: Element = container
        while (true) {
            val parent = level.parent() ?: return
            if (parent === ancestor) break
            val followers = generateSequence(level.nextSibling()) { it.nextSibling() }.toList()
            if (followers.any { !it.isBlankText() }) {
                if (parent.normalName() in LIST_TAGS) {
                    val list = parent.shallowClone()
                    followers.forEach(list::appendChild)
                    wrapper.appendChild(list)
                } else {
                    followers.forEach(wrapper::appendChild)
                }
            }
            level = parent
        }
        val bottom = liftEnd(last, ancestor, touched)
        level.nextSibling()?.let { moveRun(it, bottom, wrapper) }
    }

    /** Moves [from] and each sibling after it into [into], stopping after [through] when given. */
    private fun moveRun(from: Node, through: Node?, into: Element) {
        val run = ArrayList<Node>()
        var node: Node? = from
        while (node != null) {
            run.add(node)
            if (node === through) break
            node = node.nextSibling()
        }
        run.forEach(into::appendChild)
    }

    /**
     * Splits [node]'s ancestors below [ancestor] so the returned child of [ancestor] starts at
     * [node]. What came before [node] stays behind in the originals.
     */
    private fun liftStart(node: Node, ancestor: Element, touched: MutableList<Element>): Node {
        var current = node
        while (true) {
            val parent = current.parent() as? Element ?: return current
            if (parent === ancestor) return current
            val clone = parent.shallowClone()
            generateSequence(current) { it.nextSibling() }.toList().forEach(clone::appendChild)
            parent.after(clone)
            touched.add(parent)
            touched.add(clone)
            current = clone
        }
    }

    /** The mirror of [liftStart]: the returned child of [ancestor] ends at [node]. */
    private fun liftEnd(node: Node, ancestor: Element, touched: MutableList<Element>): Node {
        var current = node
        while (true) {
            val parent = current.parent() as? Element ?: return current
            if (parent === ancestor) return current
            val clone = parent.shallowClone()
            val before = parent.childNodes().takeWhile { it !== current }
            (before + current).forEach(clone::appendChild)
            parent.before(clone)
            touched.add(parent)
            touched.add(clone)
            current = clone
        }
    }

    private fun blockContainer(node: Element): Element {
        var current: Element? = node.parent()
        var outermost: Element = node
        while (current != null) {
            if (current.normalName() in BLOCK_CONTAINERS) return current
            outermost = current
            current = current.parent()
        }
        return outermost
    }

    private fun Node.isBlankText(): Boolean = this is TextNode && isBlank

    private fun Node.isInside(ancestor: Element): Boolean {
        var current: Node? = parent()
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent()
        }
        return false
    }

    private fun commonAncestor(a: Node, b: Node): Element? {
        val chain: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())
        var node: Node? = a
        while (node != null) {
            chain.add(node)
            node = node.parent()
        }
        var other: Node? = b.parent()
        while (other != null) {
            if (other in chain) return other as? Element
            other = other.parent()
        }
        return null
    }

    /** Drops an element the wrap left with nothing to show, and any emptied parent above it. */
    private fun pruneIfEmpty(element: Element) {
        var current: Element? = element
        while (current != null && current.parent() != null && current.normalName() in PRUNABLE) {
            if (current.hasText() || current.select(CONTENT_SELECTOR).isNotEmpty()) return
            val parent = current.parent()
            current.remove()
            current = parent
        }
    }
}
