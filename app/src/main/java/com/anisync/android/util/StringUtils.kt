package com.anisync.android.util

/**
 * Strips HTML tags from a string and decodes a small set of HTML entities.
 */
fun String.stripHtml(): String {
    if (isEmpty()) return this

    val stripped = if (indexOf('<') < 0) this else stripTagsFast(this)

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
            val close = s.indexOf('>', i + 1)
            if (close < 0) break
            i = close + 1
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}
