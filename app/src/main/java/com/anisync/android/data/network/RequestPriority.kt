package com.anisync.android.data.network

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.MutableExecutionOptions

/**
 * How much of the AniList request budget a call is entitled to.
 *
 * AniList allows 30 requests a minute in its current degraded mode, which is not enough for the
 * app's background work and whatever the user is looking at to both spend freely. Priority is what
 * [RateLimitGate] uses to decide who gets the last of the window.
 *
 * The default when a call carries no priority is [Interactive], so an untagged call site behaves
 * exactly as it did before. Only background and speculative work needs tagging.
 */
enum class RequestPriority {
    /** A user is waiting on this. Spends the budget down to the last request. */
    Interactive,

    /** Speculative work for something on screen but not asked for, such as a details preview. */
    Prefetch,

    /**
     * Workers, widget refreshes and notification polls. Never competes with the user: when the
     * budget is low the gate refuses immediately with [com.anisync.android.data.util.ApiError.Deferred]
     * so WorkManager can reschedule instead of holding a coroutine open.
     */
    Background,
}

/**
 * Carries [RequestPriority] from the call site down to the HTTP layer.
 *
 * [com.apollographql.apollo.api.http.DefaultHttpRequestComposer] copies the operation's execution
 * context onto the `HttpRequest`, so the interceptor can read this without a smuggled header.
 */
class RequestPriorityContext(val priority: RequestPriority) : ExecutionContext.Element {
    override val key: ExecutionContext.Key<RequestPriorityContext> get() = Key

    companion object Key : ExecutionContext.Key<RequestPriorityContext>
}

/** Tags an Apollo call, or a whole client, with the budget tier it is allowed to spend from. */
fun <T> MutableExecutionOptions<T>.priority(priority: RequestPriority): T =
    addExecutionContext(RequestPriorityContext(priority))

/** The priority a request was tagged with, or [RequestPriority.Interactive] if it was not. */
val ExecutionContext.requestPriority: RequestPriority
    get() = this[RequestPriorityContext]?.priority ?: RequestPriority.Interactive

/**
 * Marks a client that talks to AniList as an account other than the active one.
 *
 * [com.anisync.android.data.account.TokenedApolloClientFactory] tags its clients with this so a
 * rejected token on a background poll fails that one account instead of ending the session the user
 * is actually in.
 */
object TokenScopedContext : ExecutionContext.Element {
    override val key: ExecutionContext.Key<TokenScopedContext> get() = Key

    object Key : ExecutionContext.Key<TokenScopedContext>
}

/** True when the request was issued by a client bound to a specific, non-active account token. */
val ExecutionContext.isTokenScoped: Boolean
    get() = this[TokenScopedContext.Key] != null
