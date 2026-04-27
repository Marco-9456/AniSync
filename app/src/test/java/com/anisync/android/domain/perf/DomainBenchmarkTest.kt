package com.anisync.android.domain.perf

import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.formatScore
import com.anisync.android.domain.parser.RichTextNormalizer
import com.anisync.android.domain.parser.RichTextParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Micro-benchmarks for the domain-layer parser hot paths.
 *
 * What's exercised here:
 *   - RichTextNormalizer.normalize(): a representative AniList description that touches
 *     every normalize stage (HTML-entity escapes, mangled markdown, center blocks,
 *     spoiler markdown, blockquotes). Compared against an inline copy of the legacy
 *     implementation so regex-recompile + chained-replace overhead shows up clearly.
 *   - Setext underline preprocessing: O(n²) split/removeAt vs single-pass StringBuilder.
 *   - ScoreFormatter.formatOneDecimal: legacy `String.format("%.1f", x)` vs the manual
 *     integer-rounding fast path.
 *   - End-to-end RichTextParser.parse() on a non-trivial body to confirm no regression.
 */
class DomainBenchmarkTest {

    private val warmup = 200
    private val iterations = 2_000

    // -----------------------------------------------------------------
    // Realistic AniList-flavoured description that flexes every pass:
    //   - HTML entity-encoded parens (&amp;rpar;)
    //   - Mangled markdown link with <em>/<strong> inside the URL
    //   - Linked image
    //   - <center> tag
    //   - Markdown center block (~~~center ... ~~~)
    //   - Markdown spoiler (~!...!~)
    //   - <span class="markdown_spoiler"> block
    //   - YouTube-class div
    //   - Markdown blockquotes (>) and code fences
    // -----------------------------------------------------------------
    private val sampleHtml = """
        <p>Hello &amp;rpar; check this <a href="https://x.com">link</a>.</p>
        [Visit <em>my</em> site](https://example.com/wiki/Kotlin&amp;rpar;)
        [<img src='https://cdn.example.com/icon.png' width='16'> Instagram](https://instagram.com/anilist)
        <center>Centered HTML</center>
        ~~~center
        Centered markdown line
        with multiple lines
        ~~~
        ~!hidden spoiler text!~
        <span class='markdown_spoiler'><span>nested span spoiler</span></span>
        <div class='youtube' id='dQw4w9WgXcQ'>YT placeholder</div>
        > quote line one
        >> nested quote
        ```
        val x = 1
        ```
        Plain trailing line with bold **emphasis** and italic _accent_.
    """.trimIndent().repeat(2)

    @Test
    fun benchmark_richTextNormalizer_legacy_vs_optimized() {
        val newOut = RichTextNormalizer.normalize(sampleHtml)
        val legacyOut = legacyNormalize(sampleHtml)
        // Same output expected — semantics preserved.
        assertEquals(legacyOut, newOut)

        repeat(warmup) {
            RichTextNormalizer.normalize(sampleHtml)
            legacyNormalize(sampleHtml)
        }

        val newNs = measureNanos {
            repeat(iterations) { RichTextNormalizer.normalize(sampleHtml) }
        }
        val legacyNs = measureNanos {
            repeat(iterations) { legacyNormalize(sampleHtml) }
        }
        report("RichTextNormalizer.normalize", legacyNs, newNs)
        assertTrue("expected new normalizer faster, legacy=$legacyNs new=$newNs", newNs < legacyNs)
    }

    @Test
    fun benchmark_setextPreprocess_split_vs_singlePass() {
        // Build a body with many setext-style headings interleaved with normal text.
        val builder = StringBuilder()
        repeat(80) {
            builder.append("Heading number $it\n")
            builder.append("=".repeat(20)).append('\n')
            builder.append("body line $it\n")
            builder.append("Another header $it\n")
            builder.append("-".repeat(15)).append('\n')
        }
        val body = builder.toString()

        val legacy = legacySetextPreprocess(body)
        val optimized = optimizedSetextPreprocess(body)
        // The optimized version should produce the same content (whitespace canonicalized).
        assertEquals(legacy.trim(), optimized.trim())

        repeat(warmup) {
            legacySetextPreprocess(body)
            optimizedSetextPreprocess(body)
        }

        val legacyNs = measureNanos { repeat(iterations) { legacySetextPreprocess(body) } }
        val optimizedNs = measureNanos { repeat(iterations) { optimizedSetextPreprocess(body) } }
        report("setext preprocess", legacyNs, optimizedNs)
        assertTrue("expected single-pass faster, legacy=$legacyNs new=$optimizedNs", optimizedNs < legacyNs)
    }

