package com.anisync.android.data.media

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.di.MediaUploadModule
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end upload against the real hosts, because that is the only thing that answers the
 * question users actually ask: does attaching an image work today.
 *
 * A host being down is not our bug, so each test first probes the host and reports a skip rather
 * than a failure when it cannot be reached. Read a skip as "could not test", never as "passed".
 */
@RunWith(AndroidJUnit4::class)
class MediaUploadTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val client: OkHttpClient = MediaUploadModule.provideMediaUploadOkHttpClient()
    private val sharedDir = File(context.cacheDir, "shared")
    private val fixtures = mutableListOf<File>()

    @Before
    fun setUp() {
        sharedDir.mkdirs()
    }

    @After
    fun tearDown() {
        fixtures.forEach { it.delete() }
        fixtures.clear()
    }

    @Test
    fun catboxReturnsAUrlForAContentUri() = runBlocking {
        assumeTrue("catbox.moe is down", hostUp(CATBOX_API))
        val uploader = CatboxUploader(context, client).apply { userhash = "" }

        val result = uploader.upload(pngFixture("catbox-probe.png"), "image/png") { _, _ -> }

        val url = result.getOrElse { throw AssertionError("upload failed: ${it.message}", it) }.url
        assertTrue("unexpected url: $url", url.startsWith("https://files.catbox.moe/"))
    }

    @Test
    fun litterboxReturnsAUrlForAContentUri() = runBlocking {
        assumeTrue("litterbox.catbox.moe is down", hostUp(LITTERBOX_API))
        val uploader = LitterboxUploader(context, client).apply { time = "1h" }

        val result = uploader.upload(pngFixture("litterbox-probe.png"), "image/png") { _, _ -> }

        val url = result.getOrElse { throw AssertionError("upload failed: ${it.message}", it) }.url
        assertTrue("unexpected url: $url", url.startsWith("https://litter.catbox.moe/"))
    }

    /** Progress has to reach the total, or the sheet's bar stalls short of done on every upload. */
    @Test
    fun progressRunsToTheTotal() = runBlocking {
        assumeTrue("catbox.moe is down", hostUp(CATBOX_API))
        val uploader = CatboxUploader(context, client)
        var lastUploaded = 0L
        var lastTotal = -1L

        uploader.upload(pngFixture("progress-probe.png"), "image/png") { uploaded, total ->
            lastUploaded = uploaded
            lastTotal = total
        }.getOrThrow()

        assertTrue("never reported a total", lastTotal > 0)
        assertTrue("stopped at $lastUploaded of $lastTotal", lastUploaded == lastTotal)
    }

    /**
     * The failure users are most likely to hit, and the one the app used to blame on the file:
     * a user hash Catbox no longer accepts. It has to arrive as [UploadFailure.BadUserHash] so
     * the sheet can point at the setting that fixes it.
     */
    @Test
    fun aStaleUserHashIsReportedAsSuch() = runBlocking {
        assumeTrue("catbox.moe is down", hostUp(CATBOX_API))
        val uploader = CatboxUploader(context, client).apply { userhash = NOT_A_USER_HASH }

        val error = uploader.upload(pngFixture("userhash-probe.png"), "image/png") { _, _ -> }
            .exceptionOrNull()

        val failure = error as? MediaUploadException
            ?: throw AssertionError("expected a typed failure, got $error")
        assertTrue(
            "catbox reported ${failure.reason} for a bad hash",
            failure.reason == UploadFailure.BadUserHash
        )
    }

    /** A 1x1 PNG behind the app's FileProvider, which is how the picker hands files over. */
    private fun pngFixture(name: String): Uri {
        val file = File(sharedDir, name)
        file.writeBytes(ONE_PIXEL_PNG)
        fixtures += file
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Whether the host can serve its upload endpoint at all. A 4xx here is the API telling us it
     * wants a real POST, which is fine; a 5xx is the outage Litterbox was in on 2026-09-20 and
     * has to read as "could not test", not as a failure of ours.
     */
    private fun hostUp(endpoint: String): Boolean = runCatching {
        client.newCall(Request.Builder().url(endpoint).get().build()).execute()
            .use { it.code < 500 }
    }.getOrDefault(false)

    private companion object {
        const val CATBOX_API = "https://catbox.moe/user/api.php"
        const val LITTERBOX_API = "https://litterbox.catbox.moe/resources/internals/api.php"

        /** Well-formed but not a real key, so Catbox answers "Not signed in!". */
        const val NOT_A_USER_HASH = "0123456789abcdef01234567"

        /** Smallest valid PNG: 1x1, opaque black. */
        val ONE_PIXEL_PNG = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, 0x90.toByte(), 0x77, 0x53,
            0xDE.toByte(), 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x08, 0xD7.toByte(), 0x63, 0xF8.toByte(), 0xCF.toByte(),
            0xC0.toByte(), 0x00, 0x00, 0x03, 0x01, 0x01, 0x00,
            0x18.toByte(), 0xDD.toByte(), 0x8D.toByte(), 0xB0.toByte(),
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )
    }
}
