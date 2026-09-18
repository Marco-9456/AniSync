package com.anisync.android.data.network

import com.anisync.android.GetViewerQuery
import com.anisync.android.data.util.ApiError
import com.anisync.android.data.util.Clock
import com.anisync.android.data.util.safeApiCall
import com.anisync.android.domain.Result
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * The whole chain against a real HTTP server: identity headers, pacing, classification and retry,
 * exactly as they are wired in [com.anisync.android.di.ApolloModule].
 *
 * The unit tests around it prove each piece in isolation. This one proves they are assembled the
 * right way round, which is the part that silently breaks.
 *
 * Note the plain `safeApiCall { apollo.query(...).execute() }` in the failure cases. That is the
 * shape most of the repositories use, and it is deliberately not `dataOrThrow()`: the point being
 * checked is that a failure reaches the caller at all, rather than arriving as a response with a
 * null body that reads as an empty success.
 */
class AniListClientTest {

    /** `android.util.Log` is a throwing stub on the JVM. Nothing here is asserting on log output. */
    @Before
    fun quietNetworkLogs() {
        NetLog.enabled = false
    }


    private val mockServer = MockServer()

    @After
    fun tearDown() {
        mockServer.close()
    }

    private val viewerJson =
        """{"data":{"Viewer":{"__typename":"User","id":1,"name":"n","avatar":null,"unreadNotificationCount":0,"options":null,"mediaListOptions":null,"bannerImage":null}}}"""

    private class FakeSession(private val token: String? = "t") : SessionTokens {
        var expiredCount = 0
        override fun getToken() = token
        override fun onSessionExpired() {
            expiredCount++
        }
    }

    private suspend fun client(
        gate: RateLimitGate,
        auth: SessionTokens,
        retry: RetryPolicy = RetryPolicy(RetryConfig(jitterMs = 0), Random(0)),
    ): ApolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpInterceptor(AniListHttpInterceptor(auth, gate))
        .httpExposeErrorBody(true)
        .addInterceptor(RequestCoalescer(), ApolloInterceptor.InsertionPoint.BeforeNetwork)
        .addInterceptor(
            AniListErrorInterceptor(auth, gate, retry),
            ApolloInterceptor.InsertionPoint.BeforeNetwork,
        )
        .build()

    private fun gate() = RateLimitGate(
        clock = Clock { System.nanoTime() / 1_000_000 },
        monitor = RateLimitMonitor(),
        persistence = RateLimitPersistence.None,
        config = RateLimitConfig(minGapMs = 0, jitterMs = 0),
        random = Random(0),
    )

    @Test
    fun `every request identifies the app to AniList`() = runTest {
        mockServer.enqueueString(viewerJson)
        val auth = FakeSession()
        client(gate(), auth).query(GetViewerQuery()).execute()

        val headers = mockServer.takeRequest().headers.mapKeys { it.key.lowercase() }
        assertEquals(AniListIdentity.USER_AGENT, headers["user-agent"])
        assertEquals(AniListIdentity.REFERER, headers["referer"])
    }

    /**
     * A bad token answers HTTP 400 with the real reason in the body. Reading only the status line,
     * as the previous interceptor did, reported this as a generic "HTTP error 400" and never ended
     * the session it was written to end.
     */
    @Test
    fun `an invalid token is recognised from the body, not the status line`() = runTest {
        mockServer.enqueue(
            MockResponse.Builder()
                .statusCode(400)
                .addHeader("Content-Type", "application/json")
                .body("""{"data":null,"errors":[{"message":"Invalid token","status":400}]}""")
                .build(),
        )
        val auth = FakeSession()
        val apollo = client(gate(), auth)

        val result = safeApiCall { apollo.query(GetViewerQuery()).execute() }

        val error = result as Result.Error
        assertEquals(401, error.code)
        assertTrue("was ${error.exception}", error.exception is ApiError.SessionExpired)
        assertEquals("the signed-out flow must fire exactly once", 1, auth.expiredCount)
    }

    @Test
    fun `a rate limited response is retried once after the wait the server asked for`() = runTest {
        mockServer.enqueue(
            MockResponse.Builder()
                .statusCode(429)
                .addHeader("Content-Type", "application/json")
                .addHeader("Retry-After", "1")
                .addHeader("X-RateLimit-Limit", "30")
                .addHeader("X-RateLimit-Remaining", "0")
                .body("""{"data":null,"errors":[{"message":"Too Many Requests.","status":429}]}""")
                .build(),
        )
        mockServer.enqueueString(viewerJson)

        val auth = FakeSession()
        val response = client(gate(), auth).query(GetViewerQuery()).execute()

        assertNotNull(response.data)
        assertEquals(1, response.data?.Viewer?.id)
    }

    @Test
    fun `a server error surfaces as a typed failure rather than a raw Apollo exception`() = runTest {
        val retryNothing = RetryPolicy(RetryConfig(serverErrorAttempts = 1, jitterMs = 0), Random(0))
        mockServer.enqueue(
            MockResponse.Builder()
                .statusCode(503)
                .addHeader("Content-Type", "text/html")
                .body("<html>Service Unavailable</html>")
                .build(),
        )

        val auth = FakeSession()
        val apollo = client(gate(), auth, retryNothing)
        val result = safeApiCall { apollo.query(GetViewerQuery()).execute() }

        val error = result as Result.Error
        assertEquals(503, error.code)
        assertTrue("was ${error.exception}", error.exception is ApiError.ServerError)
    }

    @Test
    fun `the gate learns the budget from the response headers`() = runTest {
        val monitor = RateLimitMonitor()
        val gate = RateLimitGate(
            clock = Clock { System.nanoTime() / 1_000_000 },
            monitor = monitor,
            persistence = RateLimitPersistence.None,
            config = RateLimitConfig(minGapMs = 0, jitterMs = 0),
            random = Random(0),
        )
        mockServer.enqueue(
            MockResponse.Builder()
                .statusCode(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-RateLimit-Limit", "30")
                .addHeader("X-RateLimit-Remaining", "17")
                .body(viewerJson)
                .build(),
        )

        val auth = FakeSession()
        client(gate, auth).query(GetViewerQuery()).execute()

        assertEquals(30, monitor.stats.value.limit)
        assertEquals(17, monitor.stats.value.remaining)
    }
}
