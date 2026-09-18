package com.anisync.android.data.network

import com.anisync.android.data.util.InflightTracker
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import okio.Buffer

/**
 * Collapses identical queries that are in flight at the same time into one request.
 *
 * With 30 requests a minute, sending the same query twice is a real cost. `GetViewer` is the worst
 * offender: nine call sites want it, two of them force the network, and on a cold start the user
 * options pull, the notification badge and the library sync can all ask for it in the same second.
 *
 * Only queries are joined. Repeating a mutation is a different question entirely, and a second
 * caller of `SaveMediaListEntry` with identical variables genuinely means it twice.
 *
 * One instance per [com.apollographql.apollo.ApolloClient], never a singleton: the per-account
 * clients in [com.anisync.android.data.account.TokenedApolloClientFactory] would otherwise share a
 * key space and hand one account another account's viewer.
 */
class RequestCoalescer : ApolloInterceptor {

    private val inflight = InflightTracker()

    override fun <D : Operation.Data> intercept(
        request: ApolloRequest<D>,
        chain: ApolloInterceptorChain,
    ): Flow<ApolloResponse<D>> {
        if (request.operation !is Query) return chain.proceed(request)

        return flow {
            val priority = resolveRequestPriority()
            @Suppress("UNCHECKED_CAST")
            val response = inflight.deduplicate(
                key = key(request),
                // The shared request owns its tier rather than inheriting the first caller's, so
                // raising it for a joiner cannot follow that caller back into the rest of its work.
                context = AmbientRequestPriority(priority),
                onJoin = { leader -> leader[AmbientRequestPriority]?.raiseTo(priority) },
            ) {
                NetLog.d(TAG) { "AniSyncNet event=fetch op=${request.operation.name()}" }
                chain.proceed(request).first()
            } as ApolloResponse<D>
            emit(response)
        }
    }

    /**
     * Operation name plus serialised variables.
     *
     * The name alone is not enough: `GetMediaDetails(1)` and `GetMediaDetails(2)` are different
     * requests, and joining them would hand one screen another anime.
     */
    internal fun <D : Operation.Data> key(request: ApolloRequest<D>): String {
        val adapters = request.executionContext[CustomScalarAdapters] ?: CustomScalarAdapters.Empty
        val buffer = Buffer()
        BufferedSinkJsonWriter(buffer).use { writer ->
            writer.beginObject()
            request.operation.serializeVariables(writer, adapters, withDefaultValues = true)
            writer.endObject()
        }
        return "${request.operation.name()}:${buffer.readUtf8()}"
    }

    private companion object {
        const val TAG = "RequestCoalescer"
    }
}
