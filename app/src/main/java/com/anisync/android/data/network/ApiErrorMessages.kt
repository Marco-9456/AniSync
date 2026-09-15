package com.anisync.android.data.network

import android.content.Context
import com.anisync.android.R
import com.anisync.android.data.util.ApiError
import com.anisync.android.data.util.AppLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place an [ApiError] becomes a sentence a user reads.
 *
 * [com.anisync.android.data.util.safeApiCall] is a free function called from 92 repository sites, so
 * it has nowhere to inject a `Context` from. The singleton publishes itself into [instance] when
 * Hilt builds it, which happens in `AniSyncApplication.onCreate`, and the mapper falls back to a
 * plain English string if anything ever asks before then. That keeps every existing caller reading
 * `Result.Error.message` and gets all of them localised without touching one of them.
 *
 * Strings are read through [AppLocale], not off the application context, so they follow the language
 * chosen in the app rather than the one the device is set to.
 */
@Singleton
class ApiErrorMessages @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    init {
        instance = this
    }

    fun message(error: ApiError): String {
        val context = AppLocale.wrap(appContext)
        return when (error) {
            is ApiError.RateLimited ->
                context.getString(R.string.api_error_rate_limited, error.retryAfterSeconds)

            is ApiError.Deferred ->
                context.getString(R.string.api_error_deferred)

            is ApiError.SessionExpired ->
                context.getString(R.string.api_error_session_expired)

            is ApiError.TokenRejected ->
                context.getString(R.string.api_error_token_rejected)

            is ApiError.PermissionDenied ->
                error.reason ?: context.getString(R.string.api_error_permission_denied)

            // AniList writes this one itself, and it usually points at their Discord for detail.
            is ApiError.ApiDisabled -> error.notice

            is ApiError.ServerError ->
                context.getString(R.string.api_error_server, error.statusCode)

            is ApiError.Offline ->
                context.getString(R.string.api_error_offline)

            is ApiError.Timeout ->
                context.getString(R.string.api_error_timeout)

            // Written by AniList to be shown to the user, per their mutation docs.
            is ApiError.Validation -> error.message ?: context.getString(R.string.api_error_unknown)

            is ApiError.GraphQLError ->
                error.errors.firstOrNull() ?: context.getString(R.string.api_error_unknown)

            is ApiError.Unknown -> error.message ?: context.getString(R.string.api_error_unknown)
        }
    }

    companion object {
        @Volatile
        private var instance: ApiErrorMessages? = null

        /** Localised text for [error], or an English fallback if the app is not built yet. */
        fun describe(error: ApiError): String = instance?.message(error) ?: fallback(error)

        private fun fallback(error: ApiError): String = when (error) {
            is ApiError.RateLimited -> "Too many requests. Please wait ${error.retryAfterSeconds}s."
            is ApiError.Deferred -> "Waiting for the request budget to recover."
            is ApiError.SessionExpired -> "Your session has expired. Please log in again."
            is ApiError.TokenRejected -> "This account needs to be signed in again."
            is ApiError.PermissionDenied -> error.reason ?: "You don't have permission to do that."
            is ApiError.ApiDisabled -> error.notice
            is ApiError.ServerError -> "Server error (${error.statusCode}). Please try again later."
            is ApiError.Offline -> "No internet connection. Check your network and try again."
            is ApiError.Timeout -> "The request took too long. Please try again."
            else -> error.message ?: "An unexpected error occurred."
        }
    }
}
