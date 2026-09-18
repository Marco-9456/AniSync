package com.anisync.android.data.util

import com.anisync.android.domain.Result
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.DefaultApolloException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SafeApiCallTest {

    @Test
    fun `a successful call is passed through`() = runTest {
        val result = safeApiCall { 42 }
        assertEquals(42, (result as Result.Success).data)
    }

    /**
     * The old blanket `catch (e: Exception)` turned cancellation into a `Result.Error`, so a screen
     * the user had already left reported a failure and the coroutine that owned it never learned it
     * had been cancelled.
     */
    @Test
    fun `cancellation propagates instead of becoming an error`() = runTest {
        val job = async {
            safeApiCall {
                delay(10_000)
                "never"
            }
        }
        job.cancel()

        try {
            job.await()
            fail("cancellation was swallowed")
        } catch (_: CancellationException) {
            // Expected.
        }
    }

    @Test
    fun `a directly thrown CancellationException is not captured`() = runTest {
        try {
            safeApiCall<Unit> { throw CancellationException("stop") }
            fail("cancellation was swallowed")
        } catch (_: CancellationException) {
            // Expected.
        }
    }

    /**
     * ApolloException is sealed, so ApiError can never be one and Apollo always hands ours back
     * wrapped. Reading only the top of the chain reported a rate limit as "no internet connection".
     */
    @Test
    fun `a wrapped ApiError is unwrapped, not reported as a network failure`() = runTest {
        val result = safeApiCall<Unit> {
            throw ApolloNetworkException("Error while reading JSON response", ApiError.RateLimited(45))
        }

        val error = result as Result.Error
        assertEquals(429, error.code)
        assertEquals(45L, error.countdownSeconds)
        assertTrue(error.exception is ApiError.RateLimited)
    }

    @Test
    fun `a deeply nested ApiError is still found`() = runTest {
        val nested = RuntimeException("outer", IllegalStateException("inner", ApiError.SessionExpired()))
        val result = safeApiCall<Unit> { throw nested }

        val error = result as Result.Error
        assertEquals(401, error.code)
        assertTrue(error.exception is ApiError.SessionExpired)
    }

    @Test
    fun `a deferral carries its countdown so a caller can reschedule`() = runTest {
        val result = safeApiCall<Unit> { throw ApiError.Deferred(30) }

        val error = result as Result.Error
        assertEquals(429, error.code)
        assertEquals(30L, error.countdownSeconds)
    }

    @Test
    fun `a validation failure surfaces the field message AniList wrote`() = runTest {
        val result = safeApiCall<Unit> {
            throw ApiError.Validation(mapOf("score" to listOf("The score may not be greater than 100.")))
        }

        val error = result as Result.Error
        assertEquals(400, error.code)
        assertEquals("The score may not be greater than 100.", error.message)
    }

    @Test
    fun `an unclassified HTTP failure keeps its status code`() = runTest {
        val result = safeApiCall<Unit> {
            throw ApolloHttpException(503, emptyList(), null, "Service Unavailable")
        }

        val error = result as Result.Error
        assertEquals(503, error.code)
    }

    @Test
    fun `a bare Apollo failure does not invent a status code`() = runTest {
        val result = safeApiCall<Unit> { throw DefaultApolloException("malformed json") }

        val error = result as Result.Error
        assertNull(error.code)
        assertNull(error.countdownSeconds)
    }

    @Test
    fun `an offline failure has no countdown to show`() = runTest {
        val result = safeApiCall<Unit> { throw ApiError.Offline() }

        val error = result as Result.Error
        assertNull(error.code)
        assertNull(error.countdownSeconds)
    }
}
