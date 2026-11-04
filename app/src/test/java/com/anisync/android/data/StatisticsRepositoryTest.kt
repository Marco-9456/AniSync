package com.anisync.android.data

import com.anisync.android.data.network.AniListErrorInterceptor
import com.anisync.android.data.network.AniListHttpInterceptor
import com.anisync.android.data.network.NetLog
import com.anisync.android.data.network.RateLimitConfig
import com.anisync.android.data.network.RateLimitGate
import com.anisync.android.data.network.RateLimitMonitor
import com.anisync.android.data.network.RateLimitPersistence
import com.anisync.android.data.network.RequestCoalescer
import com.anisync.android.data.network.RetryConfig
import com.anisync.android.data.network.RetryPolicy
import com.anisync.android.data.network.SessionTokens
import com.anisync.android.data.util.ApiError
import com.anisync.android.data.util.Clock
import com.anisync.android.domain.CachePolicy
import com.anisync.android.domain.PeopleList
import com.anisync.android.domain.PeopleSort
import com.anisync.android.domain.Result
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/** Statistics failures have to reach the screen as what they are, a rate limit with its countdown above all. */
class StatisticsRepositoryTest {

    @Before
    fun quietNetworkLogs() {
        NetLog.enabled = false
    }

    private val mockServer = MockServer()

    @After
    fun tearDown() {
        mockServer.close()
    }

    private object Session : SessionTokens {
        override fun getToken() = "t"
        override fun onSessionExpired() = Unit
    }

    private suspend fun repository(): StatisticsRepositoryImpl {
        val gate = RateLimitGate(
            clock = Clock { System.nanoTime() / 1_000_000 },
            monitor = RateLimitMonitor(),
            persistence = RateLimitPersistence.None,
            config = RateLimitConfig(minGapMs = 0, jitterMs = 0),
            random = Random(0),
        )
        val retry = RetryPolicy(RetryConfig(serverErrorAttempts = 1, jitterMs = 0), Random(0))
        val apollo = ApolloClient.Builder()
            .serverUrl(mockServer.url())
            .addHttpInterceptor(AniListHttpInterceptor(Session, gate))
            .httpExposeErrorBody(true)
            .addInterceptor(RequestCoalescer(), ApolloInterceptor.InsertionPoint.BeforeNetwork)
            .addInterceptor(AniListErrorInterceptor(Session, gate, retry), ApolloInterceptor.InsertionPoint.BeforeNetwork)
            .build()
        return StatisticsRepositoryImpl(apollo)
    }

    private fun rateLimited() = MockResponse.Builder()
        .statusCode(429)
        .addHeader("Content-Type", "application/json")
        .addHeader("Retry-After", "1")
        .addHeader("X-RateLimit-Limit", "30")
        .addHeader("X-RateLimit-Remaining", "0")
        .body("""{"data":null,"errors":[{"message":"Too Many Requests.","status":429}]}""")
        .build()

    @Test
    fun `a rate limit keeps its countdown`() = runTest {
        repeat(2) { mockServer.enqueue(rateLimited()) }

        val error = repository().getUserStatistics(1, CachePolicy.NetworkOnly) as Result.Error

        assertTrue("was ${error.exception}", error.exception is ApiError.RateLimited)
        assertEquals(1L, error.countdownSeconds)
    }

    @Test
    fun `a People list keeps the rate limit too`() = runTest {
        repeat(2) { mockServer.enqueue(rateLimited()) }

        val error = repository().getPeopleStatistics(1, PeopleList.ANIME_STAFF, PeopleSort.COUNT) as Result.Error

        assertTrue("was ${error.exception}", error.exception is ApiError.RateLimited)
        assertEquals(1L, error.countdownSeconds)
    }

    @Test
    fun `a server error stays a server error`() = runTest {
        mockServer.enqueue(
            MockResponse.Builder()
                .statusCode(503)
                .addHeader("Content-Type", "text/html")
                .body("<html>Service Unavailable</html>")
                .build(),
        )

        val error = repository().getUserStatistics(1, CachePolicy.NetworkOnly) as Result.Error

        assertEquals(503, error.code)
        assertTrue("was ${error.exception}", error.exception is ApiError.ServerError)
    }
}
