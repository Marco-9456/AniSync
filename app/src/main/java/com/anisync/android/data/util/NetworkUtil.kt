package com.anisync.android.data.util

import com.anisync.android.domain.Result
import com.apollographql.apollo.exception.ApolloException

/**
 * Executes a network call and catches any exceptions, wrapping the result in [Result].
 */
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): Result<T> {
    return try {
        Result.Success(apiCall())
    } catch (e: ApolloException) {
        Result.Error("Network error: ${e.message}", e)
    } catch (e: Exception) {
        Result.Error("Unexpected error: ${e.message}", e)
    }
}
