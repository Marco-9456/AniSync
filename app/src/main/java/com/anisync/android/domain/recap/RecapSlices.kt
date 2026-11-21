package com.anisync.android.domain.recap

import java.time.ZoneId

/**
 * What a recap of [period] keeps from [records]: every countable activity inside it, plus the few
 * before it that decide where each of its titles stood when it began.
 *
 * [countEvents] only needs those earlier activities to carry on counting, so a stored recap can be
 * counted again later, with other settings, without fetching anything before it.
 */
fun sliceForPeriod(
    period: RecapPeriod,
    zone: ZoneId,
    records: List<ListActivityRecord>,
): List<ListActivityRecord> {
    val window = period.window(zone)
    val inside = records.filter { it.createdAt >= window.from && it.createdAt < window.to && isCountable(it.status) }
    val titles = inside.mapTo(HashSet()) { it.mediaId }
    val counts = HashMap<Int, TitleCount>()
    records.filter { it.createdAt < window.from && it.mediaId in titles }
        .sortedWith(recordOrder)
        .forEach { counts.getOrPut(it.mediaId, ::TitleCount).count(it, total = null) }
    val before = counts.values.flatMap { it.standing }
    return (before + inside).distinctBy { it.id }.sortedWith(recordOrder)
}
