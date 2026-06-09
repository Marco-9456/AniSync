package com.anisync.android.data

import com.anisync.android.domain.AdultMode
import com.anisync.android.domain.AniListListActivityStatus
import com.anisync.android.domain.AniListStaffNameLanguage
import com.anisync.android.domain.AniListTitleLanguage
import com.anisync.android.domain.AniListUserOptions
import com.anisync.android.domain.ScoreFormat
import com.anisync.android.domain.UserActivity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserOptionsMappingTest {

    // ── resolveAdultIsAdult (search floor — fixes the inert toggle) ───────────────────────────────

    @Test
    fun `ANY with adult off hides adult`() {
        assertEquals(false, resolveAdultIsAdult(AdultMode.ANY, showAdultContent = false))
    }

    @Test
    fun `ANY with adult on leaves filter off`() {
        assertNull(resolveAdultIsAdult(AdultMode.ANY, showAdultContent = true))
    }

    @Test
    fun `HIDE always hides regardless of preference`() {
        assertEquals(false, resolveAdultIsAdult(AdultMode.HIDE, showAdultContent = true))
        assertEquals(false, resolveAdultIsAdult(AdultMode.HIDE, showAdultContent = false))
    }

    @Test
    fun `ONLY always shows only adult regardless of preference`() {
        assertEquals(true, resolveAdultIsAdult(AdultMode.ONLY, showAdultContent = false))
        assertEquals(true, resolveAdultIsAdult(AdultMode.ONLY, showAdultContent = true))
    }

    // ── filterAdultActivities (feed — fixes #53) ─────────────────────────────────────────────────

    private fun activity(id: Int, isAdult: Boolean) =
        UserActivity(id = id, timestamp = 0L, mediaIsAdult = isAdult)

    @Test
    fun `feed drops adult list activity when adult content off`() {
        val feed = listOf(activity(1, isAdult = false), activity(2, isAdult = true))
        val filtered = feed.filterAdultActivities(showAdultContent = false)
        assertEquals(listOf(1), filtered.map { it.id })
    }

    @Test
    fun `feed keeps adult list activity when adult content on`() {
        val feed = listOf(activity(1, isAdult = false), activity(2, isAdult = true))
        val filtered = feed.filterAdultActivities(showAdultContent = true)
        assertEquals(listOf(1, 2), filtered.map { it.id })
    }

    // ── Title / staff-name language mapping (6→3 collapse) ───────────────────────────────────────

    @Test
    fun `stylised title languages collapse to their base`() {
        assertEquals(TitleLanguage.ROMAJI, AniListTitleLanguage.ROMAJI.toLocalTitleLanguage())
        assertEquals(TitleLanguage.ROMAJI, AniListTitleLanguage.ROMAJI_STYLISED.toLocalTitleLanguage())
        assertEquals(TitleLanguage.ENGLISH, AniListTitleLanguage.ENGLISH.toLocalTitleLanguage())
        assertEquals(TitleLanguage.ENGLISH, AniListTitleLanguage.ENGLISH_STYLISED.toLocalTitleLanguage())
        assertEquals(TitleLanguage.NATIVE, AniListTitleLanguage.NATIVE.toLocalTitleLanguage())
        assertEquals(TitleLanguage.NATIVE, AniListTitleLanguage.NATIVE_STYLISED.toLocalTitleLanguage())
    }

    @Test
    fun `staff name language maps one to one`() {
        assertEquals(StaffNameLanguage.ROMAJI_WESTERN, AniListStaffNameLanguage.ROMAJI_WESTERN.toLocalStaffNameLanguage())
        assertEquals(StaffNameLanguage.ROMAJI, AniListStaffNameLanguage.ROMAJI.toLocalStaffNameLanguage())
        assertEquals(StaffNameLanguage.NATIVE, AniListStaffNameLanguage.NATIVE.toLocalStaffNameLanguage())
    }

    // ── Persistence round-trip (per-account cache JSON) ──────────────────────────────────────────

    @Test
    fun `options survive a json round trip including enum-keyed map`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val original = AniListUserOptions(
            titleLanguage = AniListTitleLanguage.NATIVE_STYLISED,
            displayAdultContent = true,
            airingNotifications = false,
            profileColor = "purple",
            activityMergeTime = 60,
            staffNameLanguage = AniListStaffNameLanguage.NATIVE,
            restrictMessagesToFollowing = true,
            disabledListActivity = mapOf(
                AniListListActivityStatus.PLANNING to true,
                AniListListActivityStatus.COMPLETED to false,
            ),
            scoreFormat = ScoreFormat.POINT_5,
        )

        val restored = json.decodeFromString<AniListUserOptions>(json.encodeToString(original))

        assertEquals(original, restored)
        assertTrue(restored.disabledListActivity[AniListListActivityStatus.PLANNING] == true)
    }
}
