package com.anisync.android.data.network

import com.anisync.android.GetActivityLikesQuery
import com.anisync.android.GetMediaStatsQuery
import com.anisync.android.GetViewerQuery
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/**
 * The key is the whole safety property here. Two requests share a response only when they are the
 * same request, so a key that ignored variables would hand one screen another anime's data.
 */
class RequestCoalescerTest {

    /** `android.util.Log` is a throwing stub on the JVM. Nothing here is asserting on log output. */
    @Before
    fun quietNetworkLogs() {
        NetLog.enabled = false
    }


    private val coalescer = RequestCoalescer()

    @Test
    fun `the same query with the same variables shares a key`() {
        val a = ApolloRequest.Builder(GetActivityLikesQuery(activityId = 7)).build()
        val b = ApolloRequest.Builder(GetActivityLikesQuery(activityId = 7)).build()

        assertEquals(coalescer.key(a), coalescer.key(b))
    }

    @Test
    fun `the same query with different variables does not`() {
        val a = ApolloRequest.Builder(GetActivityLikesQuery(activityId = 7)).build()
        val b = ApolloRequest.Builder(GetActivityLikesQuery(activityId = 8)).build()

        assertNotEquals(coalescer.key(a), coalescer.key(b))
    }

    @Test
    fun `different queries do not share a key`() {
        val a = ApolloRequest.Builder(GetViewerQuery()).build()
        val b = ApolloRequest.Builder(GetMediaStatsQuery(id = Optional.present(1))).build()

        assertNotEquals(coalescer.key(a), coalescer.key(b))
    }

    @Test
    fun `an absent optional is distinguished from a present one`() {
        val absent = ApolloRequest.Builder(GetMediaStatsQuery(id = Optional.absent())).build()
        val present = ApolloRequest.Builder(GetMediaStatsQuery(id = Optional.present(1))).build()

        assertNotEquals(coalescer.key(absent), coalescer.key(present))
    }

    @Test
    fun `a variable-free query keys on its name`() {
        val request = ApolloRequest.Builder(GetViewerQuery()).build()
        assertEquals("GetViewer:{}", coalescer.key(request))
    }
}
