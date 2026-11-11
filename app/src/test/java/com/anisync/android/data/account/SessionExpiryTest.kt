package com.anisync.android.data.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExpiryTest {

    private val main = account(5603468, "Marco9456")
    private val test = account(8009875, "Marco9455")

    private val marked = mutableListOf<Pair<Int, Boolean>>()
    private val switched = mutableListOf<Int>()

    private fun expire(active: Account?, accounts: List<Account>) = expireSession(
        active = active,
        accounts = accounts,
        markExpired = { id, keepActive -> marked += id to keepActive },
        switchTo = { switched += it },
    )

    @Test
    fun `the only account drops to sign in`() {
        val expiry = expire(test, listOf(test))

        assertEquals(SessionExpiry(expired = "Marco9455", switchedTo = null), expiry)
        assertEquals(listOf(test.id to false), marked)
        assertTrue(switched.isEmpty())
    }

    @Test
    fun `another signed in account takes over`() {
        val expiry = expire(test, listOf(main, test))

        assertEquals(SessionExpiry(expired = "Marco9455", switchedTo = "Marco9456"), expiry)
        assertEquals(listOf(test.id to true), marked)
        assertEquals(listOf(main.id), switched)
    }

    @Test
    fun `an account that has expired too is skipped`() {
        val expiry = expire(test, listOf(main.copy(expiresAt = Account.REFUSED_AT), test))

        assertNull(expiry?.switchedTo)
        assertEquals(listOf(test.id to false), marked)
        assertTrue(switched.isEmpty())
    }

    @Test
    fun `requests failing after the first change nothing`() {
        assertNull(expire(test.copy(expiresAt = Account.REFUSED_AT), listOf(main, test)))
        assertNull(expire(null, listOf(main, test)))
        assertTrue(marked.isEmpty())
        assertTrue(switched.isEmpty())
    }

    @Test
    fun `a token past its own expiry is still handled`() {
        val lapsed = test.copy(expiresAt = 1_000L)

        assertEquals("Marco9456", expire(lapsed, listOf(main, lapsed))?.switchedTo)
        assertEquals(listOf(test.id to true), marked)
    }

    private fun account(id: Int, name: String) = Account(
        id = id,
        name = name,
        avatarUrl = null,
        expiresAt = 0L,
        token = "token-$id",
    )
}
