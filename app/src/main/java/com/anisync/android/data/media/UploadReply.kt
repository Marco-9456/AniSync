package com.anisync.android.data.media

import java.io.IOException

/**
 * Why an upload did not produce a URL. The hosts answer failures with an HTTP code and a short
 * line of text, and those lines mean very different things to the person holding the phone: one
 * is fixed by clearing a setting, one by waiting, one by picking a different file. Collapsing
 * them into a single "rejected" message is what made a bad Catbox user hash look like a broken
 * feature, with nothing on screen pointing at the setting that actually caused it.
 */
sealed interface UploadFailure {
    /** 5xx, or a web page where a URL belongs: the host itself is not working. */
    data class HostDown(val code: Int) : UploadFailure

    /** Catbox turned down the account key, not the file. Uploading anonymously still works. */
    data object BadUserHash : UploadFailure

    /** The request arrived with no file attached, so nothing was stored. */
    data object NoFileReceived : UploadFailure

    /** The host refused this particular content, usually by hash. */
    data object FileRejected : UploadFailure

    /** Anything the hosts have not shown us yet; [detail] is safe to print. */
    data class Unexpected(val code: Int, val detail: String) : UploadFailure
}

/** Carries [reason] so callers can phrase the failure instead of parsing its message. */
class MediaUploadException(
    val host: String,
    val reason: UploadFailure,
    message: String
) : IOException(message)

/**
 * Turns a host's reply into the uploaded URL, or a [MediaUploadException] naming what went wrong.
 *
 * Observed against the live APIs on 2026-09-20: Catbox answers `412` with a bare reason line
 * ("Not signed in!" for a stale user hash, "No file!" when the multipart carried nothing), and a
 * Litterbox outage answers `500` with a full HTML error page. The page check runs first, because
 * an HTML body means the request never reached the upload handler whatever the code says.
 */
fun readUploadReply(host: String, code: Int, isSuccessful: Boolean, body: String): Result<String> {
    val text = body.trim()

    if (isSuccessful && text.startsWith("http", ignoreCase = true)) {
        return Result.success(text)
    }

    val reason = when {
        code >= 500 || text.looksLikeWebPage() -> UploadFailure.HostDown(code)
        text.contains("not signed in", ignoreCase = true) -> UploadFailure.BadUserHash
        text.contains("no file", ignoreCase = true) -> UploadFailure.NoFileReceived
        code == 412 || text.contains("invalid uploader", ignoreCase = true) ->
            UploadFailure.FileRejected
        else -> UploadFailure.Unexpected(code, text.summarise())
    }

    // An error page has nothing worth quoting, and quoting it anyway is how a wall of markup
    // ended up in front of the user during the Litterbox outage.
    val detail = when (reason) {
        is UploadFailure.HostDown -> "the host returned an error page"
        else -> text.summarise()
    }
    return Result.failure(
        MediaUploadException(host, reason, "$host upload failed ($code): $detail")
    )
}

/** A host that answers with markup answered with an error page, not an upload result. */
private fun String.looksLikeWebPage(): Boolean =
    startsWith("<!doctype", ignoreCase = true) || startsWith("<html", ignoreCase = true)

/** Keeps stray markup out of anything a person reads, however the host phrased it. */
private fun String.summarise(): String {
    val plain = replace(TAG, " ").replace(WHITESPACE, " ").trim()
    return if (plain.length <= MAX_DETAIL_CHARS) plain else plain.take(MAX_DETAIL_CHARS) + "…"
}

private val TAG = Regex("<[^>]*>")
private val WHITESPACE = Regex("\\s+")
private const val MAX_DETAIL_CHARS = 120
