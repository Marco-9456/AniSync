package com.anisync.android.util

/**
 * Strips HTML tags from a string while preserving newlines for Markdown content.
 *
 * Was: `Regex("<[^>]*>")` strip followed by 5 chained literal replaces. The regex
 * compile + Pattern walk dominated the cost — String.replace(literal, literal) is
 * already a C-intrinsic scan in HotSpot, but Regex.replace builds and walks an NFA.
 *
 * Now: a hand-rolled forward scan removes tags in a single pass without invoking
 * the regex engine. Short-circuits when the input has no '<'. Entity decoding stays
 * as the chained literal replaces — those are JVM intrinsics and cheaper than any
 * StringBuilder rewrite for a 5-entity vocabulary.
 */
fun String.stripHtml(): String {
    if (isEmpty()) return this

    // 1) Tag strip — the original regex tested every char with a generic NFA.
    //    A linear scan is faster: at each '<' jump to the matching '>' and skip.
    val stripped = if (indexOf('<') < 0) this else stripTagsFast(this)

    // 2) Entity decoding — chained literal replaces hit JVM string-search intrinsics.
    //    Cheap to skip entirely when the result has no '&'.
    return if (stripped.indexOf('&') < 0) {
        stripped.trim()
    } else {
        stripped
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
    }
}

private fun stripTagsFast(s: String): String {
    val len = s.length
    val sb = StringBuilder(len)
    var i = 0
    while (i < len) {
        val c = s[i]
        if (c == '<') {
            // Skip ahead to matching '>' (or end of string for malformed input).
            val close = s.indexOf('>', i + 1)
            if (close < 0) {
                // Unterminated tag — drop the rest, matching the regex's greedy behaviour.
                break
            }
            i = close + 1
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}
