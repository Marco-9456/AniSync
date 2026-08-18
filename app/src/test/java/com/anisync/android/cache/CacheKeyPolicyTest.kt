package com.anisync.android.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the declared cache IDs, which are generated from `extra.graphqls` and are otherwise easy to
 * lose without noticing: an empty map compiles and runs, it just silently returns the cache to
 * storing one copy of every anime per query path.
 */
class CacheKeyPolicyTest {

    @Test
    fun typePoliciesAreDeclared() {
        assertTrue(
            "no type policies generated, so records would fall back to path keys",
            Cache.typePolicies.isNotEmpty()
        )
        assertTrue("Media" in Cache.typePolicies)
        assertTrue("User" in Cache.typePolicies)
    }

    @Test
    fun everyPolicyKeysOnId() {
        val odd = Cache.typePolicies.filterValues { it.keyFields != listOf("id") }
        assertEquals("every AniList type is keyed by its id alone", emptyMap<String, Any>(), odd)
    }

    /**
     * `MediaTag` carries a global `id`, but the schema documents `rank` as "the relevance ranking
     * of the tag out of the 100 **for this media**" and `isMediaSpoiler` as "if the tag is a
     * spoiler for **this media**". MediaDetails selects both. Keying the tag by `id` would let the
     * last anime opened rewrite the tag ranks and spoiler flags shown for every other one.
     */
    @Test
    fun mediaTagIsNotNormalizedBecauseItsFieldsBelongToItsParent() {
        assertFalse(
            "MediaTag.rank and isMediaSpoiler are per-media, so it must stay path-keyed",
            "MediaTag" in Cache.typePolicies
        )
    }
}
