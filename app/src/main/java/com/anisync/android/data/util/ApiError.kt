package com.anisync.android.data.util

/**
 * Typed failures for the AniList GraphQL API.
 *
 * These are thrown from the network interceptors and folded into
 * [com.anisync.android.domain.Result.Error] by [safeApiCall]. They carry structure rather than a
 * pre-built sentence, because the same failure reads differently in a toast, on an error screen and
 * in a worker's retry decision.
 *
 * Note that [com.apollographql.apollo.exception.ApolloException] is sealed, so none of these can be
 * one. Apollo therefore wraps whatever an interceptor throws in an `ApolloNetworkException` and
 * hands it back as `ApolloResponse.exception`. [safeApiCall] unwraps that cause chain.
 *
 * References:
 * - https://docs.anilist.co/guide/rate-limiting
 * - https://docs.anilist.co/guide/graphql/errors
 * - https://docs.anilist.co/guide/considerations
 */
sealed class ApiError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * HTTP 429. AniList answers with a one minute timeout and a `Retry-After` saying how much of it
     * is left. Reaching a caller means the retry inside the network layer also failed.
     */
    class RateLimited(
        val retryAfterSeconds: Long,
        val limit: Int = 0,
    ) : ApiError("Rate limited for ${retryAfterSeconds}s")

    /**
     * The client's own gate refused a [com.anisync.android.data.network.RequestPriority.Background]
     * request because the remaining budget belongs to the user.
     *
     * Nothing went wrong and nothing was sent. Workers turn this into `Result.retry()` rather than
     * sitting on a coroutine until the window rolls over.
     */
    class Deferred(
        val retryAfterSeconds: Long,
    ) : ApiError("Deferred for ${retryAfterSeconds}s to protect the request budget")

    /** HTTP 401 on the active account. The token is expired or revoked and the session is over. */
    class SessionExpired : ApiError("Session expired")

    /**
     * HTTP 401 on a request that carried its own token, which is how
     * [com.anisync.android.data.account.TokenedApolloClientFactory] polls accounts that are not
     * active. Only that one account is dead, so the active session must be left alone.
     */
    class TokenRejected : ApiError("Account token rejected")

    /**
     * The token is valid but not allowed to do this. AniList conflates permission with auth and
     * answers 401 for it, sometimes inside an HTTP 200 body.
     */
    class PermissionDenied(
        val reason: String? = null,
    ) : ApiError(reason ?: "Permission denied")

    /**
     * The documented whole-API shutdown: a 403 carrying a GraphQL message pointing at the AniList
     * Discord. Distinct from [PermissionDenied] because nothing the user does will help.
     */
    class ApiDisabled(
        val notice: String,
    ) : ApiError(notice)

    /** HTTP 5xx. Retried inside the network layer before it reaches a caller. */
    class ServerError(
        val statusCode: Int,
    ) : ApiError("Server error $statusCode")

    /** The device could not reach AniList at all. */
    class Offline(
        cause: Throwable? = null,
    ) : ApiError("Offline", cause)

    /**
     * The request reached the network but did not finish in time. Kept apart from [Offline] because
     * "check your connection" is the wrong advice on a slow but working one.
     */
    class Timeout(
        cause: Throwable? = null,
    ) : ApiError("Timed out", cause)

    /**
     * A mutation failed AniList's validation rules. The `validation` object maps each rejected
     * field to its messages, which are written to be shown to the user.
     */
    class Validation(
        val fields: Map<String, List<String>>,
    ) : ApiError(fields.values.firstOrNull()?.firstOrNull() ?: "Validation failed") {

        /** Every message across every field, in the order AniList returned them. */
        val messages: List<String> get() = fields.values.flatten()
    }

    /** Anything else in the response's `errors` array. */
    class GraphQLError(
        val errors: List<String>,
        val statusCode: Int? = null,
    ) : ApiError(errors.firstOrNull() ?: "API error")

    /** Nothing above matched. */
    class Unknown(
        message: String,
        cause: Throwable? = null,
    ) : ApiError(message, cause)
}
