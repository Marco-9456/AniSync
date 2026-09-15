package com.anisync.android.data.network

import android.util.Log
import com.anisync.android.data.util.ApiError
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import okio.Buffer
import okio.BufferedSource
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns an AniList failure into a typed [ApiError], and decides whether to try again.
 *
 * Installed at [ApolloInterceptor.InsertionPoint.BeforeNetwork], which puts it below the normalized
 * cache and above the transport. That position is the whole point: it only runs when a request
 * really goes to the network, and it sees the response before anything downstream has touched it,
 * with `errors` already parsed.
 *
 * Retry lives here rather than in the HTTP layer because only here is it known whether the
 * operation is a mutation, which is what decides whether repeating it is safe.
 *
 * A classified failure is **thrown**, not attached to the response.
 *
 * `ApolloCall.execute()` does not throw: it returns the response with `exception` set. So the 85
 * repository sites that read `response.data?.field ?: emptyList()` turned every failed request into
 * an empty success, and a section that could not load looked exactly like a section with nothing in
 * it. Throwing sends all of them through [com.anisync.android.data.util.safeApiCall] instead, which
 * is where the typed error was always meant to land.
 */
@Singleton
class AniListErrorInterceptor @Inject constructor(
    private val session: SessionTokens,
    private val gate: RateLimitGate,
    private val retryPolicy: RetryPolicy,
) : ApolloInterceptor {

    override fun <D : Operation.Data> intercept(
        request: ApolloRequest<D>,
        chain: ApolloInterceptorChain,
    ): Flow<ApolloResponse<D>> {
        val operation = request.operation
        // Anything that streams more than one response is passed straight through: collecting it
        // to a single value to inspect would break it.
        if (operation !is Query && operation !is Mutation) return chain.proceed(request)

        val isMutation = operation is Mutation
        val tokenScoped = request.executionContext.isTokenScoped

        return flow {
            val priority = resolveRequestPriority(request.executionContext.explicitPriority)
            var attempt = 0
            while (true) {
                attempt++
                val response = chain.proceed(request).first()
                val error = classify(response, operation.name(), tokenScoped)
                if (error == null) {
                    emit(response)
                    return@flow
                }

                val decision = retryPolicy.decide(
                    error = error,
                    attempt = attempt,
                    isMutation = isMutation,
                    priority = priority,
                    blockedForMs = gate.blockedForMs(),
                )
                if (decision is RetryDecision.After) {
                    gate.onRetry()
                    Log.i(
                        TAG,
                        "AniSyncNet event=retry op=${operation.name()} attempt=$attempt " +
                            "in_ms=${decision.delayMs} cause=${error::class.simpleName}",
                    )
                    delay(decision.delayMs)
                    continue
                }

                report(error, operation.name())
                throw error
            }
        }
    }

    private fun <D : Operation.Data> classify(
        response: ApolloResponse<D>,
        operationName: String,
        tokenScoped: Boolean,
    ): ApiError? {
        response.exception?.let { return fromException(it, operationName, tokenScoped) }

        val errors = response.errors
        if (errors.isNullOrEmpty()) return null

        // A response that still carries data is a partial success. Failing it would throw away
        // fields that did arrive, so those stay with the repository layer as before.
        if (response.data != null) return null

        val parsed = AniListErrors.fromApolloErrors(errors)
        // Only claim an error AniList has labelled. A plain GraphQL error with partial data is left
        // for the repository layer to handle as it always has.
        if (parsed.none { it.status != null || it.validation.isNotEmpty() }) return null

        return AniListErrors.classify(
            httpStatus = null,
            errors = parsed,
            operationName = operationName,
            tokenScoped = tokenScoped,
            retryAfterSeconds = null,
        )
    }

    private fun fromException(
        exception: Throwable,
        operationName: String,
        tokenScoped: Boolean,
    ): ApiError? = when (exception) {
        // AniList serves errors as `application/json`, never `application/graphql-response+json`,
        // so Apollo never parses a non-2xx body and hands the whole response over here instead.
        // These bodies are the small ones, which is why reading them here is affordable.
        is ApolloHttpException -> AniListErrors.classify(
            httpStatus = exception.statusCode,
            errors = AniListErrors.parseBody(exception.body?.readCapped(MAX_ERROR_BODY_BYTES)),
            operationName = operationName,
            tokenScoped = tokenScoped,
            retryAfterSeconds = exception.headers.longValue(AniListIdentity.HEADER_RETRY_AFTER),
        )

        is ApolloNetworkException -> {
            val cause = exception.platformCause as? Throwable
            cause.findApiError() ?: when (cause) {
                is SocketTimeoutException, is InterruptedIOException -> ApiError.Timeout(cause)
                is IOException -> ApiError.Offline(cause)
                else -> ApiError.Offline(cause)
            }
        }

        // Malformed JSON and the like. Nothing typed to say about it, so leave it as Apollo built it.
        else -> null
    }

    private fun report(error: ApiError, operationName: String) {
        if (error is ApiError.SessionExpired) {
            Log.w(TAG, "AniSyncNet event=session_expired op=$operationName")
            session.onSessionExpired()
        }
    }

    private companion object {
        const val TAG = "AniListErrors"

        /** An AniList error body is a few hundred bytes. This is only here to bound the pathological case. */
        const val MAX_ERROR_BODY_BYTES = 64L * 1024
    }
}

/** Walks a cause chain for an [ApiError] the network layer threw and Apollo then wrapped. */
internal fun Throwable?.findApiError(): ApiError? =
    generateSequence(this) { it.cause }.filterIsInstance<ApiError>().firstOrNull()

/**
 * Reads at most [maxBytes] and closes the source.
 *
 * Apollo only hands over an error body when `httpExposeErrorBody` is on, and makes closing it the
 * caller's problem. The cap is for the pathological case: a real AniList error is a few hundred
 * bytes, and this is the only body the network layer ever reads.
 */
private fun BufferedSource.readCapped(maxBytes: Long): String = use { source ->
    val buffer = Buffer()
    source.read(buffer, maxBytes)
    buffer.readUtf8()
}
