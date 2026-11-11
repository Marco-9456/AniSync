package com.anisync.android.data

import com.anisync.android.data.util.ApiError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ActiveAccountFetchTest {

    private val previous = 5603468
    private val next = 8009875

    private var active: Int? = previous
    private val asked = mutableListOf<Int>()
    private val stored = mutableListOf<Pair<Int, String>>()

    private suspend fun sync(fetch: suspend (owner: Int) -> String) = fetchForActiveAccount(
        activeOwner = { active },
        fetch = { owner -> asked += owner; fetch(owner) },
        store = { owner, answer -> stored += owner to answer },
    )

    @Test
    fun `the answer is stored for the account that asked`() = runTest {
        sync { owner -> "list of $owner" }

        assertEquals(listOf(previous), asked)
        assertEquals(listOf(previous to "list of $previous"), stored)
    }

    @Test
    fun `an answer that arrives after a switch is dropped and the next account is asked`() = runTest {
        sync { owner ->
            if (owner == previous) active = next
            "list of $owner"
        }

        assertEquals(listOf(previous, next), asked)
        assertEquals(listOf(next to "list of $next"), stored)
    }

    @Test
    fun `an answer the network layer dropped for a switch is asked again for the next account`() = runTest {
        sync { owner ->
            if (owner == previous) {
                active = next
                throw ApiError.SessionChanged()
            }
            "list of $owner"
        }

        assertEquals(listOf(previous, next), asked)
        assertEquals(listOf(next to "list of $next"), stored)
    }

    @Test
    fun `nothing is stored while the account keeps changing`() = runTest {
        try {
            sync { owner ->
                active = if (owner == previous) next else previous
                "list of $owner"
            }
            fail("expected the fetch to give up")
        } catch (e: ApiError.SessionChanged) {
            assertTrue(stored.isEmpty())
            assertEquals(3, asked.size)
        }
    }

    @Test
    fun `other failures reach the caller untouched`() = runTest {
        try {
            sync { throw IllegalStateException("Server error") }
            fail("expected the failure to propagate")
        } catch (e: IllegalStateException) {
            assertEquals("Server error", e.message)
            assertEquals(listOf(previous), asked)
            assertTrue(stored.isEmpty())
        }
    }

    @Test
    fun `nothing is fetched while signed out`() = runTest {
        active = null
        try {
            sync { owner -> "list of $owner" }
            fail("expected a signed-out failure")
        } catch (e: IllegalStateException) {
            assertTrue(asked.isEmpty())
        }
    }
}
