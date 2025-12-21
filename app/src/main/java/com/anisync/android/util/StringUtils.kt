package com.anisync.android.util

/**
 * Strips HTML tags from a string while preserving newlines for Markdown content.
 * Replaces common HTML entities.
 */
fun String.stripHtml(): String {
    return this.replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .trim()
}
