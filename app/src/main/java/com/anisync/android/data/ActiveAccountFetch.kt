package com.anisync.android.data

import com.anisync.android.data.network.findApiError
import com.anisync.android.data.util.ApiError

/**
 * Runs [fetch] for the account that is active when it starts, and hands the answer to [store] only
 * if that account is still the active one once the answer is in.
 *
 * A switch while the request is out leaves an answer that belongs to the previous account, or one
 * that went out under the next account's token. Either way it is dropped and the account active by
 * then is asked instead, so a sync that started before the switch still ends up syncing the
 * account switched to.
 */
internal suspend fun <T> fetchForActiveAccount(
    activeOwner: () -> Int?,
    fetch: suspend (owner: Int) -> T,
    store: suspend (owner: Int, answer: T) -> Unit,
    attempts: Int = 3,
) {
    repeat(attempts) {
        val owner = activeOwner() ?: throw IllegalStateException("Not signed in")
        val answer = try {
            fetch(owner)
        } catch (e: Exception) {
            if (e.findApiError() is ApiError.SessionChanged) return@repeat
            throw e
        }
        if (activeOwner() != owner) return@repeat
        store(owner, answer)
        return
    }
    throw ApiError.SessionChanged()
}
