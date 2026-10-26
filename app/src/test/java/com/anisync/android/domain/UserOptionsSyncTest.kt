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

    @Test
    fun `the 18+ sub-options sync as fields of their own`() {
        val server = base.copy(includeAdultContentInSearch = false, displayAdultActivities = false)
        assertEquals(
            setOf(UserOptionField.INCLUDE_ADULT_IN_SEARCH, UserOptionField.DISPLAY_ADULT_ACTIVITIES),
            base.differingFields(server),
        )

        val patch = server.patchForFields(setOf(UserOptionField.DISPLAY_ADULT_ACTIVITIES))
        assertEquals(setOf(UserOptionField.DISPLAY_ADULT_ACTIVITIES), patch.affectedFields())

        val applied = base.applyPatch(patch)
        assertEquals(false, applied.displayAdultActivities)
        assertEquals(base.includeAdultContentInSearch, applied.includeAdultContentInSearch)
        assertEquals(base.includeAdultContentInQuickSearch, applied.includeAdultContentInQuickSearch)
    }

    private val adultSubOptions = setOf(
        UserOptionField.INCLUDE_ADULT_IN_SEARCH,
        UserOptionField.INCLUDE_ADULT_IN_QUICK_SEARCH,
        UserOptionField.DISPLAY_ADULT_ACTIVITIES,
    )

    // What AniList answers with while adult content is off.
    private val adultOff = base.copy(
        displayAdultContent = false,
        includeAdultContentInSearch = false,
        includeAdultContentInQuickSearch = false,
        displayAdultActivities = false,
    )

    @Test
    fun `turning adult content on turns the 18+ sub-options on, as AniList does`() {
        val synced = LocalOptionsState(serverSnapshot = adultOff, local = adultOff)
        val edited = synced.edited(UserOptionsPatch(displayAdultContent = true))
        assertTrue(edited.local.displayAdultContent)
        assertTrue(edited.local.includeAdultContentInSearch)
        assertTrue(edited.local.includeAdultContentInQuickSearch)
        assertTrue(edited.local.displayAdultActivities)
        assertEquals(adultSubOptions + UserOptionField.DISPLAY_ADULT_CONTENT, edited.dirty)

        val withSearchOff = synced.edited(
            UserOptionsPatch(displayAdultContent = true, includeAdultContentInSearch = false),
        )
        assertEquals(false, withSearchOff.local.includeAdultContentInSearch)
    }

    @Test
    fun `other edits leave the 18+ sub-options alone`() {
        val synced = LocalOptionsState(serverSnapshot = adultOff, local = adultOff)
        val edited = synced.edited(UserOptionsPatch(scoreFormat = ScoreFormat.POINT_5))
        assertEquals(setOf(UserOptionField.SCORE_FORMAT), edited.dirty)
        assertEquals(false, edited.local.includeAdultContentInSearch)
    }

    @Test
    fun `the 18+ sub-options go out after adult content is turned on, never with it`() {
        val fields = adultSubOptions + UserOptionField.DISPLAY_ADULT_CONTENT + UserOptionField.SCORE_FORMAT
        assertEquals(
            listOf(setOf(UserOptionField.DISPLAY_ADULT_CONTENT, UserOptionField.SCORE_FORMAT), adultSubOptions),
            base.copy(displayAdultContent = true).pushBatches(fields),
        )
        assertEquals(listOf(fields), base.copy(displayAdultContent = false).pushBatches(fields))
        assertEquals(listOf(adultSubOptions), base.copy(displayAdultContent = true).pushBatches(adultSubOptions))
    }

    @Test
    fun `AniList resetting the sub-options in its answer is not a conflict`() {
        // Search was switched off again after turning adult content on, and is still to be pushed.
        val sent = adultOff.copy(
            displayAdultContent = true,
            includeAdultContentInQuickSearch = true,
            displayAdultActivities = true,
        )
        val state = LocalOptionsState(
            serverSnapshot = adultOff,
            local = sent,
            dirty = adultSubOptions + UserOptionField.DISPLAY_ADULT_CONTENT,
        )
        val answer = adultOff.copy(
            displayAdultContent = true,
            includeAdultContentInSearch = true,
            includeAdultContentInQuickSearch = true,
            displayAdultActivities = true,
        )
        val acked = state.acknowledged(setOf(UserOptionField.DISPLAY_ADULT_CONTENT), sent, answer)
        assertEquals(null, acked.conflict)
        assertEquals(setOf(UserOptionField.INCLUDE_ADULT_IN_SEARCH), acked.dirty)
        assertEquals(false, acked.local.includeAdultContentInSearch)
        assertEquals(answer, acked.serverSnapshot)
    }

    @Test
    fun `a field edited again while its push was out stays dirty`() {
        val sent = base.copy(scoreFormat = ScoreFormat.POINT_5)
        val now = LocalOptionsState(
            serverSnapshot = base,
            local = base.copy(scoreFormat = ScoreFormat.POINT_3),
            dirty = setOf(UserOptionField.SCORE_FORMAT),
        )
        val acked = now.acknowledged(setOf(UserOptionField.SCORE_FORMAT), sent, answer = sent)
        assertEquals(setOf(UserOptionField.SCORE_FORMAT), acked.dirty)
        assertEquals(ScoreFormat.POINT_3, acked.local.scoreFormat)
        assertEquals(null, acked.conflict)
    }

    @Test
    fun `a pending edit the server also changed still becomes a conflict`() {
        val state = LocalOptionsState(
            serverSnapshot = base,
            local = base.copy(scoreFormat = ScoreFormat.POINT_100),
            dirty = setOf(UserOptionField.SCORE_FORMAT),
        )
        val merged = state.reconciledWith(base.copy(scoreFormat = ScoreFormat.POINT_5))
        assertEquals(setOf(UserOptionField.SCORE_FORMAT), merged.conflict?.fields)
        assertEquals(ScoreFormat.POINT_100, merged.local.scoreFormat)
    }
}
