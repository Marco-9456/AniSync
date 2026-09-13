package com.anisync.android.data.network

import com.anisync.android.data.AuthRepository
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identity, pacing and rate limit accounting for every AniList request.
 *
 * Deliberately small. It attaches who we are, waits its turn at [RateLimitGate], and folds the
 * response's rate limit headers back into the window. It classifies nothing and it never reads a
 * response body.
 *
 * That last part matters. The interceptor this replaces called `readUtf8()` on every response,
 * including multi-megabyte library collections, purely to substring-search for an embedded error
 * status. Classification now happens in [AniListErrorInterceptor], one layer up, where Apollo has
 * already parsed the errors and where the only bodies that get read are the small ones that failed.
 *
 * A request that arrives already carrying an `Authorization` header is left alone. That is how
 * [com.anisync.android.data.account.TokenedApolloClientFactory] polls accounts other than the
 * active one, and its failures must not disturb the active session.
 */
@Singleton
class AniListHttpInterceptor @Inject constructor(
    private val authRepository: AuthRepository,
    private val gate: RateLimitGate,
) : HttpInterceptor {

    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        val priority = request.executionContext.requestPriority
        gate.acquire(priority)
        try {
            val response = chain.proceed(identify(request))
            gate.onResponse(
                statusCode = response.statusCode,
                limit = response.headers.intValue(AniListIdentity.HEADER_RATE_LIMIT),
                remaining = response.headers.intValue(AniListIdentity.HEADER_RATE_REMAINING),
                retryAfterSeconds = response.headers.longValue(AniListIdentity.HEADER_RETRY_AFTER),
            )
            return response
        } finally {
            gate.release()
        }
    }

    private fun identify(request: HttpRequest): HttpRequest {
        val builder = request.newBuilder()
            .addHeader(AniListIdentity.HEADER_USER_AGENT, AniListIdentity.USER_AGENT)
            .addHeader(AniListIdentity.HEADER_REFERER, AniListIdentity.REFERER)

        if (!request.headers.has(AniListIdentity.HEADER_AUTHORIZATION)) {
            authRepository.getToken()?.let {
                builder.addHeader(AniListIdentity.HEADER_AUTHORIZATION, "Bearer $it")
            }
        }
        return builder.build()
    }
}

private fun List<HttpHeader>.has(name: String): Boolean =
    any { it.name.equals(name, ignoreCase = true) }

private fun List<HttpHeader>.value(name: String): String? =
    firstOrNull { it.name.equals(name, ignoreCase = true) }?.value?.trim()

internal fun List<HttpHeader>.intValue(name: String): Int? = value(name)?.toIntOrNull()

internal fun List<HttpHeader>.longValue(name: String): Long? = value(name)?.toLongOrNull()
