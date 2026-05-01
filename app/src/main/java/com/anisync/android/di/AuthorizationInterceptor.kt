package com.anisync.android.di

import android.util.Log
import com.anisync.android.data.AuthRepository
import com.anisync.android.data.util.ApiError
import com.anisync.android.di.AuthorizationInterceptor.Companion.MAX_RETRY_AFTER_SECONDS
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * HTTP interceptor for all AniList API requests.
 *
 * Responsibilities:
 * 1. **Authorization**: Attaches the Bearer token to every request
 * 2. **Rate limit tracking**: Reads `X-RateLimit-Remaining` from every response
 * 3. **Proactive throttling**: Adds a small delay when remaining requests are low
 * 4. **429 handling**: On rate limit, reads `Retry-After` → waits → retries **once**
 * 5. **401 handling**: On auth failure, notifies [AuthRepository] to trigger session-expired flow
 *
 * AniList API rate limits (from https://docs.anilist.co/guide/rate-limiting):
 * - Normal: 90 requests/minute
 * - Degraded (currently active): 30 requests/minute
 * - Headers on every response: `X-RateLimit-Limit`, `X-RateLimit-Remaining`
 * - Headers on 429: `Retry-After` (seconds), `X-RateLimit-Reset` (unix timestamp)
 * - Burst limiter also exists (undocumented threshold)
 */
class AuthorizationInterceptor @Inject constructor(
    private val authRepository: AuthRepository
) : HttpInterceptor {

    companion object {
        private const val TAG = "AuthInterceptor"

        /** If remaining requests drop to this, start proactive throttling */
        private const val THROTTLE_THRESHOLD = 5

        /** Delay in ms when proactively throttling (remaining < THROTTLE_THRESHOLD) */
        private const val THROTTLE_DELAY_MS = 2000L

        /** Default wait time in seconds if 429 has no Retry-After header (AniList docs say "1 minute timeout") */
        private const val DEFAULT_RETRY_AFTER_SECONDS = 60L

        /** Maximum wait time we're willing to delay for (cap at 2 minutes) */
        private const val MAX_RETRY_AFTER_SECONDS = 120L
    }

    /**
     * Tracks the remaining requests in the current rate limit window.
     * Updated from `X-RateLimit-Remaining` on every response.
     * Thread-safe via AtomicInteger.
     */
    private val rateLimitRemaining = AtomicInteger(90)

    /**
     * Unix timestamp (seconds) of when the rate limit resets.
     * Updated from `X-RateLimit-Reset` on 429 responses.
     */
    private val rateLimitResetAt = AtomicLong(0)

    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        // ── Step 1: Attach Bearer token ─────────────────────────────────
        val token = authRepository.getToken()
        val authorizedRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        // ── Step 2: Proactive throttling ────────────────────────────────
        // If we're running low on remaining requests, slow down to avoid
        // hitting the hard limit. This is better than slamming into a 429.
        val remaining = rateLimitRemaining.get()
        if (remaining in 1 until THROTTLE_THRESHOLD) {
            Log.w(TAG, "Rate limit low ($remaining remaining), throttling ${THROTTLE_DELAY_MS}ms")
            delay(THROTTLE_DELAY_MS)
        }

        // ── Step 3: Execute the request ─────────────────────────────────
        val response = chain.proceed(authorizedRequest)

        // ── Step 4: Read rate limit headers from every response ─────────
        updateRateLimitState(response)

        // ── Step 5: Handle error status codes ───────────────────────────
        return when (response.statusCode) {
            429 -> handleRateLimited(response, authorizedRequest, chain)
            401 -> handleUnauthorized(response, authorizedRequest)
            in 500..599 -> handleServerError(response)
            else -> response
        }
    }

    /**
     * Operations that AniList returns 401 for when the token is *valid* but the
     * user lacks permission (e.g. deleting a moderator's message). Treating these
     * as session-expired would log the user out for benign permission errors.
     */
    private val permissionGated401Operations = setOf(
        "DeleteActivity",
        "DeleteActivityReply",
        "DeleteThread",
        "DeleteThreadComment"
    )

    /**
     * Extract rate limit headers from every API response.
     *
     * Headers present on all responses:
     * - `X-RateLimit-Limit`: Total requests allowed (typically 90 or 30 in degraded mode)
     * - `X-RateLimit-Remaining`: Requests left in the current window
     *
     * Additional headers on 429 responses:
     * - `Retry-After`: Seconds to wait before retrying
     * - `X-RateLimit-Reset`: Unix timestamp when the window resets
     */
    private fun updateRateLimitState(response: HttpResponse) {
        response.headers.forEach { header ->
            when (header.name.lowercase()) {
                "x-ratelimit-remaining" -> {
                    header.value.toIntOrNull()?.let { value ->
                        rateLimitRemaining.set(value)
                        if (value <= THROTTLE_THRESHOLD) {
                            Log.w(TAG, "Rate limit remaining: $value")
                        }
                    }
                }
                "x-ratelimit-limit" -> {
                    Log.d(TAG, "Rate limit: ${header.value}/min")
                }
                "x-ratelimit-reset" -> {
                    header.value.toLongOrNull()?.let { value ->
                        rateLimitResetAt.set(value)
                    }
                }
            }
        }
    }

    /**
     * Handle HTTP 429 — Too Many Requests.
     *
     * Strategy:
     * 1. Read `Retry-After` header (seconds to wait)
     * 2. Wait that exact duration (capped at [MAX_RETRY_AFTER_SECONDS])
     * 3. Retry the request **once**
     * 4. If the retry also 429s, throw [ApiError.RateLimited] so the caller gets a clean error
     *
     * We retry only once to prevent infinite loops. If two consecutive 429s happen,
     * something is seriously wrong and the user should be told to wait.
     */
    private suspend fun handleRateLimited(
        originalResponse: HttpResponse,
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        val retryAfter = originalResponse.headers
            .firstOrNull { it.name.equals("Retry-After", ignoreCase = true) }
            ?.value?.toLongOrNull()
            ?: DEFAULT_RETRY_AFTER_SECONDS

        val cappedRetryAfter = retryAfter.coerceAtMost(MAX_RETRY_AFTER_SECONDS)

        Log.w(TAG, "Rate limited (429). Waiting ${cappedRetryAfter}s before retrying...")

        // Reset our tracked remaining count
        rateLimitRemaining.set(0)

        // Wait the time the server told us to wait
        delay(cappedRetryAfter * 1000)

        // Retry once
        val retryResponse = chain.proceed(request)
        updateRateLimitState(retryResponse)

        // If still rate limited, don't loop — propagate the error
        if (retryResponse.statusCode == 429) {
            val secondRetryAfter = retryResponse.headers
                .firstOrNull { it.name.equals("Retry-After", ignoreCase = true) }
                ?.value?.toLongOrNull()
                ?: DEFAULT_RETRY_AFTER_SECONDS

            Log.e(TAG, "Still rate limited after retry. Giving up.")
            throw ApiError.RateLimited(secondRetryAfter)
        }

        Log.i(TAG, "Retry successful after rate limit wait.")
        return retryResponse
    }

    /**
     * Handle HTTP 401 — Unauthorized.
     *
     * The token is expired or revoked. We:
     * 1. Tell AuthRepository to clear it and emit the session-expired event
     * 2. Throw [ApiError.Unauthorized] so the caller gets a typed error
     *
     * The UI layer collects `authRepository.sessionExpired` to show the dialog.
     */
    private fun handleUnauthorized(response: HttpResponse, request: HttpRequest): HttpResponse {
        val operationName = request.headers
            .firstOrNull { it.name.equals("X-APOLLO-OPERATION-NAME", ignoreCase = true) }
            ?.value
        if (operationName != null && operationName in permissionGated401Operations) {
            Log.w(TAG, "401 on $operationName — treating as permission denial, not session expiry.")
            throw ApiError.Forbidden()
        }
        Log.w(TAG, "Unauthorized (401). Token expired or revoked.")
        authRepository.onSessionExpired()
        throw ApiError.Unauthorized()
    }

    /**
     * Handle HTTP 5xx — Server errors.
     * Log and throw typed error so the UI can show "Server error, try again later."
     */
    private fun handleServerError(response: HttpResponse): HttpResponse {
        Log.e(TAG, "Server error: ${response.statusCode}")
        throw ApiError.ServerError(response.statusCode)
    }
}
