package com.anisync.android.data.util

import com.anisync.android.data.network.ApiErrorMessages
import com.anisync.android.data.network.findApiError
import com.anisync.android.domain.Result
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import kotlinx.coroutines.CancellationException

/**
 * Runs an AniList call and folds whatever goes wrong into [Result.Error].
 *
 * The single entry point for the data layer. Classification happens up in
 * [com.anisync.android.data.network.AniListErrorInterceptor], so by the time a failure reaches here
 * it is usually already an [ApiError] and this only has to unwrap and describe it. The `when`
 * branches below are the safety net for anything that bypassed the interceptor.
 *
 * Two details that are easy to get wrong and were wrong before:
 *
 * **Cancellation is rethrown.** A blanket `catch (e: Exception)` swallows `CancellationException`,
 * which turns a screen the user navigated away from into a spurious error and breaks structured
 * concurrency for everything above it.
 *
 * **The cause chain is unwrapped.** `ApolloException` is sealed, so [ApiError] cannot be one and
 * Apollo always hands ours back wrapped in an `ApolloNetworkException`. Reading only the top of the
 * chain reports a rate limit as "no internet connection" and sends the reader after the wrong
 * problem.
 */
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T,
): Result<T> {
    return try {
        Result.Success(apiCall())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.findApiError()?.toResult() ?: e.toResult()
    }
}

private fun ApiError.toResult(): Result.Error = Result.Error(
    message = ApiErrorMessages.describe(this),
    code = statusCode(),
    countdownSeconds = countdownSeconds(),
    exception = this,
)

/**
 * The HTTP status a consumer would recognise.
 *
 * A handful of screens branch on this: 429 drives the countdown toast and the refresh gate, 404
 * pops the activity detail screen. Everything else only shows the message.
 */
private fun ApiError.statusCode(): Int? = when (this) {
    is ApiError.RateLimited -> 429
    is ApiError.Deferred -> 429
    is ApiError.SessionExpired -> 401
    is ApiError.TokenRejected -> 401
    is ApiError.PermissionDenied -> 401
    is ApiError.ApiDisabled -> 403
    is ApiError.ServerError -> statusCode
    is ApiError.Validation -> 400
    is ApiError.GraphQLError -> statusCode
    is ApiError.Offline, is ApiError.Timeout, is ApiError.Unknown -> null
}

private fun ApiError.countdownSeconds(): Long? = when (this) {
    is ApiError.RateLimited -> retryAfterSeconds
    is ApiError.Deferred -> retryAfterSeconds
    else -> null
}

private fun Exception.toResult(): Result.Error = when (this) {
    is ApolloHttpException -> Result.Error(
        ApiErrorMessages.describe(ApiError.ServerError(statusCode)),
        statusCode,
        null,
        this,
    )

    is ApolloNetworkException -> Result.Error(
        ApiErrorMessages.describe(ApiError.Offline(this)),
        null,
        null,
        this,
    )

    is ApolloException -> Result.Error(
        ApiErrorMessages.describe(ApiError.Unknown(message ?: "")),
        null,
        null,
        this,
    )

    else -> Result.Error(
        message ?: ApiErrorMessages.describe(ApiError.Unknown("")),
        null,
        null,
        this,
    )
}

/**
 * The response's payload, or the reason there isn't one.
 *
 * Apollo does not throw on a transport failure: `execute()` returns a response carrying the
 * exception with a null `data`. Code that read `response.data?...?: emptyList()` therefore turned
 * every failed request into an empty success, which is why a section that could not load was
 * indistinguishable from a section with nothing in it.
 */
fun <D : Operation.Data> ApolloResponse<D>.dataOrThrow(): D {
    exception?.let { throw it }
    if (hasErrors()) {
        val messages = errors?.map { it.message } ?: listOf("Unknown error")
        val statusCode = (errors?.firstOrNull()?.nonStandardFields?.get("status") as? Number)?.toInt()
        throw ApiError.GraphQLError(messages, statusCode)
    }
    return data ?: throw ApiError.Unknown("The server returned no data.")
}
