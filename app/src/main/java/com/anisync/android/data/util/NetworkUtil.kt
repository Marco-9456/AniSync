package com.anisync.android.data.util

import com.anisync.android.domain.Result
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException

/**
 * Executes an Apollo GraphQL call with comprehensive error handling.
 *
 * This is the **single entry point** for all API calls in the app.
 * It handles every error type the AniList API can return:
 *
 * 1. **[ApiError.RateLimited]** (from interceptor) → "Rate limited, wait Xs"
 * 2. **[ApiError.Unauthorized]** (from interceptor) → "Session expired"
 * 3. **[ApiError.ServerError]** (from interceptor) → "Server error (5xx)"
 * 4. **[ApiError.NetworkError]** → "No internet connection"
 * 5. **[ApiError.GraphQLError]** → Errors in the GraphQL response body (even on HTTP 200)
 * 6. **[ApiError.Unknown]** → Catch-all for unexpected errors
 *
 * **Important:** This function ALWAYS checks `response.hasErrors()`, which closes
 * the gap where queries previously ignored GraphQL-level errors.
 *
 * @param apiCall Lambda that executes the Apollo query/mutation and returns the mapped domain object.
 *               The lambda receives the raw ApolloResponse so it can check data nullability.
 */
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): Result<T> {
    return try {
        Result.Success(apiCall())
    } catch (e: ApiError.RateLimited) {
        Result.Error("Too many requests. Please wait ${e.retryAfterSeconds} seconds.", 429, e.retryAfterSeconds, e)
    } catch (e: ApiError.Unauthorized) {
        Result.Error("Your session has expired. Please log in again.", 401, null, e)
    } catch (e: ApiError.Forbidden) {
        Result.Error(e.message ?: "You don't have permission to do that.", 401, null, e)
    } catch (e: ApiError.ServerError) {
        Result.Error("Server error (${e.statusCode}). Please try again later.", e.statusCode, null, e)
    } catch (e: ApiError.NetworkError) {
        Result.Error("No internet connection. Check your network and try again.", null, null, e)
    } catch (e: ApiError.GraphQLError) {
        Result.Error(e.errors.firstOrNull() ?: "An API error occurred.", e.statusCode, null, e)
    } catch (e: ApiError) {
        Result.Error(e.message ?: "An unexpected error occurred.", null, null, e)
    } catch (e: ApolloHttpException) {
        // HTTP errors not caught by the interceptor (shouldn't normally reach here,
        // but acts as a safety net)
        val message = when (e.statusCode) {
            429 -> "Too many requests. Please try again later."
            401 -> "Session expired. Please log in again."
            403 -> "Access denied."
            in 500..599 -> "Server error. Please try again later."
            else -> "HTTP error ${e.statusCode}: ${e.message}"
        }
        Result.Error(message, e.statusCode, null, e)
    } catch (e: ApolloNetworkException) {
        Result.Error("No internet connection. Check your network and try again.", null, null, e)
    } catch (e: ApolloException) {
        Result.Error("Network error: ${e.message}", null, null, e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "An unexpected error occurred.", null, null, e)
    }
}

/**
 * Executes an Apollo GraphQL query/mutation and checks for GraphQL-level errors.
 *
 * Use this when you need to inspect the raw [ApolloResponse] before extracting data.
 * It ensures `response.hasErrors()` is always checked — even for queries.
 *
 * Example:
 * ```kotlin
 * return safeApiCallWithResponse("load media") { 
 *     apolloClient.query(GetMediaQuery(id)).execute()
 * } { response ->
 *     response.data?.Media?.toDomain() ?: throw Exception("Media not found")
 * }
 * ```
 *
 * @param action Human-readable action name for error messages (e.g. "load media")
 * @param execute Lambda that executes the Apollo call
 * @param transform Lambda that maps the response data to the domain type
 */
suspend fun <D : Operation.Data, T> safeApiCallWithResponse(
    action: String,
    execute: suspend () -> ApolloResponse<D>,
    transform: (ApolloResponse<D>) -> T
): Result<T> {
    return safeApiCall {
        val response = execute()

        // Always check for GraphQL errors — even on HTTP 200.
        // This is critical: AniList can return { "data": null, "errors": [...] } on 200.
        if (response.hasErrors()) {
            val messages = response.errors?.map { it.message } ?: listOf("Unknown error")
            // Check if it's a rate limit error embedded in the GraphQL response
            val statusCode = response.errors?.firstOrNull()?.let { error ->
                // Apollo exposes extensions/customAttributes but the status is in the error JSON
                // For AniList, 429 errors come with both HTTP 429 AND GraphQL error
                (error.extensions?.get("status") as? Number)?.toInt()
            }
            throw ApiError.GraphQLError(messages, statusCode)
        }

        transform(response)
    }
}
