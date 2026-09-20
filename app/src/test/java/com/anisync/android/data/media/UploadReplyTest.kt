package com.anisync.android.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bodies here are what the live hosts actually answered on 2026-09-20, not invented shapes.
 * Every one of them used to reach the user as "Catbox rejected this file", because the old mapper
 * only asked whether the message mentioned 412.
 */
class UploadReplyTest {

    @Test
    fun `a url body is the upload result`() {
        val reply = readUploadReply("Catbox", 200, isSuccessful = true, body = CATBOX_URL + "\n")

        assertEquals(CATBOX_URL, reply.getOrNull())
    }

    @Test
    fun `a stale user hash points at the setting, not the file`() {
        val reason = failureOf(readUploadReply("Catbox", 412, false, "Not signed in!"))

        assertEquals(UploadFailure.BadUserHash, reason)
    }

    @Test
    fun `an empty multipart says the file never arrived`() {
        val reason = failureOf(readUploadReply("Catbox", 412, false, "No file!"))

        assertEquals(UploadFailure.NoFileReceived, reason)
    }

    @Test
    fun `a content block stays a rejection`() {
        val reason = failureOf(readUploadReply("Catbox", 412, false, "Invalid uploader"))

        assertEquals(UploadFailure.FileRejected, reason)
    }

    @Test
    fun `a host outage reads as the host being down`() {
        val reason = failureOf(readUploadReply("Litterbox", 500, false, LITTERBOX_OUTAGE_PAGE))

        assertEquals(UploadFailure.HostDown(500), reason)
    }

    /** A proxy can answer 200 with an error page; the body decides, not the code. */
    @Test
    fun `an error page served as 200 is still the host being down`() {
        val reason = failureOf(readUploadReply("Litterbox", 200, true, LITTERBOX_OUTAGE_PAGE))

        assertEquals(UploadFailure.HostDown(200), reason)
    }

    @Test
    fun `an unknown reply keeps its text for the report`() {
        val reason = failureOf(readUploadReply("Custom host", 418, false, "teapot"))

        assertEquals(UploadFailure.Unexpected(418, "teapot"), reason)
    }

    /** Nothing a host says should be able to push a wall of markup into the UI. */
    @Test
    fun `the message never carries a page of html`() {
        val error = readUploadReply("Litterbox", 500, false, LITTERBOX_OUTAGE_PAGE).exceptionOrNull()

        val message = error?.message.orEmpty()
        assertFalse("html leaked into the message: $message", message.contains("<!doctype"))
        assertTrue("message is too long to read: ${message.length}", message.length < 200)
    }

    private fun failureOf(reply: Result<String>): UploadFailure {
        val error = reply.exceptionOrNull()
        assertTrue("expected a failure, got ${reply.getOrNull()}", error is MediaUploadException)
        return (error as MediaUploadException).reason
    }

    private companion object {
        const val CATBOX_URL = "https://files.catbox.moe/v7ve7s.png"

        /** Trimmed from the real 68 KB page litterbox.catbox.moe served during the outage. */
        val LITTERBOX_OUTAGE_PAGE = """
            <!doctype html><html lang="en"><meta charset="UTF-8">
            <title>500 | Internal Server Error</title>
            <meta content="The request was not completed." name="description">
        """.trimIndent()
    }
}
