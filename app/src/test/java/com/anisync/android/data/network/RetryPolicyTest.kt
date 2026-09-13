package com.anisync.android.data.network

import com.anisync.android.data.util.ApiError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RetryPolicyTest {

    private val policy = RetryPolicy(RetryConfig(jitterMs = 0), Random(0))

    private fun decide(
        error: ApiError,
        attempt: Int = 1,
        isMutation: Boolean = false,
        priority: RequestPriority = RequestPriority.Interactive,
        blockedForMs: Long = 0,
    ) = policy.decide(error, attempt, isMutation, priority, blockedForMs)

    @Test
    fun `background work never retries in process`() {
        // WorkManager already retries with its own backoff, off the critical path. Retrying here
        // too would spend requests the user is about to want.
        val errors = listOf(
            ApiError.RateLimited(5),
            ApiError.ServerError(500),
            ApiError.Timeout(),
            ApiError.Offline(),
        )
        errors.forEach {
            assertEquals(
                "retried ${it::class.simpleName}",
                RetryDecision.Stop,
                decide(it, priority = RequestPriority.Background),
            )
        }
    }

    @Test
    fun `a short rate limit is retried once, after the server's own wait`() {
        val first = decide(ApiError.RateLimited(5), attempt = 1, blockedForMs = 5_000)
        assertEquals(RetryDecision.After(5_000), first)

        assertEquals(RetryDecision.Stop, decide(ApiError.RateLimited(5), attempt = 2))
    }

    @Test
    fun `a rate limit longer than a user should wait fails instead of retrying`() {
        assertEquals(RetryDecision.Stop, decide(ApiError.RateLimited(60), blockedForMs = 60_000))
    }

    /** A 429 is refused before it reaches the application, so repeating it cannot double-write. */
    @Test
    fun `a mutation is retried after a rate limit`() {
        val decision = decide(ApiError.RateLimited(3), isMutation = true, blockedForMs = 3_000)
        assertEquals(RetryDecision.After(3_000), decision)
    }

    /** A 5xx may well have applied the write, so repeating it could post twice. */
    @Test
    fun `a mutation is not retried after a server error`() {
        assertEquals(RetryDecision.Stop, decide(ApiError.ServerError(502), isMutation = true))
    }

    @Test
    fun `a mutation is not retried after a timeout`() {
        assertEquals(RetryDecision.Stop, decide(ApiError.Timeout(), isMutation = true))
    }

    @Test
    fun `a query backs off exponentially through its server error attempts`() {
        assertEquals(RetryDecision.After(500), decide(ApiError.ServerError(500), attempt = 1))
        assertEquals(RetryDecision.After(1_000), decide(ApiError.ServerError(500), attempt = 2))
        assertEquals(RetryDecision.Stop, decide(ApiError.ServerError(500), attempt = 3))
    }

    @Test
    fun `a query retries a transport failure once`() {
        assertEquals(RetryDecision.After(500), decide(ApiError.Offline(), attempt = 1))
        assertEquals(RetryDecision.Stop, decide(ApiError.Offline(), attempt = 2))
    }

    @Test
    fun `decisions the server will simply repeat are not retried`() {
        val terminal = listOf(
            ApiError.SessionExpired(),
            ApiError.TokenRejected(),
            ApiError.PermissionDenied("nope"),
            ApiError.ApiDisabled("down"),
            ApiError.Validation(mapOf("score" to listOf("too big"))),
            ApiError.GraphQLError(listOf("bad field"), 400),
            ApiError.Unknown("?"),
        )
        terminal.forEach {
            assertEquals("retried ${it::class.simpleName}", RetryDecision.Stop, decide(it))
        }
    }

    @Test
    fun `a deferral is not retried, it is handed back`() {
        assertEquals(RetryDecision.Stop, decide(ApiError.Deferred(30)))
    }

    @Test
    fun `backoff is jittered so parallel failures do not retry in lockstep`() {
        val jittered = RetryPolicy(RetryConfig(jitterMs = 250), Random(1))
        val delays = (1..20).map {
            val decision = jittered.decide(
                ApiError.ServerError(500), 1, isMutation = false,
                priority = RequestPriority.Interactive, blockedForMs = 0,
            )
            (decision as RetryDecision.After).delayMs
        }

        assertTrue("all delays were identical", delays.distinct().size > 1)
        assertTrue(delays.all { it in 500..749 })
    }

    @Test
    fun `backoff is capped`() {
        val capped = RetryPolicy(
            RetryConfig(serverErrorAttempts = 20, backoffMaxMs = 2_000, jitterMs = 0),
            Random(0),
        )
        val decision = capped.decide(
            ApiError.ServerError(500), attempt = 10, isMutation = false,
            priority = RequestPriority.Interactive, blockedForMs = 0,
        )
        assertEquals(RetryDecision.After(2_000), decision)
    }
}
