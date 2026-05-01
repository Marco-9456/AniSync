package com.anisync.android.data.util

/**
 * Typed API error hierarchy for the AniList GraphQL API.
 *
 * Instead of generic "Network error" strings, each error type carries
 * structured data so the UI can show contextual messages and take
 * appropriate recovery actions.
 *
 * Reference: https://docs.anilist.co/guide/rate-limiting
 * Reference: https://docs.anilist.co/guide/graphql/errors
 */
sealed class ApiError(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * HTTP 429 — Too Many Requests.
     * The AniList API returns `Retry-After` (seconds) and `X-RateLimit-Reset` (unix timestamp).
     * The interceptor will auto-retry once after waiting; if this propagates to the caller,
     * the retry also failed.
     *
     * @param retryAfterSeconds Seconds to wait before retrying (from `Retry-After` header, default 60)
     */
    class RateLimited(
        val retryAfterSeconds: Long
    ) : ApiError("Rate limited. Please wait ${retryAfterSeconds}s before trying again.")

    /**
     * HTTP 401 — Bearer token expired or revoked.
     * The app should clear the stored token and redirect to the login screen.
     */
    class Unauthorized : ApiError("Your session has expired. Please log in again.")

    /**
     * HTTP 401 returned for an action the token is valid for but not allowed to do
     * (e.g. deleting a moderator's message). AniList conflates auth and permission
     * errors at the HTTP layer; we suppress session-expired flow for known cases.
     */
    class Forbidden(message: String = "You don't have permission to do that.") : ApiError(message)

    /**
     * HTTP 500, 502, 503 — Server-side failure.
     * The AniList API is likely experiencing issues.
     */
    class ServerError(
        val statusCode: Int
    ) : ApiError("Server error ($statusCode). Please try again later.")

    /**
     * No internet, DNS failure, connection timeout, socket timeout.
     * The device cannot reach the AniList API.
     */
    class NetworkError(
        cause: Throwable
    ) : ApiError("No internet connection. Check your network and try again.", cause)

    /**
     * GraphQL errors returned in the response body (even on HTTP 200).
     * See: https://docs.anilist.co/guide/graphql/errors
     *
     * @param errors List of error messages from the `errors` array
     * @param statusCode The HTTP status code of the `status` field inside the error object, if present
     */
    class GraphQLError(
        val errors: List<String>,
        val statusCode: Int? = null
    ) : ApiError(errors.firstOrNull() ?: "An API error occurred.")

    /**
     * Catch-all for unexpected errors not covered by other types.
     */
    class Unknown(
        message: String,
        cause: Throwable? = null
    ) : ApiError(message, cause)
}
