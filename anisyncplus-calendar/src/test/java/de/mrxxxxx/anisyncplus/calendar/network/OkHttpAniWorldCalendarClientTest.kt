package de.mrxxxxx.anisyncplus.calendar.network

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OkHttpAniWorldCalendarClientTest {
    private lateinit var server: MockWebServer
    private val instant = Instant.parse("2026-07-13T10:00:00Z")

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetch sends bounded calendar-only HTML request`() = runTest {
        val diagnostics = mutableListOf<String>()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<html><body><div id=\"seriesContainer\"></div></body></html>")
        )
        val response = client(diagnostics::add).fetch()
        val request = server.takeRequest()

        assertEquals(200, response.httpStatus)
        assertEquals(instant, response.fetchedAt)
        assertEquals("/animekalender", request.path)
        assertEquals("de-DE,de;q=0.9", request.getHeader("Accept-Language"))
        assertTrue(request.getHeader("User-Agent").orEmpty().startsWith("AniSyncPlus/"))
        assertEquals("text/html,application/xhtml+xml", request.getHeader("Accept"))
        assertTrue(diagnostics.any { "request_start" in it })
        assertTrue(
            diagnostics.any {
                "response_ok status=200" in it && Regex("sha256=[0-9a-f]{64}").containsMatchIn(it)
            }
        )
    }

    @Test
    fun `non success status is typed`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("maintenance"))
        val error = assertThrows(AniWorldClientException.HttpStatus::class.java) {
            kotlinx.coroutines.test.runTest { client().fetch() }
        }
        assertEquals(503, error.status)
    }

    @Test
    fun `non HTML is rejected`() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("{}"))
        assertThrows(AniWorldClientException.InvalidContentType::class.java) {
            kotlinx.coroutines.test.runTest { client().fetch() }
        }
    }

    @Test
    fun `HTML client accepts challenge content for structural parser classification`() = runTest {
        server.enqueue(
            MockResponse().setHeader("Content-Type", "text/html").setBody("<title>Just a moment...</title>")
        )
        assertEquals(200, client().fetch().httpStatus)
    }

    @Test
    fun `benign cloudflare and captcha words in a valid page are accepted`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody(
                    """
                    <html><head>
                      <title>Animekalender</title>
                      <link href="https://cdnjs.cloudflare.com/library.css">
                    </head><body>
                      <p>Captcha is a fictional anime title.</p>
                      <div id="seriesContainer"><section class="calendarList"></section></div>
                    </body></html>
                    """.trimIndent()
                )
        )

        val response = client().fetch()

        assertTrue("cdnjs.cloudflare.com" in response.html)
    }

    @Test
    fun `diagnostics omit URL query values`() = runTest {
        val diagnostics = mutableListOf<String>()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody("<div id=\"seriesContainer\"></div>")
        )
        val client = OkHttpAniWorldCalendarClient(
            client = OkHttpClient(),
            sourceUrl = server.url("/animekalender?token=must-not-be-logged").toString(),
            clock = Clock.fixed(instant, ZoneOffset.UTC),
            diagnosticLogger = diagnostics::add
        )

        client.fetch()

        assertFalse(diagnostics.any { "must-not-be-logged" in it || "token=" in it })
    }

    @Test
    fun `oversized body is rejected`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody("x".repeat(OkHttpAniWorldCalendarClient.MAX_BODY_BYTES + 1))
        )
        assertThrows(AniWorldClientException.BodyTooLarge::class.java) {
            kotlinx.coroutines.test.runTest { client().fetch() }
        }
    }

    private fun client(diagnostics: (String) -> Unit = {}) = OkHttpAniWorldCalendarClient(
        client = OkHttpClient(),
        sourceUrl = server.url("/animekalender").toString(),
        clock = Clock.fixed(instant, ZoneOffset.UTC),
        diagnosticLogger = diagnostics
    )
}
