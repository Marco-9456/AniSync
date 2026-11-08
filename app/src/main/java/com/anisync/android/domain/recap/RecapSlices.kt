package com.anisync.android.domain.recap

import java.time.ZoneId

/**
 * What a recap of [period] keeps from [records]: every countable activity inside it, plus the last
 * one before it that moved each of its titles along.
 *
 * [countEvents] only needs that one earlier activity per title to resolve a completion, so a stored
 * recap can be counted again later, with other settings, without fetching anything before it.
 */
fun sliceForPeriod(
    period: RecapPeriod,
    zone: ZoneId,
    records: List<ListActivityRecord>,
): List<ListActivityRecord> {
    val window = period.window(zone)
    val inside = records.filter { it.createdAt >= window.from && it.createdAt < window.to && isCountable(it.status) }
    val titles = inside.mapTo(HashSet()) { it.mediaId }
    val before = records
        .filter { it.createdAt < window.from && it.mediaId in titles && it.movesProgress() }
        .groupBy { it.mediaId }
        .map { (_, earlier) -> earlier.maxWith(recordOrder) }
    return (before + inside).distinctBy { it.id }.sortedWith(recordOrder)
}

/** The order [countEvents] reads activities in. */
private val recordOrder = compareBy<ListActivityRecord>({ it.createdAt }, { it.id })