    @Test
    fun benchmark_scoreFormatter_legacyFormat_vs_manual() {
        // Same answer for representative scores.
        for (s in listOf(7.5, 8.7, 0.4, 9.99, 6.05)) {
            val legacyVal = String.format(java.util.Locale.US, "%.1f", s)
            val newVal = formatScore(s, ScoreFormat.POINT_10_DECIMAL)
            assertEquals(legacyVal, newVal)
        }

        val scores = doubleArrayOf(7.5, 8.7, 0.4, 9.99, 6.05, 1.2, 3.3, 4.6, 5.5)

        repeat(warmup) {
            for (s in scores) {
                formatScore(s, ScoreFormat.POINT_10_DECIMAL)
                String.format(java.util.Locale.US, "%.1f", s)
            }
        }

        val newNs = measureNanos {
            repeat(iterations * 5) {
                for (s in scores) formatScore(s, ScoreFormat.POINT_10_DECIMAL)
            }
        }
        val legacyNs = measureNanos {
            repeat(iterations * 5) {
                for (s in scores) String.format(java.util.Locale.US, "%.1f", s)
            }
        }
        report("score format (1 decimal)", legacyNs, newNs)
        assertTrue("expected manual format faster, legacy=$legacyNs new=$newNs", newNs < legacyNs)
    }

    @Test
    fun benchmark_richTextParser_endToEnd() = runBlocking {
        // Sanity: parser still produces blocks for the sample.
        val parsed = RichTextParser.parse(sampleHtml)
        assertTrue("parser produced no blocks", parsed.blocks.isNotEmpty())

        repeat(warmup / 4) { RichTextParser.parse(sampleHtml) }

        val ns = measureNanos {
            repeat(iterations / 4) { RichTextParser.parse(sampleHtml) }
        }
        val perCallMicros = ns / 1_000.0 / (iterations / 4)
        println("[bench] RichTextParser.parse end-to-end  ${"%.2f".format(perCallMicros)}µs/call  total=${ns / 1_000_000.0}ms")
    }

    // ---------- Legacy (pre-refactor) implementations preserved here so we can ----------
    // ---------- compare head-to-head without resurrecting the old code in main.  ----------

    private fun legacyNormalize(html: String): String {
        var processed = html.replace("\r", "")
        processed = legacyConvertMixedMarkdownLinksToHtml(processed)
        processed = legacyFixMangledMarkdownLinks(processed)
        processed = legacyDecodeAniListEscapedParenthesis(processed)
        processed = legacyConvertLinkedImages(processed)
        processed = legacyConvertMarkdownSpoilerSpans(processed)
        processed = legacyReplaceCenterTags(processed)
        processed = legacyReplaceCenterMarkdownBlocks(processed)
        processed = legacyReplaceMarkdownSpoilers(processed)
        processed = legacyPreserveYoutubeDivs(processed)
        processed = legacyNormalizeMarkdownBlockquotes(processed)
        return processed
    }

    private fun legacyDecodeAniListEscapedParenthesis(text: String): String =
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

    private val legacyLinkedImg = Regex("""\[\s*(<img\s[^>]*>)\s*]\(([^)]+)\)""")
    private fun legacyConvertLinkedImages(text: String): String =
        text.replace(legacyLinkedImg) { m -> "<a href=\"${m.groupValues[2]}\">${m.groupValues[1]}</a>" }

    private fun legacyReplaceCenterTags(text: String): String =
        text.replace("<center>", "<div align=\"center\">").replace("</center>", "</div>")

    private fun legacyReplaceCenterMarkdownBlocks(text: String): String {
        val regex = Regex("""~~~(?:center)?(.*?)~~~""", RegexOption.DOT_MATCHES_ALL)
        return text.replace(regex) { m -> "<div align=\"center\">${m.groupValues[1]}</div>" }
    }

    private fun legacyReplaceMarkdownSpoilers(text: String): String {
        val regex = Regex("""~!(.*?)!~""", RegexOption.DOT_MATCHES_ALL)
        return text.replace(regex) { m -> "<spoiler>${m.groupValues[1]}</spoiler>" }
    }

