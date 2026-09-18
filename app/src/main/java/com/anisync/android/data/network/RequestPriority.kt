package com.anisync.android.data.network

import com.apollographql.apollo.api.ExecutionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * How much of the AniList request budget a call is entitled to.
 *
 * AniList allows 30 requests a minute in its current degraded mode, which is not enough for the
 * app's background work and whatever the user is looking at to both spend freely. Priority is what
 * [RateLimitGate] uses to decide who gets the last of the window.
 *
 * The default when a call carries no priority is [Interactive], so an untagged call site behaves
 * exactly as it did before. Only background and speculative work needs tagging.
 *
 * Declared highest first: [RequestPriority.isAtLeast] reads the order, so a new tier has to be
 * inserted where it belongs rather than appended.
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
    Background;

    /** True when this tier may spend at least as freely as [other]. */
    fun isAtLeast(other: RequestPriority): Boolean = ordinal <= other.ordinal
}

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

/**
 * Ambient priority for everything a coroutine does, including calls made several layers down.
 *
 * Repositories build their own Apollo calls, so tagging a worker's requests per call would mean
 * threading a networking concern through about thirty repository signatures. `flowOn` preserves
 * context elements from the collector, so an element set here reaches the interceptor instead.
 *
 * The tier is mutable because [RequestCoalescer] hands one request to several callers. The request
 * goes out under whoever asked first, and a caller who joins it needing it more raises the tier for
 * the work in flight rather than being held to a background poll's reserve. Only the element the
 * coalescer creates for a shared request is ever raised, never one a caller brought with it, or a
 * worker that joined one interactive request would spend the rest of its run at that tier.
 */
class AmbientRequestPriority private constructor(
    private val tier: AtomicReference<RequestPriority>,
) : AbstractCoroutineContextElement(Key) {

    constructor(priority: RequestPriority) : this(AtomicReference(priority))

    val priority: RequestPriority get() = tier.get()

    /** Raises the tier to [other] if [other] outranks what is set. Never lowers it. */
    fun raiseTo(other: RequestPriority) {
        while (true) {
            val current = tier.get()
            if (current.isAtLeast(other)) return
            if (tier.compareAndSet(current, other)) return
        }
    }

    companion object Key : CoroutineContext.Key<AmbientRequestPriority>
}

/** Runs [block] with every AniList request it makes tagged [priority]. */
suspend fun <T> withRequestPriority(
    priority: RequestPriority,
    block: suspend CoroutineScope.() -> T,
): T = withContext(AmbientRequestPriority(priority), block)

/** The priority in force for the calling coroutine. */
suspend fun resolveRequestPriority(): RequestPriority =
    currentCoroutineContext()[AmbientRequestPriority]?.priority ?: RequestPriority.Interactive
