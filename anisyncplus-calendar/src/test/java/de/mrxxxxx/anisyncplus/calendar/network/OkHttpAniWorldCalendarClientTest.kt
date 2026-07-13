package de.mrxxxxx.anisyncplus.calendar.network

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
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
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<html><body><div id=\"seriesContainer\"></div></body></html>")
        )
        val response = client().fetch()
        val request = server.takeRequest()

        assertEquals(200, response.httpStatus)
        assertEquals(instant, response.fetchedAt)
        assertEquals("/animekalender", request.path)
        assertEquals("de-DE,de;q=0.9", request.getHeader("Accept-Language"))
        assertTrue(request.getHeader("User-Agent").orEmpty().startsWith("AniSyncPlus/"))
        assertEquals("text/html,application/xhtml+xml", request.getHeader("Accept"))
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
    fun `non HTML and block pages are rejected`() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("{}"))
        assertThrows(AniWorldClientException.InvalidContentType::class.java) {
            kotlinx.coroutines.test.runTest { client().fetch() }
        }
        server.enqueue(
            MockResponse().setHeader("Content-Type", "text/html").setBody("<title>Just a moment...</title>")
        )
        assertThrows(AniWorldClientException.BlockPage::class.java) {
            kotlinx.coroutines.test.runTest { client().fetch() }
        }
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

    private fun client() = OkHttpAniWorldCalendarClient(
        client = OkHttpClient(),
        sourceUrl = server.url("/animekalender").toString(),
        clock = Clock.fixed(instant, ZoneOffset.UTC)
    )
}