    private fun legacyPreserveYoutubeDivs(text: String): String {
        val regex = Regex(
            """<div([^>]*)class=['\"]youtube['\"]([^>]*)>(.*?)</div>""",
            RegexOption.DOT_MATCHES_ALL
        )
        return text.replace(regex) { m ->
            "<youtube${m.groupValues[1]}class=\"youtube\"${m.groupValues[2]}>${m.groupValues[3]}</youtube>"
        }
    }

    private val legacyMarkdownSpoilerOpenRegex = Regex(
        """<span\s+class=['"]markdown_spoiler['"]\s*>\s*<span\s*>"""
    )

    private fun legacyConvertMarkdownSpoilerSpans(html: String): String {
        val openMatches = legacyMarkdownSpoilerOpenRegex.findAll(html).toList()
        if (openMatches.isEmpty()) return html

        val sb = StringBuilder()
        var lastEnd = 0
        for (match in openMatches) {
            sb.append(html, lastEnd, match.range.first)
            sb.append("<div rel=\"spoiler\">")
            lastEnd = match.range.last + 1
        }
        sb.append(html, lastEnd, html.length)

        var result = sb.toString()
        val closeTag = "</span></span>"
        var remaining = openMatches.size
        while (remaining > 0) {
            val idx = result.indexOf(closeTag)
            if (idx == -1) break
            result = result.substring(0, idx) + "</div>" + result.substring(idx + closeTag.length)
            remaining--
        }
        return result
    }

    private fun legacyConvertMixedMarkdownLinksToHtml(html: String): String {
        // Identical to current production implementation; here only so that legacy
        // pipeline runs the same prior steps as production, for a fair perf comparison.
        val sb = StringBuilder()
        var i = 0
        while (i < html.length) {
            if (html[i] == '[') {
                var closeBracket = -1
                var depth = 1
                var j = i + 1
                while (j < html.length) {
                    if (html[j] == '[') depth++
                    else if (html[j] == ']') { depth--; if (depth == 0) { closeBracket = j; break } }
                    j++
                }
                if (closeBracket != -1 && closeBracket + 1 < html.length && html[closeBracket + 1] == '(') {
                    val closeParen = legacyFindBalancedCloseParen(html, closeBracket + 1)
                    if (closeParen != -1) {
                        val linkText = html.substring(i + 1, closeBracket)
                        val url = html.substring(closeBracket + 2, closeParen)
                        if (linkText.contains("<") && linkText.contains(">")) {
                            sb.append("<a href=\"").append(url).append("\">").append(linkText).append("</a>")
                            i = closeParen + 1; continue
                        }
                    }
                }
            }
            sb.append(html[i]); i++
        }
        return sb.toString()
    }

