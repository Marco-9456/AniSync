package com.anisync.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the pure helpers behind local-first sync: dirty tracking, patch building, and the
 *  conflict rule (a field is in conflict when it is dirty *and* changed on the server since baseline). */
class UserOptionsSyncTest {

    private val base = AniListUserOptions(
        titleLanguage = AniListTitleLanguage.ROMAJI,
        displayAdultContent = false,
        scoreFormat = ScoreFormat.POINT_10,
        activityMergeTime = 0,
    )

    @Test
    fun `differingFields finds exactly the changed fields`() {
        val other = base.copy(displayAdultContent = true, scoreFormat = ScoreFormat.POINT_5)
        assertEquals(
            setOf(UserOptionField.DISPLAY_ADULT_CONTENT, UserOptionField.SCORE_FORMAT),
            base.differingFields(other),
        )
    }

    @Test
    fun `applyPatch applies only the set fields`() {
        val patched = base.applyPatch(UserOptionsPatch(displayAdultContent = true))
        assertTrue(patched.displayAdultContent)
        assertEquals(base.scoreFormat, patched.scoreFormat)
        assertEquals(base.titleLanguage, patched.titleLanguage)
    }

    @Test
    fun `affectedFields reflects the patch's non-null members`() {
        val patch = UserOptionsPatch(
            titleLanguage = AniListTitleLanguage.ENGLISH,
            airingNotifications = false,
        )
        assertEquals(
            setOf(UserOptionField.TITLE_LANGUAGE, UserOptionField.AIRING_NOTIFICATIONS),
            patch.affectedFields(),
        )
    }

    @Test
    fun `takeFields takes only the requested fields from the source`() {
        val server = base.copy(displayAdultContent = true, titleLanguage = AniListTitleLanguage.NATIVE)
        val merged = base.takeFields(setOf(UserOptionField.DISPLAY_ADULT_CONTENT), server)
        assertTrue(merged.displayAdultContent) // taken from server
        assertEquals(AniListTitleLanguage.ROMAJI, merged.titleLanguage) // kept from base
    }

    // The repository's conflict detection is exactly this intersection.
    private fun conflicts(
        baseline: AniListUserOptions,
        fresh: AniListUserOptions,
        dirty: Set<UserOptionField>,
    ): Set<UserOptionField> = dirty intersect baseline.differingFields(fresh)

    @Test
    fun `dirty field also changed on server is a conflict`() {
        val fresh = base.copy(displayAdultContent = true)
        val dirty = setOf(UserOptionField.DISPLAY_ADULT_CONTENT)
        assertEquals(setOf(UserOptionField.DISPLAY_ADULT_CONTENT), conflicts(base, fresh, dirty))
    }

    @Test
    fun `dirty field unchanged on server is not a conflict`() {
        val dirty = setOf(UserOptionField.DISPLAY_ADULT_CONTENT)
        assertTrue(conflicts(base, fresh = base, dirty = dirty).isEmpty())
    }

    @Test
    fun `clean field changed on server is not a conflict`() {
        val fresh = base.copy(scoreFormat = ScoreFormat.POINT_5) // server changed a field we didn't touch
        val dirty = setOf(UserOptionField.DISPLAY_ADULT_CONTENT)
        assertTrue(conflicts(base, fresh, dirty).isEmpty())
    }

    @Test
    fun `patchForFields then applyPatch round-trips only selected fields`() {
        val source = base.copy(scoreFormat = ScoreFormat.POINT_5, airingNotifications = false)
        val patch = source.patchForFields(setOf(UserOptionField.SCORE_FORMAT))
        assertEquals(setOf(UserOptionField.SCORE_FORMAT), patch.affectedFields())

        val applied = base.applyPatch(patch)
        assertEquals(ScoreFormat.POINT_5, applied.scoreFormat) // selected → taken
        assertEquals(base.airingNotifications, applied.airingNotifications) // unselected → unchanged
    }
}