    private fun legacyFixMangledMarkdownLinks(html: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < html.length) {
            if (html[i] == '[') {
                var closeBracket = -1
                var depth = 1
                var j = i + 1
                while (j < html.length) {
                    if (html[j] == '[') depth++
                    else if (html[j] == ']') { depth--; if (depth == 0) { closeBracket = j; break } }
                    j++
                }
                if (closeBracket != -1 && closeBracket + 1 < html.length && html[closeBracket + 1] == '(') {
                    val closeParen = legacyFindBalancedCloseParen(html, closeBracket + 1)
                    if (closeParen != -1) {
                        val linkText = html.substring(i + 1, closeBracket)
                        val mangled = html.substring(closeBracket + 2, closeParen)
                        if (mangled.contains("<")) {
                            var restored = mangled
                                .replace("<em>", "_").replace("</em>", "_")
                                .replace("<i>", "_").replace("</i>", "_")
                                .replace("<strong>", "**").replace("</strong>", "**")
                                .replace("<b>", "**").replace("</b>", "**")
                            // Legacy: regex compiled per call.
                            restored = restored.replace(Regex("""<[^>]+>"""), "").trim()
                            sb.append("[$linkText]($restored)")
                            i = closeParen + 1; continue
                        }
                    }
                }
            }
            sb.append(html[i]); i++
        }
        return sb.toString()
    }

    private fun legacyFindBalancedCloseParen(text: String, openParenIndex: Int): Int {
        var depth = 1; var i = openParenIndex + 1
        while (i < text.length) {
            when {
                text[i] == '(' -> depth++
                text[i] == ')' -> { depth--; if (depth == 0) return i }
                i + 2 < text.length && text[i] == '%' && text[i + 1] == '2' -> {
                    when (text[i + 2]) {
                        '8' -> { depth++; i += 2 }
                        '9' -> { depth--; if (depth == 0) return i + 2; i += 2 }
                    }
                }
            }
            i++
        }
        return -1
    }

    private fun legacyNormalizeMarkdownBlockquotes(text: String): String {
        // Identical to production; included so the legacy pipeline ends with the same step.
        val lines = text.split("\n"); val out = StringBuilder()
        var depth = 0; var inCode = false
        for (line in lines) {
            val tr = line.trimStart()
            if (!inCode) { if (tr.startsWith("```") || tr.startsWith("<pre")) inCode = true }
            else { if (tr.startsWith("```") || tr.contains("</pre>")) inCode = false }
            if (inCode) {
                while (depth > 0) { out.append("</blockquote>\n"); depth-- }
                out.append(line).append('\n'); continue
            }
            var d = 0; var content = tr
            while (content.startsWith(">")) { d++; content = content.substring(1).trimStart() }
            while (depth < d) { out.append("<blockquote>\n"); depth++ }
            while (depth > d) { out.append("</blockquote>\n"); depth-- }
            out.append(content).append('\n')
        }
        while (depth > 0) { out.append("</blockquote>\n"); depth-- }
        return out.toString()
    }

    // Setext head-to-head: legacy was split + toMutableList + removeAt, optimized is single-pass.

    private fun legacySetextPreprocess(text: String): String {
        if (!text.contains('\n')) return text
        val lines = text.split('\n').toMutableList()
        var i = 0
        while (i < lines.size - 1) {
            val current = lines[i]
            val next = lines[i + 1].trim()
            val isSetext = next.matches(Regex("[=-]{2,}"))
            if (isSetext) {
                val level = if (next.first() == '=') 1 else 2
                val currentTrimmed = current.trimStart()
                if (!currentTrimmed.startsWith("#") && !currentTrimmed.contains('<') && !currentTrimmed.contains('>')) {
                    val prefix = current.takeWhile { it == ' ' || it == '\t' }
                    lines[i] = "$prefix${"#".repeat(level)} ${current.trim()}"
                    lines.removeAt(i + 1)
                    continue
                }
            }
            i++
        }
        return lines.joinToString("\n")
    }

    private val setextRegex = Regex("[=-]{2,}")
    private fun optimizedSetextPreprocess(text: String): String {
        if (!text.contains('\n')) return text
        val len = text.length
        val sb = StringBuilder(len + 4)
        var i = 0
        while (i < len) {
            val nl = text.indexOf('\n', i).let { if (it == -1) len else it }
            val ns0 = nl + 1
            val ne0 = if (ns0 >= len) len else text.indexOf('\n', ns0).let { if (it == -1) len else it }
            var ns = ns0; var ne = ne0
            while (ns < ne && (text[ns] == ' ' || text[ns] == '\t')) ns++
            while (ne > ns && (text[ne - 1] == ' ' || text[ne - 1] == '\t')) ne--
            val firstNext = if (ns < ne) text[ns] else 0.toChar()
            if ((firstNext == '=' || firstNext == '-') && (ne - ns) >= 2 &&
                setextRegex.matches(text.substring(ns, ne))) {
                var cs = i
                while (cs < nl && (text[cs] == ' ' || text[cs] == '\t')) cs++
                val firstCur = if (cs < nl) text[cs] else 0.toChar()
                var hasAngle = false
                run { var k = i; while (k < nl) { val ch = text[k]; if (ch == '<' || ch == '>') { hasAngle = true; break }; k++ } }
                if (firstCur != '#' && !hasAngle) {
                    val level = if (firstNext == '=') 1 else 2
                    sb.append(text, i, cs)
                    sb.append(if (level == 1) "# " else "## ")
                    var trail = nl
                    while (trail > cs && (text[trail - 1] == ' ' || text[trail - 1] == '\t')) trail--
                    sb.append(text, cs, trail)
                    i = if (ne0 < len) ne0 + 1 else ne0
                    if (i < len || ne0 < len) sb.append('\n')
                    continue
                }
            }
            sb.append(text, i, nl); if (nl < len) sb.append('\n'); i = nl + 1
        }
        return sb.toString()
    }

    // ---------- helpers ----------

    private inline fun measureNanos(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return System.nanoTime() - start
    }

    private fun report(label: String, legacyNs: Long, newNs: Long) {
        val speedup = if (newNs == 0L) Double.POSITIVE_INFINITY else legacyNs.toDouble() / newNs
        println("[bench] $label  legacy=${legacyNs / 1_000_000.0}ms  new=${newNs / 1_000_000.0}ms  speedup=${"%.2f".format(speedup)}x")
        assertNotNull(label)
    }
}
